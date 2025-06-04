package platformgame;

import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import platformgame.Objects.SuperObject;

import java.util.Set;

public class Player {
    private double x, y;
    private final double width;
    private final double height;
    private double speed;

    private Image sprite;
    private final int frameWidth = 128;
    private final int frameHeight = 128;
    private final int spriteWidth = 50;   // Actual visible width of sprite
    private final int spriteHeight = 40;  // Actual visible height of sprite
    private final int offsetX = (frameWidth - spriteWidth) / 2;
    private final int offsetY = (frameHeight - spriteHeight); // Bottom align

    private int currentFrame = 0;
    private final int totalFrames_walk = 10;
    private int currentRow = 0;
    private boolean facingRight = true;

    private WritableImage[] frames;

    private long lastFrameTime = 0;

    public Player(double x, double y, double width, double height, double speed) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.speed = speed;

        imageSet(totalFrames_walk);
    }

    public void update(Set<KeyCode> keys, TileMap tileMap, Game game, long now) {
        boolean moved = false;

        double newX = x;
        double newY = y;

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

        if (moved) {
            currentRow = 3;
            if (now - lastFrameTime > 100_000_000) {
                nextFrame();
                lastFrameTime = now;
            }
        } else {
            currentFrame = 0;
            currentRow = 0;
        }
    }

    public void draw(GraphicsContext gc, double camX, double camY, double scale) {
        int xIndex = currentFrame * frameWidth;
        int yIndex = currentRow * frameHeight;

        double drawX = (x - camX) * scale - offsetX * scale;
        double drawY = (y - camY) * scale - offsetY * scale;
        double drawW = frameWidth * scale;
        double drawH = frameHeight * scale;

        if (facingRight) {
            gc.drawImage(sprite, xIndex, yIndex, frameWidth, frameHeight,
                    drawX, drawY, drawW, drawH);
        } else {
            gc.save();
            gc.translate(drawX + drawW, drawY);
            gc.scale(-1, 1);
            gc.drawImage(sprite, xIndex, yIndex, frameWidth, frameHeight,
                    0, 0, drawW, drawH);
            gc.restore();
        }
    }

    private void imageSet(int n) {
        sprite = new Image(getClass().getResourceAsStream("/image/main_character.png"));
        frames = new WritableImage[n];

        for (int i = 0; i < n; i++) {
            frames[i] = new WritableImage(sprite.getPixelReader(), i * frameWidth, 0, frameWidth, frameHeight);
        }
    }

    public void nextFrame() {
        currentFrame = (currentFrame + 1) % totalFrames_walk;
    }

    public void setFrame(int frameIndex, int row) {
        currentFrame = frameIndex % totalFrames_walk;
    }




    // adding collusion for object and logic for collecting and others (Borshon)
    public boolean checkObjectCollisionsAndInteract(double nextX, double nextY, double width, double height, Game game) {
      Rectangle2D playerRect = new Rectangle2D(nextX, nextY, width, height);

        for (int i = 0; i < game.object.length; i++) {
            SuperObject obj = game.object[i];
            if (obj != null) {
                javafx.geometry.Rectangle2D objRect = obj.getBoundingBox();

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

                            speed+=2;
                            game.object[i]=null;
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
        return false;  // no blocking collision found
    }


    public double getX() { return x; }
    public double getY() { return y; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
}
