module org.example.disktest2 {
    requires javafx.controls;
    requires javafx.fxml;
    requires jdk.compiler;
    requires javafx.base;
    requires javafx.graphics;
    requires java.desktop;
    requires javafx.web;
    requires javafx.media;


    //exports org.example.disktest2.pd.view ;
    opens org.example.disktest2 to javafx.fxml;
    exports org.example.disktest2;
    /*exports org.example.disktest2.Application;
    opens org.example.image.Application to javafx.fxml;*/

    exports org.example.disktest2.file.Controller;
    opens org.example.disktest2.file.Controller to javafx.fxml;

    exports org.example.disktest2.layout;
    opens org.example.disktest2.layout to javafx.fxml;
    exports org.example.disktest2.file.entity;
    opens org.example.disktest2.file.entity to javafx.fxml;
    exports org.example.disktest2.file;
    opens org.example.disktest2.file to javafx.fxml;

    exports org.example.disktest2.pd.view to javafx.graphics;

    exports org.example.disktest2.memory;
    opens org.example.disktest2.memory to javafx.fxml;
}