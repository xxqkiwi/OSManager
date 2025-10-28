package org.example.disktest2.layout;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.text.Font;
import javafx.scene.web.WebView;
import javafx.scene.web.WebEngine;
import javafx.util.Duration;
import org.example.disktest2.HelloApplication;
import org.example.disktest2.pd.view.OSProcessView;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import javafx.scene.paint.Color;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainLayout {

    public MainLayout(Stage stage) {
        init(stage);
    }

    private Stage primaryStage;


    public void init(Stage stage) {
        // 背景
        Image backgroundImage = new Image(getClass().getResourceAsStream("/org/example/disktest2/images/bg.jpg"));
        ImageView bgView = new ImageView(backgroundImage);
        bgView.fitWidthProperty().bind(stage.widthProperty());
        bgView.fitHeightProperty().bind(stage.heightProperty());

        // 文件管理图标
        Image appImage = new Image(getClass().getResourceAsStream("/org/example/disktest2/images/file.jpg"));
        ImageView appView = new ImageView(appImage);
        appView.setFitHeight(60);
        appView.setFitWidth(60);
        Label appName = new Label("文件管理");
        VBox app = new VBox(10);
        app.getChildren().addAll(appView, appName);
        appView.setOnMouseClicked(mouseEvent -> {
            try {
                openFileManage();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        appName.setOnMouseClicked(mouseEvent -> {
            try {
                openFileManage();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });


        // 进程管理图标
        Image appImage_P = new Image(getClass().getResourceAsStream("/org/example/disktest2/images/process.png"));
        ImageView appView_P = new ImageView(appImage_P);
        appView_P.setFitHeight(60);
        appView_P.setFitWidth(60);
        Label appName_P = new Label("进程管理");
        VBox app_p = new VBox(10);
        app_p.getChildren().addAll(appView_P, appName_P);
        appView_P.setOnMouseClicked(mouseEvent -> {
            try {
                openProcessManage();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        appName_P.setOnMouseClicked(mouseEvent -> {
            try {
                openProcessManage();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });


        // 内存管理图标
        Image memoryImage = new Image(getClass().getResourceAsStream("/org/example/disktest2/images/memory.png"));
        ImageView memoryView = new ImageView(memoryImage);
        memoryView.setFitHeight(60);
        memoryView.setFitWidth(60);
        Label memoryName = new Label("内存管理");
        VBox memoryApp = new VBox(10);
        memoryApp.getChildren().addAll(memoryView, memoryName);
        // 绑定点击事件
        memoryView.setOnMouseClicked(mouseEvent -> {
            try {
                openMemoryManage();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        memoryName.setOnMouseClicked(mouseEvent -> {
            try {
                openMemoryManage();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        // 百度图标
        Image baiduImage = new Image(getClass().getResourceAsStream("/org/example/disktest2/images/baidu.jpg"));
        ImageView baiduView = new ImageView(baiduImage);
        baiduView.setFitHeight(60);
        baiduView.setFitWidth(60);
        Label baiduName = new Label("百度浏览器");
        VBox baiduApp = new VBox(10);
        baiduApp.getChildren().addAll(baiduView, baiduName);
       // 百度图标点击事件 - 打开百度网页
        baiduView.setOnMouseClicked(mouseEvent -> openBaiduInApp());
        baiduName.setOnMouseClicked(mouseEvent -> openBaiduInApp());

        //创建水平容器放置应用图标，间距设为10
        HBox appContainer = new HBox(10);
        appContainer.getChildren().addAll(app, app_p, memoryApp, baiduApp);
        // 设置图标容器居中对齐
        appContainer.setAlignment(javafx.geometry.Pos.CENTER);
        // 设置图标容器的位置（底部）和边距
        VBox.setMargin(appContainer, new Insets(0, 0, 20, 0)); // 底部留出20px间距

        // 创建日期和时间标签
        Label timeLabel = new Label(); // 时间标签
        Label dateLabel = new Label(); // 日期标签
        // 设置字体大小和样式
        timeLabel.setFont(Font.font("SimHei", 16.0));
        dateLabel.setFont(Font.font("SimHei", 14.0));
        // 设置标签颜色
        timeLabel.setTextFill(Color.BLACK);
        dateLabel.setTextFill(Color.BLACK);
        // 创建日期时间容器
        VBox dateTimeContainer = new VBox(5);
        dateTimeContainer.getChildren().addAll(timeLabel,dateLabel);
        dateTimeContainer.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        // 设置日期时间容器的边距（靠右下）
        StackPane.setMargin(dateTimeContainer, new Insets(570, 0, 10, 570));

        // 创建中间容器（用于放置图标容器，控制其在底部）
        VBox bottomContainer = new VBox();
        bottomContainer.getChildren().add(new Region()); // 空白区域填充顶部空间
        bottomContainer.getChildren().add(appContainer); // 图标容器被推到底部
        bottomContainer.setAlignment(Pos.BOTTOM_CENTER); // 确保图标容器在底部居中

        // 根容器使用StackPane（控制堆叠顺序）
        StackPane root = new StackPane();
        root.getChildren().add(bgView); // 添加背景（最底层）
        root.getChildren().add(bottomContainer); // 添加图标容器（在背景之上）
        root.getChildren().add(dateTimeContainer); // 添加日期时间（在最顶层）


        // 创建时间线动画，每秒更新一次日期和时间
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            Date now = new Date();
            // 格式化日期和时间
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd EEEE");
            timeLabel.setText(timeFormat.format(now));
            dateLabel.setText(dateFormat.format(now));
        }));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();


        stage.setScene(new Scene(root, 800, 600)); // 设置初始大小
        stage.show();
    }

    //打开文件管理界面的方法
    public void openFileManage() throws IOException {
        System.out.println("open fileManage");
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("diskPane.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1100, 700);
        javafx.stage.Stage fileManageStage = new javafx.stage.Stage();
        fileManageStage.setTitle("文件管理");
        fileManageStage.setScene(scene);
        fileManageStage.show();
    }

    //打开进程管理界面的方法
    public void openProcessManage() throws IOException {
        try {
            Stage processStage = new Stage();
            processStage.initOwner(primaryStage);
            new OSProcessView().start(processStage);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("打开进程管理界面失败：" + e.getMessage());
        }
    }
    // 打开内存管理界面的方法
    public void openMemoryManage() throws IOException {
        try {
            Stage memoryStage = new Stage();
            memoryStage.initOwner(primaryStage);
            memoryStage.setTitle("内存管理");
            FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("memory/memory-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 1000, 600);
            memoryStage.setScene(scene);
            memoryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("打开内存管理界面失败：" + e.getMessage());
        }
    }
    // 打开内置百度界面（使用WebView）
    private void openBaiduInApp() {
        try {
            Stage baiduStage = new Stage();
            baiduStage.initOwner(primaryStage);
            baiduStage.setTitle("百度浏览器");

            // 创建WebView组件用于显示网页
            WebView webView = new WebView();
            WebEngine webEngine = webView.getEngine();
            // 加载百度首页
            webEngine.load("https://www.baidu.com");

            // 设置场景并显示
            Scene scene = new Scene(webView, 1024, 768);
            baiduStage.setScene(scene);
            baiduStage.show();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("打开百度界面失败：" + e.getMessage());
        }
    }
}