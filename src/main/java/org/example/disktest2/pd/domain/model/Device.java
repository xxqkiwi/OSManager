package org.example.disktest2.pd.domain.model;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 设备控制块（DCB）
 * 包含：使用状态、占用进程、剩余时间、等待队列
 */
public class Device {
    private boolean inUse; // 是否被占用
    private int usingPid; // 占用进程PID
    private int remainingTime; // 剩余使用时间
    private final Queue<Integer> waitQueue; // 等待队列（进程PID）
    private final Lock lock; // 线程安全锁

    public Device() {
        this.inUse = false;
        this.usingPid = -1;
        this.remainingTime = 0;
        this.waitQueue = new LinkedList<>();
        this.lock = new ReentrantLock(); // 保证多线程安全
    }

    // 设备使用时间递减
    public void decrementTime() {
        lock.lock();
        try {
            if (inUse && remainingTime > 0) {
                remainingTime--;
            }
        } finally {
            lock.unlock();
        }
    }

    // 占用设备
    public void occupy(int pid, int useTime) {
        lock.lock();
        try {
            this.inUse = true;
            this.usingPid = pid;
            this.remainingTime = useTime;
        } finally {
            lock.unlock();
        }
    }

    // 释放设备
    public void release() {
        lock.lock();
        try {
            this.inUse = false;
            this.usingPid = -1;
            this.remainingTime = 0;
        } finally {
            lock.unlock();
        }
    }

    // Getter
    public boolean isInUse() { return inUse; }
    public int getUsingPid() { return usingPid; }
    public int getRemainingTime() { return remainingTime; }
    public Queue<Integer> getWaitQueue() { return waitQueue; }
    public Lock getLock() { return lock; }
}