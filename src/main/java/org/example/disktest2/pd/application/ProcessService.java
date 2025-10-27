package org.example.disktest2.pd.application;

import org.example.disktest2.pd.domain.dto.ProcessDetail;
import org.example.disktest2.pd.domain.model.PCB;
import org.example.disktest2.pd.infrastructure.SystemClock;
import org.example.disktest2.pd.service.DeviceManager;
import org.example.disktest2.pd.service.ProcessManager;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 应用服务：对外提供简洁API，协调核心服务
 */
public class ProcessService {
    private final ProcessManager processManager;
    private boolean allowCreateProcess = true; // 控制是否允许新建进程
    private int speedMultiplier = 1; // 速度倍率

    // 构造注入依赖
    public ProcessService() {
        SystemClock clock = new SystemClock();
        ProcessManager processManager = new ProcessManager(clock, null); // 先创建空依赖
        DeviceManager deviceManager = new DeviceManager(processManager); // 注入processManager
        processManager.setDeviceManager(deviceManager); // 反向设置设备管理器
        this.processManager = processManager;
    }

    // 获取所有进程详情
    public List<ProcessDetail> getAllProcessDetails() {
        return processManager.getAllProcesses().stream()
                .map(ProcessDetail::new)
                .collect(Collectors.toList());
    }

    // 控制进程创建开关
    public void setAllowCreateProcess(boolean allow) {
        this.allowCreateProcess = allow;
    }

    // 设置速度倍率
    public void setSpeedMultiplier(int multiplier) {
        this.speedMultiplier = multiplier;
    }

    public int getSpeedMultiplier() {
        return speedMultiplier;
    }

    // 创建进程
    public boolean createProcess(String exeFile, List<String> instructions, int memorySize) {
        if (!allowCreateProcess) return false;
        return processManager.createProcess(exeFile, instructions, memorySize);
    }

    // 执行一条指令
    public String executeNextInstruction() {
        return processManager.executeInstruction();
    }

    // 获取系统状态（供UI展示）
    public SystemState getSystemState() {
        return new SystemState(
                processManager.getSystemTime(),
                processManager.getRunningPcb(),
                (List<PCB>) processManager.getReadyQueue().getSnapshot(),
                (List<PCB>) processManager.getBlockedQueue().getSnapshot(),
                processManager.getDeviceManager().getDevices(),
                processManager.getRemainingTime()
        );
    }

    // 系统状态DTO（数据传输对象）
    public static class SystemState {
        public final int systemTime;
        public final PCB runningPcb;
        public final List<PCB> readyQueue;
        public final List<PCB> blockedQueue;
        public final Object devices; // 简化为Object，实际用泛型
        public final int remainingTime;

        public SystemState(int systemTime, PCB runningPcb, List<PCB> readyQueue,
                           List<PCB> blockedQueue, Object devices, int remainingTime) {
            this.systemTime = systemTime;
            this.runningPcb = runningPcb;
            this.readyQueue = readyQueue;
            this.blockedQueue = blockedQueue;
            this.devices = devices;
            this.remainingTime = remainingTime;
        }
    }
}