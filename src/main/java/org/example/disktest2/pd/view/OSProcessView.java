package org.example.disktest2.pd.view;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import org.example.disktest2.pd.application.ProcessService;
import org.example.disktest2.pd.domain.dto.ProcessDetail;
import org.example.disktest2.pd.domain.enums.DeviceType;
import org.example.disktest2.pd.domain.enums.ProcessFilter;
import org.example.disktest2.pd.domain.enums.ProcessState;
import org.example.disktest2.pd.domain.model.Device;
import org.example.disktest2.pd.domain.model.PCB;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 可视化界面：展示系统状态，提供操作入口
 */
public class OSProcessView extends Application {
    private final ProcessService processService = new ProcessService();
    private TextArea logArea;
    private ListView<String> readyListView;
    private ListView<String> blockedListView;
    private ListView<String> deviceListView;
    private Label clockLabel;
    private Thread executionThread;
    private TableView<ProcessDetail> processTable;
    private VBox runningProcessPanel;
    private ComboBox<ProcessFilter> filterCombo;
    private ComboBox<ProcessState> highlightCombo;
    private ToggleGroup speedGroup;
    private CheckBox allowCreateCheck;

    @Override
    public void start(Stage stage) {
        stage.setTitle("进程管理器");
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        // 顶部：系统时钟和运行进程详情
        VBox topBox = new VBox(10);
        clockLabel = new Label("系统时钟：0");
        clockLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        // 当前运行进程面板
        runningProcessPanel = new VBox(5);
        runningProcessPanel.setBorder(new Border(new BorderStroke(
                Color.LIGHTGRAY, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));
        runningProcessPanel.setPadding(new Insets(5));
        runningProcessPanel.getChildren().add(new Label("当前运行进程详情："));

        topBox.getChildren().addAll(clockLabel, runningProcessPanel);
        root.setTop(topBox);

        // 中间：分为左右两部分
        HBox centerBox = new HBox(10);

        // 左侧：队列和设备状态
        VBox leftBox = new VBox(10);
        // 就绪队列
        VBox readyBox = new VBox(5);
        readyBox.getChildren().add(new Label("就绪队列"));
        readyListView = new ListView<>();
        readyBox.getChildren().add(readyListView);
        // 阻塞队列
        VBox blockedBox = new VBox(5);
        blockedBox.getChildren().add(new Label("阻塞队列"));
        blockedListView = new ListView<>();
        blockedBox.getChildren().add(blockedListView);
        // 设备状态
        VBox deviceBox = new VBox(5);
        deviceBox.getChildren().add(new Label("设备状态"));
        deviceListView = new ListView<>();
        deviceBox.getChildren().add(deviceListView);

        leftBox.getChildren().addAll(readyBox, blockedBox, deviceBox);
        leftBox.setPrefWidth(300);

        // 右侧：进程详表
        VBox rightBox = new VBox(10);
        rightBox.getChildren().add(new Label("进程详表"));

        // 进程表格
        processTable = new TableView<>();
        processTable.setPrefWidth(600);

        TableColumn<ProcessDetail, Integer> pidCol = new TableColumn<>("进程编号");
        pidCol.setCellValueFactory(cellData ->
                new SimpleIntegerProperty(cellData.getValue().getPid()).asObject()
        );

        TableColumn<ProcessDetail, String> stateCol = new TableColumn<>("进程状态");
        stateCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getState().toString())
        );

        TableColumn<ProcessDetail, String> exeCol = new TableColumn<>("执行文件");
        exeCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getExeFile())
        );

        TableColumn<ProcessDetail, String> deviceCol = new TableColumn<>("设备使用");
        deviceCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDeviceUsage())
        );

        TableColumn<ProcessDetail, String> pcbCol = new TableColumn<>("PCB信息");
        pcbCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getPcbInfo())
        );

        TableColumn<ProcessDetail, String> resultCol = new TableColumn<>("执行结果");
        resultCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getExecutionResult())
        );

        TableColumn<ProcessDetail, Double> progressCol = new TableColumn<>("完成进度");
        progressCol.setCellValueFactory(cellData ->
                new SimpleDoubleProperty(cellData.getValue().getCompletionProgress()).asObject()
        );
        progressCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double progress, boolean empty) {
                super.updateItem(progress, empty);
                if (empty || progress == null) {
                    setGraphic(null);
                } else {
                    ProgressBar bar = new ProgressBar(progress);
                    bar.setPrefWidth(100);
                    setGraphic(bar);
                }
            }
        });

        processTable.getColumns().addAll(pidCol, stateCol, exeCol, deviceCol, pcbCol, resultCol, progressCol);
        rightBox.getChildren().add(processTable);

        centerBox.getChildren().addAll(leftBox, rightBox);
        root.setCenter(centerBox);

        // 底部：操作区和日志
        VBox bottomBox = new VBox(10);

        // 控制区
        HBox controlBox = new HBox(10);

        // 显示设置
        VBox filterBox = new VBox(5);
        filterBox.getChildren().add(new Label("显示设置"));
        filterCombo = new ComboBox<>();
        filterCombo.getItems().addAll(ProcessFilter.values());
        filterCombo.setValue(ProcessFilter.ALL);
        filterCombo.setOnAction(e -> updateUI());
        filterBox.getChildren().add(filterCombo);

        // 标记设置
        VBox highlightBox = new VBox(5);
        highlightBox.getChildren().add(new Label("标记设置"));
        highlightCombo = new ComboBox<>();
        highlightCombo.getItems().addAll(ProcessState.values());
        highlightCombo.setValue(ProcessState.RUNNING);
        highlightCombo.setOnAction(e -> updateUI());
        highlightBox.getChildren().add(highlightCombo);

        // 进程控制
        VBox processControlBox = new VBox(5);
        processControlBox.getChildren().add(new Label("进程控制"));

        allowCreateCheck = new CheckBox("允许新建进程");
        allowCreateCheck.setSelected(true);
        allowCreateCheck.setOnAction(e ->
                processService.setAllowCreateProcess(allowCreateCheck.isSelected()));

        HBox speedBox = new HBox(5);
        speedBox.getChildren().add(new Label("速度倍率："));
        speedGroup = new ToggleGroup();
        for (int speed : new int[]{1, 2, 4, 8}) {
            RadioButton rb = new RadioButton(speed + "x");
            rb.setToggleGroup(speedGroup);
            rb.setUserData(speed);
            if (speed == 1) rb.setSelected(true);
            speedBox.getChildren().add(rb);
        }
        speedGroup.selectedToggleProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                processService.setSpeedMultiplier((Integer) newVal.getUserData());
            }
        });

        processControlBox.getChildren().addAll(allowCreateCheck, speedBox);

        // 操作按钮
        HBox btnBox = new HBox(10);
        Button createBtn = new Button("创建进程");
        Button startBtn = new Button("启动执行");
        Button stopBtn = new Button("停止执行");
        btnBox.getChildren().addAll(createBtn, startBtn, stopBtn);

        createBtn.setOnAction(e -> createTestProcess());
        startBtn.setOnAction(e -> startExecution());
        stopBtn.setOnAction(e -> stopExecution());

        controlBox.getChildren().addAll(filterBox, highlightBox, processControlBox, btnBox);

        // 日志区
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(150);

        bottomBox.getChildren().addAll(controlBox, logArea);
        root.setBottom(bottomBox);

        // 显示界面
        Scene scene = new Scene(root, 900, 800);
        stage.setScene(scene);
        stage.show();

        // 初始更新UI
        updateUI();
    }

    private void createTestProcess() {
        // 随机选择一个可执行文件
        ExeFile randomExe = EXE_FILES.get(new Random().nextInt(EXE_FILES.size()));

        // 直接使用该文件的指令集（保证指令兼容且不同文件指令有差异）
        List<String> instructions = randomExe.getInstructions();

        // 创建进程时传入选中的文件名
        boolean success = processService.createProcess(
                randomExe.getName(),
                instructions,
                32  // 内存大小保持不变
        );

        // 日志提示更新为选中的文件名
        logArea.appendText(success ?
                "创建进程成功（文件：" + randomExe.getName() + "）\n" :
                "创建进程失败（已禁止或资源不足）\n");
        updateUI();
    }

    private void startExecution() {
        if (executionThread != null && executionThread.isAlive()) {
            logArea.appendText("已在执行中\n");
            return;
        }
        executionThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                String log = processService.executeNextInstruction();
                logArea.appendText(log);
                Platform.runLater(this::updateUI);

                // 根据速度倍率调整休眠时间
                int delay = 1000 / processService.getSpeedMultiplier();
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        executionThread.start();
    }

    private void stopExecution() {
        if (executionThread != null && executionThread.isAlive()) {
            executionThread.interrupt();
            logArea.appendText("已停止执行\n");
        }
    }

    private void updateUI() {
        ProcessService.SystemState state = processService.getSystemState();
        // 系统时钟
        clockLabel.setText("系统时钟：" + state.systemTime);

        // 更新当前运行进程详情
        updateRunningProcessPanel(state.runningPcb, state.remainingTime);

        // 就绪队列
        readyListView.getItems().clear();
        state.readyQueue.forEach(pcb -> readyListView.getItems().add(
                String.format("PID=%d（%s）", pcb.getPid(), pcb.getState())
        ));

        // 阻塞队列
        blockedListView.getItems().clear();
        state.blockedQueue.forEach(pcb -> blockedListView.getItems().add(
                String.format("PID=%d（原因：设备%s）",
                        pcb.getPid(), (char) ('A' + pcb.getBlockReason()))
        ));

        // 设备状态
        deviceListView.getItems().clear();
        @SuppressWarnings("unchecked")
        Map<DeviceType, List<Device>> devices = (Map<DeviceType, List<Device>>) state.devices;
        devices.forEach((type, devList) -> {
            for (int i = 0; i < devList.size(); i++) {
                Device dev = devList.get(i);
                String status = dev.isInUse() ?
                        String.format("占用（PID=%d，剩余%d）",
                                dev.getUsingPid(), dev.getRemainingTime()) :
                        "空闲";
                deviceListView.getItems().add(
                        String.format("%s-%d：%s（等待数=%d）",
                                type.getName(), i + 1, status, dev.getWaitQueue().size())
                );
            }
        });

        // 更新进程表格
        updateProcessTable();
    }

    private void updateRunningProcessPanel(PCB running, int remainingTime) {
        runningProcessPanel.getChildren().removeIf(node -> node instanceof GridPane);
        if (running == null) return;

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(5);

        int row = 0;
        grid.add(new Label("进程编号："), 0, row);
        grid.add(new Label(String.valueOf(running.getPid())), 1, row++);

        grid.add(new Label("当前指令："), 0, row);
        Label instrLabel = new Label();
        if (running.getPc() < running.getInstructions().size()) {
            instrLabel.setText(running.getInstructions().get(running.getPc()));
            instrLabel.setStyle("-fx-background-color: yellow;"); // 高亮当前指令
        } else {
            instrLabel.setText("已完成");
        }
        grid.add(instrLabel, 1, row++);

        grid.add(new Label("数据寄存器(AX)："), 0, row);
        grid.add(new Label(String.valueOf(running.getAx())), 1, row++);

        grid.add(new Label("剩余时间片："), 0, row);
        grid.add(new Label(String.valueOf(remainingTime)), 1, row++);

        grid.add(new Label("总指令数："), 0, row);
        grid.add(new Label(String.valueOf(running.getInstructions().size())), 1, row++);

        grid.add(new Label("已执行指令："), 0, row);
        grid.add(new Label(String.valueOf(running.getPc())), 1, row);

        runningProcessPanel.getChildren().add(grid);
    }

    private void updateProcessTable() {
        List<ProcessDetail> allDetails = processService.getAllProcessDetails();
        ProcessFilter filter = filterCombo.getValue();
        ProcessState highlightState = highlightCombo.getValue();

        // 应用过滤
        List<ProcessDetail> filtered = allDetails.stream()
                .filter(detail -> {
                    if (filter == ProcessFilter.ALL) return true;
                    if (filter == ProcessFilter.CURRENT) {
                        return processService.getSystemState().runningPcb != null &&
                                detail.getPid() == processService.getSystemState().runningPcb.getPid();
                    }
                    if (filter == ProcessFilter.READY) return detail.getState() == ProcessState.READY;
                    if (filter == ProcessFilter.BLOCKED) return detail.getState() == ProcessState.BLOCKED;
                    if (filter == ProcessFilter.TERMINATED) return detail.getState() == ProcessState.TERMINATED;
                    return false;
                })
                .collect(Collectors.toList());

        processTable.getItems().setAll(filtered);

        // 定义状态与颜色的映射关系
        Map<ProcessState, String> stateColorMap = new HashMap<>();
        stateColorMap.put(ProcessState.RUNNING, "#99ff99");    // 运行中-浅绿色
        stateColorMap.put(ProcessState.READY, "#ffff99");      // 就绪中-浅黄色
        stateColorMap.put(ProcessState.TERMINATED, "#ff9999"); // 终止-浅红色
        stateColorMap.put(ProcessState.BLOCKED, "#9999ff");    // 阻塞-浅蓝色

        // 设置表格行高亮逻辑
        processTable.setRowFactory(tv -> new TableRow<ProcessDetail>() {
            @Override
            protected void updateItem(ProcessDetail item, boolean empty) {
                super.updateItem(item, empty);

                // 清除所有高亮样式
                getStyleClass().removeAll("highlighted-row", "default-row");
                setStyle(""); // 重置行样式

                if (!empty && item != null) {
                    // 获取当前选择的高亮状态
                    ProcessState highlightState = highlightCombo.getValue();

                    // 只有当选择了具体状态且匹配时才设置高亮
                    if (highlightState != null && item.getState() == highlightState) {
                        // 从映射中获取对应颜色，默认使用灰色
                        String color = stateColorMap.getOrDefault(highlightState, "#e0e0e0");
                        setStyle("-fx-background-color: " + color + ";");
                        getStyleClass().add("highlighted-row");
                    } else {
                        getStyleClass().add("default-row");
                    }
                }
            }
        });

        // 强制刷新表格以应用样式
        processTable.refresh();
    }

    // 定义10个可执行文件及其指令集（基于基础指令扩展）
    private static final List<ExeFile> EXE_FILES = Arrays.asList(
            new ExeFile("p1.exe", Arrays.asList("x=0", "x++", "!B 2", "x--", "x++", "end")),
            new ExeFile("p2.exe", Arrays.asList("x=5", "x++", "!B 3", "x--", "x=9","end")),
            new ExeFile("p3.exe", Arrays.asList("x=0", "x++", "!C 1", "x--", "end")),
            new ExeFile("p4.exe", Arrays.asList("x=3", "x++", "!A 2", "x--", "end")),
            new ExeFile("p5.exe", Arrays.asList("x=7", "x++", "!B 1", "x--","x--","x--", "end")),
            new ExeFile("p6.exe", Arrays.asList("x=5", "x++", "!C 2", "x--", "end")),
            new ExeFile("p7.exe", Arrays.asList("x=6", "x++", "!A 2", "x--", "x++","x++","x++","end")),
            new ExeFile("p8.exe", Arrays.asList("x=4", "x++", "!B 3", "x--", "end")),
            new ExeFile("p9.exe", Arrays.asList("x=8", "x++", "!C 3", "x--", "end")),
            new ExeFile("p10.exe",Arrays.asList("x=9", "x++", "!A 1", "x--", "end"))
    );

    // 内部类：封装可执行文件信息
    private static class ExeFile {
        private final String name;
        private final List<String> instructions;

        public ExeFile(String name, List<String> instructions) {
            this.name = name;
            this.instructions = instructions;
        }

        public String getName() { return name; }
        public List<String> getInstructions() { return instructions; }
    }

    public static void main(String[] args) {
        launch(args);
    }
}