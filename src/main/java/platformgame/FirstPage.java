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
import java.util.Objects;

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
        buttons[0] = newGameButton;
        buttons[1] = continueButton;
        buttons[2] = settingsButton;
        buttons[3] = exitButton;

        updateButtonImages();

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
                System.out.println("Launching New Game...");
                try {
                    Stage currentStage = (Stage) rootPane.getScene().getWindow(); // Close current menu
                    Game game = new Game();
                    Scene scene = new Scene(game);
                    Stage stage = new Stage();
                    stage.setTitle("JavaFX Game - Joddha");
                    stage.setScene(scene);
                    stage.setResizable(false);
                    stage.setFullScreen(false);
                    stage.show();

                    //set up objects before the game start (borshon)
                    game.setUpObject();
                    game.startGameLoop();
                    game.requestFocus();
                    currentStage.close(); // Close the previous window
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            case 1 -> System.out.println("Continue logic here");
            case 2 -> System.out.println("Settings logic here");
            case 3 -> {
                System.out.println("Exit logic here");
                Platform.exit(); // Optional: close the app
            }
        }
    }
}
