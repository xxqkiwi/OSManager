package org.example.disktest2;

import com.sun.tools.javac.Main;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.disktest2.layout.MainLayout;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("mainLayout.fxml"));
        MainLayout mainLayout= new MainLayout(stage);
        /*Scene scene = new Scene(fxmlLoader.load(), 1000, 600);
        stage.setTitle("主界面");
        stage.setScene(scene);
        stage.show();*/
    }

    public static void main(String[] args) {
        launch();
    }
}