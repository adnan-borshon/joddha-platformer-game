package platformgame.Entity;

import javafx.scene.canvas.GraphicsContext;
import platformgame.Game;

public class Enemy extends Entity {

    private final int totalFramesIdle = 7;
    private final int totalFramesWalk = 8;
    private final int totalFramesRun = 6;
    private final int totalFramesAttack = 8;

    private boolean isIdle = true;
    private boolean isWalking = false;
    private boolean isRunning = false;
    private boolean isAttacking = false;

    private long attackStartTime = 0;
    private double patrolRange = 7 * gp.tileSize; // Patrol range
    private double patrolStartX, patrolStartY; // Initial patrol position
    private double patrolTargetX, patrolTargetY; // Target position for patrol
    private boolean isFollowingPlayer = false;

    public Enemy(double x, double y, double width, double height, double speed, Game gp) {
        super(x, y, width, height, speed, gp);
        imageSet(totalFramesIdle, "/image/Enemy.png");
        patrolStartX = x;
        patrolStartY = y;
        setNewPatrolTarget();
    }

    public void update(long deltaTime, long now) {
        // Distance to player
        double distanceToPlayer = Math.sqrt(Math.pow(x - gp.player.getX(), 2) + Math.pow(y - gp.player.getY(), 2));

        // If player is within attack range (2 tiles), start attacking
        if (distanceToPlayer <= 2 * gp.tileSize && !isAttacking) {
            startAttack(now);
        } else if (distanceToPlayer <= 6 * gp.tileSize) {
            // If player is within 6 tiles, start following the player
            startFollowingPlayer(now);
        } else {
            // If player is out of range, patrol
            patrol(now);
        }

        // Handle attack and movement animations
        if (isAttacking) {
            currentRow = 6; // Attack animation row
            int frameIndex = (int) ((now - attackStartTime) / 80_000_000);
            if (frameIndex < totalFramesAttack) {
                currentFrame = frameIndex;
            } else {
                currentFrame = 0;
                isAttacking = false;
                isWalking = false;
            }
        } else if (isWalking) {
            currentRow = 2; // Walking animation row
            currentFrame = (int) ((now / 100_000_000) % totalFramesWalk);
        } else if (isRunning) {
            currentRow = 3; // Running animation row
            currentFrame = (int) ((now / 100_000_000) % totalFramesRun);
        } else {
            currentRow = 1; // Idle animation row
            currentFrame = (int) ((now / 100_000_000) % totalFramesIdle);
        }
    }

    // Starts the attack animation
    private void startAttack(long now) {
        isAttacking = true;
        attackStartTime = now;
        isWalking = false;
        isRunning = false;
    }

    // Starts following the player
    private void startFollowingPlayer(long now) {
        isFollowingPlayer = true;
        isWalking = false;
        isRunning = true;

        // Move towards the player
        moveTowardsTarget(gp.player.getX(), gp.player.getY(), speed);
    }

    // Patrols between points
    private void patrol(long now) {
        isFollowingPlayer = false;
        isWalking = true;
        isRunning = false;

        // If patrol target reached, set a new target
        if (hasReachedTarget()) {
            setNewPatrolTarget();
        }

        // Move towards patrol target
        moveTowardsTarget(patrolTargetX, patrolTargetY, speed * 0.5); // Slower speed while patrolling
    }

    // Sets a new patrol target within the patrol range
    private void setNewPatrolTarget() {
        patrolTargetX = patrolStartX + (Math.random() * patrolRange) - (patrolRange / 2);
        patrolTargetY = patrolStartY + (Math.random() * patrolRange) - (patrolRange / 2);

        // Ensure patrol target stays within the patrol bounds
        patrolTargetX = Math.max(patrolStartX, Math.min(patrolTargetX, patrolStartX + patrolRange));
        patrolTargetY = Math.max(patrolStartY, Math.min(patrolTargetY, patrolStartY + patrolRange));
    }

    // Checks if the enemy has reached its patrol target
    private boolean hasReachedTarget() {
        double distance = Math.sqrt(Math.pow(x - patrolTargetX, 2) + Math.pow(y - patrolTargetY, 2));
        return distance < 10; // Consider reached if within 10 pixels
    }

    // Moves the enemy towards a target (either the player or patrol target)
    private void moveTowardsTarget(double targetX, double targetY, double moveSpeed) {
        if (Math.abs(x - targetX) > moveSpeed) {
            if (x < targetX) {
                x += moveSpeed;
                facingRight = true;  // Moving right
            } else {
                x -= moveSpeed;
                facingRight = false; // Moving left
            }
        } else {
            x = targetX; // Snap to target if close enough
        }

        if (Math.abs(y - targetY) > moveSpeed) {
            if (y < targetY) {
                y += moveSpeed;
            } else {
                y -= moveSpeed;
            }
        } else {
            y = targetY; // Snap to target if close enough
        }
    }

    public void draw(GraphicsContext gc, double camX, double camY, double scale) {
        // Draw the enemy entity with the current animation state and facing direction
        drawEntity(gc, camX, camY, scale);
    }

    // Collision detection with other entities (NPCs, Player, Scout, etc.)
    public boolean checkCollisionWithEntities() {
        return isColliding(x, y);
    }

    // Getter methods for position and size
    public double getX() { return x; }
    public double getY() { return y; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }

    // Getter for facing direction
    public boolean isFacingRight() { return facingRight; }

    // Setter for facing direction
    public void setFacingRight(boolean facingRight) { this.facingRight = facingRight; }
}
