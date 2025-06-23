package platformgame.Entity;

import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import platformgame.Game;
import platformgame.Map.Level_1;  // Import Level_1 class
import platformgame.Objects.SuperObject;

import java.util.Set;

public class Player extends Entity {
    private final int totalFrames_walk = 10;

    public Player(double x, double y, double width, double height, double speed, Game gp) {
        super(x, y, width, height, speed, gp);

        // Load player sprite and frames
        imageSet(totalFrames_walk, "/image/main_character.png");
    }

// Replace the update method in your Player.java with this:

    // Inside the Player class
    public void update(Set<KeyCode> keys, Level_1 level1, Game game, long now, long deltaTime) {
        boolean moved = false;
        double newX = x;
        double newY = y;

        // Check if the game is in dialogue state or pause state and do not allow movement
        if (game.GameState == game.playState) {
            // WASD input handling with collision checking
            if (keys.contains(KeyCode.W)) {
                double testY = y - speed;
                // Check for level collision, object collision, and NPC collision
                if (!level1.isCollisionRect(x, testY, width, height) &&
                        !checkObjectCollisionsAndInteract(x, testY, width, height, game) &&
                        !checkNpcCollision(x, testY, game)) { // Check NPC collision
                    newY = testY;
                    moved = true;
                }
            }
            if (keys.contains(KeyCode.S)) {
                double testY = y + speed;
                // Check for level collision, object collision, and NPC collision
                if (!level1.isCollisionRect(x, testY, width, height) &&
                        !checkObjectCollisionsAndInteract(x, testY, width, height, game) &&
                        !checkNpcCollision(x, testY, game)) { // Check NPC collision
                    newY = testY;
                    moved = true;
                }
            }
            if (keys.contains(KeyCode.A)) {
                double testX = x - speed;
                // Check for level collision, object collision, and NPC collision
                if (!level1.isCollisionRect(testX, y, width, height) &&
                        !checkObjectCollisionsAndInteract(testX, y, width, height, game) &&
                        !checkNpcCollision(testX, y, game)) { // Check NPC collision
                    newX = testX;
                    moved = true;
                    facingRight = false;
                }
            }
            if (keys.contains(KeyCode.D)) {
                double testX = x + speed;
                // Check for level collision, object collision, and NPC collision
                if (!level1.isCollisionRect(testX, y, width, height) &&
                        !checkObjectCollisionsAndInteract(testX, y, width, height, game) &&
                        !checkNpcCollision(testX, y, game)) { // Check NPC collision
                    newX = testX;
                    moved = true;
                    facingRight = true;
                }
            }
        }

        // Check if the player is touching an NPC
        for (Npc npcEntity : game.npc) {
            if (npcEntity != null && npcEntity.playerIsTouching) {
                // Trigger dialogue state
                game.GameState = game.dialogueState;  // Switch to dialogue state
                npcEntity.speak();  // Show NPC dialogue
                break;  // Exit after triggering dialogue for the first NPC touched
            }
        }

        // Apply movement only if no collision
        x = newX;
        y = newY;

        // Animate walking
        if (moved) {
            currentRow = 3;
            animationTimer += deltaTime;
            if (animationTimer > 100_000_000) {
                nextFrame(totalFrames_walk);
                animationTimer = 0;
            }
        } else {
            currentFrame = 0;
            currentRow = 0;
        }
    }




    // Add this new method to check NPC collision
    // Inside checkNpcCollision method in Player class
// Inside the Player class
    private boolean checkNpcCollision(double playerX, double playerY, Game game) {
        Rectangle2D playerRect = new Rectangle2D(playerX, playerY, width, height);

        for (Npc npcEntity : game.npc) {
            if (npcEntity != null) {
                Rectangle2D npcRect = new Rectangle2D(npcEntity.getX(), npcEntity.getY(),
                        npcEntity.getWidth(), npcEntity.getHeight());

                // Check for intersection (collision)
                if (playerRect.intersects(npcRect)) {
                    npcEntity.notifyPlayerCollision(); // Notify NPC about collision
                    return true;  // Collision detected, return true to block movement
                }
            }
        }

        return false;  // No collision, return false to allow movement
    }




    public void draw(GraphicsContext gc, double camX, double camY, double scale) {
        drawEntity(gc, camX, camY, scale);

        // Debugging
        gc.save();
        gc.setLineWidth(1);
        gc.setStroke(javafx.scene.paint.Color.GREEN);

        double drawX = (x - camX) * scale;
        double drawY = (y - camY) * scale;
        double drawW = width * scale;
        double drawH = height * scale;

        gc.strokeRect(drawX, drawY, drawW, drawH);
        gc.restore();
    }

    // Adding collision for object and logic for collecting and others (Borshon)
    public boolean checkObjectCollisionsAndInteract(double nextX, double nextY, double width, double height, Game game) {
        Rectangle2D playerRect = new Rectangle2D(nextX, nextY, width, height);

        for (int i = 0; i < game.object.length; i++) {
            SuperObject obj = game.object[i];
            if (obj != null) {
                double dx = Math.abs(obj.worldX - nextX);
                double dy = Math.abs(obj.worldY - nextY);

                if (dx < 128 && dy < 128) { // ✅ OPTIMIZED: only check nearby objects
                    Rectangle2D objRect = obj.getBoundingBox();
                    if (playerRect.intersects(objRect)) {
                        switch (obj.name.toLowerCase()) {
                            case "key":
                                game.hasKey++;
                                game.object[i] = null;  // remove the key (disappear)
                                game.playSoundEffects(1);
                                game.ui.showMessage("You got a key");
                                // Don't block movement for keys, so continue loop
                                break;

                            case "door":
                                if (game.hasKey > 0) {
                                    game.hasKey--;
                                    game.object[i] = null;  // open door (disappear)
                                    game.playSoundEffects(3);
                                    game.ui.showMessage("Door has opened");
                                    // Allow player to move through door this time
                                } else {
                                    // No key to open door, block movement
                                    game.ui.showMessage("You need a key to open");
                                    return true;
                                }
                                break;

                            case "boots":
                                speed += 2;
                                game.object[i] = null;
                                game.playSoundEffects(2);
                                game.ui.showMessage("You got speed up +2");
                                break;

                            default:
                                if (obj.collision) {
                                    // Any other collidable object blocks movement
                                    return true;
                                }
                                break;
                        }
                    }
                }
            }
        }

        return false;  // no blocking collision found
    }
}
