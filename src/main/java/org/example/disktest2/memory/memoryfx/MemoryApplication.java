package org.example.disktest2.memory.memoryfx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MemoryApplication extends Application {
    public MemoryApplication() {
    }

    @Override
    public void start(Stage stage) throws Exception {
        // 使用FXMLLoader加载FXML布局文件
        // 在 FXMLLoader 初始化前添加
        try{
            FXMLLoader loader = new FXMLLoader(getClass().getResource("memory-view.fxml"));
            System.out.println("FXML 资源路径：" + getClass().getResource("memory-view.fxml"));
            // 创建场景并加载FXML文件
            Scene scene = new Scene(loader.load());
            // 将场景设置到舞台
            stage.setScene(scene);
            // 设置窗口标题
            stage.setTitle("存储管理模拟");
        } catch (Exception e) {
            e.printStackTrace();
        }


        /* ===== 等比例锁死 ===== */
        double ratio = 15.0 / 10.0;          // 保持的宽高比
        stage.setMinWidth (640);
        stage.setMinHeight(640 / ratio);
        stage.widthProperty().addListener((obs, old, neo) ->
                stage.setHeight(neo.doubleValue() / ratio));

        stage.show();
    }

/**
 * 程序的主入口方法
 * @param args 命令行参数，用于接收程序运行时传入的参数
 */
    public static void main(String[] args) {
    // 调用launch()方法启动程序
        launch();
    }
}