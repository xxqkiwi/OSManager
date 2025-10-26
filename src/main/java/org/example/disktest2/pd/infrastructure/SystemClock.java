package org.example.disktest2.pd.infrastructure;

/**
 * 系统时钟服务（全局时钟管理）
 */
public class SystemClock {
    private int currentTime;

    public SystemClock() {
        this.currentTime = 0;
    }

    // 时钟递增（每执行一个单位时间+1）
    public void tick() {
        currentTime
                ++;
    }

    public int getCurrentTime() {
        return currentTime;
    }
}

