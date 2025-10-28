module org.example.disktest2 {
    requires javafx.controls;
    requires javafx.fxml;
    requires jdk.compiler;
    requires java.desktop;
    requires javafx.web;
    exports org.example.disktest2.pd.view ;
    opens org.example.disktest2 to javafx.fxml;
    exports org.example.disktest2;
    opens org.example.disktest2.memory to javafx.fxml;
    exports org.example.disktest2.memory;
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
}