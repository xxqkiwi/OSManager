package org.example.disktest2.layout;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.disktest2.HelloApplication;
import org.example.disktest2.pd.view.OSProcessView;

import java.io.IOException;

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
        appView.setFitHeight(50);
        appView.setFitWidth(50);
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
        appView_P.setFitHeight(50);
        appView_P.setFitWidth(50);
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
        memoryView.setFitHeight(50);
        memoryView.setFitWidth(50);
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


        //创建水平容器放置三个应用图标，间距设为20
        HBox appContainer = new HBox(20);
        appContainer.getChildren().addAll(app, app_p, memoryApp); // 添加内存管理图标

        StackPane root = new StackPane();
        root.getChildren().add(0, bgView);//背景图片放最底层
        root.getChildren().add(1, appContainer);

        stage.setScene(new Scene(root, stage.getWidth(), stage.getHeight()));
        stage.show();
    }

    //打开文件管理界面
    public void openFileManage() throws IOException {
        System.out.println("open fileManage");
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("diskPane.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1000, 600);
        javafx.stage.Stage fileManageStage = new javafx.stage.Stage();
        fileManageStage.setTitle("文件管理");
        fileManageStage.setScene(scene);
        fileManageStage.show();
    }

    //打开进程管理界面
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
}