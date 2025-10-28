package org.example.disktest2.memory;

/**
 * 内存块类，用于表示内存中的一个块
 * 该类包含内存块的基本信息和链表指针
 */
public class MemoryBlock {
    boolean free;        // true 空闲，false 已分配
    int start;           // 字节起始
    int size;            // 字节大小
    MemoryBlock next;    // 链表
    public MemoryBlock(boolean free, int start, int size) {
        this.free = free; this.start = start; this.size = size;
    }
}