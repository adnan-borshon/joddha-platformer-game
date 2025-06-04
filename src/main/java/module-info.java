module platformgame {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;


    opens platformgame to javafx.fxml;
    exports platformgame;
}