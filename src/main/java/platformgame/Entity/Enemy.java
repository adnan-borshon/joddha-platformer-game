package platformgame.Entity;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import platformgame.Game;

public class Enemy extends Entity {

    protected final int totalFramesIdle = 7;
    protected final int totalFramesWalk = 8;
    protected final int totalFramesRun = 6;
    protected final int totalFramesAttack = 8;

    protected boolean isIdle = true;
    protected boolean isWalking = false;
    protected boolean isRunning = false;
    protected boolean isAttacking = false;

    protected long attackStartTime = 0;
    protected double patrolRange = 7 * gp.tileSize;
    protected double patrolStartX, patrolStartY;
    protected double patrolTargetX, patrolTargetY;
    protected boolean isFollowingPlayer = false;

    // 🔴 Health system
    protected int maxHealth = 5;
    protected int currentHealth = 5;
    protected boolean isDead = false;

    // ✅ Damage Cooldown (2 seconds)
    protected long lastAttackTime = 0;
    protected long damageCooldown = 2_000_000_000L;

    // ✅ Hit Animation (Row 9)
    protected boolean isHit = false;
    protected long hitStartTime = 0;
    protected final int hitRow = 9;
    protected final int totalHitFrames = 4;
    protected final long hitFrameDuration = 100_000_000L;

    public Enemy(double x, double y, double width, double height, double speed, Game gp) {
        super(x, y, width, height, speed, gp);
        imageSet(totalFramesIdle, "/image/Enemy.png");
        patrolStartX = x;
        patrolStartY = y;
        setNewPatrolTarget();
    }

    public void update(long deltaTime, long now) {
        if (isDead) return;

        // ✅ If in hit animation, override everything else
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

        if (isAttacking) {
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
    }

    protected void startAttack(long now) {
        isAttacking = true;
        attackStartTime = now;
        isWalking = false;
        isRunning = false;
    }

    protected void startFollowingPlayer(long now) {
        isFollowingPlayer = true;
        isWalking = false;
        isRunning = true;
        moveTowardsTarget(gp.player.getX(), gp.player.getY(), speed);
    }

    protected void patrol(long now) {
        isFollowingPlayer = false;
        isWalking = true;
        isRunning = false;

        if (hasReachedTarget()) {
            setNewPatrolTarget();
        }

        moveTowardsTarget(patrolTargetX, patrolTargetY, speed * 0.5);
    }

    private void setNewPatrolTarget() {
        patrolTargetX = patrolStartX + (Math.random() * patrolRange) - (patrolRange / 2);
        patrolTargetY = patrolStartY + (Math.random() * patrolRange) - (patrolRange / 2);

        patrolTargetX = Math.max(patrolStartX, Math.min(patrolTargetX, patrolStartX + patrolRange));
        patrolTargetY = Math.max(patrolStartY, Math.min(patrolTargetY, patrolStartY + patrolRange));
    }

    private boolean hasReachedTarget() {
        double distance = Math.sqrt(Math.pow(x - patrolTargetX, 2) + Math.pow(y - patrolTargetY, 2));
        return distance < 10;
    }

    protected void moveTowardsTarget(double targetX, double targetY, double moveSpeed) {
        double newX = x;
        double newY = y;

        if (Math.abs(x - targetX) > moveSpeed) {
            if (x < targetX) {
                newX += moveSpeed;
                facingRight = true;
            } else {
                newX -= moveSpeed;
                facingRight = false;
            }
        } else {
            newX = targetX;
        }

        if (!isColliding(newX, y)) {
            x = newX;
        }

        if (Math.abs(y - targetY) > moveSpeed) {
            if (y < targetY) {
                newY += moveSpeed;
            } else {
                newY -= moveSpeed;
            }
        } else {
            newY = targetY;
        }

        if (!isColliding(x, newY)) {
            y = newY;
        }
    }


    public void draw(GraphicsContext gc, double camX, double camY, double scale) {
        if (isDead) return;

        // Draw the actual enemy (including animations)
        drawEntity(gc, camX, camY, scale);

        // 🔴 Draw health bar above enemy
        double barWidth = 40;
        double barHeight = 6;
        double healthPercent = (double) currentHealth / maxHealth;
        double barX = (x - camX) * scale + width * scale / 2 - barWidth / 2;
        double barY = (y - camY) * scale - 40;

        gc.setFill(Color.GRAY);
        gc.fillRect(barX, barY, barWidth, barHeight);

        gc.setFill(Color.RED);
        gc.fillRect(barX, barY, barWidth * healthPercent, barHeight);

        gc.setStroke(Color.BLACK);
        gc.strokeRect(barX, barY, barWidth, barHeight);

        // 🔴 Debug Rectangle: Show bounding box around the enemy entity
        double debugRectX = (x - camX) * scale;
        double debugRectY = (y - camY) * scale;
        double debugRectWidth = width * scale;
        double debugRectHeight = height * scale;

        gc.setStroke(Color.BLUE);  // Color for the debug rectangle
        gc.setLineWidth(2);        // Make the outline a little thicker
        gc.strokeRect(debugRectX, debugRectY, debugRectWidth, debugRectHeight);
    }


    // 🔴 Called from player punch detection
    public void receiveDamage() {
        if (isDead) return;

        currentHealth--;
        if (currentHealth <= 0) {
            isDead = true;
        } else {
            // ✅ Trigger hit animation
            isHit = true;
            hitStartTime = System.nanoTime();
            currentFrame = 0;
        }
    }

    public boolean isDead() {
        return isDead;
    }

    public boolean checkCollisionWithEntities() {
        return isColliding(x, y);
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public boolean isFacingRight() { return facingRight; }
    public void setFacingRight(boolean facingRight) { this.facingRight = facingRight; }
}
