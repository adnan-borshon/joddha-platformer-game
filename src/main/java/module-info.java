module platformgame {
    requires javafx.base;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;


    opens platformgame to javafx.fxml;
    exports platformgame;
}