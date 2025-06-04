package platformgame.Entity;

import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import platformgame.Game;

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

    protected int currentFrame = 0;
    protected int currentRow = 0;
    protected boolean facingRight = true;

    protected long animationTimer = 0; // to accumulate time

    protected  int actionCounter=0; //movement of Entity subclasses

    // Constructor for initializing common entity attributes
    public Entity(double x, double y, double width, double height, double speed, Game gp) {

        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.speed = speed;
        this.gp=gp;
    }

    // Method for saving the position of the entity
    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    // Draw the entity with optional horizontal flipping
    protected void drawEntity(GraphicsContext gc, double camX, double camY, double scale) {
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

    // Set frame manually
    public void setFrame(int frameIndex, int totalFrames) {
        currentFrame = frameIndex % totalFrames;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
}
