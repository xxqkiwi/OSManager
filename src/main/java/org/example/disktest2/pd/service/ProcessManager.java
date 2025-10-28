package org.example.disktest2.pd.service;

import org.example.disktest2.memory.MemoryManager;
import org.example.disktest2.pd.domain.enums.DeviceType;
import org.example.disktest2.pd.domain.enums.InterruptType;
import org.example.disktest2.pd.domain.enums.ProcessState;
import org.example.disktest2.pd.domain.model.Device;
import org.example.disktest2.pd.domain.model.PCB;
import org.example.disktest2.pd.infrastructure.SystemClock;
import org.example.disktest2.pd.infrastructure.ThreadSafeQueue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.stream.Collectors;

/**
 * 进程管理核心服务（实现create/destroy/block/awake原语）
 */
public class ProcessManager {
    // 队列管理（空白/就绪/阻塞队列）
    private final ThreadSafeQueue<PCB> freePcbQueue = new ThreadSafeQueue<>();
    private final ThreadSafeQueue<PCB> readyQueue = new ThreadSafeQueue<>();
    private ThreadSafeQueue<PCB> blockedQueue = new ThreadSafeQueue<>();
    private PCB runningPcb; // 当前运行进程
    private final PCB idleProcess; // 闲逛进程

    // 依赖服务
    private final SystemClock systemClock;
    private DeviceManager deviceManager;

    // 时间片配置（时间片=4）
    private final int timeSlice = 4;
    private int remainingTime = timeSlice;
    private InterruptType currentInterrupt = InterruptType.NO_INTERRUPT;

    public ProcessManager(SystemClock systemClock, DeviceManager deviceManager) {
        this.systemClock = systemClock;
        this.deviceManager = deviceManager;

        // 初始化空白PCB队列（最多10个）
        for (int i = 1; i <= 10; i++) {
            freePcbQueue.add(new PCB(i, "free.pcb", new ArrayList<>(), 0));
        }

        // 初始化闲逛进程
        this.idleProcess = new PCB(0, "idle.exe", List.of("idle"), 16);
        this.idleProcess.setState(ProcessState.READY);
        readyQueue.add(idleProcess);
    }

    public void setDeviceManager(DeviceManager deviceManager) {
        this.deviceManager = deviceManager;
    }

    /**
     * 进程创建原语
     * 1. 从空白队列取PCB
     * 2. 检查内存（≤512字节）
     * 3. 初始化状态为就绪，加入就绪队列
     */
    public boolean createProcess(String exeFile, List<String> instructions, int memorySize) {
        PCB freePcb = freePcbQueue.poll();
        if (freePcb == null) {
            System.out.println("创建失败：空白PCB队列已满（最多10个）");
            return false;
        }

        // 尝试分配内存
        int memoryStart = MemoryManager.get().load(String.valueOf(freePcb.getPid()), memorySize);
        if (memoryStart == -1) { // 分配失败
            freePcbQueue.add(freePcb);
            System.out.println("创建失败：内存不足");
            return false;
        }

        // 初始化PCB（使用实际分配的内存地址）
        PCB newPcb = new PCB(freePcb.getPid(), exeFile, instructions, memorySize);
        newPcb.setMemoryStart(memoryStart); // 设置真实内存起始地址

        // 加入就绪队列
        if (readyQueue.getSnapshot().stream().anyMatch(p -> p.getPid() == 0)) {
            readyQueue.poll(); // 移除闲逛进程
        }
        readyQueue.add(newPcb);
        System.out.printf("进程创建成功：PID=%d，内存地址=%d%n", newPcb.getPid(), memoryStart);
        return true;
    }

    /**
     * 进程撤销原语
     * 1. 查找进程（运行/就绪/阻塞队列）
     * 2. 回收内存，PCB归还空白队列
     * 3. 触发结束中断（若为运行态）
     */
    public boolean destroyProcess(int pid) {
        // 查找进程
        PCB target = findProcess(pid);
        if (target == null) {
            System.out.printf("撤销失败：PID=%d不存在%n", pid);
            return false;
        }
        // 释放该进程占用的所有设备
        releaseProcessDevices(pid); // 调用设备释放方法

        // 回收内存
        boolean isUnloaded = MemoryManager.get().unload(String.valueOf(pid));
        if (isUnloaded) {
            System.out.printf("释放内存：%d-%d%n",
                    target.getMemoryStart(),
                    target.getMemoryStart() + target.getMemorySize() - 1);
        }

        // 归还PCB到空白队列
        target.setState(ProcessState.TERMINATED);
        freePcbQueue.add(target);

        // 处理运行态进程中断
        if (runningPcb != null && runningPcb.getPid() == pid) {
            runningPcb = null;
            currentInterrupt = InterruptType.END_INTERRUPT;
        }

        if (readyQueue.isEmpty()) {
            readyQueue.add(idleProcess);
        }
        System.out.printf("进程撤销成功：PID=%d%n", pid);
        return true;
    }

    // 释放进程占用的设备并唤醒等待队列
    private void releaseProcessDevices(int pid) {
        for (DeviceType type : DeviceType.values()) {
            List<Device> deviceList = deviceManager.getDevices().get(type);
            for (Device device : deviceList) {
                if (device.isInUse() && device.getUsingPid() == pid) {
                    device.release(); // 释放设备
                    // 唤醒等待队列中的下一个进程
                    Queue<Integer> waitQueue = deviceManager.getDeviceWaitQueues().get(type);
                    if (!waitQueue.isEmpty()) {
                        int nextPid = waitQueue.poll();
                        awakeProcess(nextPid); // 需实现唤醒逻辑（如移至就绪队列）
                    }
                }
            }
        }
    }


    /**
     * 进程阻塞原语
     * 1. 仅阻塞当前运行进程
     * 2. 保存上下文，状态改为阻塞，加入阻塞队列
     * 3. 触发I/O中断
     */
    public boolean blockProcess(int pid, int deviceType) {
        if (runningPcb == null || runningPcb.getPid() != pid ||
                runningPcb.getState() != ProcessState.RUNNING) {
            System.out.println("阻塞失败：仅运行态进程可阻塞");
            return false;
        }

        // 保存上下文（寄存器状态）
        runningPcb.setBlockReason(deviceType);
        runningPcb.setState(ProcessState.BLOCKED);
        blockedQueue.add(runningPcb);
        runningPcb = null;

        // 触发I/O中断
        currentInterrupt = InterruptType.IO_INTERRUPT;
        System.out.printf("进程阻塞成功：PID=%d，设备=%s%n",
                pid, DeviceType.values()[deviceType].getName());
        return true;
    }

    /**
     * 进程唤醒原语
     * 1. 从阻塞队列查找对应设备的进程
     * 2. 状态改为就绪，加入就绪队列
     */
    public boolean awakeProcess(int pid) {
        // 从阻塞队列查找目标进程
        List<PCB> blockedSnapshot = (List<PCB>) blockedQueue.getSnapshot();
        Optional<PCB> toAwake = blockedSnapshot.stream()
                .filter(p -> p.getPid() == pid)
                .findFirst();

        if (toAwake.isEmpty()) {
            System.out.println("唤醒失败：PID=" + pid + " 未处于阻塞状态");
            return false;
        }

        // 从阻塞队列移除并加入就绪队列
        PCB pcb = toAwake.get();
        removeFromBlockedQueue(pid); // 实际移除队列元素
        pcb.setBlockReason(-1); // 清除阻塞原因
        pcb.setState(ProcessState.READY);
        readyQueue.add(pcb);

        // 若当前无运行进程，立即调度
        if (runningPcb == null || runningPcb.getPid() == idleProcess.getPid()) {
            schedule();
        }

        System.out.printf("进程唤醒成功：PID=%d%n", pid);
        return true;
    }

    /**
     * 时间片轮转调度
     * 1. 保存当前进程上下文，加入就绪队列
     * 2. 从就绪队列取队首进程，设置为运行态
     * 3. 重置时间片
     */
    public void schedule() {
        // 保存当前运行进程
        if (runningPcb != null && runningPcb.getState() == ProcessState.RUNNING) {
            runningPcb.setState(ProcessState.READY);
            readyQueue.add(runningPcb);
        }

        // 选择下一个进程
        runningPcb = readyQueue.isEmpty() ? idleProcess : readyQueue.poll();
        runningPcb.setState(ProcessState.RUNNING);
        remainingTime = timeSlice; // 重置时间片

        System.out.printf("调度进程：PID=%d，剩余时间片=%d%n",
                runningPcb.getPid(), remainingTime);
    }

    /**
     * CPU执行模拟（指令执行+中断处理）
     */
    public String executeInstruction() {
        // 处理中断
        String interruptMsg = handleInterrupt();
        if (!interruptMsg.isEmpty()) {
            return interruptMsg;
        }

        // 无运行进程则调度
        if (runningPcb == null) {
            schedule();
        }

        PCB current = runningPcb;
        List<String> instructions = current.getInstructions();
        int pc = current.getPc();

        // 指令执行完毕，触发结束中断
        if (pc >= instructions.size()) {
            currentInterrupt = InterruptType.END_INTERRUPT;
            return handleInterrupt();
        }

        // 执行当前指令
        String instruction = instructions.get(pc);
        current.setIr(instruction);
        StringBuilder log = new StringBuilder();
        log.append(String.format("时钟=%d，PID=%d执行：%s%n",
                systemClock.getCurrentTime(), current.getPid(), instruction));

        // 解析指令（支持x=、x++、x--、!设备、end）
        if (instruction.startsWith("x=")) {
            int value = Integer.parseInt(instruction.split("=")[1].trim());
            current.setAx(value);
            log.append("  AX=").append(value).append("\n");
        } else if (instruction.equals("x++")) {
            current.setAx(Math.min(current.getAx() + 1, 255)); // 限制x≤255
            log.append("  AX=").append(current.getAx()).append("\n");
        } else if (instruction.equals("x--")) {
            current.setAx(Math.max(current.getAx() - 1, 0)); // 限制x≥0
            log.append("  AX=").append(current.getAx()).append("\n");
        } else if (instruction.startsWith("!")) {
            // I/O指令：!设备 时间（例：!A 3）
            String[] parts = instruction.substring(1).split(" ");
            DeviceType type = DeviceType.valueOf("DEVICE_" + parts[0]);
            int useTime = Integer.parseInt(parts[1]);
            boolean allocated = deviceManager.allocateDevice(type, current.getPid(), useTime);
            if (!allocated) {
                // 设备忙，阻塞进程
                blockProcess(current.getPid(), type.ordinal());
            }
            log.append("  设备").append(type.getName()).append(allocated ? "分配成功" : "阻塞\n");
        } else if (instruction.equals("end")) {
            currentInterrupt = InterruptType.END_INTERRUPT;
            log.append("  触发程序结束中断\n");
        }

        // 更新程序计数器和时间片
        current.setPc(pc + 1);
        remainingTime--;
        if (remainingTime <= 0) {
            currentInterrupt = InterruptType.TIME_SLICE_INTERRUPT;
        }

        // 系统时钟递增，设备时间递减
        systemClock.tick();
        deviceManager.decrementAllDeviceTime();

        return log.toString();
    }

    // 中断处理（按类型处理）
    private String handleInterrupt() {
        if (currentInterrupt == InterruptType.NO_INTERRUPT) {
            return "";
        }

        StringBuilder log = new StringBuilder();
        log.append("处理中断：").append(currentInterrupt).append("\n");

        switch (currentInterrupt) {
            case END_INTERRUPT:
                // 程序结束→撤销进程+调度
                destroyProcess(runningPcb.getPid());
                schedule();
                break;
            case TIME_SLICE_INTERRUPT:
                // 时间片结束→直接调度
                schedule();
                break;
            case IO_INTERRUPT:
                // I/O中断→释放设备+唤醒进程
                for (DeviceType type : DeviceType.values()) {
                    int awakePid = deviceManager.releaseFinishedDevices(type);
                    if (awakePid != -1) {
                        awakeProcess(type.ordinal());
                        log.append("  唤醒PID=").append(awakePid).append("\n");
                    }
                }
                if (runningPcb == null) {
                    schedule();
                }
                break;
        }

        currentInterrupt = InterruptType.NO_INTERRUPT;
        return log.toString();
    }

    //获取所有进程列表
    public List<PCB> getAllProcesses() {
        List<PCB> allProcesses = new ArrayList<>();

        // 添加运行进程
        if (runningPcb != null) {
            allProcesses.add(runningPcb);
        }

        // 添加就绪队列进程
        allProcesses.addAll(readyQueue.getSnapshot());

        // 添加阻塞队列进程
        allProcesses.addAll(blockedQueue.getSnapshot());

        // 添加已终止进程（空白队列中）
        allProcesses.addAll(freePcbQueue.getSnapshot().stream()
                .filter(p -> p.getState() == ProcessState.TERMINATED)
                .collect(Collectors.toList()));

        return allProcesses;
    }

    // 辅助方法：查找进程
    private PCB findProcess(int pid) {
        // 检查运行进程
        if (runningPcb != null && runningPcb.getPid() == pid) {
            return runningPcb;
        }
        // 检查就绪队列
        Optional<PCB> ready = readyQueue.getSnapshot().stream()
                .filter(p -> p.getPid() == pid).findFirst();
        // 检查阻塞队列
        return ready.orElseGet(() -> blockedQueue.getSnapshot().stream()
                .filter(p -> p.getPid() == pid).findFirst().orElse(null));
    }

    // 辅助方法：从阻塞队列移除进程
    private void removeFromBlockedQueue(int pid) {
        ThreadSafeQueue<PCB> newQueue = new ThreadSafeQueue<>();
        while (!blockedQueue.isEmpty()) {
            PCB pcb = blockedQueue.poll();
            if (pcb.getPid() != pid) {
                newQueue.add(pcb);
            }
        }
        blockedQueue = newQueue;
    }

    // 暴露状态供UI使用
    public int getSystemTime() {
        return systemClock.getCurrentTime();
    }

    public PCB getRunningPcb() {
        return runningPcb;
    }

    public ThreadSafeQueue<PCB> getReadyQueue() {
        return readyQueue;
    }

    public ThreadSafeQueue<PCB> getBlockedQueue() {
        return blockedQueue;
    }

    public DeviceManager getDeviceManager() {
        return deviceManager;
    }

    public int getRemainingTime() {
        return remainingTime;
    }
}
