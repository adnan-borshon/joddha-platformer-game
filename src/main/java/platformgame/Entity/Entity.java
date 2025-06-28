package platformgame.Entity;

import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import platformgame.Game;
import platformgame.ImageLoader;
import platformgame.Objects.SuperObject;

public abstract class Entity {
    //without gun

    //walk and run
    protected final int frontWalkFrame=4;
    protected final int frontWalkRow=3;

    protected final int backWalkFrame=4;
    protected final int backWalkRow=4;

    protected final int WalkFrame=6;
    protected final int WalkRow=5;

    //idle
    protected final int frontIdleFrame=2;
    protected final int frontIdleRow=0;

    protected final int IdleFrame=2;
    protected final int IdleRow=1;

    protected final int backIdleFrame=2;
    protected final int backIdleRow=2;


    //fist
    protected final int FrontFistFrame=2;
    protected final int FrontFistRow=6;

    protected final int FistFrame=2;
    protected final int FistRow=7;

    protected final int BackFistFrame=2;
    protected final int BackFistRow=8;

    //hurt with no gun
    protected final int HitFrame=2;
    protected final int HitRow=19;

    protected final int FrontHitFrame=2;
    protected final int FrontHitRow=20;

    protected final int BackHitFrame=2;
    protected final int BackHitRow=21;

    //with gun
    //shoot
    protected final int FrontShootFrame=2;
    protected final int FrontShootRow=9;

    protected final int ShootFrame=2;
    protected final int ShootRow=10;
    protected final int BackShootFrame=2;
    protected final int BackShootRow=11;


    //dead
    protected final int deadFrame=3;
    protected final int deadRow=12;

    //walk and run with gun
    protected final int GunFrontWalkFrame=4;
    protected final int GunFrontWalkRow=14;

    protected final int GunBackWalkFrame=4;
    protected final int GunBackWalkRow=15;

    protected final int GunWalkFrame=6;
    protected final int GunWalkRow=13;

    //idle with gun (NEW: Using gun idle frames)
    protected final int GunFrontIdleFrame=2;
    protected final int GunFrontIdleRow=16; // Assuming gun front idle is row 16

    protected final int GunIdleFrame=2;
    protected final int GunIdleRow=17; // Assuming gun idle is row 17

    protected final int GunBackIdleFrame=1;
    protected final int GunBackIdleRow=11; // Assuming gun back idle is row 18

    //hurt with gun
    protected final int GunHitFrame=2;
    protected final int GunHitRow=17;

    protected final int GunFrontHitFrame=2;
    protected final int GunFrontHitRow=16;

    protected final int GunBackHitFrame=2;
    protected final int GunBackHitRow=18;





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

    // Sprite frame
    protected int currentFrame = 0;
    protected int currentRow = 0;
    protected boolean facingRight = true;

    protected long animationTimer = 0; // to accumulate time

    protected int actionCounter = 0; // movement of Entity subclasses

    Image dialogues[] = new Image[20];

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

    // Loads sprite image and extracts animation frames using ImageLoader
    protected void imageSet(int n, String spritePath) {
        sprite = ImageLoader.load(spritePath);  // Use ImageLoader to load image
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
            if (npcEntity != null && npcEntity != this) { // KEY: Exclude self
                Rectangle2D npcRect = new Rectangle2D(npcEntity.getX(), npcEntity.getY(), npcEntity.getWidth(), npcEntity.getHeight());
                Rectangle2D entityRect = new Rectangle2D(x, y, width, height);
                if (entityRect.intersects(npcRect)) {
                    return true;
                }
            }
        }

        // Also check against enemies and soldiers arrays
        if (gp.enemies != null) {
            for (Enemy enemy : gp.enemies) {
                if (enemy != null && enemy != this && !enemy.isDead()) {
                    Rectangle2D enemyRect = new Rectangle2D(enemy.getX(), enemy.getY(), enemy.getWidth(), enemy.getHeight());
                    Rectangle2D entityRect = new Rectangle2D(x, y, width, height);
                    if (entityRect.intersects(enemyRect)) {
                        return true;
                    }
                }
            }
        }

        if (gp.soldiers != null) {
            for (Soldier soldier : gp.soldiers) {
                if (soldier != null && soldier != this && !soldier.isDead()) {
                    Rectangle2D soldierRect = new Rectangle2D(soldier.getX(), soldier.getY(), soldier.getWidth(), soldier.getHeight());
                    Rectangle2D entityRect = new Rectangle2D(x, y, width, height);
                    if (entityRect.intersects(soldierRect)) {
                        return true;
                    }
                }
            }
        }

        return false;
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
