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

    //For explosion
    private boolean reactingToExplosion = false;
    private long explosionReactionStartTime = 0;
    private final int explosionReactionFrames = 4;
    private final int explosionReactionRow = 8; // 9th row (0-indexed)
    private final long explosionFrameDuration = 120_000_000; // 120ms per frame


    // ✅ Health & Ammo System
    public int hp = 10;
    public int maxHp = 10;
    public int ammo = 0;

    public Player(double x, double y, double width, double height, double speed, Game gp) {
        super(x, y, width, height, speed, gp);

        // Load player sprite and frames
        imageSet(totalFrames_walk, "/image/main_character.png");
    }

// Replace the update method in your Player.java with this:

    public void update(Set<KeyCode> keys, Level_1 level1, Game game, long now, long deltaTime) {

        // Explosion reaction takes priority
        if (reactingToExplosion) {
            currentRow = explosionReactionRow;
            int frameIndex = (int) ((now - explosionReactionStartTime) / explosionFrameDuration);

            if (frameIndex < explosionReactionFrames) {
                currentFrame = frameIndex;
            } else {
                currentFrame = 0;
                currentRow = 0; // Idle
                reactingToExplosion = false;
            }
            return; // Skip input/movement during explosion reaction
        }

        boolean moved = false;
        double newX = x;
        double newY = y;

        if (game.GameState == game.playState) {
            if (keys.contains(KeyCode.W)) {
                double testY = y - speed;
                if (!level1.isCollisionRect(x, testY, width, height) &&
                        !checkObjectCollisionsAndInteract(x, testY, width, height, game) &&
                        !checkNpcCollision(x, testY, game)) {
                    newY = testY;
                    moved = true;
                }
            }
            if (keys.contains(KeyCode.S)) {
                double testY = y + speed;
                if (!level1.isCollisionRect(x, testY, width, height) &&
                        !checkObjectCollisionsAndInteract(x, testY, width, height, game) &&
                        !checkNpcCollision(x, testY, game)) {
                    newY = testY;
                    moved = true;
                }
            }
            if (keys.contains(KeyCode.A)) {
                double testX = x - speed;
                if (!level1.isCollisionRect(testX, y, width, height) &&
                        !checkObjectCollisionsAndInteract(testX, y, width, height, game) &&
                        !checkNpcCollision(testX, y, game)) {
                    newX = testX;
                    moved = true;
                    facingRight = false;
                }
            }
            if (keys.contains(KeyCode.D)) {
                double testX = x + speed;
                if (!level1.isCollisionRect(testX, y, width, height) &&
                        !checkObjectCollisionsAndInteract(testX, y, width, height, game) &&
                        !checkNpcCollision(testX, y, game)) {
                    newX = testX;
                    moved = true;
                    facingRight = true;
                }
            }
        }

        for (Npc npcEntity : game.npc) {
            if (npcEntity != null && npcEntity.playerIsTouching) {
                game.GameState = game.dialogueState;
                npcEntity.speak();
                break;
            }
        }

        x = newX;
        y = newY;

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

        // Check collision with NPCs
        for (Npc npcEntity : game.npc) {
            if (npcEntity != null) {
                Rectangle2D npcRect = new Rectangle2D(npcEntity.getX(), npcEntity.getY(), npcEntity.getWidth(), npcEntity.getHeight());
                if (playerRect.intersects(npcRect)) {
                    npcEntity.notifyPlayerCollision(); // Notify NPC about collision
                    return true;  // Collision detected, return true to block movement
                }
            }
        }

        // Check collision with Scouts
        for (Scout scoutEntity : game.scout) {
            if (scoutEntity != null) {
                Rectangle2D scoutRect = new Rectangle2D(scoutEntity.getX(), scoutEntity.getY(), scoutEntity.getWidth(), scoutEntity.getHeight());
                if (playerRect.intersects(scoutRect)) {
                    scoutEntity.setPlayerInRange(true); // Notify Scout about collision with Player
                    return true;  // Collision detected, return true to block movement
                }
            }
        }

        return false;  // No collision, return false to allow movement
    }


    //for explision
    public void triggerExplosionReaction(long now) {
        reactingToExplosion = true;
        explosionReactionStartTime = now;
        currentFrame = 0;
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
                            case "ammo":
                                ammo += 10;
                                game.object[i] = null;
                                game.playSoundEffects(3); // Using door sound for ammo
                                game.ui.showMessage("Picked up 10 ammo");
                                break;

                            case "life":
                                if (hp < maxHp) {
                                    hp += maxHp * 0.1;
                                    if (hp > maxHp) hp = maxHp;
                                    game.object[i] = null;
                                    game.playSoundEffects(3);
                                    game.ui.showMessage("Life restored +10%");
                                } else {
                                    game.ui.showMessage("Health already full");
                                }
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
