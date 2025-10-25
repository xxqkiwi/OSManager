package org.example.disktest2.Device;

/**
 * 设备实体类
 */
public class Device {
    private String type;  // 设备类型：A, B, C
    private int id;       // 设备ID
    private boolean isAvailable; // 是否可用
    private String usingProcessId; // 正在使用的进程ID
    private int remainingTime;    // 剩余使用时间

    public Device(String type, int id) {
        this.type = type;
        this.id = id;
        this.isAvailable = true;
        this.usingProcessId = null;
        this.remainingTime = 0;
    }

    // Getters
    public String getType() { return type; }
    public int getId() { return id; }
    public boolean isAvailable() { return isAvailable; }
    public String getUsingProcessId() { return usingProcessId; }
    public int getRemainingTime() { return remainingTime; }

    // Setters
    public void setAvailable(boolean available) { isAvailable = available; }
    public void setUsingProcessId(String processId) { usingProcessId = processId; }
    public void setRemainingTime(int time) { remainingTime = time; }

    @Override
    public String toString() {
        return type + id + " - " +
                (isAvailable ? "空闲" : "被进程" + usingProcessId + "使用(" + remainingTime + "单位)");
    }
}