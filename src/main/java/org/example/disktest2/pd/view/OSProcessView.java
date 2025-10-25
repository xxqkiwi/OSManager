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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
        Scene scene = new Scene(root, 1200, 800);
        stage.setScene(scene);
        stage.show();

        // 初始更新UI
        updateUI();
    }

    private void createTestProcess() {
        char[] devices = {'A', 'B', 'C'};
        char randomDevice = devices[new Random().nextInt(3)];
        int useTime = new Random().nextInt(3) + 1;

        List<String> instructions = Arrays.asList(
                "x=10",
                "x++",
                "!%c %d".formatted(randomDevice, useTime),
                "x--",
                "end"
        );

        boolean success = processService.createProcess("test.e", instructions, 32);
        logArea.appendText(success ?
                "创建进程成功（使用设备" + randomDevice + "，时长" + useTime + "）\n" :
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

        // 设置高亮
        processTable.setRowFactory(tv -> new TableRow<ProcessDetail>() {
            @Override
            protected void updateItem(ProcessDetail item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else if (item.getState() == highlightState) {
                    setStyle("-fx-background-color: lightgreen;");
                } else {
                    setStyle("");
                }
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}