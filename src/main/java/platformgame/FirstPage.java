package platformgame;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import platformgame.Objects.Obj_Boots;
import platformgame.Objects.Obj_Door;
import platformgame.Objects.Obj_Key;
import platformgame.Objects.SuperObject;

import java.io.IOException;
import java.io.InputStream;

public class FirstPage {

    @FXML private AnchorPane rootPane;
    @FXML private Button newGameButton, continueButton, settingsButton, exitButton;

    private final String[] normalPaths = {
            "/image/00_UI/Buttons/NG.png",
            "/image/00_UI/Buttons/Continue.png",
            "/image/00_UI/Buttons/Settings.png",
            "/image/00_UI/Buttons/Exit.png"
    };
    private final String[] selectedPaths = {
            "/image/00_UI/Buttons/NG Click.png",
            "/image/00_UI/Buttons/Continue Click.png",
            "/image/00_UI/Buttons/Settings Click.png",
            "/image/00_UI/Buttons/Exit Click.png"
    };
    private static final int MENU_INDEX  = 6;
    private static final int CLICK_INDEX = 5;
    private final Button[] buttons = new Button[4];
    private int focusedIndex = 0;

    @FXML
    public void initialize() {
        // 1) pull the last‐saved preference
        boolean shouldPlay = GameManager.getInstance().isMusicEnabled();
        // 2) push it into your Sound singleton
        Sound.getInstance().setMusicEnabled(shouldPlay);

        // 3) now stop everything, then loop only if enabled
        Sound.getInstance().stopAll();
        if (shouldPlay) {
            Sound.getInstance().loop(MENU_INDEX);
        }

        buttons[0] = newGameButton;
        buttons[1] = continueButton;
        buttons[2] = settingsButton;
        buttons[3] = exitButton;

        updateButtonImages();

        if (!GameManager.getInstance().hasSavedState()) {
            continueButton.setDisable(true);
        }

        Platform.runLater(() -> {
            rootPane.requestFocus();
            rootPane.setOnKeyPressed(this::handleKeyPress);
        });
    }



    private void updateButtonImages() {
        for (int i = 0; i < buttons.length; i++) {
            String path = (i == focusedIndex) ? selectedPaths[i] : normalPaths[i];
            try (InputStream is = getClass().getResourceAsStream(path)) {
                if (is != null && buttons[i].getGraphic() instanceof ImageView iv) {
                    iv.setImage(new Image(is));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        rootPane.requestFocus();
    }

    private void handleKeyPress(KeyEvent event) {
        if (event.getCode() == KeyCode.DOWN || event.getCode() == KeyCode.UP) {
            // play click on navigation
            Sound.getInstance().play(CLICK_INDEX);
            focusedIndex = (event.getCode() == KeyCode.DOWN)
                    ? (focusedIndex + 1) % buttons.length
                    : (focusedIndex - 1 + buttons.length) % buttons.length;
            updateButtonImages();
        } else if (event.getCode() == KeyCode.ENTER) {
            // play click on confirm
            Sound.getInstance().play(CLICK_INDEX);
            handleEnter();
        }
    }

    private void handleEnter() {
        switch (focusedIndex) {
            case 0 -> startNewGame();
            case 1 -> continueGame();
            case 2 -> openSettings();
            case 3 -> {
                Sound.getInstance().play(CLICK_INDEX);
                Platform.exit();
            }
        }
    }

    private void startNewGame() {
        try {
            // stop menu music
            Sound.getInstance().stopAll();
            Sound.getInstance().setMusicEnabled(
                    GameManager.getInstance().isMusicEnabled()
            ); // kill menu music & effects
            if (Sound.getInstance().isMusicEnabled()) {
                Sound.getInstance().loop(0); // start game BGM only if enabled
            }
            Game game = new Game();
//            Game_2 game = new Game_2();
            game.startGameLoop();
            Scene scene = rootPane.getScene();
            scene.setRoot(game);
            game.requestFocus();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void continueGame() {
        if (!GameManager.getInstance().hasSavedState()) return;
        try {
            // stop menu music
            Sound.getInstance().stop(1);

            Game game = new Game();
            GameState state = GameManager.getInstance().getLastState();
            // ... restore player & objects ...

            game.player.setPosition(state.playerX, state.playerY);
            game.hasKey = state.hasKey;
            game.player.speed=state.playerSpeed; //  Restore speed
            // Restore saved objects

            for (int i = 0; i < state.savedObjects.length; i++) {
                GameState.SavedObject saved = state.savedObjects[i];
                if (!saved.exists) {
                    game.object[i] = null;
                    continue;
                }

                SuperObject obj = createObjectByName(saved.type);
                if (obj != null) {
                    obj.worldX = saved.worldX;
                    obj.worldY = saved.worldY;
                    game.object[i] = obj;
                }
            }
            game.startGameLoop();
            Scene scene = rootPane.getScene();
            scene.setRoot(game);

            // restore in-game music state
            if (state.musicOn) {
                Sound.getInstance().loop(0);
            } else {
                Sound.getInstance().stop(0);
            }

            game.requestFocus();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openSettings() {
        Sound.getInstance().play(CLICK_INDEX);
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/Settings.fxml")
            );
            AnchorPane settingsRoot = loader.load();
            Scene scene = rootPane.getScene();
            scene.setRoot(settingsRoot);
            settingsRoot.requestFocus();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private SuperObject createObjectByName(String name) {
        return switch (name) {
            case "Key"   -> new Obj_Key();
            case "Door"  -> new Obj_Door();
            case "Boots" -> new Obj_Boots();
            default      -> null;
        };
    }
}
