package platformgame.Entity;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import platformgame.Game;
import platformgame.Objects.SuperObject;
import platformgame.TileMap;

import java.util.Set;

public class Player extends Entity {
    private final int totalFrames_walk = 10;

    public Player(double x, double y, double width, double height, double speed, Game gp) {
        super(x, y, width, height, speed, gp);

        // Load player sprite and frames
        imageSet(totalFrames_walk, "/image/main_character.png");
    }

    public void update(Set<KeyCode> keys, TileMap tileMap, Game game, long now, long deltaTime) {
        boolean moved = false;
        double newX = x;
        double newY = y;

        // WASD input handling
        if (keys.contains(KeyCode.W)) { newY -= speed; moved = true; }
        if (keys.contains(KeyCode.S)) { newY += speed; moved = true; }
        if (keys.contains(KeyCode.A)) {
            newX -= speed;
            moved = true;
            facingRight = false;
        }
        if (keys.contains(KeyCode.D)) {
            newX += speed;
            moved = true;
            facingRight = true;
        }

        // Toggle between play and pause states
        if(keys.contains(KeyCode.ESCAPE)){
            if(game.GameState == game.playState){
                game.GameState = game.pauseState;
            }
            else if(game.GameState == game.pauseState){
                game.GameState = game.playState;
            }
        }

        // Tile collision check
        boolean canMoveX = !tileMap.isColliding(newX, y, width, height);
        boolean canMoveY = !tileMap.isColliding(x, newY, width, height);

        // Object collision & interaction check using Game reference
        boolean collidesX = checkObjectCollisionsAndInteract(newX, y, width, height, game);
        boolean collidesY = checkObjectCollisionsAndInteract(x, newY, width, height, game);

        if (canMoveX && !collidesX) {
            x = newX;
        }
        if (canMoveY && !collidesY) {
            y = newY;
        }

        // Animate walking
        if (moved) {
            currentRow = 3;
            animationTimer += deltaTime; // ✅ accumulate time
            if (animationTimer > 100_000_000) {
                nextFrame(totalFrames_walk);
                animationTimer = 0;
            }
        } else {
            currentFrame = 0;
            currentRow = 0;
        }
    }

    public void draw(GraphicsContext gc, double camX, double camY, double scale) {
        drawEntity(gc, camX, camY, scale);
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
