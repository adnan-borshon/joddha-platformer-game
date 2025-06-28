package platformgame.Entity;

import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import platformgame.Game;
import platformgame.Objects.SuperObject;

public class Enemy extends Entity {

    // Animation frame constants
    // Front facing (S key pressed - going down)
    private final int frontWalkFrame = 4;
    private final int frontWalkRow = 3;
    private final int frontIdleFrame = 2;
    private final int frontIdleRow = 0;
    private final int frontFistFrame = 2;
    private final int frontFistRow = 6;
    private final int frontShootFrame = 2; // Add if needed
    private final int frontShootRow = 9; // Add if needed

    // Back facing (W key pressed - going up)
    private final int backWalkFrame = 4;
    private final int backWalkRow = 4;
    private final int backIdleFrame = 2;
    private final int backIdleRow = 2;
    private final int backFistFrame = 2;
    private final int backFistRow = 8;
    private final int backShootFrame = 2; // Add if needed
    private final int backShootRow = 11; // Add if needed

    // Normal facing (D key or flipped for A key)
    private final int walkFrame = 6;
    private final int walkRow = 5;
    private final int idleFrame = 2;
    private final int idleRow = 1; // Change to with gun idle
    private final int fistFrame = 2;
    private final int fistRow = 7;
    private final int shootFrame = 2; // Add if needed
    private final int shootRow = 10; // Add if needed

    // Hit animations
    private final int frontHitFrame = 2;
    private final int frontHitRow = 20;
    private final int backHitFrame = 2;
    private final int backHitRow = 21;
    private final int hitFrame = 2;
    private final int hitRow = 19;

    // Dead animation
    private final int deadFrame = 3;
    private final int deadRow = 12;

    // Animation states
    protected boolean isIdle = true;
    protected boolean isWalking = false;
    protected boolean isRunning = false;
    protected boolean isAttacking = false;
    protected boolean isShooting = false;

    // Movement direction states
    protected boolean isFacingFront = false; // S key (going down)
    protected boolean isFacingBack = false;  // W key (going up)
    protected boolean isFacingLeft = false;  // A key
    protected boolean isFacingRight = true;  // D key (default)

    // Combat and AI variables
    protected long attackStartTime = 0;
    protected long shootStartTime = 0;
    protected double patrolRange = 7 * gp.tileSize;
    protected double patrolStartX, patrolStartY;
    protected double patrolTargetX, patrolTargetY;
    protected boolean isFollowingPlayer = false;

    protected int maxHealth = 5;
    protected int currentHealth = 5;
    protected boolean isDead = false;
    protected long deathStartTime = 0;
    protected boolean deathAnimationComplete = false;

    protected long lastAttackTime = 0;
    protected long damageCooldown = 2_000_000_000L;

    protected boolean isHit = false;
    protected long hitStartTime = 0;
    protected final long hitFrameDuration = 100_000_000L;

    // Collision detection
    private double collisionMargin = 5.0; // Pixels of margin for collision detection

    public Enemy(double x, double y, double width, double height, double speed, Game gp) {
        super(x, y, width, height, speed * 1.3, gp); // Increased walk speed by 30%
        imageSet(walkFrame, "/image/Enemy.png");
        patrolStartX = x;
        patrolStartY = y;
        setNewPatrolTarget();
    }

    public void update(long deltaTime, long now) {
        if (isDead) {
            handleDeadAnimation(now);
            return;
        }

        if (isHit) {
            handleHitAnimation(now);
            return;
        }

        double distanceToPlayer = Math.sqrt(Math.pow(x - gp.player.getX(), 2) + Math.pow(y - gp.player.getY(), 2));

        // AI behavior
        if (distanceToPlayer <= 2 * gp.tileSize) {
            if (now - lastAttackTime > damageCooldown) {
                startAttack(now);
                gp.player.takeMeleeDamageFromEnemy(1, now);
                lastAttackTime = now;
            }
        } else if (distanceToPlayer <= 6 * gp.tileSize) {
            startFollowingPlayer(now);
        } else {
            patrol(now);
        }

        // Handle animations based on current state
        if (isAttacking) {
            handleAttackAnimation(now);
        } else if (isShooting) {
            handleShootAnimation(now);
        } else if (isRunning) {
            handleRunAnimation(now);
        } else if (isWalking) {
            handleWalkingAnimation(now);
        } else {
            handleIdleAnimation(now);
        }
    }

    private void handleDeadAnimation(long now) {
        if (deathStartTime == 0) {
            deathStartTime = now;
        }

        currentRow = deadRow;
        int frameIndex = (int) ((now - deathStartTime) / 200_000_000); // Slightly slower death animation

        if (frameIndex < deadFrame) {
            currentFrame = frameIndex;
        } else {
            currentFrame = deadFrame - 1; // Stay on last frame briefly
            // Mark animation as complete after a short delay on the last frame
            if ((now - deathStartTime) > (deadFrame * 200_000_000L + 400_000_000L)) { // Extra 0.5 seconds on last frame
                deathAnimationComplete = true;
            }
        }
    }

    private void handleRunAnimation(long now) {
        if (isFacingFront) {
            currentRow = frontWalkRow;
            currentFrame = (int) ((now / 150_000_000) % frontWalkFrame); // Slower animation
        } else if (isFacingBack) {
            currentRow = backWalkRow;
            currentFrame = (int) ((now / 150_000_000) % backWalkFrame); // Slower animation
        } else {
            currentRow = walkRow;
            currentFrame = (int) ((now / 150_000_000) % walkFrame); // Slower animation
        }
    }

    private void handleHitAnimation(long now) {
        if (isFacingFront) {
            currentRow = frontHitRow;
        } else if (isFacingBack) {
            currentRow = backHitRow;
        } else {
            currentRow = hitRow;
        }

        int frameIndex = (int) ((now - hitStartTime) / hitFrameDuration);
        int maxFrames = isFacingFront ? frontHitFrame : (isFacingBack ? backHitFrame : hitFrame);

        if (frameIndex < maxFrames) {
            currentFrame = frameIndex;
        } else {
            isHit = false;
            currentFrame = 0;
        }
    }

    private void handleAttackAnimation(long now) {
        if (isFacingFront) {
            currentRow = frontFistRow;
        } else if (isFacingBack) {
            currentRow = backFistRow;
        } else {
            currentRow = fistRow;
        }

        int frameIndex = (int) ((now - attackStartTime) / 150_000_000);
        int maxFrames = isFacingFront ? frontFistFrame : (isFacingBack ? backFistFrame : fistFrame);

        if (frameIndex < maxFrames) {
            currentFrame = frameIndex;
        } else {
            currentFrame = 0;
            isAttacking = false;
            isWalking = false;
        }
    }

    private void handleShootAnimation(long now) {
        if (isFacingFront) {
            currentRow = frontShootRow;
        } else if (isFacingBack) {
            currentRow = backShootRow;
        } else {
            currentRow = shootRow;
        }

        int frameIndex = (int) ((now - shootStartTime) / 150_000_000);
        int maxFrames = isFacingFront ? frontShootFrame : (isFacingBack ? backShootFrame : shootFrame);

        if (frameIndex < maxFrames) {
            currentFrame = frameIndex;
        } else {
            currentFrame = 0;
            isShooting = false;
            isWalking = false;
        }
    }

    private void handleWalkingAnimation(long now) {
        if (isFacingFront) {
            currentRow = frontWalkRow;
            currentFrame = (int) ((now / 150_000_000) % frontWalkFrame); // Slower animation
        } else if (isFacingBack) {
            currentRow = backWalkRow;
            currentFrame = (int) ((now / 150_000_000) % backWalkFrame); // Slower animation
        } else {
            currentRow = walkRow;
            currentFrame = (int) ((now / 150_000_000) % walkFrame); // Slower animation
        }
    }

    private void handleIdleAnimation(long now) {
        // Use gun idle frames by default
        if (isFacingFront) {
            currentRow = frontIdleRow;
            currentFrame = (int) ((now / 200_000_000) % frontIdleFrame);
        } else if (isFacingBack) {
            currentRow = backIdleRow;
            currentFrame = (int) ((now / 200_000_000) % backIdleFrame);
        } else {
            currentRow = idleRow; // This should be with gun idle
            currentFrame = (int) ((now / 200_000_000) % idleFrame);
        }
    }

    protected void startAttack(long now) {
        isAttacking = true;
        attackStartTime = now;
        isWalking = false;
        isRunning = false;
        isShooting = false;
    }

    protected void startShooting(long now) {
        isShooting = true;
        shootStartTime = now;
        isWalking = false;
        isRunning = false;
        isAttacking = false;
    }

    protected void startFollowingPlayer(long now) {
        isFollowingPlayer = true;
        isWalking = false;
        isRunning = true;

        // Determine facing direction based on player position
        updateFacingDirection(gp.player.getX(), gp.player.getY());

        // Move with collision detection
        moveTowardsTargetWithCollision(gp.player.getX(), gp.player.getY(), speed);
    }

    protected void patrol(long now) {
        isFollowingPlayer = false;
        isWalking = true;
        isRunning = false;

        if (hasReachedTarget()) {
            setNewPatrolTarget();
        }

        // Determine facing direction based on patrol target
        updateFacingDirection(patrolTargetX, patrolTargetY);

        // Move with collision detection
        moveTowardsTargetWithCollision(patrolTargetX, patrolTargetY, speed * 0.6); // Slightly faster patrol
    }

    private void updateFacingDirection(double targetX, double targetY) {
        double dx = targetX - x;
        double dy = targetY - y;

        // Reset all facing directions
        isFacingFront = false;
        isFacingBack = false;
        isFacingLeft = false;
        isFacingRight = false;

        // Determine primary direction based on larger component
        if (Math.abs(dy) > Math.abs(dx)) {
            if (dy > 0) {
                isFacingFront = true; // Moving down (S direction) - FIXED
            } else {
                isFacingBack = true;  // Moving up (W direction) - FIXED
            }
        } else {
            if (dx > 0) {
                isFacingRight = true; // Moving right (D direction)
                facingRight = true;
            } else {
                isFacingLeft = true;  // Moving left (A direction)
                facingRight = false;
            }
        }
    }

    private void setNewPatrolTarget() {
        patrolTargetX = patrolStartX + (Math.random() * patrolRange) - (patrolRange / 2);
        patrolTargetY = patrolStartY + (Math.random() * patrolRange) - (patrolRange / 2);
        patrolTargetX = Math.max(patrolStartX - patrolRange/2, Math.min(patrolTargetX, patrolStartX + patrolRange/2));
        patrolTargetY = Math.max(patrolStartY - patrolRange/2, Math.min(patrolTargetY, patrolStartY + patrolRange/2));
    }

    private boolean hasReachedTarget() {
        double distance = Math.sqrt(Math.pow(x - patrolTargetX, 2) + Math.pow(y - patrolTargetY, 2));
        return distance < 20; // Increased threshold for better movement
    }

    protected void moveTowardsTargetWithCollision(double targetX, double targetY, double moveSpeed) {
        // Calculate the difference in position
        double dx = targetX - x;
        double dy = targetY - y;

        // Calculate the distance to the target
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance > 0) {
            // Normalize the direction vector
            dx /= distance;
            dy /= distance;

            // Calculate new position
            double newX = x + dx * moveSpeed;
            double newY = y + dy * moveSpeed;

            // Check collision for X movement
            if (!checkCollisionAt(newX, y)) {
                x = newX;
            } else {
                // Try to slide along walls
                if (!checkCollisionAt(x, newY)) {
                    y = newY;
                }
            }

            // Check collision for Y movement
            if (!checkCollisionAt(x, newY)) {
                y = newY;
            } else {
                // Try to slide along walls
                if (!checkCollisionAt(newX, y)) {
                    x = newX;
                }
            }
        }
    }

    private boolean checkCollisionAt(double newX, double newY) {
        // Simply use the Entity class collision method
        return isColliding(newX, newY);
    }

    public void draw(GraphicsContext gc, double camX, double camY, double scale) {
        // Only draw if not dead or if dead but animation not complete
        if (!isDead || (isDead && !deathAnimationComplete)) {
            drawEntity(gc, camX, camY, scale);
        }

        // Only draw health bar if alive
        if (!isDead) {
            drawHealthBar(gc, camX, camY, scale);
        }

        // Uncomment for debugging
        // drawDebugRectangle(gc, camX, camY, scale);
    }

    public Rectangle2D getHitbox() {
        return new Rectangle2D(
                x + collisionMargin,
                y + collisionMargin,
                width - 2 * collisionMargin,
                height - 2 * collisionMargin
        );
    }

    private void drawHealthBar(GraphicsContext gc, double camX, double camY, double scale) {
        double barWidth = 40;
        double barHeight = 6;
        double healthPercent = (double) currentHealth / maxHealth;
        double barX = (x - camX) * scale + width * scale / 2 - barWidth / 2;
        double barY = (y - camY) * scale - 15; // Adjusted position

        gc.setFill(Color.GRAY);
        gc.fillRect(barX, barY, barWidth, barHeight);

        gc.setFill(Color.RED);
        gc.fillRect(barX, barY, barWidth * healthPercent, barHeight);

        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1);
        gc.strokeRect(barX, barY, barWidth, barHeight);
    }

    private void drawDebugRectangle(GraphicsContext gc, double camX, double camY, double scale) {
        Rectangle2D hitbox = getHitbox();
        double debugRectX = (hitbox.getMinX() - camX) * scale;
        double debugRectY = (hitbox.getMinY() - camY) * scale;
        double debugRectWidth = hitbox.getWidth() * scale;
        double debugRectHeight = hitbox.getHeight() * scale;

        gc.setStroke(Color.BLUE);
        gc.setLineWidth(2);
        gc.strokeRect(debugRectX, debugRectY, debugRectWidth, debugRectHeight);
    }

    public void receiveDamage() {
        if (isDead) return;
        currentHealth--;
        if (currentHealth <= 0) {
            isDead = true;
            deathStartTime = System.nanoTime();
        } else {
            isHit = true;
            hitStartTime = System.nanoTime();
            currentFrame = 0;
        }
    }

    // Getters and setters
    public boolean isDead() { return isDead; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public boolean isFacingRight() { return facingRight; }
    public void setFacingRight(boolean facingRight) { this.facingRight = facingRight; }

    // Additional getters for animation states
    public boolean isFacingFront() { return isFacingFront; }
    public boolean isFacingBack() { return isFacingBack; }
    public boolean isFacingLeft() { return isFacingLeft; }
}