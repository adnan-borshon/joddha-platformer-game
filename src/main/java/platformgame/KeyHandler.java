package platformgame;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import platformgame.Entity.Npc;
import platformgame.chat.OptimizedChatUI;

import java.util.HashSet;
import java.util.Set;

/**
 * Centralized key handling system for the game
 * Manages priority between chat system and game controls
 */
public class KeyHandler {
    private final Game game;
    private final Set<KeyCode> keysPressed = new HashSet<>();
    private OptimizedChatUI chatUI;

    public KeyHandler(Game game) {
        this.game = game;
    }

    public void setChatUI(OptimizedChatUI chatUI) {
        this.chatUI = chatUI;
    }

    /**
     * Main key pressed handler - determines priority order
     */
    public void handleKeyPressed(KeyEvent e) {
        KeyCode key = e.getCode();

        // ✅ PRIORITY 1: Chat system gets first chance to handle keys
        if (chatUI != null && chatUI.handleKeyEvent(key)) {
            e.consume();
            return;
        }

        // ✅ PRIORITY 2: Global game shortcuts (work in any state)
        if (handleGlobalKeys(key, e)) {
            return;
        }

        // ✅ PRIORITY 3: State-specific game keys
        if (handleGameStateKeys(key, e)) {
            return;
        }

        // ✅ PRIORITY 4: Movement and action keys (only in play state)
        if (game.GameState == game.playState) {
            handleGameplayKeys(key, e);
        }
    }

    /**
     * Main key released handler
     */
    public void handleKeyReleased(KeyEvent e) {
        KeyCode key = e.getCode();

        // Don't remove keys from pressed set if chat is handling input
        if (chatUI != null && chatUI.isChatInputFocused()) {
            return;
        }

        keysPressed.remove(key);
    }

    /**
     * Handle global keys that work in any game state
     */
    private boolean handleGlobalKeys(KeyCode key, KeyEvent e) {
        switch (key) {
            case T:
                // Toggle chat - only in play state
                if (game.GameState == game.playState && chatUI != null) {
                    chatUI.toggleChat();
                    e.consume();
                    return true;
                }
                break;

            case ESCAPE:
                // Handle escape based on current state
                if (game.GameState == game.playState) {
                    GameManager.getInstance().saveState(game);
                    game.openMainMenu();
                    game.GameState = game.pauseState;
                    e.consume();
                    return true;
                } else if (chatUI != null && chatUI.isVisible()) {
                    chatUI.toggleChat();
                    e.consume();
                    return true;
                }
                break;
        }
        return false;
    }

    /**
     * Handle keys specific to different game states
     */
    private boolean handleGameStateKeys(KeyCode key, KeyEvent e) {
        switch (game.GameState) {
            case 4: // gameOverState
                if (key == KeyCode.ENTER) {
                    game.openMainMenu();
                    e.consume();
                    return true;
                }
                break;

            case 3: // dialogueState
                if (key == KeyCode.ENTER) {
                    handleDialogueKeys();
                    e.consume();
                    return true;
                }
                break;
        }

        // Handle mission completed state
        if (game.missionCompleted && game.ui.isImageDialogue && key == KeyCode.ENTER) {
            Sound.getInstance().stop(7);
            game.GameState = game.playState;
            Sound.getInstance().loop(0);
            e.consume();
            return true;
        }

        return false;
    }

    /**
     * Handle gameplay keys (movement, actions, etc.)
     */
    private void handleGameplayKeys(KeyCode key, KeyEvent e) {
        // Add to pressed keys for continuous actions (movement, etc.)
        keysPressed.add(key);

        // Handle immediate action keys
        switch (key) {
            case I:{
                game.inventory.toggleVisibility();
                e.consume();
               break;
            }
            case PLUS:
            case EQUALS:
                // Debug: Add ammo (remove in production)
                game.onAmmoCollected(10);
                e.consume();
                break;

            case MINUS:
                // Debug: Remove ammo (remove in production)
                game.onAmmoUsed(5);
                e.consume();
                break;

            // Add other immediate action keys here
        }
    }

    /**
     * Handle dialogue-specific actions
     */
    private void handleDialogueKeys() {
        if (game.eventHandler.isShowingBridgePopup()) {
            game.eventHandler.triggerBridgeExplosion(game, System.nanoTime());
            return;
        }

        if (game.hasKey1 && !game.LeftWallRemoved) {
            game.level1.removeLeftWallLayer();
            game.LeftWallRemoved = true;
            game.inventory.useItem("key",1);
            game.playSoundEffects(2);
            game.ui.showMessage("Left wall opened! The villagers are free!");
        }

        if (game.hasKey3 && !game.RightWallRemoved) {
            game.level1.removeRightWallLayer();
            game.RightWallRemoved = true;
            game.inventory.useItem("key",1);
            game.playSoundEffects(2);
            game.ui.showMessage("Right wall opened! The villagers are free!");
        }

        if (game.hasLauncher && !game.ContainerGateRemoved) {
            game.granadeCounter--;
            game.inventory.useItem("granade",1);
            long now = System.nanoTime();
            game.eventHandler.triggerContainerGateExplosion(game, now);
            game.level1.removeContainerGateLayer();
            game.ContainerGateRemoved = true;
            game.playSoundEffects(2);
            game.ui.showMessage("Container gate destroyed");
        }

        if (game.boomCollected && !game.bridgeDestroyed) {
            long now = System.nanoTime();
            game.inventory.useItem("granade",1);
            game.eventHandler.triggerBridgeExplosion(game, now);
            game.level1.removeBridgeLayer();
            game.level1.removeBridgeBackLayer();
            game.bridgeDestroyed = true;
            game.ui.showMessage("Bridge has been destroyed");
        }

        // Exit dialogue state
        game.GameState = game.playState;
        game.ui.dialogue = "";
        game.ui.narrator = null;
        game.ui.isImageDialogue = false;

        for (Npc n : game.npc) {
            if (n != null && n.playerIsTouching) {
                n.speak();
                break;
            }
        }
    }

    /**
     * Get the current set of pressed keys
     */
    public Set<KeyCode> getKeysPressed() {
        return new HashSet<>(keysPressed);
    }

    /**
     * Check if a specific key is currently pressed
     */
    public boolean isKeyPressed(KeyCode key) {
        return keysPressed.contains(key);
    }

    /**
     * Clear all pressed keys (useful for state transitions)
     */
    public void clearPressedKeys() {
        keysPressed.clear();
    }
}