module org.example.disktest2 {

    requires javafx.base;
    requires javafx.graphics;

    requires javafx.controls;
    requires javafx.fxml;
    requires jdk.compiler;

    exports org.example.disktest2.pd.view ;
    opens org.example.disktest2 to javafx.fxml;
    exports org.example.disktest2;
    opens org.example.disktest2.memory.memoryfx to javafx.fxml;
    exports org.example.disktest2.memory.memoryfx;
    /*exports org.example.disktest2.Application;
    opens org.example.image.Application to javafx.fxml;*/

    exports org.example.disktest2.Controller;
    opens org.example.disktest2.Controller to javafx.fxml;
    exports org.example.disktest2.layout;
    opens org.example.disktest2.layout to javafx.fxml;
}