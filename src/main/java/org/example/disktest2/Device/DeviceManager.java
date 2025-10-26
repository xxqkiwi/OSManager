package org.example.disktest2.Device;

import java.util.*;

/**
 * 设备管理类 - 根据指导书要求完善
 */
public class DeviceManager {
    // 所有设备列表
    private List<Device> allDevices;

    // 设备分配表：设备类型 -> 设备列表
    private Map<String, List<Device>> deviceAllocationTable;

    // 等待队列：设备类型 -> 等待的进程列表（进程ID, 请求时间）
    private Map<String, Queue<DeviceRequest>> waitingQueues;

    public DeviceManager() {
        initializeDevices();
    }

    /**
     * 设备请求类
     */
    private static class DeviceRequest {
        String processId;
        int requestTime;

        DeviceRequest(String processId, int requestTime) {
            this.processId = processId;
            this.requestTime = requestTime;
        }
    }

    /**
     * 初始化所有设备 - 根据指导书要求
     */
    private void initializeDevices() {
        allDevices = new ArrayList<>();
        deviceAllocationTable = new HashMap<>();
        waitingQueues = new HashMap<>();

        // 初始化A设备（2个）
        List<Device> devicesA = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            Device device = new Device("A", i);
            allDevices.add(device);
            devicesA.add(device);
        }
        deviceAllocationTable.put("A", devicesA);
        waitingQueues.put("A", new LinkedList<>());

        // 初始化B设备（3个）
        List<Device> devicesB = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Device device = new Device("B", i);
            allDevices.add(device);
            devicesB.add(device);
        }
        deviceAllocationTable.put("B", devicesB);
        waitingQueues.put("B", new LinkedList<>());

        // 初始化C设备（3个）
        List<Device> devicesC = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Device device = new Device("C", i);
            allDevices.add(device);
            devicesC.add(device);
        }
        deviceAllocationTable.put("C", devicesC);
        waitingQueues.put("C", new LinkedList<>());

        System.out.println("设备管理器初始化完成：A设备2个，B设备3个，C设备3个");
    }

    /**
     * 申请设备 - 根据指导书要求：!? ? 指令引起
     * @param processId 进程ID
     * @param deviceType 设备类型（A、B、C）
     * @param requestTime 请求使用时间
     * @return 分配结果：成功分配返回true，加入等待队列返回false
     */
    public boolean requestDevice(String processId, String deviceType, int requestTime) {
        // 验证设备类型
        if (!deviceAllocationTable.containsKey(deviceType)) {
            System.out.println("✗ 错误：无效的设备类型 " + deviceType);
            return false;
        }

        // 特殊处理：请求时间为0，立即返回成功但不实际占用设备
        if (requestTime <= 0) {
            System.out.println("✓ 进程 " + processId + " 申请设备 " + deviceType + " 0单位时间，立即完成");
            return true;
        }

        // 查找该类型的空闲设备
        for (Device device : deviceAllocationTable.get(deviceType)) {
            if (device.isAvailable()) {
                // 分配设备
                allocateDevice(device, processId, requestTime);
                System.out.println("✓ 进程 " + processId + " 成功分配设备 " + deviceType +
                        device.getId() + "，使用时间: " + requestTime);
                return true;
            }
        }

        // 没有空闲设备，加入等待队列
        DeviceRequest request = new DeviceRequest(processId, requestTime);
        waitingQueues.get(deviceType).add(request);

        System.out.println("⏳ 进程 " + processId + " 等待设备 " + deviceType +
                "，前面有 " + (waitingQueues.get(deviceType).size() - 1) + " 个进程在等待");
        return false;
    }

    /**
     * 分配设备给进程
     */
    private void allocateDevice(Device device, String processId, int requestTime) {
        device.setAvailable(false);
        device.setUsingProcessId(processId);
        device.setRemainingTime(requestTime);
    }

    /**
     * 释放设备 - 根据指导书要求：设备使用完后立即释放
     * @param processId 进程ID
     * @param deviceType 设备类型
     */
    public void releaseDevice(String processId, String deviceType) {
        boolean found = false;
        for (Device device : deviceAllocationTable.get(deviceType)) {
            if (!device.isAvailable() && processId.equals(device.getUsingProcessId())) {
                // 释放设备
                device.setAvailable(true);
                device.setUsingProcessId(null);
                device.setRemainingTime(0);
                System.out.println("✓ 进程 " + processId + " 释放设备 " + deviceType + device.getId());
                found = true;

                // 检查等待队列并分配
                assignToWaitingProcess(deviceType);
            }
        }

        if (!found) {
            System.out.println("✗ 未找到进程 " + processId + " 使用的设备 " + deviceType);
        }
    }

    /**
     * 为等待队列中的进程分配设备
     */
    private void assignToWaitingProcess(String deviceType) {
        Queue<DeviceRequest> queue = waitingQueues.get(deviceType);

        if (!queue.isEmpty()) {
            // 查找该类型的空闲设备
            for (Device device : deviceAllocationTable.get(deviceType)) {
                if (device.isAvailable()) {
                    // 分配设备给等待的进程
                    DeviceRequest request = queue.poll();
                    allocateDevice(device, request.processId, request.requestTime);

                    System.out.println("✓ 为等待进程 " + request.processId + " 分配设备 " +
                            deviceType + device.getId());

                    // 唤醒进程（需要与进程管理模块集成）
                    // processManager.wakeUpProcess(request.processId);
                    break;
                }
            }
        }
    }

    /**
     * 更新设备时间 - 每个时间单位调用一次
     * 根据指导书要求：设备使用倒计时至0后释放设备并唤醒进程
     */
    public void updateDeviceTime() {
        for (Device device : allDevices) {
            if (!device.isAvailable()) {
                int remainingTime = device.getRemainingTime();
                if (remainingTime > 0) {
                    device.setRemainingTime(remainingTime - 1);

                    // 使用时间结束，自动释放
                    if (device.getRemainingTime() == 0) {
                        String processId = device.getUsingProcessId();
                        String deviceType = device.getType();

                        device.setAvailable(true);
                        device.setUsingProcessId(null);

                        System.out.println("⏰ 设备 " + deviceType + device.getId() +
                                " 使用时间结束，自动释放，唤醒进程 " + processId);

                        // 分配设备给等待的进程
                        assignToWaitingProcess(deviceType);

                        // 唤醒原进程（需要与进程管理模块集成）
                        // processManager.wakeUpProcess(processId);
                    }
                }
            }
        }
    }

    /**
     * 获取设备状态信息 - 用于界面显示
     * 根据指导书要求显示：每个设备是否被使用，哪个进程在使用，哪些进程在等待
     */
    public Map<String, Object> getDeviceStatus() {
        Map<String, Object> status = new HashMap<>();

        for (String deviceType : new String[]{"A", "B", "C"}) {
            Map<String, Object> typeStatus = new HashMap<>();

            // 设备使用状态
            List<Map<String, String>> deviceStatusList = new ArrayList<>();
            for (Device device : deviceAllocationTable.get(deviceType)) {
                Map<String, String> deviceStatus = new HashMap<>();
                deviceStatus.put("deviceId", deviceType + device.getId());
                deviceStatus.put("status", device.isAvailable() ? "空闲" : "忙碌");
                deviceStatus.put("usingProcess", device.getUsingProcessId() != null ?
                        device.getUsingProcessId() : "无");
                deviceStatus.put("remainingTime", String.valueOf(device.getRemainingTime()));
                deviceStatusList.add(deviceStatus);
            }

            // 等待队列信息
            List<String> waitingList = new ArrayList<>();
            for (DeviceRequest request : waitingQueues.get(deviceType)) {
                waitingList.add("进程" + request.processId + "(" + request.requestTime + "单位)");
            }

            typeStatus.put("devices", deviceStatusList);
            typeStatus.put("waitingQueue", waitingList);
            typeStatus.put("waitingCount", waitingList.size());
            status.put(deviceType, typeStatus);
        }

        return status;
    }

    /**
     * 获取设备分配表 - 用于调试和显示
     */
    public void printDeviceAllocationTable() {
        System.out.println("\n=== 设备分配表 ===");
        for (String type : new String[]{"A", "B", "C"}) {
            System.out.println(type + "设备:");
            for (Device device : deviceAllocationTable.get(type)) {
                System.out.println("  " + device.toString());
            }
        }

        System.out.println("\n=== 设备等待队列 ===");
        for (String type : new String[]{"A", "B", "C"}) {
            Queue<DeviceRequest> queue = waitingQueues.get(type);
            if (!queue.isEmpty()) {
                System.out.print(type + "设备等待队列: ");
                for (DeviceRequest request : queue) {
                    System.out.print("进程" + request.processId + "(" + request.requestTime + ") ");
                }
                System.out.println();
            } else {
                System.out.println(type + "设备等待队列: 空");
            }
        }
        System.out.println("==================\n");
    }

    /**
     * 获取指定类型设备的统计信息
     */
    public Map<String, Integer> getDeviceStatistics(String deviceType) {
        Map<String, Integer> stats = new HashMap<>();
        if (!deviceAllocationTable.containsKey(deviceType)) {
            return stats;
        }

        List<Device> devices = deviceAllocationTable.get(deviceType);
        int total = devices.size();
        int available = 0;
        int used = 0;

        for (Device device : devices) {
            if (device.isAvailable()) {
                available++;
            } else {
                used++;
            }
        }

        stats.put("total", total);
        stats.put("available", available);
        stats.put("used", used);
        stats.put("waiting", waitingQueues.get(deviceType).size());

        return stats;
    }
}