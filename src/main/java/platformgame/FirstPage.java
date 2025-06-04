package platformgame;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

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

    private final Button[] buttons = new Button[4];
    private int focusedIndex = 0;

    @FXML
    public void initialize() {
        Sound.getInstance().stopAll();

        buttons[0] = newGameButton;
        buttons[1] = continueButton;
        buttons[2] = settingsButton;
        buttons[3] = exitButton;

        updateButtonImages();

        if (!GameManager.getInstance().hasSavedState()) {
            continueButton.setDisable(true);  // disables the button visually
        }

        Platform.runLater(() -> {
            rootPane.requestFocus(); // Ensure pane receives key events
            rootPane.setOnKeyPressed(this::handleKeyPress);
        });
    }

    private void handleKeyPress(KeyEvent event) {
        if (event.getCode() == KeyCode.DOWN) {
            focusedIndex = (focusedIndex + 1) % buttons.length;
            updateButtonImages();
        } else if (event.getCode() == KeyCode.UP) {
            focusedIndex = (focusedIndex - 1 + buttons.length) % buttons.length;
            updateButtonImages();
        } else if (event.getCode() == KeyCode.ENTER) {
            handleEnter();
        }
    }

    private void updateButtonImages() {
        for (int i = 0; i < buttons.length; i++) {
            String path = (i == focusedIndex) ? selectedPaths[i] : normalPaths[i];
            InputStream imageStream = getClass().getResourceAsStream(path);

            if (imageStream == null) {
                System.err.println("Image not found: " + path);
                continue;
            }

            Image image = new Image(imageStream);

            // Get the existing ImageView from the Button's graphic and just change its image
            if (buttons[i].getGraphic() instanceof ImageView iv) {
                iv.setImage(image);
            } else {
                System.err.println("Graphic is not an ImageView for button index: " + i);
            }
        }

        rootPane.requestFocus();
    }


    private void handleEnter() {
        switch (focusedIndex) {
            case 0 -> {
                // New Game
                startNewGame();
            }
            case 1 -> { // Continue
                continueGame();
            }
            case 2 -> System.out.println("Settings logic here");
            case 3 -> {

                Platform.exit();
            }
        }
    }


    //
    private void startNewGame() {
        try {
            Game game = new Game();

            game.startGameLoop();

            Scene currentScene = rootPane.getScene();
            currentScene.setRoot(game); //  Reuse current scene
            game.requestFocus();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void continueGame() {
        if (!GameManager.getInstance().hasSavedState()) return;

        try {
            Game game = new Game();
            GameState state = GameManager.getInstance().getLastState();
            game.player.setPosition(state.playerX, state.playerY);
            game.hasKey = state.hasKey;

            game.startGameLoop();

            Scene currentScene = rootPane.getScene();
            currentScene.setRoot(game); // Resume in same window
            game.requestFocus();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



}
