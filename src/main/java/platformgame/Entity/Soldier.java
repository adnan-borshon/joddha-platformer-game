package platformgame.Entity;

import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import platformgame.Bullet;
import platformgame.Game;
import java.util.ArrayList;
import java.util.Iterator;

public class Soldier extends Enemy {
    //shoot
    private final int FrontShootFrame=2;
    private final int FrontShootRow=9;

    private final int ShootFrame=2;
    private final int ShootRow=10;
    private final int BackShootFrame=2;
    private final int BackShootRow=11;

    //dead
    private final int deadFrame=3;
    private final int deadRow=12;

    //walk and run with gun
    private final int GunFrontWalkFrame=4;
    private final int GunFrontWalkRow=14;

    private final int GunBackWalkFrame=4;
    private final int GunBackWalkRow=15;

    private final int GunWalkFrame=6;
    private final int GunWalkRow=13;

    //idle with gun (NEW: Using gun idle frames)
    private final int GunFrontIdleFrame=1;
    private final int GunFrontIdleRow=16; // Assuming gun front idle is row 16

    private final int GunIdleFrame=1;
    private final int GunIdleRow=17; // Assuming gun idle is row 17

    private final int GunBackIdleFrame=1;
    private final int GunBackIdleRow=11; // Assuming gun back idle is row 18

    //hurt with gun
    private final int GunHitFrame=2;
    private final int GunHitRow=17;

    private final int GunFrontHitFrame=2;
    private final int GunFrontHitRow=16;

    private final int GunBackHitFrame=2;
    private final int GunBackHitRow=18;
    private int totalFramesShoot=0;

    // Direction tracking for animation
    private String currentDirection = "right"; // "front", "back", "right", "left"
    private String lastMovementDirection = "right";

    // Dead animation properties
    private boolean isDeadAnimationPlaying = false;
    private long deadStartTime = 0;
    private final long deadFrameDuration = 200_000_000L; // 200ms per frame

    // Shooting animation properties
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
        imageSet(GunWalkFrame, "/image/Soldier.png");
    }

    @Override
    public void update(long deltaTime, long now) {
        // Handle dead animation - check the isDead flag directly instead of isDead() method
        if (isDead) {
            updateDeadAnimation(now);
            // Still update bullets even when dead to let them finish their trajectory
            updateBullets(deltaTime, now);
            return;
        }

        // Handle hit animation first (from parent class)
        if (isHit) {
            updateHitAnimation(now);
            return;
        }

        double distanceToPlayer = Math.sqrt(Math.pow(x - gp.player.getX(), 2) + Math.pow(y - gp.player.getY(), 2));

        // Update direction based on movement toward player
        updateDirection();

        // Shooting logic - prioritize shooting over other actions
        if (distanceToPlayer <= shootRange) {
            if(hasLineOfSight() && now - lastShotTime > shootCooldown) {
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
        else if (distanceToPlayer >= 6 * gp.tileSize && distanceToPlayer <= 8 * gp.tileSize) {
            startFollowingPlayer(now);
        }



        // Handle animations based on current state and direction
        updateAnimations(now);

        // Update bullets
        updateBullets(deltaTime, now);
    }

    private void updateDirection() {
        double playerX = gp.player.getX();
        double playerY = gp.player.getY();

        double dx = playerX - x;
        double dy = playerY - y;

        // Determine direction based on player position relative to soldier
        if (Math.abs(dy) > Math.abs(dx)) {
            // Vertical movement is dominant
            if (dy < 0) {
                currentDirection = "back"; // Player is above, soldier moves up (corrected)
                lastMovementDirection = "back";
            } else {
                currentDirection = "front"; // Player is below, soldier moves down (corrected)
                lastMovementDirection = "front";
            }
        } else {
            // Horizontal movement is dominant
            if (dx > 0) {
                currentDirection = "right"; // Player is to the right
                lastMovementDirection = "right";
                facingRight = true;
            } else {
                currentDirection = "left"; // Player is to the left
                lastMovementDirection = "left";
                facingRight = false;
            }
        }
    }

    private void updateDeadAnimation(long now) {
        // Start dead animation if not already started
        if (!isDeadAnimationPlaying) {
            isDeadAnimationPlaying = true;
            deadStartTime = now;
        }

        currentRow = deadRow;
        int frameIndex = (int) ((now - deadStartTime) / deadFrameDuration);

        // Play through all dead frames once, then stay on the last frame
        if (frameIndex < deadFrame) {
            currentFrame = frameIndex;
        } else {
            currentFrame = deadFrame - 1; // Stay on the last frame
        }
    }

    private void updateHitAnimation(long now) {
        // Use appropriate hit animation based on current direction
        int totalHitFrames;
        switch (currentDirection) {
            case "front":
                currentRow = GunFrontHitRow;
                totalHitFrames = GunFrontHitFrame;
                break;
            case "back":
                currentRow = GunBackHitRow;
                totalHitFrames = GunBackHitFrame;
                break;
            case "right":
            case "left":
                currentRow = GunHitRow;
                totalHitFrames = GunHitFrame;
                break;
            default:
                currentRow = GunHitRow;
                totalHitFrames = GunHitFrame;
                break;
        }

        int frameIndex = (int) ((now - hitStartTime) / hitFrameDuration);
        if (frameIndex < totalHitFrames) {
            currentFrame = frameIndex;
        } else {
            isHit = false;
            currentFrame = 0;
        }
    }

    private void updateAnimations(long now) {
        if (isShooting) {
            updateShootingAnimation(now);
        } else if (isAttacking) {
            updateAttackAnimation(now);
        } else if (isWalking) {
            updateWalkingAnimation(now);
        } else if (isRunning) {
            updateRunningAnimation(now);
        } else {
            updateIdleAnimation(now);
        }
    }

    private void updateShootingAnimation(long now) {
        // Set appropriate shooting animation based on direction
        switch (currentDirection) {
            case "front":
                currentRow = FrontShootRow;
                totalFramesShoot = FrontShootFrame;
                break;
            case "back":
                currentRow = BackShootRow;
                totalFramesShoot = BackShootFrame;
                break;
            case "right":
            case "left":
                currentRow = ShootRow;
                totalFramesShoot = ShootFrame;
                break;
            default:
                currentRow = ShootRow;
                totalFramesShoot = ShootFrame;
                break;
        }

        int frameIndex = (int) ((now - shootStartTime) / shootFrameDuration);
        if (frameIndex < totalFramesShoot) {
            currentFrame = frameIndex;
            // Fire bullet on the last frame
            if (frameIndex == totalFramesShoot - 1 && bullets.size() == 0) {
                fireBullet();
            }
        } else {
            isShooting = false;
            currentFrame = 0;
        }
    }

    private void updateAttackAnimation(long now) {
        // For now, using the same attack animation as before
        // You can expand this to use directional attack animations if available
        currentRow = 6;
        int frameIndex = (int) ((now - attackStartTime) / 150_000_000);
        if (frameIndex < totalFramesShoot) {
            currentFrame = frameIndex;
        } else {
            currentFrame = 0;
            isAttacking = false;
            isWalking = false;
        }
    }

    private void updateWalkingAnimation(long now) {
        // Set appropriate walking animation based on direction
        // Using slower frame rate to make running animation look like walking
        long walkFrameDuration = 200_000_000L; // 200ms per frame (slower than normal)

        switch (currentDirection) {
            case "front":
                currentRow = GunFrontWalkRow;
                currentFrame = (int) ((now / walkFrameDuration) % GunFrontWalkFrame);
                break;
            case "back":
                currentRow = GunBackWalkRow;
                currentFrame = (int) ((now / walkFrameDuration) % GunBackWalkFrame);
                break;
            case "right":
            case "left":
                currentRow = GunWalkRow;
                currentFrame = (int) ((now / walkFrameDuration) % GunWalkFrame);
                break;
        }
    }

    private void updateRunningAnimation(long now) {
        // For running, use faster frame rate
        long runFrameDuration = 80_000_000L; // 80ms per frame (faster)

        switch (currentDirection) {
            case "front":
                currentRow = GunFrontWalkRow;
                currentFrame = (int) ((now / runFrameDuration) % GunFrontWalkFrame);
                break;
            case "back":
                currentRow = GunBackWalkRow;
                currentFrame = (int) ((now / runFrameDuration) % GunBackWalkFrame);
                break;
            case "right":
            case "left":
                currentRow = GunWalkRow;
                currentFrame = (int) ((now / runFrameDuration) % GunWalkFrame);
                break;
        }
    }

    private void updateIdleAnimation(long now) {
        // Set appropriate idle animation based on last movement direction
        switch (lastMovementDirection) {
            case "front":
                currentRow = GunFrontIdleRow;
                currentFrame = (int) ((now / 100_000_000) % GunFrontIdleFrame);
                break;
            case "back":
                currentRow = GunBackIdleRow;
                currentFrame = (int) ((now / 100_000_000) % GunBackIdleFrame);
                break;
            case "right":
            case "left":
                currentRow = GunIdleRow;
                currentFrame = (int) ((now / 100_000_000) % GunIdleFrame);
                break;
        }
    }

    protected void startShooting(long now) {
        isShooting = true;
        shootStartTime = now;
        isWalking = false;
        isRunning = false;
        isAttacking = false;
    shootSound();
        // Face the player when shooting (this will be handled by updateDirection())
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

    // In Soldier class (or override in Soldier class if necessary)
    @Override
    public Rectangle2D getHitbox() {
        return new Rectangle2D(x, y, width, height);
    }

    @Override
    public void draw(GraphicsContext gc, double camX, double camY, double scale) {
        // Always draw the soldier, even when dead (for death animation)
        // We'll handle the death animation visibility in the parent class draw method
        super.draw(gc, camX, camY, scale);

        // Only draw bullets if soldier is not dead
        if (!isDead) {
            for (Bullet bullet : bullets) {
                bullet.draw(gc, camX, camY, scale, facingRight);
            }
        }
    }

    // Clean up bullets when soldier dies
    @Override
    public void receiveDamage() {
        super.receiveDamage();
        // Don't clear bullets immediately - let them finish their trajectory
        // bullets.clear(); // Commented out to let bullets continue
    }

    // Getter for bullets (if needed for collision detection elsewhere)
    public ArrayList<Bullet> getBullets() {
        return bullets;
    }
}