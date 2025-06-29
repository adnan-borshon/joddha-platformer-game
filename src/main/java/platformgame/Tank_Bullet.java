package platformgame;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import platformgame.Tanks.Tank;
import platformgame.Tanks.Main_Tank;
import platformgame.Tanks.Enemy_Tank;
import platformgame.Tanks.Tank2;
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

    private final double maxDistance = 8*32;  // 8 tiles as requested
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

    // FIXED: Check for collisions with all tanks (enemies and main tank) - CENTERED COLLISION
    public void checkCollisionWithTanks() {
        // Check collision with Enemy_Tank ArrayList
        if (gp2 != null && gp2.getEnemyTanksList() != null) {
            for (Tank tank : gp2.getEnemyTanksList()) {
                if (tank != null && tank.isAlive() && tank != shooter && isCollidingWithCentered(tank)) {
                    System.out.println("Bullet hit Enemy_Tank!");
                    tank.takeDamage(10);
                    shouldRemove = true;
                    return;
                }
            }
        }

        // FIXED: Check collision with Tank2 ArrayList - CENTERED COLLISION
        if (gp2 != null && gp2.Tanks != null) {
            for (Tank2 tank2 : gp2.Tanks) {
                if (tank2 != null && tank2.isAlive() && tank2 != shooter && isCollidingWithCentered(tank2)) {
                    System.out.println("Bullet hit Tank2!");
                    tank2.takeDamage(10);
                    shouldRemove = true;
                    return;
                }
            }
        }

        // FIXED: Also check the arrays for backward compatibility - CENTERED COLLISION
        if (gp2 != null && gp2.enemyTank != null) {
            for (Tank tank : gp2.enemyTank) {
                if (tank != null && tank.isAlive() && tank != shooter && isCollidingWithCentered(tank)) {
                    System.out.println("Bullet hit Enemy_Tank (array)!");
                    tank.takeDamage(10);
                    shouldRemove = true;
                    return;
                }
            }
        }

        if (gp2 != null && gp2.Tanks2 != null) {
            for (Tank2 tank2 : gp2.Tanks2) {
                if (tank2 != null && tank2.isAlive() && tank2 != shooter && isCollidingWithCentered(tank2)) {
                    System.out.println("Bullet hit Tank2 (array)!");
                    tank2.takeDamage(10);
                    shouldRemove = true;
                    return;
                }
            }
        }

        // Check collision with main tank - CENTERED COLLISION
        if (gp2 != null && gp2.mainTank != null && gp2.mainTank.isAlive() && gp2.mainTank != shooter) {
            if (isCollidingWithCentered(gp2.mainTank)) {
                System.out.println("Bullet hit Main Tank!");
                gp2.mainTank.takeDamage(10);
                shouldRemove = true;
            }
        }
    }

    // Legacy method for backward compatibility
    public void checkCollisionWithEnemies() {
        checkCollisionWithTanks();
    }

    // FIXED: Centered collision detection - uses tank center instead of collision box
    private boolean isCollidingWithCentered(Tank tank) {
        // Get tank's center position
        double tankCenterX = tank.getX() + tank.getTankWidth() / 2;
        double tankCenterY = tank.getY() + tank.getTankHeight() / 2;

        // Define collision radius for tanks (adjust as needed)
        double tankCollisionRadius = 40.0; // Half the tank size for circular collision

        // Bullet center point
        double bulletCenterX = x + width / 2;
        double bulletCenterY = y + height / 2;

        // Calculate distance between bullet center and tank center
        double distance = Math.sqrt(
                Math.pow(bulletCenterX - tankCenterX, 2) +
                        Math.pow(bulletCenterY - tankCenterY, 2)
        );

        // Debug output (remove or comment out for production)
        System.out.println("Centered collision check - Bullet: (" + bulletCenterX + "," + bulletCenterY +
                ") Tank center: (" + tankCenterX + "," + tankCenterY + ") Distance: " + distance +
                " Collision radius: " + tankCollisionRadius);

        // Check if distance is less than collision radius
        boolean colliding = distance <= tankCollisionRadius;

        if (colliding) {
            System.out.println("CENTERED COLLISION DETECTED between bullet and " + tank.getClass().getSimpleName() + "!");
        }

        return colliding;
    }

    // OLD METHOD: Keep for comparison or fallback
    private boolean isCollidingWith(Tank tank) {
        // Get tank's collision box
        double tankX = tank.getX() + tank.getCollisionOffsetX();
        double tankY = tank.getY() + tank.getCollisionOffsetY();
        double tankWidth = tank.getCollisionWidth();
        double tankHeight = tank.getCollisionHeight();

        // Bullet center point
        double bulletCenterX = x + width / 2;
        double bulletCenterY = y + height / 2;

        // Check if bullet center is within tank's collision box
        boolean colliding = (bulletCenterX >= tankX &&
                bulletCenterX <= tankX + tankWidth &&
                bulletCenterY >= tankY &&
                bulletCenterY <= tankY + tankHeight);

        return colliding;
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

        // FIXED: Draw bullet collision box for debugging
        if (false) { // Set to true to enable debug drawing
            gc.setStroke(Color.BLUE);
            gc.setLineWidth(1);
            gc.strokeRect(screenX, screenY, width * scale, height * scale);
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