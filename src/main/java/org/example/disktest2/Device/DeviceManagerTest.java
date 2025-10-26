package org.example.disktest2.Device;

/**
 * 设备管理器测试类
 */
public class DeviceManagerTest {

    public static void main(String[] args) {
        // 测试1: 基本功能测试
        testBasicFunctionality();

        // 测试2: 等待队列测试
        testWaitingQueue();

        // 测试3: 时间更新测试
        testTimeUpdate();

        // 测试4: 边界条件测试
        testEdgeCases();
    }

    /**
     * 测试1: 基本功能测试
     */
    public static void testBasicFunctionality() {
        System.out.println("=== 测试1: 基本功能测试 ===");
        DeviceManager manager = new DeviceManager();

        // 测试设备申请
        boolean result1 = manager.requestDevice("P1", "A", 5);
        boolean result2 = manager.requestDevice("P2", "A", 3);
        boolean result3 = manager.requestDevice("P3", "B", 2);

        System.out.println("P1申请A设备: " + (result1 ? "成功" : "失败"));
        System.out.println("P2申请A设备: " + (result2 ? "成功" : "失败"));
        System.out.println("P3申请B设备: " + (result3 ? "成功" : "失败"));

        manager.printDeviceAllocationTable();

        // 测试设备释放
        manager.releaseDevice("P1", "A");
        manager.printDeviceAllocationTable();
    }

    /**
     * 测试2: 等待队列测试
     */
    public static void testWaitingQueue() {
        System.out.println("=== 测试2: 等待队列测试 ===");
        DeviceManager manager = new DeviceManager();

        // 申请超过设备数量的进程
        manager.requestDevice("P1", "A", 5);
        manager.requestDevice("P2", "A", 3);
        manager.requestDevice("P3", "A", 2); // 这个应该进入等待队列
        manager.requestDevice("P4", "A", 4); // 这个也应该进入等待队列

        manager.printDeviceAllocationTable();

        // 释放一个设备，应该自动分配给等待队列
        manager.releaseDevice("P1", "A");
        manager.printDeviceAllocationTable();
    }

    /**
     * 测试3: 时间更新测试
     */
    public static void testTimeUpdate() {
        System.out.println("=== 测试3: 时间更新测试 ===");
        DeviceManager manager = new DeviceManager();

        manager.requestDevice("P1", "A", 3);
        manager.requestDevice("P2", "B", 2);

        System.out.println("初始状态:");
        manager.printDeviceAllocationTable();

        // 模拟时间流逝
        for (int i = 1; i <= 4; i++) {
            System.out.println("--- 时间单位 " + i + " ---");
            manager.updateDeviceTime();
            manager.printDeviceAllocationTable();
        }
    }

    /**
     * 测试4: 边界条件测试
     */
    public static void testEdgeCases() {
        System.out.println("=== 测试4: 边界条件测试 ===");
        DeviceManager manager = new DeviceManager();

        // 测试无效设备类型
        manager.requestDevice("P1", "D", 5); // 应该失败

        // 测试释放不存在的设备
        manager.releaseDevice("P99", "A"); // 应该失败

        // 测试零时间申请
        manager.requestDevice("P1", "A", 0); // 应该成功但立即释放

        manager.printDeviceAllocationTable();
    }
}