module org.example.disktest2 {
    requires javafx.controls;
    requires javafx.fxml;
    requires jdk.compiler;


    opens org.example.disktest2 to javafx.fxml;
    exports org.example.disktest2;
    /*exports org.example.disktest2.Application;
    opens org.example.image.Application to javafx.fxml;*/
    exports org.example.disktest2.Controller;
    opens org.example.disktest2.Controller to javafx.fxml;
    exports org.example.disktest2.layout;
    opens org.example.disktest2.layout to javafx.fxml;
}