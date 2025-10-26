package org.example.disktest2.memory.memoryfx;

public class PCB {
    public String pid;   // 进程名（如 bb.e）
    public int memorySize;     // 请求字节数
    public int memoryStart;    // 分区起始地址
/**
 * PCB（Process Control Block，进程控制块）类的构造方法
 * 用于初始化进程控制块的基本属性
 * @param pid 进程标识符，用于唯一标识一个进程
 * @param size 进程所需内存大小，以字节为单位
 * @param memoryStart 进程在内存中的起始地址，用于内存分配管理
 */
    public PCB(String pid, int size, int memoryStart) {
        this.pid = pid; this.memorySize = size; this.memoryStart = memoryStart; // 使用this关键字为当前实例的属性赋值
    }
}