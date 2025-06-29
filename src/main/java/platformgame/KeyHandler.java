package platformgame;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.util.HashSet;
import java.util.Set;

public class KeyHandler {
    private final Set<KeyCode> pressedKeys = new HashSet<>();
    private final Game game;

    public KeyHandler(Game game) {
        this.game = game;
    }

    public void onKeyPressed(KeyEvent e) {
        KeyCode key = e.getCode();
        pressedKeys.add(key);

        if (game.GameState == game.dialogueState && key == KeyCode.ENTER) {
            game.GameState = game.playState;

        }
    }

    public void onKeyReleased(KeyEvent e) {
        KeyCode key = e.getCode();
        pressedKeys.remove(key);
    }

    public Set<KeyCode> getPressedKeys() {
        return pressedKeys;
    }
}

