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

import java.io.IOException;

public class MainLayout {

    public MainLayout(Stage stage) {
        init(stage);
    }

    public void init(Stage stage) {
        // 背景
        Image backgroundImage = new Image(getClass().getResourceAsStream("/org/example/disktest2/images/bg.jpg"));
        ImageView bgView = new ImageView(backgroundImage);
        bgView.fitWidthProperty().bind(stage.widthProperty());
        bgView.fitHeightProperty().bind(stage.heightProperty());

        // app图标
        Image appImage = new Image(getClass().getResourceAsStream("/org/example/disktest2/images/file.jpg"));
        ImageView appView = new ImageView(appImage);
        appView.setFitHeight(50);
        appView.setFitWidth(50);
        Label appName = new Label("文件管理");
        VBox app = new VBox(10);
        app.getChildren().addAll(appView, appName);
        app.setOnMouseClicked(mouseEvent -> {
                    try {
                        openFileManage();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                );

        StackPane root = new StackPane();
        root.getChildren().add(0,bgView);//背景图片放最底层
        root.getChildren().add(1,app);

        stage.setScene(new Scene(root, stage.getWidth(), stage.getHeight()));
        stage.show();
    }

    public void openFileManage() throws IOException {
        System.out.println("open fileManage");
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("diskPane.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1100, 700);
        javafx.stage.Stage fileManageStage = new javafx.stage.Stage();
        fileManageStage.setTitle("文件管理");
        fileManageStage.setScene(scene);
        fileManageStage.show();
    }

}