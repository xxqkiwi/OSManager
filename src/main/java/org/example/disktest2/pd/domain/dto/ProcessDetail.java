package org.example.disktest2.pd.domain.dto;

import org.example.disktest2.pd.domain.enums.DeviceType;
import org.example.disktest2.pd.domain.enums.ProcessState;
import org.example.disktest2.pd.domain.model.PCB;

public class ProcessDetail {
    private final int pid;
    private final ProcessState state;
    private final String exeFile;
    private final String deviceUsage;
    private final String pcbInfo;
    private final String executionResult;
    private final double completionProgress;
    private final String currentInstruction;

    public ProcessDetail(PCB pcb) {
        this.pid = pcb.getPid();
        this.state = pcb.getState();
        this.exeFile = pcb.getExeFile();

        // 设备使用情况
        int blockReason = pcb.getBlockReason();
        this.deviceUsage = blockReason == -1 ? "无" :
                DeviceType.values()[blockReason].getName() + "(阻塞)";

        // PCB信息
        this.pcbInfo = String.format("内存: %d-%d, PC: %d, AX: %d",
                pcb.getMemoryStart(),
                pcb.getMemoryStart() + pcb.getMemorySize() - 1,
                pcb.getPc(),
                pcb.getAx());

        // 执行结果
        this.executionResult = pcb.getIr() != null ? pcb.getIr() + " → AX=" + pcb.getAx() : "未执行";

        // 完成进度
        int total = pcb.getInstructions().size();
        this.completionProgress = total == 0 ? 0 : (double) pcb.getPc() / total;

        // 当前指令
        this.currentInstruction = pcb.getPc() < total ?
                pcb.getInstructions().get(pcb.getPc()) : "已完成";
    }

    // Getters
    public int getPid() { return pid; }
    public ProcessState getState() { return state; }
    public String getExeFile() { return exeFile; }
    public String getDeviceUsage() { return deviceUsage; }
    public String getPcbInfo() { return pcbInfo; }
    public String getExecutionResult() { return executionResult; }
    public double getCompletionProgress() { return completionProgress; }
    public String getCurrentInstruction() { return currentInstruction; }
}