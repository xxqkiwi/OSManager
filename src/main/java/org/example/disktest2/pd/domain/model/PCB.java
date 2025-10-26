package org.example.disktest2.pd.domain.model;

import org.example.disktest2.pd.domain.enums.ProcessState;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 进程控制块（PCB）结构
 * 包含：进程ID、状态、寄存器现场、阻塞原因、内存信息、指令列表
 */
public class PCB {
    private final int pid;                 // 进程编号
    private final String exeFile;          // 执行文件名（新增字段对应的存储）
    private ProcessState state;            // 进程状态
    private final List<String> instructions; // 指令列表
    private int pc;                        // 程序计数器（当前指令索引）
    private int ax;                        // 数据寄存器
    private String ir;                     // 指令寄存器
    private int blockReason;               // 阻塞原因（设备编号，-1表示未阻塞）
    private int memoryStart;         // 内存起始地址
    private int memorySize;          // 内存大小
    private int remainingTimeSlice;        // 剩余时间片
    private static final int DEFAULT_TIME_SLICE = 6; // 默认时间片

    // 构造方法：初始化流程
    public PCB(int pid, String exeFile, List<String> instructions, int memorySize) {
        this.pid = pid;
        this.exeFile = exeFile;//初始化执行文件名
        this.instructions = new CopyOnWriteArrayList<>(instructions);
        this.memorySize = memorySize;
        this.memoryStart = memorySize * pid; // 简化内存分配（实际需调用内存管理）

        // 进程创建时初始状态为就绪
        this.state = ProcessState.READY;
        this.ax = 0; // 初始值0
        this.pc = 0; // 从第0条指令开始
        this.blockReason = -1; // 初始无阻塞
        this.memoryStart = memoryStart;
        this.memorySize = memorySize;
        this.remainingTimeSlice = DEFAULT_TIME_SLICE;
    }

    // 用户区内存最大512字节，检查是否溢出
    public boolean isMemoryOverflow() {
        return memoryStart + memorySize > 512;
    }

    // Getter/Setter：严格暴露必要字段，封装内部状态
    public int getPid() { return pid; }
    public ProcessState getState() { return state; }
    public void setState(ProcessState state) { this.state = state; }
    public int getAx() { return ax; }
    public void setAx(int ax) { this.ax = ax; }
    public int getPc() { return pc; }
    public void setPc(int pc) { this.pc = pc; }
    public String getIr() { return ir; }
    public void setIr(String ir) { this.ir = ir; }
    public int getBlockReason() { return blockReason; }
    public void setBlockReason(int blockReason) { this.blockReason = blockReason; }
    public List<String> getInstructions() { return instructions; }
    public int getMemoryStart() { return memoryStart; }
    public int getMemorySize() { return memorySize; }

    public String getExeFile() {
        return exeFile;
    }
}
