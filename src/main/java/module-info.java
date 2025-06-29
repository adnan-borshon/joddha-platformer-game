module platformgame {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires javafx.graphics;
    requires java.xml;

    opens platformgame to javafx.fxml;
    exports platformgame;
    exports platformgame.Entity;
    opens platformgame.Entity to javafx.fxml;

    exports platformgame.Map;
    opens platformgame.Map to javafx.fxml;
    exports platformgame.Tanks;
    opens platformgame.Tanks to javafx.fxml;
}
