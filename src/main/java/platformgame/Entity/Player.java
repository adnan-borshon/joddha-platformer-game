package platformgame.Entity;

import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import platformgame.Game;
import platformgame.Map.Level_1;
import platformgame.Objects.SuperObject;

import java.util.Set;

public class Player extends Entity {
    private final int totalFrames_walk = 10;

    // ✅ Explosion
    private boolean reactingToExplosion = false;
    private long explosionReactionStartTime = 0;
    private final int explosionReactionFrames = 4;
    private final int explosionReactionRow = 8;
    private final long explosionFrameDuration = 120_000_000;

    // ✅ Fist attack
    private boolean attackingWithFist = false;
    private long fistAttackStartTime = 0;
    private final int totalFistFrames = 5;
    private final int fistAttackRow = 6;
    private final long fistFrameDuration = 100_000_000;

    // ✅ Health & Ammo
    public int hp = 10;
    public int maxHp = 10;
    public int ammo = 0;

    public Player(double x, double y, double width, double height, double speed, Game gp) {
        super(x, y, width, height, speed, gp);
        imageSet(totalFrames_walk, "/image/main_character.png");
    }

    public void update(Set<KeyCode> keys, Level_1 level1, Game game, long now, long deltaTime) {

        // Explosion priority
        if (reactingToExplosion) {
            currentRow = explosionReactionRow;
            int frameIndex = (int) ((now - explosionReactionStartTime) / explosionFrameDuration);

            if (frameIndex < explosionReactionFrames) {
                currentFrame = frameIndex;
            } else {
                currentFrame = 0;
                currentRow = 0;
                reactingToExplosion = false;
            }
            return;
        }

        // Fist attack priority
        if (attackingWithFist) {
            currentRow = fistAttackRow;
            int frameIndex = (int) ((now - fistAttackStartTime) / fistFrameDuration);

            if (frameIndex < totalFistFrames) {
                currentFrame = frameIndex;
            } else {
                attackingWithFist = false;
                currentFrame = 0;
                currentRow = 0;
            }
            return;
        }

        // Start fist attack if 'F' is pressed
        if (keys.contains(KeyCode.F) && !attackingWithFist) {
            attackingWithFist = true;
            fistAttackStartTime = now;
            currentFrame = 0;
            currentRow = fistAttackRow;

            // Add punch hitbox
            Rectangle2D punchBox = facingRight
                    ? new Rectangle2D(x + width, y, width * 0.6, height)
                    : new Rectangle2D(x - width * 0.6, y, width * 0.6, height);

            // Check if punch hits any scout
            for (Scout scoutEntity : game.scout) {
                if (scoutEntity != null && punchBox.intersects(scoutEntity.getHitbox())) {
                    scoutEntity.takeDamage();
                }
            }
            return;
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

    private boolean checkNpcCollision(double playerX, double playerY, Game game) {
        Rectangle2D playerRect = new Rectangle2D(playerX, playerY, width, height);

        for (Npc npcEntity : game.npc) {
            if (npcEntity != null) {
                Rectangle2D npcRect = new Rectangle2D(npcEntity.getX(), npcEntity.getY(), npcEntity.getWidth(), npcEntity.getHeight());
                if (playerRect.intersects(npcRect)) {
                    npcEntity.notifyPlayerCollision();
                    return true;
                }
            }
        }

        for (Scout scoutEntity : game.scout) {
            if (scoutEntity != null) {
                Rectangle2D scoutRect = new Rectangle2D(scoutEntity.getX(), scoutEntity.getY(), scoutEntity.getWidth(), scoutEntity.getHeight());
                if (playerRect.intersects(scoutRect)) {
                    scoutEntity.setPlayerInRange(true);
                    return true;
                }
            }
        }

        return false;
    }

    public void triggerExplosionReaction(long now) {
        reactingToExplosion = true;
        explosionReactionStartTime = now;
        currentFrame = 0;
    }

    public void draw(GraphicsContext gc, double camX, double camY, double scale) {
        drawEntity(gc, camX, camY, scale);

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

    public boolean checkObjectCollisionsAndInteract(double nextX, double nextY, double width, double height, Game game) {
        Rectangle2D playerRect = new Rectangle2D(nextX, nextY, width, height);

        for (int i = 0; i < game.object.length; i++) {
            SuperObject obj = game.object[i];
            if (obj != null) {
                double dx = Math.abs(obj.worldX - nextX);
                double dy = Math.abs(obj.worldY - nextY);

                if (dx < 128 && dy < 128) {
                    Rectangle2D objRect = obj.getBoundingBox();
                    if (playerRect.intersects(objRect)) {
                        switch (obj.name.toLowerCase()) {
                            case "key":
                                game.hasKey++;
                                game.object[i] = null;
                                game.playSoundEffects(1);
                                game.ui.showMessage("You got a key");
                                break;

                            case "door":
                                if (game.hasKey > 0) {
                                    game.hasKey--;
                                    game.object[i] = null;
                                    game.playSoundEffects(3);
                                    game.ui.showMessage("Door has opened");
                                } else {
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
                                game.playSoundEffects(3);
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
                                break;

                            default:
                                if (obj.collision) {
                                    return true;
                                }
                                break;
                        }
                    }
                }
            }
        }

        return false;
    }
}
