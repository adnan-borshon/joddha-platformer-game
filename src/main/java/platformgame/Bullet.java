package platformgame;

import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import platformgame.Entity.Player;

public class Bullet {
    private double x, y;
    private double velocityX, velocityY;
    private final double width = 8;
    private final double height = 8;
    private Game gp;
    private Image bulletSprite;
    private boolean shouldRemove = false;
    private final double maxDistance = 8 * 64; // 8 tiles as requested
    private double traveledDistance = 0;
    private double startX, startY; // Store starting position

    // Smooth rotation for bullet
    private double rotation = 0;

    public Bullet(double startX, double startY, double velX, double velY, Game gp) {
        this.x = startX;
        this.y = startY;
        this.startX = startX; // Store starting position
        this.startY = startY;
        this.velocityX = velX;
        this.velocityY = velY;
        this.gp = gp;

        // Try to load bullet sprite, fallback to drawing a circle if not found
        try {
            bulletSprite = ImageLoader.load("/image/bullet.png");
        } catch (Exception e) {
            System.out.println("Warning: Could not load bullet.png, will draw as circle");
            bulletSprite = null;
        }

        // Calculate rotation angle based on velocity direction
        rotation = Math.atan2(velocityY, velocityX);
    }

    public void update(long deltaTime, long now) {
        if (shouldRemove) return;



        // Update bullet's position based on its velocity
        x += velocityX;
        y += velocityY;

        // Calculate traveled distance from start position
        double dx = x - startX;
        double dy = y - startY;
        traveledDistance = Math.sqrt(dx * dx + dy * dy);


        // Check if bullet should be removed due to maximum distance
        if (traveledDistance > maxDistance) {
            shouldRemove = true;
            System.out.println("Bullet removed - max distance reached");
            return;
        }

        // Check for collisions with tiles, objects, and boundaries
        if (gp.level1.isCollisionRect(x - width / 2, y - height / 2, width, height) ||
                checkObjectCollision() ||
                checkPlayerCollision(gp.player)) {
            shouldRemove = true;

            return;
        }

//        // Check if the bullet is out of reasonable bounds
//        if (x < -100 || x > gp.level1.mapWidth + 100 || y < -100 || y > gp.level1.mapHeight + 100) {
//            shouldRemove = true;
//            System.out.println("Bullet removed - out of bounds");
//        }
    }

    private boolean checkObjectCollision() {
        Rectangle2D bulletRect = new Rectangle2D(x - width/2, y - height/2, width, height);

        // Check collision with objects
        for (int i = 0; i < gp.object.length; i++) {
            if (gp.object[i] != null && gp.object[i].collision) {
                Rectangle2D objRect = gp.object[i].getBoundingBox();
                if (bulletRect.intersects(objRect)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean checkPlayerCollision(Player player) {
        if (player == null) return false;

        Rectangle2D bulletRect = new Rectangle2D(x - width/2, y - height/2, width, height);
        Rectangle2D playerRect = new Rectangle2D(
                player.getX(), player.getY(),
                player.getWidth(), player.getHeight()
        );

        return bulletRect.intersects(playerRect);
    }

    public void draw(GraphicsContext gc, double camX, double camY, double scale, boolean facingRight) {
        if (shouldRemove) return;

        double drawX = (x - camX) * scale;
        double drawY = (y - camY) * scale;
        double drawW = width * scale;
        double drawH = height * scale;

        if (bulletSprite != null) {

          if(facingRight)gc.drawImage(bulletSprite, drawX+15 , drawY-35 , drawW*1.5, drawH*1.5);
        else gc.drawImage(bulletSprite, drawX-50 , drawY-35 , drawW*1.5, drawH*1.5);
        }

    }

    public boolean shouldRemove() {
        return shouldRemove;
    }

    public void markForRemoval() {
        shouldRemove = true;
    }

    // Getters
    public double getX() { return x; }
    public double getY() { return y; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public double getTraveledDistance() { return traveledDistance; } // For debugging
}