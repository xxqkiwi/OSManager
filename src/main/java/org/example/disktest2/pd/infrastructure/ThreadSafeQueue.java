package org.example.disktest2.pd.infrastructure;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 线程安全队列（用于PCB队列管理）
 */
public class ThreadSafeQueue<T> {
    private final Queue<T> queue = new LinkedList<>();
    private final Lock lock = new ReentrantLock();

    public void add(T element) {
        lock.lock();
        try {
            queue.add(element);
        } finally {
            lock.unlock();
        }
    }

    public T poll() {
        lock.lock();
        try {
            return queue.poll();
        } finally {
            lock.unlock();
        }
    }

    public boolean isEmpty() {
        lock.lock();
        try {
            return queue.isEmpty();
        } finally {
            lock.unlock();
        }
    }

    public Queue<T> getSnapshot() {
        lock.lock();
        try {
            return new LinkedList<>(queue);
        } finally {
            lock.unlock();
        }
    }
}