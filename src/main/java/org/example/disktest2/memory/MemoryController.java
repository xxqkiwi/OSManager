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
import javafx.scene.shape.Rectangle;
import org.example.disktest2.file.Controller.OSManager;

public class MemoryController {

    @FXML private ProgressBar usageBar;    // 内存使用进度条
    @FXML private Label usageText;    // 内存使用率文本标签
    @FXML private Canvas canvas;           // 内存可视化画布
    @FXML private Canvas diskCanvas;  // 磁盘可视化布
    @FXML private ProgressBar diskUsageBar;    // 磁盘使用进度条
    @FXML private Label diskUsageText;         // 磁盘使用率文本标签
    @FXML private Label diskSizeInfoLabel;     // 磁盘大小信息标签
    @FXML private HBox diskLegendBox;          // 磁盘图例容器
    private final MemoryManager mm = MemoryManager.get();
    private Label sizeInfoLabel;
    private Label colorDescLabel; // 用于显示颜色说明的标签
    private HBox legendBox;
    private Timeline refreshTimeline;

    private final int DISK_TOTAL_SIZE = 128;// 磁盘管理相关引用（假设磁盘总大小为128块，与OSManager中fat数组大小对应）
    //private OSManager osManager = new OSManager();  // 引用磁盘管理类
    private OSManager osManager = OSManager.getInstance();

    @FXML
    private void initialize() {
        // 创建大小信息标签，用于显示画布尺寸相关信息
        sizeInfoLabel = new Label();
        sizeInfoLabel.getStyleClass().add("size-info");  // 为标签添加CSS样式类

        // 创建图例，用于解释可视化图表中的符号或颜色含义
        createLegend();

        // 将信息标签和图例添加到容器（修正容器获取逻辑）
        VBox infoContainer = new VBox(5);
        // 先添加进度条和使用率文本
        HBox usageBox = new HBox(10);
        usageBox.getChildren().addAll(usageBar, usageText);
        infoContainer.getChildren().add(usageBox);
        // 再添加大小信息和图例
        infoContainer.getChildren().addAll(legendBox,sizeInfoLabel,new Label("内存使用情况："));

        canvas.setWidth(800);
        canvas.setHeight(100);

        // 获取 canvas 的直接父容器
        Pane canvasParent = (Pane) canvas.getParent();

        // 将信息容器添加到 canvas 下方
        if (canvasParent != null) {
            // 清除 canvasParent 中可能存在的旧信息容器（避免重复添加）
            canvasParent.getChildren().removeIf(node -> node instanceof VBox && ((VBox) node).getStyleClass().contains("info-container"));
            // 标记信息容器以便后续移除
            infoContainer.getStyleClass().add("info-container");
            // 添加到 canvas 父容器，放在 canvas 后面
            int canvasIndex = canvasParent.getChildren().indexOf(canvas);
            canvasParent.getChildren().add(canvasIndex + 1, infoContainer);
        }

        // 尺寸变化即重绘
        canvas.widthProperty().addListener((obs, old, neo) -> draw());
        canvas.heightProperty().addListener((obs, old, neo) -> draw());

        draw(); // 首次绘制
        // 磁盘显示初始化
        initializeDiskDisplay();
        // 初始化定时刷新任务
        refreshTimeline = new Timeline(new KeyFrame(Duration.millis(500), e -> {
            if (mm.isMemoryChanged()) { // 现在可以正常调用了
                draw(); // 刷新界面
                mm.resetMemoryChanged(); // 重置标识
            }
            updateDiskUsage();
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


        //testDiskDisplayChanges();
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

        // 系统区图例（匹配界面深蓝色）
        HBox sysLegend = new HBox(5);
        sysLegend.getStyleClass().add("legend-item");
        Rectangle sysRect = new Rectangle(15, 15);
        sysRect.setFill(Color.rgb(59, 89, 152));  // 深蓝色（与界面系统区颜色匹配）
        sysRect.getStyleClass().add("legend-color");
        sysLegend.getChildren().addAll(sysRect, new Label("系统区"));

        // 空闲区图例（匹配界面浅绿色）
        HBox freeLegend = new HBox(5);
        freeLegend.getStyleClass().add("legend-item");
        Rectangle freeRect = new Rectangle(15, 15);
        freeRect.setFill(Color.rgb(144, 238, 144));  // 浅绿色（与界面空闲区颜色匹配）
        freeRect.getStyleClass().add("legend-color");
        freeLegend.getChildren().addAll(freeRect, new Label("空闲区"));

        // 进程区图例（匹配界面红色）
        HBox processLegend = new HBox(5);
        processLegend.getStyleClass().add("legend-item");
        Rectangle processRect = new Rectangle(15, 15);
        processRect.setFill(Color.rgb(255, 107, 107));  // 红色（与界面进程区颜色匹配）
        processRect.getStyleClass().add("legend-color");
        processLegend.getChildren().addAll(processRect, new Label("进程区"));

        legendBox.getChildren().addAll(sysLegend, freeLegend, processLegend);
    }


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
                //g.fillText(seg.label(), seg.start() + 1, 20);
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
        usageText.setText(String.format("内存使用率:%.1f %%", percent * 100));
    }

/*
 计算内存中空闲区域的总大小,该方法遍历内存快照中的所有段，累加标记为"空闲"的段的大小,@return 返回内存中所有空闲段的总大小（以字节为单位）
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

    // 新增磁盘显示初始化方法
    private void initializeDiskDisplay() {
        // 创建磁盘大小信息标签
        diskUsageBar = new ProgressBar();
        diskUsageBar.getStyleClass().add("usage-bar");
        diskUsageBar.setId("diskUsageBar");

        diskUsageText = new Label();
        diskUsageText.getStyleClass().add("usage-label");

        diskSizeInfoLabel = new Label();
        diskSizeInfoLabel.getStyleClass().add("size-info");

        // 创建磁盘图例
        createDiskLegend();

        // 创建磁盘使用进度条和文本容器
        HBox diskUsageBox = new HBox(10);
        diskUsageBox.getChildren().addAll(diskUsageBar, diskUsageText);

        VBox diskInfoContainer = new VBox(5);
        diskInfoContainer.getStyleClass().add("info-container");
        diskInfoContainer.getChildren().addAll(
                diskLegendBox,
                new Label("磁盘使用情况："),
                diskUsageBox,
                diskSizeInfoLabel

        );

        // 设置磁盘画布属性
        diskCanvas.setWidth(800);
        diskCanvas.setHeight(100);

        // 获取磁盘画布的父容器并添加信息容器
        Pane diskCanvasParent = (Pane) diskCanvas.getParent();
        if (diskCanvasParent != null) {
            diskCanvasParent.getChildren().removeIf(
                    node -> node instanceof VBox && ((VBox) node).getStyleClass().contains("disk-info-container")
            );
            diskInfoContainer.getStyleClass().add("disk-info-container");
            int diskCanvasIndex = diskCanvasParent.getChildren().indexOf(diskCanvas);
            diskCanvasParent.getChildren().add(diskCanvasIndex + 1, diskInfoContainer);
        }

        // 磁盘画布尺寸变化监听
        diskCanvas.widthProperty().addListener((obs, old, neo) -> drawDisk());
        diskCanvas.heightProperty().addListener((obs, old, neo) -> drawDisk());

        // 在内存刷新定时器中添加磁盘刷新逻辑
        refreshTimeline = new Timeline(new KeyFrame(Duration.millis(500), e -> {
            if (mm.isMemoryChanged()) {
                draw();
                mm.resetMemoryChanged();
            }
            // 每次刷新都更新磁盘显示（可根据实际情况优化）
            drawDisk();
        }));
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();
    }

    // 更新磁盘显示的方法
    private void updateDiskUsage() {
        // 从原有OSManager中获取fat表
        int[] fat = osManager.getFat();
        if (fat == null) return;

        // 原有逻辑中fat[0]存储空闲块数，直接复用
        int freeBlocks = fat[0];
        int usedBlocks = DISK_TOTAL_SIZE - freeBlocks;//计算已使用的磁盘块数（总块数 - 空闲块数）
        double usage = (double) usedBlocks / DISK_TOTAL_SIZE;//计算磁盘使用率（已使用块数 / 总块数

        // 更新原有控件的状态
        diskUsageBar.setProgress(usage);
        diskUsageText.setText(String.format("磁盘使用率: %.1f%%", usage * 100));
    }
    // 新增磁盘图例创建方法
    private void createDiskLegend() {
        diskLegendBox = new HBox();
        diskLegendBox.getStyleClass().add("legend-box");

        // 已使用块图例
        HBox usedLegend = new HBox(5);
        usedLegend.getStyleClass().add("legend-item");
        Rectangle usedRect = new Rectangle(15, 15);
        usedRect.setFill(Color.rgb(255, 107, 107));  // 红色系，与CSS中磁盘已使用颜色对应
        usedRect.getStyleClass().add("legend-color");
        usedLegend.getChildren().addAll(usedRect, new Label("已使用块"));

        // 空闲块图例
        HBox freeLegend = new HBox(5);
        freeLegend.getStyleClass().add("legend-item");
        Rectangle freeRect = new Rectangle(15, 15);
        freeRect.setFill(Color.rgb(78, 205, 196));  // 蓝绿色系，与CSS中磁盘空闲颜色对应
        freeRect.getStyleClass().add("legend-color");
        freeLegend.getChildren().addAll(freeRect, new Label("空闲块"));

        // 系统块图例
        HBox sysLegend = new HBox(5);
        sysLegend.getStyleClass().add("legend-item");
        Rectangle sysRect = new Rectangle(15, 15);
        sysRect.setFill(Color.rgb(0, 198, 255));  // 蓝色系，与内存系统区颜色保持一致
        sysRect.getStyleClass().add("legend-color");
        sysLegend.getChildren().addAll(sysRect, new Label("系统块"));

        diskLegendBox.getChildren().addAll(usedLegend, freeLegend, sysLegend);
    }

    // 新增磁盘绘制方法
    private void drawDisk() {
        GraphicsContext g = diskCanvas.getGraphicsContext2D();
        g.clearRect(0, 0, diskCanvas.getWidth(), diskCanvas.getHeight());

        //OSManager osManager = new OSManager();  // 获取磁盘管理实例
        int[] fat = osManager.getFat();
        //int DISK_TOTAL_SIZE = 128;

        // 计算磁盘使用率
        int freeBlocks = fat[0];  // FAT表第0项存储空闲块数
        int usedBlocks = DISK_TOTAL_SIZE - freeBlocks;

        // 计算缩放比例
        double scaleX = diskCanvas.getWidth() / DISK_TOTAL_SIZE;
        double scaleY = diskCanvas.getHeight() / 25.0;

        g.save();
        g.scale(scaleX, scaleY);

        // 绘制每个磁盘块
        for (int i = 0; i < DISK_TOTAL_SIZE; i++) {
            // 设置颜色：前3块为系统块，0为空闲，-1为已使用
            if (i < 3) {
                g.setFill(Color.rgb(0, 198, 255));  // 系统块（蓝色）
            } else if (fat[i] == 0) {
                g.setFill(Color.rgb(78, 205, 196));  // 空闲块（蓝绿色）
            } else {
                g.setFill(Color.rgb(255, 107, 107));  // 已使用块（红色）
            }
            g.fillRect(i, 0, 1, 30);  // 绘制块
        }

        g.restore();

        // 更新磁盘信息
        diskSizeInfoLabel.setText(String.format("磁盘总块数: %d 块  |  空闲块数: %d 块  |  已使用块数: %d 块",
                DISK_TOTAL_SIZE, freeBlocks, usedBlocks));

        // 更新磁盘进度条
        double usagePercent = (double) usedBlocks / DISK_TOTAL_SIZE;//计算磁盘使用率（已使用块数 / 总块数）
        diskUsageBar.setProgress(usagePercent);//更新磁盘进度条的进度值
        diskUsageText.setText(String.format("磁盘使用率: %.1f %%", usagePercent * 100));
    }

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
}