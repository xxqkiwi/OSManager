package org.example.disktest2.pd.domain.enums;

public enum ProcessFilter {
    CURRENT("当前进程"),
    NEW("新建进程"),
    READY("就绪进程"),
    BLOCKED("阻塞进程"),
    TERMINATED("销毁进程"),
    ALL("显示所有");

    ProcessFilter(String 当前进程) {
    }
}
