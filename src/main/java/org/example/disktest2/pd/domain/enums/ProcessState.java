package org.example.disktest2.pd.domain.enums;

/**
 * 进程状态定义
 * 就绪（READY）：等待CPU调度
 * 运行（RUNNING）：占用CPU执行
 * 阻塞（BLOCKED）：等待资源（如I/O）
 * 终止（TERMINATED）：执行完毕
 */
public enum ProcessState {
    READY("就绪"),
    RUNNING("运行"),
    BLOCKED("阻塞"),
    TERMINATED("终止");

    private final String description;

    ProcessState(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return description;
    }
}