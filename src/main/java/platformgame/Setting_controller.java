package platformgame;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;

import java.io.IOException;

public class Setting_controller {

    @FXML private AnchorPane rootPane;
    @FXML private ImageView musicImage;

    // menu index in your Sound array
    private static final int MENU_INDEX  = 6;
    private static final int CLICK_INDEX = 5;

    // the two icons
    private final String ON_PATH  = "/image/00_UI/Buttons/music_on.png";
    private final String OFF_PATH = "/image/00_UI/Buttons/music_off.png";

    // tracks the current state
    private boolean musicOn;

    @FXML
    public void initialize() {
        // 1) read the persisted global flag
        musicOn = Sound.getInstance().isMusicEnabled();

        // 2) set the icon immediately
        updateIcon();

        // 3) start or stop the menu music exactly once
        if (musicOn) {
            Sound.getInstance().loop(MENU_INDEX);
        } else {
            Sound.getInstance().stop(MENU_INDEX);
        }

        // 4) grab key events
        Platform.runLater(() -> rootPane.requestFocus());
        rootPane.setOnKeyPressed(evt -> {
            var code = evt.getCode();
            if (code == KeyCode.ESCAPE) {
                // play a click effect if you like
                Sound.getInstance().play(CLICK_INDEX);
                closeSettings();
            }
            else if (code == KeyCode.ENTER) {
                // flip the flag
                Sound.getInstance().play(CLICK_INDEX);
                musicOn = !musicOn;
                Sound.getInstance().setMusicEnabled(musicOn);

                // start/stop the track immediately
                if (musicOn) Sound.getInstance().loop(MENU_INDEX);
                else         Sound.getInstance().stop(MENU_INDEX);

                updateIcon();
            }
        });
    }

    private void updateIcon() {
        String path = musicOn ? ON_PATH : OFF_PATH;
        musicImage.setImage(new Image(getClass().getResourceAsStream(path)));
    }

    private void closeSettings() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/FirstPage.fxml")
            );
            AnchorPane firstRoot = loader.load();
            Scene scene = rootPane.getScene();
            scene.setRoot(firstRoot);
            firstRoot.requestFocus();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
