package org.example.disktest2.memory;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.List;
import java.util.Map;

/**
 * HelloController 类是一个FXML控制器，用于处理内存管理系统的用户界面交互。
 * 它包含了对内存可视化的控制、进程的载入与卸载等功能。
 */
public class MemoryController {

    @FXML private ProgressBar usageBar;    // 内存使用进度条
    @FXML private Label usageText;    // 内存使用率文本标签
    @FXML private Canvas canvas;           // 内存可视化画布
    @FXML private VBox memoryContainer; // 需要在FXML中添加这个容器

    private final MemoryManager mm = MemoryManager.get();
    private Label sizeInfoLabel;
    private Label colorDescLabel; // 用于显示颜色说明的标签
    private HBox legendBox;
    private Timeline refreshTimeline;

    @FXML
    private void initialize() {
        // 创建大小信息标签，用于显示画布尺寸相关信息
        sizeInfoLabel = new Label();
        sizeInfoLabel.getStyleClass().add("size-info");  // 为标签添加CSS样式类

        // 创建图例，用于解释可视化图表中的符号或颜色含义
        createLegend();

        // 将信息标签和图例添加到容器（修正容器获取逻辑）
        VBox infoContainer = new VBox(10);
        infoContainer.getChildren().addAll(sizeInfoLabel, legendBox);

        canvas.setWidth(800);  // 设置固定宽度
        canvas.setHeight(100); // 设置固定高度

        // 获取 canvas 的直接父容器
        Pane canvasParent = (Pane) canvas.getParent();  // 使用通用的 Pane 类型

        // 获取 canvasParent 的父容器（假设是 VBox）
        if (canvasParent.getParent() instanceof VBox parentVBox) {
            // 找到 canvas 在父容器中的索引位置
            int canvasIndex = parentVBox.getChildren().indexOf(canvasParent);
            // 在 canvas 下方插入信息区域
            parentVBox.getChildren().add(canvasIndex + 1, infoContainer);
        } else {
            // 兼容其他布局类型
            canvasParent.getChildren().add(infoContainer);
        }

        // 让 Canvas 尺寸跟随父容器变化
        //canvas.widthProperty().bind(canvasParent.widthProperty().subtract(20));
        //canvas.heightProperty().bind(canvasParent.heightProperty().subtract(20));

        // 尺寸变化即重绘
        canvas.widthProperty().addListener((obs, old, neo) -> draw());
        canvas.heightProperty().addListener((obs, old, neo) -> draw());

        draw(); // 首次绘制

        // 初始化定时刷新任务
        refreshTimeline = new Timeline(new KeyFrame(Duration.millis(500), e -> {
            if (mm.isMemoryChanged()) { // 现在可以正常调用了
                draw(); // 刷新界面
                mm.resetMemoryChanged(); // 重置标识
            }
        }));
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();

        // 2. 延迟获取窗口并绑定关闭事件
        Platform.runLater(() -> {
            try {
                // 获取当前舞台
                Stage stage = (Stage) canvas.getScene().getWindow();
                // 绑定关闭事件，停止刷新任务
                stage.setOnCloseRequest(event -> {
                    if (refreshTimeline != null) {
                        refreshTimeline.stop();
                    }
                });
            } catch (NullPointerException e) {
                // 容错处理：如果仍获取失败，打印日志但不崩溃
                System.err.println("获取内存管理窗口失败：" + e.getMessage());
            }
        });

    }
    // 添加停止刷新的方法，在界面关闭时调用
    public void stopRefresh() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
        }
    }

    // 创建图例
    private void createLegend() {
        legendBox = new HBox();
        legendBox.getStyleClass().add("legend-box");
        HBox sysLegend = new HBox();
        sysLegend.getStyleClass().add("legend-item");
        sysLegend.getChildren().addAll(new Label("颜色说明：蓝色-系统区，浅绿色-空闲区，红色-进程区"));

        legendBox.getChildren().addAll(sysLegend);
    }

/**
 * 处理内存载入按钮的点击事件
 * 从界面获取进程ID和大小，尝试在内存中分配空间
 */
/*
@FXML
private void onLoad() {
    String pid = nameField.getText().trim();
    int size;
    try {
        size = Integer.parseInt(sizeField.getText().trim());
    } catch (NumberFormatException e) {
        log.appendText("大小必须是整数\n");
        return;
    }

    // 重名检测：使用pd包PCB的getPid()
    for (PCB pcb : mm.getPcbs()) {
        if (String.valueOf(pcb.getPid()).equals(pid)) {  // int pid转String比较
            log.appendText("创建失败：进程 " + pid + " 已存在\n");
            return;
        }
    }

    int idx = mm.load(pid, size);
    if (idx == -1) {
        log.appendText("分配失败：无合适空闲区或 PCB 已满\n");
    } else {
        log.appendText("成功载入 " + pid + "\n");
    }
    draw();
}*/
/*    @FXML  // 表示这是一个由FXML控制器调用的方法
    private void onUnload() {  // 卸载进程的方法
        String pid = nameField.getText().trim();  // 获取输入框中的进程ID并去除前后空格
        boolean ok = mm.unload(pid);  // 调用卸载方法，尝试卸载指定进程
        if (ok) log.appendText("已卸载 " + pid + "\n");  // 如果卸载成功，向日志区域添加成功信息
        else log.appendText("卸载失败：进程不存在\n");  // 如果卸载失败，向日志区域添加失败信息
        draw();  // 调用draw方法更新界面显示
    }
*/
    /* 画内存图 */
    private void draw() {
        // 获取画图的图形上下文
        GraphicsContext g = canvas.getGraphicsContext2D();
        // 清除整个画布
        g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        double totalBytes = 128.0 + 512.0;          // 系统区+用户区总字节
        double scaleX = canvas.getWidth()  / totalBytes;
        double scaleY = canvas.getHeight() / 25.0;  // 高度固定30像素

        g.save();
        g.scale(scaleX, scaleY); // 整体等比缩放

        // 计算总空闲大小
        int freeSize = calculateFreeSize();
        // 更新大小信息
        sizeInfoLabel.setText(String.format("系统区大小: %d bytes  |  空闲区总大小: %d bytes",
                MemoryManager.SYS_SIZE, freeSize));

        for (var seg : mm.snapshot()) {
            g.setFill(seg.color());
            g.fillRect(seg.start(), 0, seg.size(), 30);
            if (!seg.label().equals("系统区") && !seg.label().equals("空闲")) {
                g.setFill(Color.BLACK); // 文字颜色改为黑色
                g.fillText(seg.label(), seg.start() + 1, 20);
            }
        }
        g.restore();
        /* ⑤ 更新内存使用率 */
        double total = 128.0 + 512.0;               // 总字节
        double used  = 128.0 + mm.snapshot().stream()       // 所有已分配段
                .filter(s -> !s.label().equals("系统区") && !s.label().equals("空闲"))
                .mapToDouble(s -> s.size())
                .sum();
        double percent = used / total;
        usageBar.setProgress(percent);
        usageText.setText(String.format("%.1f %%", percent * 100));
    }

/**
 * 计算内存中空闲区域的总大小
 * 该方法遍历内存快照中的所有段，累加标记为"空闲"的段的大小
 *
 * @return 返回内存中所有空闲段的总大小（以字节为单位）
 */
    // 计算空闲区总大小
    private int calculateFreeSize() {
        int freeSize = 0;  // 初始化空闲区总大小为0
    // 遍历内存快照中的所有段
        for (var seg : mm.snapshot()) {
        // 检查当前段是否为空闲段
            if (seg.label().equals("空闲")) {
            // 如果是空闲段，则将其大小累加到总空闲大小中
                freeSize += seg.size();
            }
        }
    // 返回计算得到的空闲区总大小
        return freeSize;
    }
}