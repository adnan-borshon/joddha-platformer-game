package platformgame.Entity;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import platformgame.Bullet;
import platformgame.Game;
import java.util.ArrayList;
import java.util.Iterator;

public class Soldier extends Enemy {

    // Shooting animation properties
    private final int totalFramesShoot = 4;
    private final int shootRow = 4; // 4th row (0-indexed)
    private boolean isShooting = false;
    private long shootStartTime = 0;
    private final long shootFrameDuration = 100_000_000L; // 200ms per frame

    // Shooting mechanics
    private long lastShotTime = 0;
    private final long shootCooldown = 2_000_000_000L; // 2 seconds between shots
    private final double shootRange = 8 * gp.tileSize; // Range to detect and shoot at player
    private final double bulletSpeed = 8.0;

    // Bullet management
    private ArrayList<Bullet> bullets;

    public Soldier(double x, double y, double width, double height, double speed, Game gp) {
        super(x, y, width, height, speed, gp);
        bullets = new ArrayList<>();
        // Override the sprite loading to use the same Enemy.png but focus on shooting behavior
        imageSet(totalFramesShoot, "/image/Enemy.png");
    }

    @Override
    public void update(long deltaTime, long now) {
        if (isDead()) return;

        // Handle hit animation first (from parent class)
        if (isHit) {
            currentRow = hitRow;
            int frameIndex = (int) ((now - hitStartTime) / hitFrameDuration);
            if (frameIndex < totalHitFrames) {
                currentFrame = frameIndex;
            } else {
                isHit = false;
                currentFrame = 0;
            }
            return;
        }

        double distanceToPlayer = Math.sqrt(Math.pow(x - gp.player.getX(), 2) + Math.pow(y - gp.player.getY(), 2));

        // Shooting logic - prioritize shooting over other actions
        if (distanceToPlayer <= shootRange) {

            if(  hasLineOfSight() && now - lastShotTime > shootCooldown) {
                startShooting(now);
                lastShotTime = now;
            }
        }
        // Close combat (inherited from Enemy)
        else if (distanceToPlayer <= 2 * gp.tileSize) {
            if (now - lastAttackTime > damageCooldown) {
                startAttack(now);
                gp.player.takeMeleeDamageFromEnemy(1, now);
                lastAttackTime = now;
            }
        }
        // Follow player (inherited behavior)
        else if (distanceToPlayer >= 6 * gp.tileSize  && distanceToPlayer <=8* gp.tileSize) {
            startFollowingPlayer(now);
        }
        // Patrol (inherited behavior)
        else {
            patrol(now);
        }

        // Handle shooting animation
        if (isShooting) {
            currentRow = shootRow;
            int frameIndex = (int) ((now - shootStartTime) / shootFrameDuration);
            if (frameIndex < totalFramesShoot) {
                currentFrame = frameIndex;
                // Fire bullet on the last frame (frame 3, which is index 3)
                if (frameIndex == totalFramesShoot - 1 && bullets.size() == 0) {
                    fireBullet();
                }
            } else {
                isShooting = false;
                currentFrame = 0;
            }
        }
        // Use parent class animation logic for other states
        else if (isAttacking) {
            currentRow = 6;
            int frameIndex = (int) ((now - attackStartTime) / 150_000_000);
            if (frameIndex < totalFramesAttack) {
                currentFrame = frameIndex;
            } else {
                currentFrame = 0;
                isAttacking = false;
                isWalking = false;
            }
        } else if (isWalking) {
            currentRow = 2;
            currentFrame = (int) ((now / 100_000_000) % totalFramesWalk);
        } else if (isRunning) {
            currentRow = 3;
            currentFrame = (int) ((now / 100_000_000) % totalFramesRun);
        } else {
            currentRow = 1;
            currentFrame = (int) ((now / 100_000_000) % totalFramesIdle);
        }

        // Update bullets
        updateBullets(deltaTime, now);
    }

    private void startShooting(long now) {
        isShooting = true;
        shootStartTime = now;
        isWalking = false;
        isRunning = false;
        isAttacking = false;

        // Face the player when shooting
        if (gp.player.getX() > x) {
            facingRight = true;
        } else {
            facingRight = false;
        }
    }
    private boolean hasLineOfSight() {
        // Get the player's center position
        double playerX = gp.player.getX() + gp.player.getWidth() / 2;
        double playerY = gp.player.getY() + gp.player.getHeight() / 2;

        // Get the soldier's center position
        double soldierX = x + width / 2;
        double soldierY = y + height / 2;

        // Calculate the direction vector from soldier to player
        double deltaX = playerX - soldierX;
        double deltaY = playerY - soldierY;
        double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);

        // Normalize direction vector
        double directionX = deltaX / distance;
        double directionY = deltaY / distance;

        // Check for obstacles along the ray from soldier to player
        // Let's increase the step size for efficiency (e.g., checking every 5 pixels)
        double stepSize = 32;  // Increase step size to skip some pixels for performance

        for (double i = 0; i < distance; i += stepSize) {
            double checkX = soldierX + directionX * i;
            double checkY = soldierY + directionY * i;

            // Check for collisions at each step
            if (checkTileCollision(checkX, checkY) || checkObjectCollision(checkX, checkY) || checkNpcCollision(checkX, checkY)) {
                return false;  // Line of sight is blocked by an obstacle
            }
        }

        return true;  // No obstacles, clear line of sight
    }



    private void fireBullet() {
        double playerX = gp.player.getX() + gp.player.getWidth() / 2;
        double playerY = gp.player.getY() + gp.player.getHeight() / 2;
        double soldierX = x + width / 2;
        double soldierY = y + height / 2;

        // Calculate direction to player
        double dx = playerX - soldierX;
        double dy = playerY - soldierY;
        double distance = Math.sqrt(dx * dx + dy * dy);

        // Normalize direction
        dx /= distance;
        dy /= distance;


        // Create bullet with higher speed for testing
        double actualSpeed = 5.0; // Increased speed for visibility
        Bullet bullet = new Bullet(soldierX, soldierY, dx * actualSpeed, dy * actualSpeed, gp);
        bullets.add(bullet);

    }

    private void updateBullets(long deltaTime, long now) {

        Iterator<Bullet> bulletIterator = bullets.iterator();
        while (bulletIterator.hasNext()) {
            Bullet bullet = bulletIterator.next();
            bullet.update(deltaTime, now);

            // Check if bullet hit player
            if (bullet.checkPlayerCollision(gp.player)) {
                gp.player.takeMeleeDamageFromEnemy(2, now); // 2 damage from bullet
                bulletIterator.remove();

            }
            // Remove bullet if it's out of bounds or hit something
            else if (bullet.shouldRemove()) {
                bulletIterator.remove();

            }
        }
    }

    @Override
    public void draw(GraphicsContext gc, double camX, double camY, double scale) {
        if (isDead) return;

        // Draw soldier (inherited from Enemy)
        super.draw(gc, camX, camY, scale);

        // Draw bullets
        for (Bullet bullet : bullets) {
            bullet.draw(gc, camX, camY, scale, facingRight);
        }
    }

    // Clean up bullets when soldier dies
    @Override
    public void receiveDamage() {
        super.receiveDamage();
        if (isDead) {
            bullets.clear();
        }
    }

    // Getter for bullets (if needed for collision detection elsewhere)
    public ArrayList<Bullet> getBullets() {
        return bullets;
    }
}