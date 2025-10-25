package org.example.disktest2.pd.service;

import org.example.disktest2.pd.domain.enums.DeviceType;
import org.example.disktest2.pd.domain.model.Device;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 设备管理服务（设备分配、释放、等待队列管理）
 */
public class DeviceManager {
    private final Map<DeviceType, List<Device>> devices; // 设备池

    public DeviceManager() {
        devices = new HashMap<>();
        // 初始化设备数量（A:2, B:3, C:3）
        for (DeviceType type : DeviceType.values()) {
            List<Device> deviceList = IntStream.range(0, type.getCount())
                    .mapToObj(i -> new Device())
                    .collect(Collectors.toList());
            devices.put(type, deviceList);
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
        Device firstDevice = deviceList.get(0); // 等待队列挂在首个设备
        firstDevice.getLock().lock();
        try {
            firstDevice.getWaitQueue().add(pid);
        } finally {
            firstDevice.getLock().unlock();
        }
        return false; // 分配失败（进程需阻塞）
    }

    // 释放设备并唤醒等待进程
    public int releaseFinishedDevices(DeviceType type) {
        List<Device> deviceList = devices.get(type);
        for (Device device : deviceList) {
            if (device.isInUse() && device.getRemainingTime() == 0) {
                int releasedPid = device.getUsingPid();
                device.release(); // 释放设备
                // 唤醒等待队列首个进程
                if (!device.getWaitQueue().isEmpty()) {
                    return device.getWaitQueue().poll();
                }
            }
        }
        return -1; // 无等待进程
    }

    // 设备时间递减（每单位时间调用）
    public void decrementAllDeviceTime() {
        devices.values().forEach(deviceList ->
                deviceList.forEach(Device::decrementTime)
        );
    }

    public Map<DeviceType, List<Device>> getDevices() {
        return devices;
    }
}