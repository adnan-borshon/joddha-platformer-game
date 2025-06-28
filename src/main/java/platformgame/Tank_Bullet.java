package platformgame;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import platformgame.Tanks.Tank;
import platformgame.Tanks.Main_Tank;
import platformgame.Tanks.Enemy_Tank;
import java.util.List;
import java.util.ArrayList;

public class Tank_Bullet {
    private double x, y;
    private double velocityX, velocityY;
    private final double width = 8;
    private final double height = 8;
    private Game gp;
    private Game_2 gp2;
    private boolean shouldRemove = false;
    private Tank shooter; // Track who shot this bullet

    private final double maxDistance = 8 * 64;  // 8 tiles as requested
    private double traveledDistance = 0;

    public Tank_Bullet(double startX, double startY, double velX, double velY, Game gp, Game_2 gp2) {
        this.x = startX;
        this.y = startY;
        this.velocityX = velX;
        this.velocityY = velY;
        this.gp = gp;
        this.gp2 = gp2;
        this.shooter = null; // Will be set separately if needed
    }

    // Constructor with shooter information
    public Tank_Bullet(double startX, double startY, double velX, double velY, Game gp, Game_2 gp2, Tank shooter) {
        this.x = startX;
        this.y = startY;
        this.velocityX = velX;
        this.velocityY = velY;
        this.gp = gp;
        this.gp2 = gp2;
        this.shooter = shooter;
    }

    // Update bullet position and handle distance traveled
    public void update(long deltaTime) {
        double deltaSeconds = deltaTime / 1_000_000_000.0;  // Convert to seconds
        x += velocityX * deltaSeconds;
        y += velocityY * deltaSeconds;

        // Track traveled distance
        traveledDistance += Math.sqrt(velocityX * velocityX + velocityY * velocityY) * deltaSeconds;
        if (traveledDistance >= maxDistance) {
            shouldRemove = true;  // Mark bullet for removal if it travels too far
        }
    }

    // Check for collisions with all tanks (enemies and main tank)
    public void checkCollisionWithTanks() {
        // Check collision with enemy tanks (if bullet was shot by main tank)
        if (gp2 != null && gp2.getEnemyTanks() != null) {
            for (Tank tank : gp2.getEnemyTanks()) {
                if (tank != null && tank.isAlive() && tank != shooter && isCollidingWith(tank)) {
                    tank.takeDamage(90);  // Deal 10 damage for each bullet hit
                    shouldRemove = true;
                    return;
                }
            }
        }

        // Check collision with main tank (if bullet was shot by enemy tank)
        if (gp2 != null && gp2.mainTank != null && gp2.mainTank.isAlive() && gp2.mainTank != shooter) {
            if (isCollidingWith(gp2.mainTank)) {
                gp2.mainTank.takeDamage(10);  // Deal 10 damage to main tank
                shouldRemove = true;
                return;
            }
        }
    }

    // Legacy method for backward compatibility
    public void checkCollisionWithEnemies() {
        checkCollisionWithTanks();
    }

    // Collision check with another tank
    private boolean isCollidingWith(Tank tank) {
        double tankX = tank.getX();
        double tankY = tank.getY();
        double tankWidth = tank.getTankWidth();
        double tankHeight = tank.getTankHeight();

        // Basic rectangle collision check
        return (x < tankX + tankWidth &&
                x + width > tankX &&
                y < tankY + tankHeight &&
                y + height > tankY);
    }

    // Draw bullet to the screen
    public void draw(GraphicsContext gc, double camX, double camY, double scale) {
        if (shouldRemove) return;

        double screenX = (x - camX) * scale;
        double screenY = (y - camY) * scale;
        double size = 20 * scale;

        try {
            // Use ImageLoader to load the bullet image
            Image image = ImageLoader.load("/image/bullet.png");

            if (image != null) {
                gc.drawImage(image, screenX, screenY, size, size);
            } else {
                // Fallback: draw a simple circle if the image couldn't be loaded
                gc.setFill(Color.YELLOW);
                gc.fillOval(screenX, screenY, size, size);
            }
        } catch (Exception e) {
            // Fallback: draw a simple circle in case of any error
            gc.setFill(Color.YELLOW);
            gc.fillOval(screenX, screenY, size, size);
        }
    }

    // Remove bullet if necessary
    public boolean shouldRemove() {
        return shouldRemove;
    }

    // Set the shooter of this bullet
    public void setShooter(Tank shooter) {
        this.shooter = shooter;
    }

    // Get the shooter of this bullet
    public Tank getShooter() {
        return shooter;
    }

    // Getter methods for position if needed
    public double getX() { return x; }
    public double getY() { return y; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
}