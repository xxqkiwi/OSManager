package org.example.disktest2.memory.memoryfx;

import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * MemoryManager 类是一个内存管理器，负责管理内存的分配与回收
 * 使用首次适配算法进行内存分配，并支持内存块的合并
 */
public final class MemoryManager {
    // 系统区大小常量，128字节
    public static final int SYS_SIZE = 128;   // 系统区字节
    // 用户区大小常量，512字节
    private static final int USER_SIZE = 512;  // 用户区字节
    // 最大进程控制块(PCB)数量，10个
    private static final int MAX_PCB = 10;     // 最多 10 个 PCB

    /* ---------- 内存模型 ---------- */
    // 内存块链表头节点，包含系统区和用户区
    private final MemoryBlock head = new MemoryBlock(true, SYS_SIZE, USER_SIZE);
    // PCB数组，用于存储进程控制块
    private final PCB[] PCBArea = new PCB[MAX_PCB];
    // 已使用的PCB数量
    private int pcbUsed = 0;

    /* ---------- 单例 ---------- */
    // MemoryManager的单例实例
    private static final MemoryManager INST = new MemoryManager();
    // 私有构造函数，确保单例模式
    private MemoryManager() {}
    // 获取MemoryManager单例实例的方法
    public static MemoryManager get() { return INST; }

    /* ---------- 首次适配分配 ---------- */
    /**
     * 加载进程到内存，分配所需空间
     * @param pid 进程ID
     * @param needBytes 需要的字节数
     * @return 成功返回PCB索引，失败返回-1
     */
    public int load(String pid, int needBytes) {
        // 检查请求字节数是否合法
        if (needBytes <= 0 || needBytes > USER_SIZE) return -1;
        // 检查是否还有可用的PCB槽位
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
        // 创建新的PCB并添加到PCB区域
        PCBArea[pcbUsed++] = new PCB(pid, needBytes, p.start);
        return pcbUsed - 1;
    }

    /* ---------- 回收 + 合并 ---------- */
    /**
     * 从内存中卸载进程，回收内存空间
     * @param pid 要卸载的进程ID
     * @return 成功返回true，失败返回false
     */
    public boolean unload(String pid) {
        // 查找进程对应的PCB
        int idx = -1;
        for (int i = 0; i < pcbUsed; i++) if (pid.equals(PCBArea[i].pid)) { idx = i; break; }
        if (idx == -1) return false;

        // 获取目标进程的PCB和对应的内存块
        PCB target = PCBArea[idx];
        MemoryBlock p = head, pre = null;
        while (p != null && p.start != target.memoryStart) { pre = p; p = p.next; }
        if (p == null) return false;

        // 释放内存块并尝试与相邻空闲块合并
        p.free = true;
        if (p.next != null && p.next.free) { p.size += p.next.size; p.next = p.next.next; } // 向后合并
        if (pre != null && pre.free) { pre.size += p.size; pre.next = p.next; }              // 向前合并
        // 移动PCB数组并减少已使用计数
        System.arraycopy(PCBArea, idx + 1, PCBArea, idx, pcbUsed - idx - 1); pcbUsed--;
        return true;
    }

    /* ---------- 给 UI 的快照 ---------- */
    /**
     * 内存快照记录，用于UI显示
     * @param start 起始地址
     * @param size 大小
     * @param label 标签
     * @param color 颜色
     */
    public record Seg(int start, int size, String label, Color color) {}
    /**
     * 获取当前内存状态的快照
     * @return 包含内存区域信息的列表
     */
    public List<Seg> snapshot() {
        List<Seg> lst = new ArrayList<>();
        // 添加系统区
        lst.add(new Seg(0, SYS_SIZE, "系统区", Color.web("#3F51B5")));
        // 遍历内存块链表，添加每个内存区域的信息
        MemoryBlock p = head;
        while (p != null) {
            String lab = p.free ? "空闲" : findPid(p.start);
            Color  c   = p.free ? Color.LIGHTGREEN : Color.ORANGERED;
            lst.add(new Seg(p.start, p.size, lab, c));
            p = p.next;
        }
        return lst;
    }
    /**
     * 根据起始地址查找进程ID
     * @param st 起始地址
     * @return 对应的进程ID
     */
    private String findPid(int st) {
        for (int i = 0; i < pcbUsed; i++) if (PCBArea[i].memoryStart == st) return PCBArea[i].pid;
        return "?";
    }

    /**
     * 获取所有进程的PCB列表
     * @return PCB列表
     */
    public List<PCB> getPcbs() {
        return Arrays.asList(PCBArea).subList(0, pcbUsed);
    }
}