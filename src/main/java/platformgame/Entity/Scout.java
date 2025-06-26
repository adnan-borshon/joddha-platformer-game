package platformgame.Entity;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import platformgame.Game;
import javafx.geometry.Rectangle2D;

public class Scout extends Entity {

    private final int totalFramesWalk = 10;
    private final int totalFramesRun = 8;
    private final int totalFramesAttack = 12;

    private boolean playerInRange = false;
    private boolean attacking = false;
    private long attackStartTime;
    private int patrolRange = 6 * 32;
    private double patrolStartX, patrolStartY;
    private double patrolTargetX, patrolTargetY;
    private boolean facingRight = true;
    private boolean isFollowingPlayer = false;
    private boolean movingToTarget = false;

    private boolean hasSeenPlayer = false;
    private boolean showingDialogue = false;
    private boolean runningToBase = false;
    private boolean idleAtBase = false;
    private long dialogueStartTime;
    private final long dialogueDuration = 2_000_000_000L;
    private double baseCampX = 53 * 32;
    private double baseCampY = 23 * 32;
    private final double runSpeed = speed * 0.5;

    private int health = 3;
    private boolean isDead = false;
    private boolean visibleHealthBar = false;
    private long deathStartTime;
    private final int totalDeathFrames = 5;
    private final int deathAnimationRow = 9;
    private final long deathFrameDuration = 120_000_000;

    private boolean canBeAttacked = false;
    private boolean inCombat = false;
    private boolean isAggressive = false;
    private long lastAttackTime = 0;
    private final long attackCooldown = 1_500_000_000L;
    private final double combatRange = 2 * 32;
    private final double aggroRange = 4 * 32;
    private final double combatSpeed = speed * 1.2;

    private Game gp;

    public Scout(double x, double y, double width, double height, double speed, Game gp) {
        super(x, y, width, height, speed, gp);
        this.gp = gp;
        imageSet(totalFramesWalk, "/image/Scout.png");
        patrolStartX = x;
        patrolStartY = y;
        baseCampX = 53 * gp.tileSize;
        baseCampY = 23 * gp.tileSize;
        setNewPatrolTarget();
    }

    public void takeDamage() {
        if (!canBeAttacked || isDead) return;

        health--;
        visibleHealthBar = true;

        if (!isAggressive && !isDead) {
            isAggressive = true;
            inCombat = true;
            isFollowingPlayer = true;
            runningToBase = false;

            if (gp.player.getX() < x) facingRight = false;
            else facingRight = true;
        }

        if (health <= 0) {
            isDead = true;
            runningToBase = false;
            inCombat = false;
            isAggressive = false;
            currentFrame = 0;
            currentRow = deathAnimationRow;
            deathStartTime = System.nanoTime();
        }


    }



    public Rectangle2D getHitbox() {
        return new Rectangle2D(x, y, width, height);
    }

    public boolean canDamagePlayer() {
        if (!attacking || isDead) return false;
        long now = System.nanoTime();
        int frameIndex = (int) ((now - attackStartTime) / 100_000_000);
        return frameIndex >= 6 && frameIndex <= 8;
    }

    public void update(long deltaTime, long now) {
        if (isDead) {
            int frameIndex = (int) ((now - deathStartTime) / deathFrameDuration);
            if (frameIndex < totalDeathFrames) {
                currentRow = deathAnimationRow;
                currentFrame = frameIndex;
            } else {
                currentFrame = totalDeathFrames;
                if (gp.scout != null) gp.scout[0] = null;
            }
            return;
        }

        if (idleAtBase) return;

        if (showingDialogue) {
            if (now - dialogueStartTime >= dialogueDuration) {
                showingDialogue = false;
                if (!isAggressive) runningToBase = true;
                canBeAttacked = true;
            }
            return;
        }

        double distanceToPlayer = Math.hypot(x - gp.player.getX(), y - gp.player.getY());
        if (isAggressive && !isDead) {
            handleCombat(now, distanceToPlayer);
            return;
        }

        if (runningToBase) {
            Rectangle2D playerHitbox = new Rectangle2D(gp.player.getX(), gp.player.getY(), gp.player.getWidth(), gp.player.getHeight());
            Rectangle2D scoutHitbox = getHitbox();
            if (playerHitbox.intersects(scoutHitbox)) {
                if (canBeAttacked) takeDamage();
                return;
            }
            runToBase(now);
            return;
        }

        if (attacking) {
            currentRow = 5;
            int frameIndex = (int) ((now - attackStartTime) / 100_000_000);
            if (frameIndex < totalFramesAttack) {
                currentFrame = frameIndex;
            } else {
                currentFrame = 0;
                attacking = false;
                lastAttackTime = now;
                if (!isAggressive) returnToPatrolling();
            }
            return;
        }

        if (distanceToPlayer <= 3 * gp.tileSize && !hasSeenPlayer) {
            hasSeenPlayer = true;
            showingDialogue = true;
            dialogueStartTime = now;
            return;
        }

        if (distanceToPlayer <= 3 * gp.tileSize && !isFollowingPlayer && hasSeenPlayer && !runningToBase) {
            isFollowingPlayer = true;
        }

        if (isFollowingPlayer && !runningToBase) followPlayer(now, distanceToPlayer);
        else if (!runningToBase) patrol(now);
    }

    private void handleCombat(long now, double distanceToPlayer) {
        double dx = gp.player.getX() - x;
        facingRight = dx > 0;

        if (distanceToPlayer > aggroRange * 2) {
            isAggressive = false;
            inCombat = false;
            returnToPatrolling();
            return;
        }

        if (distanceToPlayer > combatRange && !attacking) {
            moveTowardsPlayer(combatSpeed);
            currentRow = 3;
            currentFrame = (int) ((System.nanoTime() / 80_000_000) % totalFramesRun);
        }

        if (distanceToPlayer <= combatRange && !attacking && (now - lastAttackTime) >= attackCooldown) {
            attackStartTime = now;
            attacking = true;
            currentRow = 5;
            currentFrame = 0;
        }

        if (attacking) {
            currentRow = 5;
            int frameIndex = (int) ((now - attackStartTime) / 100_000_000);
            if (frameIndex < totalFramesAttack) {
                currentFrame = frameIndex;
            } else {
                currentFrame = 0;
                attacking = false;
                lastAttackTime = now;
            }
        }
    }

    private void moveTowardsPlayer(double moveSpeed) {
        double dx = gp.player.getX() - x;
        double dy = gp.player.getY() - y;
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance > moveSpeed) {
            dx = (dx / distance) * moveSpeed;
            dy = (dy / distance) * moveSpeed;
            x += dx;
            y += dy;
            facingRight = dx > 0;
        }
    }

    private void runToBase(long now) {
        double distanceToBase = Math.hypot(x - baseCampX, y - baseCampY);
        if (distanceToBase < gp.tileSize) {
            x = baseCampX;
            y = baseCampY;
            runningToBase = false;
            idleAtBase = true;
            currentFrame = 0;
            currentRow = 2;
            return;
        }
        moveTowardsTarget(baseCampX, baseCampY, runSpeed);
        currentRow = 3;
        currentFrame = (int) ((System.nanoTime() / 100_000_000) % totalFramesRun);
    }

    private void patrol(long now) {
        if (!movingToTarget || hasReachedTarget()) setNewPatrolTarget();
        moveTowardsTarget(patrolTargetX, patrolTargetY, speed * 0.5);
        currentRow = 2;
        currentFrame = (int) ((System.nanoTime() / 150_000_000) % totalFramesWalk);
    }

    private void setNewPatrolTarget() {
        patrolTargetX = patrolStartX + (Math.random() * patrolRange) - (patrolRange / 2);
        patrolTargetY = patrolStartY + (Math.random() * patrolRange) - (patrolRange / 2);
        movingToTarget = true;
    }

    private boolean hasReachedTarget() {
        return Math.hypot(x - patrolTargetX, y - patrolTargetY) < 10;
    }

    private void moveTowardsTarget(double targetX, double targetY, double moveSpeed) {
        double dx = targetX - x;
        double dy = targetY - y;
        double distance = Math.hypot(dx, dy);
        if (distance > moveSpeed) {
            dx = (dx / distance) * moveSpeed;
            dy = (dy / distance) * moveSpeed;
            x += dx;
            y += dy;
            facingRight = dx > 0;
        }
    }

    private void followPlayer(long now, double distanceToPlayer) {
        double dx = gp.player.getX() - x;
        facingRight = dx > 0;

        if (distanceToPlayer > 6 * gp.tileSize && !isAggressive) {
            isFollowingPlayer = false;
            returnToPatrolling();
            return;
        }

        if (distanceToPlayer < 2 * gp.tileSize && !attacking) {
            attackStartTime = now;
            attacking = true;
            currentRow = 5;
            currentFrame = 0;
            return;
        }

        if (!attacking) {
            moveTowardsTarget(gp.player.getX(), gp.player.getY(), speed);
            currentRow = 2;
            currentFrame = (int) ((System.nanoTime() / 100_000_000) % totalFramesWalk);
        }
    }

    private void returnToPatrolling() {
        playerInRange = false;
        isFollowingPlayer = false;
        movingToTarget = false;
        inCombat = false;
        isAggressive = false;
        patrolStartX = x;
        patrolStartY = y;
        setNewPatrolTarget();
    }

    public void draw(GraphicsContext gc, double camX, double camY, double scale) {
        drawEntity(gc, camX, camY, scale);
        gc.save();
        gc.setLineWidth(1);
        gc.setStroke(isAggressive ? Color.RED : Color.LIME);
        double drawX = (x - camX) * scale;
        double drawY = (y - camY) * scale;
        gc.strokeRect(drawX, drawY, width * scale, height * scale);
        gc.restore();

        if (showingDialogue) drawDialogue(gc, camX, camY, scale);
        if (visibleHealthBar && !isDead) drawHealthBar(gc, camX, camY, scale);
        if (isAggressive && !isDead) drawCombatIndicator(gc, camX, camY, scale);
    }

    private void drawHealthBar(GraphicsContext gc, double camX, double camY, double scale) {
        double barWidth = 40;
        double barHeight = 6;
        double healthRatio = health / 3.0;
        double screenX = (x - camX) * scale + width * scale / 2 - barWidth / 2;
        double screenY = (y - camY) * scale - 10;
        gc.setFill(Color.GRAY);
        gc.fillRoundRect(screenX, screenY, barWidth, barHeight, 4, 4);
        gc.setFill(Color.RED);
        gc.fillRoundRect(screenX, screenY, barWidth * healthRatio, barHeight, 4, 4);
        gc.setStroke(Color.BLACK);
        gc.strokeRoundRect(screenX, screenY, barWidth, barHeight, 4, 4);
    }

    private void drawCombatIndicator(GraphicsContext gc, double camX, double camY, double scale) {
        double screenX = (x - camX) * scale + width * scale / 2;
        double screenY = (y - camY) * scale - 20;
        gc.setFill(Color.RED);
        gc.fillText("!", screenX - 3, screenY);
    }

    private void drawDialogue(GraphicsContext gc, double camX, double camY, double scale) {
        double screenX = (x - camX) * scale;
        double screenY = (y - camY) * scale - 60;
        gc.setFill(Color.BLACK);
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2);
        gc.fillRoundRect(screenX - 50, screenY - 30, 100, 30, 10, 10);
        gc.strokeRoundRect(screenX - 50, screenY - 30, 100, 30, 10, 10);
        gc.setFill(Color.RED);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        gc.fillText("TRAITOR!", screenX - 30, screenY - 10);
        gc.setFill(Color.BLACK);
        gc.fillPolygon(new double[]{screenX - 5, screenX + 5, screenX}, new double[]{screenY, screenY, screenY + 10}, 3);
    }

    public void setPlayerInRange(boolean inRange) {
        this.playerInRange = inRange;
        if (inRange && !isFollowingPlayer && !runningToBase) {
            double distanceToPlayer = Math.hypot(x - gp.player.getX(), y - gp.player.getY());
            if (distanceToPlayer <= 3 * gp.tileSize && !hasSeenPlayer) {
                hasSeenPlayer = true;
                showingDialogue = true;
                dialogueStartTime = System.nanoTime();
            } else if (distanceToPlayer <= 3 * gp.tileSize && hasSeenPlayer && !runningToBase) {
                isFollowingPlayer = true;
            }
        }
    }

    public boolean isAggressive() { return isAggressive; }
    public boolean isInCombat() { return inCombat; }


}
