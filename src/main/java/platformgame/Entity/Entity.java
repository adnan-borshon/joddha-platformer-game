package platformgame.Entity;

import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import platformgame.Game;
import platformgame.Objects.SuperObject;

// Parent class for all entities like Player, NPC, Enemy
public abstract class Entity {
    protected double x, y;
    protected double width, height;
    public double speed;
    Game gp;
    protected Image sprite;
    protected WritableImage[] frames;
    protected final int frameWidth = 128;
    protected final int frameHeight = 128;
    protected final int spriteWidth = 50;   // Actual visible width of sprite
    protected final int spriteHeight = 40;  // Actual visible height of sprite
    protected final int offsetX = (frameWidth - spriteWidth) / 2;
    protected final int offsetY = (frameHeight - spriteHeight); // Bottom align

    //Sprite frame
    protected int currentFrame = 0;
    protected int currentRow = 0;
    protected boolean facingRight = true;

    protected long animationTimer = 0; // to accumulate time

    protected int actionCounter = 0; //movement of Entity subclasses

    String dialogues[] = new String[10];

    // Constructor for initializing common entity attributes
    public Entity(double x, double y, double width, double height, double speed, Game gp) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.speed = speed;
        this.gp = gp;
    }

    // Method for saving the position of the entity
    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    // Original drawEntity method using the entity's facingRight property
    protected void drawEntity(GraphicsContext gc, double camX, double camY, double scale) {
        drawEntity(gc, camX, camY, scale, !facingRight);
    }

    // Enhanced drawEntity method with explicit flip control
    protected void drawEntity(GraphicsContext gc, double camX, double camY, double scale, boolean flip) {
        int xIndex = currentFrame * frameWidth;
        int yIndex = currentRow * frameHeight;

        double drawX = (x - camX) * scale - offsetX * scale;
        double drawY = (y - camY) * scale - offsetY * scale;
        double drawW = frameWidth * scale;
        double drawH = frameHeight * scale;

        if (!flip) {
            // Normal drawing (right-facing or no flip needed)
            gc.drawImage(sprite, xIndex, yIndex, frameWidth, frameHeight,
                    drawX, drawY, drawW, drawH);
        } else {
            // Flipped drawing (left-facing)
            gc.save();
            gc.translate(drawX + drawW, drawY);
            gc.scale(-1, 1);
            gc.drawImage(sprite, xIndex, yIndex, frameWidth, frameHeight,
                    0, 0, drawW, drawH);
            gc.restore();
        }
    }

    // Loads sprite image and extracts animation frames
    protected void imageSet(int n, String spritePath) {
        sprite = new Image(getClass().getResourceAsStream(spritePath));
        frames = new WritableImage[n];

        for (int i = 0; i < n; i++) {
            frames[i] = new WritableImage(sprite.getPixelReader(), i * frameWidth, 0, frameWidth, frameHeight);
        }
    }

    // Advance to next animation frame (looped)
    public void nextFrame(int totalFrames) {
        currentFrame = (currentFrame + 1) % totalFrames;
    }

    public void speak() {
        // Implementation in subclasses
    }

    // Set frame manually
    public void setFrame(int frameIndex, int totalFrames) {
        currentFrame = frameIndex % totalFrames;
    }

    // Checks if the entity is behind the player
    public boolean isBehindPlayer(Game game) {
        if (game == null || game.player == null) {
            return false; // Default to front if no player reference
        }

        // Compare bottom positions for more accurate depth sorting
        double thisBottom = this.y + this.height;
        double playerBottom = game.player.getY() + game.player.getHeight();

        // Entity is behind player if its bottom is higher up (smaller Y value)
        return thisBottom < playerBottom;
    }

    // Collision checking for entities against other entities, tiles, and objects
    public boolean checkTileCollision(double x, double y) {
        return gp.level1.isCollisionRect(x, y, width, height);
    }

    public boolean checkNpcCollision(double x, double y) {
        for (Npc npcEntity : gp.npc) {
            if (npcEntity != null && npcEntity != this) {
                Rectangle2D npcRect = new Rectangle2D(npcEntity.getX(), npcEntity.getY(), npcEntity.getWidth(), npcEntity.getHeight());
                Rectangle2D entityRect = new Rectangle2D(x, y, width, height);
                if (entityRect.intersects(npcRect)) {
                    return true; // Collision detected
                }
            }
        }
        return false;  // No collision
    }

    public boolean checkObjectCollision(double x, double y) {
        for (SuperObject obj : gp.object) {
            if (obj != null) {
                Rectangle2D objRect = obj.getBoundingBox();
                Rectangle2D entityRect = new Rectangle2D(x, y, width, height);
                if (entityRect.intersects(objRect)) {
                    return true; // Collision detected
                }
            }
        }
        return false;  // No collision
    }

    // General method to check for collisions with the environment
    public boolean isColliding(double x, double y) {
        return checkTileCollision(x, y) || checkObjectCollision(x, y) || checkNpcCollision(x, y);
    }

    // Getter methods
    public double getX() { return x; }
    public double getY() { return y; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }

}