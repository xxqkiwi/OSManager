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

    private Stage primaryStage;  // 这一行是新增的


    public void init(Stage stage) {
        // 背景
        Image backgroundImage = new Image(getClass().getResourceAsStream("/org/example/disktest2/images/bg.jpg"));
        ImageView bgView = new ImageView(backgroundImage);
        bgView.fitWidthProperty().bind(stage.widthProperty());
        bgView.fitHeightProperty().bind(stage.heightProperty());

        // app1图标
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


        // app2图标（
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


        //创建水平容器放置两个应用图标，间距设为20
        HBox appContainer = new HBox(20);
        appContainer.getChildren().addAll(app, app_p); // 将app1和app2添加到HBox

        StackPane root = new StackPane();
        root.getChildren().add(0, bgView);//背景图片放最底层
        root.getChildren().add(1, appContainer);

        stage.setScene(new Scene(root, stage.getWidth(), stage.getHeight()));
        stage.show();
    }

    public void openFileManage() throws IOException {
        System.out.println("open fileManage");
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("diskPane.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1000, 600);
        javafx.stage.Stage fileManageStage = new javafx.stage.Stage();
        fileManageStage.setTitle("文件管理");
        fileManageStage.setScene(scene);
        fileManageStage.show();
    }

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
}