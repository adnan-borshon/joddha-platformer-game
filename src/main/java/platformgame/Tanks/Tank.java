package platformgame.Tanks;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import platformgame.*;
import platformgame.Entity.Entity;
import platformgame.Map.Level_2;

import java.util.ArrayList;
import java.util.List;

public abstract class Tank extends Entity {
    // Common tank properties
    protected Image spriteSheet;
    protected Game_2 gp2;
    protected final int frameHeight = 128;
    protected final int frameWidth = 160;

    // Tank physics and behavior properties
    protected double rotationSpeed = 3.0;     // radians per second
    protected double gunCooldown = 0.5;       // seconds between shots
    protected int maxHealth = 100;
    protected int health = 100;

    // Movement and rotation
    protected double tankRotation = 0;        // Tank body rotation in radians
    protected double turretRotation = 0;      // Turret rotation in radians
    protected Vector2D velocity = new Vector2D();

    // State management
    protected boolean canShoot = true;
    protected boolean alive = true;
    protected double gunTimer = 0;

    // Animation properties
    protected final int totalTankFrames = 3;
    protected int tankFrameIndex = 0;
    protected final int tankRow = 1; // 2nd row (0-indexed for idle/movement)

    // Turret/Cannon frames
    protected final int totalCannonFrames = 3;
    protected int cannonFrameIndex = 1; // Start at center position
    protected final int cannonRow = 0;
    protected List<Tank_Bullet> bullets;  // List to store bullets fired by the tank

    // Animation timing
    protected final long frameDuration = 100_000_000; // 0.1 seconds in nanoseconds
    protected long animationTimer = 0;

    // Movement state
    protected boolean isMoving = false;

    // Visual offsets for turret positioning
    protected static final double TURRET_OFFSET_X = 0;
    protected static final double TURRET_OFFSET_Y = 0;
    protected static final double MUZZLE_DISTANCE = 90; // Distance from tank center to muzzle

    // Debug flag to show/hide collision rectangle
    protected boolean showCollisionDebug = false;

    // Collision box dimensions (can be overridden by subclasses)
    protected double collisionWidth = 100;
    protected double collisionHeight = 100;
    protected double collisionOffsetX = -28;
    protected double collisionOffsetY = -28;

    public Tank(double x, double y, double width, double height, double speed, Game gp, Game_2 gp2) {
        super(x, y, width, height, speed, gp);
        this.gp2 = gp2;
        this.health = maxHealth;
        this.bullets = new ArrayList<>();  // Initialize bullets list

        loadTankSprite();
    }

    // Abstract method for loading tank-specific sprites
    protected abstract void loadTankSprite();

    // Common sprite loading utility
    protected void loadSprite(String spritePath) {
        try {
            spriteSheet = ImageLoader.load(spritePath);
            if (spriteSheet == null) {
                System.err.println("Failed to load " + spritePath + " sprite sheet");
            } else {
                System.out.println("Tank sprite loaded successfully. Size: " + spriteSheet.getWidth() + "x" + spriteSheet.getHeight());
            }
        } catch (Exception e) {
            System.err.println("Failed to load tank sprite: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Common update method - can be overridden by subclasses
    public void update(Level_2 level2, Game_2 game, long now, long deltaTime) {
        if (!alive) return;

        double deltaSeconds = deltaTime / 1_000_000_000.0; // Convert to seconds

        // Update specific tank behavior (implemented by subclasses)
        updateBehavior(level2, game, deltaSeconds);

        // Physics update
        physicsProcess(level2, deltaSeconds);

        // Update gun timer
        updateGunTimer(deltaSeconds);

        // Update animations
        updateAnimations(deltaTime);
    }

    // Abstract method for tank-specific behavior (movement, AI, etc.)
    protected abstract void updateBehavior(Level_2 level2, Game_2 game, double deltaTime);

    // Common physics processing
    protected void physicsProcess(Level_2 level2, double deltaTime) {
        if (!alive) return;

        // Calculate new position
        double newX = x + velocity.x * deltaTime;
        double newY = y + velocity.y * deltaTime;

        // Check for collisions using rectangle collision method
        if (level2 == null || !level2.checkCollisionWithRectangle(
                newX + collisionOffsetX, newY + collisionOffsetY,
                collisionWidth, collisionHeight)) {
            // No collision, update position
            x = newX;
            y = newY;
        } else {
            // Collision detected, stop movement
            velocity.set(0, 0);
            isMoving = false;
            onCollision();
        }

        // Keep tank within reasonable bounds if no level collision system
        if (level2 == null && gp2 != null) {
            double oldX = x, oldY = y;
            x = Math.max(0, Math.min(x, gp2.getScreenWidth() - width));
            y = Math.max(0, Math.min(y, gp2.getScreenHeight() - height));
            if (oldX != x || oldY != y) {
                onBoundaryHit();
            }
        }
    }

    // Hook methods for subclasses to override
    protected void onCollision() {
        // Default implementation - can be overridden
        System.out.println("✗ Collision detected! Tank stopped at: (" + String.format("%.2f", x) + ", " + String.format("%.2f", y) + ")");
    }

    protected void onBoundaryHit() {
        // Default implementation - can be overridden
        System.out.println("Tank position clamped to screen bounds: (" + String.format("%.2f", x) + ", " + String.format("%.2f", y) + ")");
    }

    // Common gun timer update
    protected void updateGunTimer(double deltaTime) {
        if (gunTimer > 0) {
            gunTimer -= deltaTime;
            if (gunTimer <= 0) {
                canShoot = true;
            }
        }
    }

    // Common animation update
    protected void updateAnimations(long deltaTime) {
        if (isMoving) {
            // If moving, switch frames for movement
            animationTimer += deltaTime;
            if (animationTimer >= frameDuration) {
                tankFrameIndex = (tankFrameIndex + 1) % totalTankFrames;
                animationTimer = 0;
            }
        } else {
            // If idle, keep the same frame
            tankFrameIndex = 0;
        }

        // Update cannon frame based on turret rotation for visual variety
        updateCannonFrame();
    }

    protected void updateCannonFrame() {
        // Map turret rotation to cannon frames for visual effect
        double normalizedRotation = (turretRotation + Math.PI) / (2 * Math.PI); // 0 to 1
        cannonFrameIndex = (int)(normalizedRotation * totalCannonFrames) % totalCannonFrames;
    }

    // Common drawing method
    public void draw(GraphicsContext gc, double camX, double camY, double scale) {
        if (!alive) return;

        drawTankBase(gc, camX, camY, scale);

        // Draw collision rectangle if debug mode is enabled
        if (showCollisionDebug) {
            drawCollisionDebug(gc, camX, camY, scale);
        }
    }

    protected void drawTankBase(GraphicsContext gc, double camX, double camY, double scale) {
        if (spriteSheet == null) return;

        int sourceX = tankFrameIndex * frameWidth;
        int sourceY = tankRow * frameHeight;

        double drawX = (x - camX) * scale;
        double drawY = (y - camY) * scale;
        double drawW = width * scale;
        double drawH = height * scale;

        // Save graphics context
        gc.save();

        // Translate to tank center for rotation
        gc.translate(drawX + drawW / 2, drawY + drawH / 2);
        gc.rotate(Math.toDegrees(tankRotation));

        // Draw tank body centered
        gc.drawImage(spriteSheet, sourceX, sourceY, frameWidth, frameHeight,
                -drawW / 2, -drawH / 2, drawW, drawH);

        // Restore graphics context
        gc.restore();
    }

    // Debug collision rectangle drawing
    protected void drawCollisionDebug(GraphicsContext gc, double camX, double camY, double scale) {
        // Calculate the center of the collision box
        double centerX = x + width / 2;
        double centerY = y + height / 2;

        // Define half-width and half-height for easier corner calculations
        double halfWidth = collisionWidth / 2;
        double halfHeight = collisionHeight / 2;

        // Define the four corners of the collision box (before rotation)
        double[] boxX = {-halfWidth, halfWidth, halfWidth, -halfWidth};
        double[] boxY = {-halfHeight, -halfHeight, halfHeight, halfHeight};

        // Rotate the corners of the collision box based on the tank's rotation
        double[] rotatedX = new double[4];
        double[] rotatedY = new double[4];

        for (int i = 0; i < 4; i++) {
            rotatedX[i] = centerX + boxX[i] * Math.cos(tankRotation) - boxY[i] * Math.sin(tankRotation);
            rotatedY[i] = centerY + boxX[i] * Math.sin(tankRotation) + boxY[i] * Math.cos(tankRotation);
        }

        // Find the axis-aligned bounding box (AABB) of the rotated rectangle
        double minX = rotatedX[0], maxX = rotatedX[0];
        double minY = rotatedY[0], maxY = rotatedY[0];

        for (int i = 1; i < 4; i++) {
            minX = Math.min(minX, rotatedX[i]);
            maxX = Math.max(maxX, rotatedX[i]);
            minY = Math.min(minY, rotatedY[i]);
            maxY = Math.max(maxY, rotatedY[i]);
        }

        // Convert the AABB to screen coordinates
        double screenX = (minX - camX) * scale;
        double screenY = (minY - camY) * scale;
        double screenWidth = (maxX - minX) * scale;
        double screenHeight = (maxY - minY) * scale;

        // Draw the collision rectangle
        gc.save();
        gc.setStroke(Color.RED);
        gc.setLineWidth(2.0);
        gc.strokeRect(screenX, screenY, screenWidth, screenHeight);

        // Optional: Fill with semi-transparent red
        gc.setFill(Color.color(1.0, 0.0, 0.0, 0.2));
        gc.fillRect(screenX, screenY, screenWidth, screenHeight);

        gc.restore();
    }

    // Get muzzle position for bullet spawning
    public Vector2D getMuzzlePosition() {
        double centerX = x + width/2;
        double centerY = y + height/2;
        double muzzleX = centerX + MUZZLE_DISTANCE * Math.cos(turretRotation);
        double muzzleY = centerY + MUZZLE_DISTANCE * Math.sin(turretRotation);
        return new Vector2D(muzzleX, muzzleY);
    }

    // Common shooting functionality
    public void shoot() {
        if (canShoot && alive) {
            canShoot = false;
            gunTimer = gunCooldown;

            // Play sound effect
            if (gp2 != null) {
                gp2.playSoundEffects(1); // Assuming 1 is shoot sound
            }

            // Get muzzle position and create bullet
            Vector2D muzzlePos = getMuzzlePosition();
            createBullet(muzzlePos.x, muzzlePos.y);
        }
    }


    // Method to load the bullet image (can be used for all tanks)
    private void loadBulletImage() {
        try {
            Image bulletImage = ImageLoader.load("/image/image.png");  // Load the bullet image
            if (bulletImage == null) {
                System.err.println("Failed to load bullet image.");
            }
        } catch (Exception e) {
            System.err.println("Error loading bullet image: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Abstract method for creating a bullet (implemented in Main_Tank class)
    protected abstract void createBullet(double x, double y);

    // Common method to update all bullets
// Common method to update all bullets
    public void updateBullets(long deltaTime) {
        if (bullets == null || bullets.isEmpty()) return;  // Check if bullets list is initialized and not empty

        for (int i = 0; i < bullets.size(); i++) {
            Tank_Bullet bullet = bullets.get(i);
            // Update bullet position
            bullet.update(deltaTime);

            // If bullet has traveled enough distance, mark for removal
            if (bullet.shouldRemove()) {
                bullets.remove(i);
                i--;  // Adjust index after removal to prevent skipping the next element
            }

            // Check for collisions with enemy tanks
            bullet.checkCollisionWithEnemies();
        }
    }

    // Method to apply damage to the tank
    public void applyDamage(int damage) {
        health -= damage;
        if (health <= 0) {
            alive = false;
            onDestroyed();
        }
    }

    // Common method to draw all bullets
    public void drawBullets(GraphicsContext gc, double camX, double camY, double scale) {
        for (Tank_Bullet bullet : bullets) {
            bullet.draw(gc, camX, camY, scale);
        }
    }

    // Method to create a bullet and add it to the list
    protected void createBullet(double x, double y, double velocityX, double velocityY) {
        Tank_Bullet bullet = new Tank_Bullet(x, y, velocityX, velocityY, null, gp2);
        bullets.add(bullet);
    }

    // Hook methods for damage/healing events
    protected void onDamageTaken(int damage) {
        // Default implementation - can be overridden
        System.out.println("Tank took " + damage + " damage. Health: " + health);
    }

    protected void onDestroyed() {
        // Default implementation - can be overridden
        System.out.println("Tank destroyed!");
    }


    // Health management
    public void takeDamage(int damage) {
        if (alive) {
            health -= damage;
            onDamageTaken(damage);
            if (health <= 0) {
                alive = false;
                onDestroyed();
            }
        }
    }

    // Hook methods for damage/healing events
    public double getTankY() { return y; }
    public double getTankWidth() { return width; }
    public double getTankHeight() { return height; }
    public double getTankRotation() { return tankRotation; }
    public double getTurretRotation() { return turretRotation; }
    public boolean isMoving() { return isMoving; }
    public boolean canShoot() { return canShoot; }
    public boolean isAlive() { return alive; }
    public boolean isCollisionDebugEnabled() { return showCollisionDebug; }
    public int getHealth() { return health; }
    public int getMaxHealth() { return maxHealth; }

    public void setHealth(int health) {
        this.health = Math.max(0, Math.min(maxHealth, health));
        if (this.health <= 0) {
            alive = false;
            onDestroyed();
        }
    }

    public void setMaxHealth(int maxHealth) {
        this.maxHealth = maxHealth;
        if (health > maxHealth) {
            health = maxHealth;
        }
    }

    public void setRotationSpeed(double rotationSpeed) {
        this.rotationSpeed = rotationSpeed;
    }

    public void setGunCooldown(double gunCooldown) {
        this.gunCooldown = gunCooldown;
    }

    public void toggleCollisionDebug() {
        showCollisionDebug = !showCollisionDebug;
        System.out.println("Collision debug: " + (showCollisionDebug ? "ON" : "OFF"));
    }

    // Utility method for setting collision box dimensions
    protected void setCollisionBox(double width, double height, double offsetX, double offsetY) {
        this.collisionWidth = width;
        this.collisionHeight = height;
        this.collisionOffsetX = offsetX;
        this.collisionOffsetY = offsetY;
    }


}

// Vector2D class (if not already defined elsewhere)
class Vector2D {
    public double x, y;

    public Vector2D() {
        this(0, 0);
    }

    public Vector2D(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void set(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double length() {
        return Math.sqrt(x * x + y * y);
    }

    public void normalize() {
        double len = length();
        if (len > 0) {
            x /= len;
            y /= len;
        }
    }
    // Getter methods for position (x and y)
    public double getTankX() {
        return x; // Return the x-coordinate of the tank
    }

    public double getTankY() {
        return y; // Return the y-coordinate of the tank
    }


    public Vector2D rotated(double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Vector2D(x * cos - y * sin, x * sin + y * cos);
    }
}