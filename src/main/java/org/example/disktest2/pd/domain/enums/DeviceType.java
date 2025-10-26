package org.example.disktest2.pd.domain.enums;

/**
 * 设备类型定义
 * 设备A：2台，设备B：3台，设备C：3台（独占设备）
 */
public enum DeviceType {
    DEVICE_A("设备A", 2),
    DEVICE_B("设备B", 3),
    DEVICE_C("设备C", 3);

    private final String name;
    private final int count; // 设备数量

    DeviceType(String name, int count) {
        this.name = name;
        this.count = count;
    }

    public String getName() {
        return name;
    }

    public int getCount() {
        return count;
    }
}