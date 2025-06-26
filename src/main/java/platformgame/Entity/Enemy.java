package platformgame.Entity;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
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
    private double patrolRange = 7 * gp.tileSize;
    private double patrolStartX, patrolStartY;
    private double patrolTargetX, patrolTargetY;
    private boolean isFollowingPlayer = false;

    // 🔴 Health system
    private int maxHealth = 5;
    private int currentHealth = 5;
    private boolean isDead = false;

    // Damage cooldown
    private long lastAttackTime = 0;
    private long damageCooldown = 1_000_000_000L; // 1 second

    public Enemy(double x, double y, double width, double height, double speed, Game gp) {
        super(x, y, width, height, speed, gp);
        imageSet(totalFramesIdle, "/image/Enemy.png");
        patrolStartX = x;
        patrolStartY = y;
        setNewPatrolTarget();
    }

    public void update(long deltaTime, long now) {
        if (isDead) return;

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
            int frameIndex = (int) ((now - attackStartTime) / 80_000_000);
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

    private void startAttack(long now) {
        isAttacking = true;
        attackStartTime = now;
        isWalking = false;
        isRunning = false;
    }

    private void startFollowingPlayer(long now) {
        isFollowingPlayer = true;
        isWalking = false;
        isRunning = true;
        moveTowardsTarget(gp.player.getX(), gp.player.getY(), speed);
    }

    private void patrol(long now) {
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

    private void moveTowardsTarget(double targetX, double targetY, double moveSpeed) {
        if (Math.abs(x - targetX) > moveSpeed) {
            if (x < targetX) {
                x += moveSpeed;
                facingRight = true;
            } else {
                x -= moveSpeed;
                facingRight = false;
            }
        } else {
            x = targetX;
        }

        if (Math.abs(y - targetY) > moveSpeed) {
            if (y < targetY) {
                y += moveSpeed;
            } else {
                y -= moveSpeed;
            }
        } else {
            y = targetY;
        }
    }

    public void draw(GraphicsContext gc, double camX, double camY, double scale) {
        if (isDead) return;

        drawEntity(gc, camX, camY, scale);

        // 🔴 Draw health bar above enemy
        double barWidth = 40;
        double barHeight = 6;
        double healthPercent = (double) currentHealth / maxHealth;
        double barX = (x - camX) * scale + width * scale / 2 - barWidth / 2;
        double barY = (y - camY) * scale -20 -20;

        gc.setFill(Color.GRAY);
        gc.fillRect(barX, barY, barWidth, barHeight);

        gc.setFill(Color.RED);
        gc.fillRect(barX, barY, barWidth * healthPercent, barHeight);

        gc.setStroke(Color.BLACK);
        gc.strokeRect(barX, barY, barWidth, barHeight);
    }

    // 🔴 Call this from player punch detection
    public void receiveDamage() {
        if (isDead) return;
        currentHealth--;
        if (currentHealth <= 0) {
            isDead = true;
            // Optionally trigger death animation or remove from entity list
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
