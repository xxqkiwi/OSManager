package org.example.disktest2.pd.service;

import org.example.disktest2.pd.domain.enums.DeviceType;
import org.example.disktest2.pd.domain.model.Device;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 设备管理服务（设备分配、释放、等待队列管理）
 */
public class DeviceManager {
    private final Map<DeviceType, List<Device>> devices; // 设备池
    private final Map<DeviceType, Queue<Integer>> deviceWaitQueues; // 设备类型 -> 等待队列
    private final ProcessManager processManager;

    public DeviceManager(ProcessManager processManager) {
        this.processManager = processManager;
        devices = new HashMap<>();
        deviceWaitQueues = new HashMap<>(); // 初始化全局等待队列
        // 初始化设备数量（A:2, B:3, C:3）
        for (DeviceType type : DeviceType.values()) {
            List<Device> deviceList = IntStream.range(0, type.getCount())
                    .mapToObj(i -> new Device())
                    .collect(Collectors.toList());
            devices.put(type, deviceList);
            deviceWaitQueues.put(type, new LinkedList<>()); // 为每种设备类型创建等待队列
        }
    }

    // 申请设备：有空闲则分配，否则加入等待队列
    public boolean allocateDevice(DeviceType type, int pid, int useTime) {
        List<Device> deviceList = devices.get(type);
        // 查找空闲设备
        for (Device device : deviceList) {
            if (!device.isInUse()) {
                device.occupy(pid, useTime);
                return true; // 分配成功
            }
        }
        // 无空闲设备，加入等待队列
        deviceWaitQueues.get(type).add(pid);
        return false; // 分配失败（进程需阻塞）
    }

    // 释放设备并唤醒等待进程
    public int releaseFinishedDevices(DeviceType type) {
        List<Device> deviceList = devices.get(type);
        for (Device device : deviceList) {
            if (device.isInUse() && device.getRemainingTime() == 0) {
                // 获取原占用设备的进程PID（需要唤醒）
                int releasedPid = device.getUsingPid();

                // 释放设备
                device.release();

                // 唤醒原占用进程
                processManager.awakeProcess(releasedPid);
                System.out.println("设备释放，唤醒原进程：PID=" + releasedPid);

                // 唤醒等待队列首个进程
                if (!device.getWaitQueue().isEmpty()) {
                    int waitingPid = device.getWaitQueue().poll();
                    processManager.awakeProcess(waitingPid); // 补充：唤醒等待进程
                    System.out.println("唤醒等待队列进程：PID=" + waitingPid);
                    return waitingPid;
                }
            }
        }
        return -1;
    }

    // 设备时间递减（每单位时间调用）
    public void decrementAllDeviceTime() {
        devices.values().forEach(deviceList ->
                deviceList.forEach(Device::decrementTime)
        );
    }

    // 对外提供设备等待队列的getter方法
    public Map<DeviceType, Queue<Integer>> getDeviceWaitQueues() {
        return deviceWaitQueues; // 返回全局等待队列的引用
    }

    public Map<DeviceType, List<Device>> getDevices() {
        return devices;
    }
}