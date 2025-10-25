package org.example.disktest2.pd.domain.enums;

/**
 * 中断类型定义
 * 程序结束中断：进程执行end指令
 * 时间片结束中断：时间片用完（6个单位）
 * I/O中断：设备操作触发（申请/释放）
 */
public enum InterruptType {
    NO_INTERRUPT("无中断"),
    END_INTERRUPT("程序结束中断"),
    TIME_SLICE_INTERRUPT("时间片结束中断"),
    IO_INTERRUPT("I/O中断");

    private final String description;

    InterruptType(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return description;
    }
}