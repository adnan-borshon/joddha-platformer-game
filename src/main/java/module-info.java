module platformgame {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;


    opens platformgame to javafx.fxml;
    exports platformgame;
    exports platformgame.Entity;
    opens platformgame.Entity to javafx.fxml;
}