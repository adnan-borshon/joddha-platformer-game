package platformgame.Entity;

import javafx.scene.canvas.GraphicsContext;
import platformgame.Game;

public class Scout extends Entity {

    private final int totalFramesWalk = 10;
    private final int totalFramesAttack = 12;

    private boolean playerInRange = false;
    private boolean attacking = false;
    private long attackStartTime;
    private int patrolRange = 6 * 32;  // Fixed patrol range (6 tiles)
    private double patrolStartX, patrolStartY;  // Original patrol starting position
    private double patrolTargetX, patrolTargetY;  // Current patrol target
    private boolean facingRight = true;
    private boolean isFollowingPlayer = false;
    private boolean movingToTarget = false;

    public Scout(double x, double y, double width, double height, double speed, Game gp) {
        super(x, y, width, height, speed, gp);
        imageSet(totalFramesWalk, "/image/Scout.png");  // Walk animation (3rd row)
        patrolStartX = x;
        patrolStartY = y;
        // Set initial patrol target
        setNewPatrolTarget();
    }

    public void update(long deltaTime, long now) {
        if (attacking) {
            currentRow = 5;  // Attack animation
            int frameIndex = (int) ((now - attackStartTime) / 100_000_000);
            if (frameIndex < totalFramesAttack) {
                currentFrame = frameIndex;
            } else {
                currentFrame = 0;
                attacking = false;
                returnToPatrolling();  // After attack, return to patrol
            }
            return;  // Skip movement during attack animation
        }

        // Check if player is within detection range (3 tiles)
        double distanceToPlayer = Math.sqrt(
                Math.pow(x - gp.player.getX(), 2) + Math.pow(y - gp.player.getY(), 2)
        );

        // Only start following player if within range and not already following
        if (distanceToPlayer <= 3 * gp.tileSize && !isFollowingPlayer) {
            isFollowingPlayer = true;
        }

        if (isFollowingPlayer) {
            followPlayer(now, distanceToPlayer);
        } else {
            patrol(now);
        }
    }

    private void patrol(long now) {
        // If we don't have a target or reached current target, set new one
        if (!movingToTarget || hasReachedTarget()) {
            setNewPatrolTarget();
        }

        // Move towards patrol target with controlled speed
        moveTowardsTarget(patrolTargetX, patrolTargetY, speed * 0.5); // Slower patrol speed

        // Update animation for walking - always use row 2 (right-facing), flip will be handled in draw
        currentRow = 2;  // Always use right-facing walk animation
        currentFrame = (int) ((now / 150_000_000) % totalFramesWalk);  // Slower animation
    }

    private void setNewPatrolTarget() {
        // Generate random target within patrol range
        patrolTargetX = patrolStartX + (Math.random() * patrolRange) - (patrolRange / 2);
        patrolTargetY = patrolStartY + (Math.random() * patrolRange) - (patrolRange / 2);

        // Ensure target stays within patrol bounds
        patrolTargetX = Math.max(patrolStartX, Math.min(patrolTargetX, patrolStartX + patrolRange));
        patrolTargetY = Math.max(patrolStartY, Math.min(patrolTargetY, patrolStartY + patrolRange));

        movingToTarget = true;
    }

    private boolean hasReachedTarget() {
        double distance = Math.sqrt(
                Math.pow(x - patrolTargetX, 2) + Math.pow(y - patrolTargetY, 2)
        );
        return distance < 10; // Consider reached if within 10 pixels
    }

    private void moveTowardsTarget(double targetX, double targetY, double moveSpeed) {
        // Move towards target with specified speed
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

        // Ensure Scout stays within patrol bounds
        x = Math.max(patrolStartX, Math.min(x, patrolStartX + patrolRange));
        y = Math.max(patrolStartY, Math.min(y, patrolStartY + patrolRange));
    }

    private void followPlayer(long now, double distanceToPlayer) {
        double playerX = gp.player.getX();
        double playerY = gp.player.getY();

        // Check if player is out of follow range (6 tiles) and return to patrolling
        if (distanceToPlayer > 6 * gp.tileSize) {
            isFollowingPlayer = false;
            returnToPatrolling();
            return;
        }

        // If close enough to attack (2 tiles), start attack
        if (distanceToPlayer < 2 * gp.tileSize && !attacking) {
            attackStartTime = now;
            attacking = true;
            currentRow = 5;  // Set attack animation row
            currentFrame = 0;
            return; // Don't move while starting attack
        }

        // Move towards player if not attacking
        if (!attacking) {
            moveTowardsTarget(playerX, playerY, speed);

            // Update animation for walking - always use row 2, flip will be handled in draw
            currentRow = 2;  // Always use right-facing walk animation
            currentFrame = (int) ((now / 100_000_000) % totalFramesWalk);
        }
    }

    private void returnToPatrolling() {
        playerInRange = false;
        isFollowingPlayer = false;
        movingToTarget = false;
        // Set new patrol target around current position
        patrolStartX = x;
        patrolStartY = y;
        setNewPatrolTarget();
    }

    public void draw(GraphicsContext gc, double camX, double camY, double scale) {
        // The Entity class will automatically handle flipping based on facingRight
        drawEntity(gc, camX, camY, scale);
    }

    public void setPlayerInRange(boolean inRange) {
        this.playerInRange = inRange;
        if (inRange && !isFollowingPlayer) {
            // When player collides, check if should start following
            double distanceToPlayer = Math.sqrt(
                    Math.pow(x - gp.player.getX(), 2) + Math.pow(y - gp.player.getY(), 2)
            );
            if (distanceToPlayer <= 3 * gp.tileSize) {
                isFollowingPlayer = true;
            }
        }
    }
}
