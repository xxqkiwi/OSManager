package org.example.disktest2.memory;


import javafx.scene.paint.Color;
import org.example.disktest2.pd.domain.model.PCB;  // 替换为pd包的PCB
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class MemoryManager {
    // 系统区大小常量，128字节
    public static final int SYS_SIZE = 128;   // 系统区字节
    // 用户区大小常量，512字节
    private static final int USER_SIZE = 512;  // 用户区字节
    // 最大进程控制块(PCB)数量，10个
    private static final int MAX_PCB = 10;     // 最多 10 个 PCB


    private boolean memoryChanged = false; // 内存变化标识

    /* ---------- 内存模型 ---------- */
    // 内存块链表头节点，包含系统区和用户区
    private final MemoryBlock head = new MemoryBlock(true, SYS_SIZE, USER_SIZE);
    // PCB数组，用于存储进程控制块（使用pd包的PCB）
    private final PCB[] PCBArea = new PCB[MAX_PCB];
    // 已使用的PCB数量
    private int pcbUsed = 0;

    /* ---------- 单例 ---------- */
    private static final MemoryManager INST = new MemoryManager();
    private MemoryManager() {}
    public static MemoryManager get() { return INST; }

    /* ---------- 首次适配分配 ---------- */
    public int load(String pid, int needBytes) {
        if (needBytes <= 0 || needBytes > USER_SIZE) return -1;
        if (pcbUsed == MAX_PCB) return -1;

        // 首次适配算法查找合适的空闲块
        MemoryBlock pre = null, p = head;
        while (p != null && !(p.free && p.size >= needBytes)) { pre = p; p = p.next; }
        if (p == null) return -1;          // 无合适空闲块

        // 分配内存块
        int left = p.size - needBytes;
        if (left == 0) p.free = false;     // 整块占用
        else {                               // 分割
            MemoryBlock split = new MemoryBlock(true, p.start + needBytes, left);
            split.next = p.next; p.next = split; p.size = needBytes; p.free = false;
        }

        // 适配pd包PCB的构造方法
        int pidInt;
        try {
            pidInt = Integer.parseInt(pid);  // 假设原String pid实际是数字
        } catch (NumberFormatException e) {
            return -1;  // 转换失败则分配失败
        }
        // 创建pd包的PCB实例（指令列表暂时传空，实际应从进程管理传入）
        PCBArea[pcbUsed++] = new PCB(pidInt, pid, new ArrayList<>(), needBytes);
        // 设置内存起始地址（pd包PCB的memoryStart需要单独设置）
        PCBArea[pcbUsed - 1].setMemoryStart(p.start);

        // 内存发生变化，设置标识
        memoryChanged = true;
        return pcbUsed - 1;
    }

    /* ---------- 回收 + 合并 ---------- */
    public boolean unload(String pid) {
        // 查找进程对应的PCB
        int pidInt;
        try {
            pidInt = Integer.parseInt(pid);
        } catch (NumberFormatException e) {
            return false;
        }

        int idx = -1;
        for (int i = 0; i < pcbUsed; i++) {
            if (PCBArea[i].getPid() == pidInt) {  // 使用pd包的getPid()方法
                idx = i;
                break;
            }
        }
        if (idx == -1) return false;

        // 获取目标进程的PCB和对应的内存块
        PCB target = PCBArea[idx];
        MemoryBlock p = head, pre = null;
        while (p != null && p.start != target.getMemoryStart()) {  // 使用getMemoryStart()
            pre = p;
            p = p.next;
        }
        if (p == null) return false;

        // 释放内存块并尝试合并
        p.free = true;
        if (p.next != null && p.next.free) {
            p.size += p.next.size;
            p.next = p.next.next;
        }
        if (pre != null && pre.free) {
            pre.size += p.size;
            pre.next = p.next;
        }
        // 移动PCB数组
        System.arraycopy(PCBArea, idx + 1, PCBArea, idx, pcbUsed - idx - 1);
        pcbUsed--;

        // 内存发生变化，设置标识
        memoryChanged = true;
        return true;
    }

    //  提供外部判断内存是否变化的方法（供Controller调用）
    public boolean isMemoryChanged() {
        return memoryChanged;
    }

    // 提供重置标识的方法（刷新后调用，避免重复刷新）
    public void resetMemoryChanged() {
        memoryChanged = false;
    }

    /* ---------- 给 UI 的快照 ---------- */
    public record Seg(int start, int size, String label, Color color) {}

    public List<Seg> snapshot() {
        List<Seg> lst = new ArrayList<>();
        lst.add(new Seg(0, SYS_SIZE, "系统区", Color.web("#3F51B5")));

        MemoryBlock p = head;
        while (p != null) {
            String lab = p.free ? "空闲" : findPid(p.start);
            Color  c   = p.free ? Color.LIGHTGREEN : Color.ORANGERED;
            lst.add(new Seg(p.start, p.size, lab, c));
            p = p.next;
        }
        return lst;
    }

    private String findPid(int st) {
        for (int i = 0; i < pcbUsed; i++) {
            if (PCBArea[i].getMemoryStart() == st) {  // 使用getMemoryStart()
                return String.valueOf(PCBArea[i].getPid());  // int pid转String
            }
        }
        return "?";
    }

    public List<PCB> getPcbs() {
        return Arrays.asList(PCBArea).subList(0, pcbUsed);
    }
}