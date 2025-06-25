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

    // Health and death
    private int health = 3;
    private boolean isDead = false;
    private boolean visibleHealthBar = false;
    private long deathStartTime;
    private final int totalDeathFrames = 6;
    private final int deathAnimationRow = 6;
    private final long deathFrameDuration = 120_000_000;

    private boolean canBeAttacked = false;

    public Scout(double x, double y, double width, double height, double speed, Game gp) {
        super(x, y, width, height, speed, gp);
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
        if (health <= 0) {
            isDead = true;
            runningToBase = false;
            currentFrame = 0;
            currentRow = deathAnimationRow;
            deathStartTime = System.nanoTime();
        }
    }

    public Rectangle2D getHitbox() {
        return new Rectangle2D(x, y, width, height);
    }

    public void update(long deltaTime, long now) {
        if (isDead) {
            int frameIndex = (int) ((now - deathStartTime) / deathFrameDuration);
            if (frameIndex < totalDeathFrames) {
                currentRow = deathAnimationRow;
                currentFrame = frameIndex;
            } else {
                currentFrame = totalDeathFrames - 1;
            }
            return;
        }

        if (idleAtBase) return;

        if (showingDialogue) {
            if (now - dialogueStartTime >= dialogueDuration) {
                showingDialogue = false;
                runningToBase = true;
                canBeAttacked = true;
            }
            return;
        }

        if (runningToBase) {
            Rectangle2D playerHitbox = new Rectangle2D(gp.player.getX(), gp.player.getY(), gp.player.getWidth(), gp.player.getHeight());
            Rectangle2D scoutHitbox = getHitbox();

            if (playerHitbox.intersects(scoutHitbox)) {
                if (canBeAttacked) {
                    takeDamage();
                    if (isDead) {
                        runningToBase = false;
                        currentRow = deathAnimationRow;
                        currentFrame = 0;
                    }
                }
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
                returnToPatrolling();
            }
            return;
        }

        double distanceToPlayer = Math.sqrt(Math.pow(x - gp.player.getX(), 2) + Math.pow(y - gp.player.getY(), 2));

        if (distanceToPlayer <= 3 * gp.tileSize && !hasSeenPlayer) {
            hasSeenPlayer = true;
            showingDialogue = true;
            dialogueStartTime = now;
            return;
        }

        if (distanceToPlayer <= 3 * gp.tileSize && !isFollowingPlayer && hasSeenPlayer && !runningToBase) {
            isFollowingPlayer = true;
        }

        if (isFollowingPlayer && !runningToBase) {
            followPlayer(now, distanceToPlayer);
        } else if (!runningToBase) {
            patrol(now);
        }
    }

    private void runToBase(long now) {
        double distanceToBase = Math.sqrt(Math.pow(x - baseCampX, 2) + Math.pow(y - baseCampY, 2));
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
        patrolTargetX = Math.max(patrolStartX, Math.min(patrolTargetX, patrolStartX + patrolRange));
        patrolTargetY = Math.max(patrolStartY, Math.min(patrolTargetY, patrolStartY + patrolRange));
        movingToTarget = true;
    }

    private boolean hasReachedTarget() {
        double distance = Math.sqrt(Math.pow(x - patrolTargetX, 2) + Math.pow(y - patrolTargetY, 2));
        return distance < 10;
    }

    private void moveTowardsTarget(double targetX, double targetY, double moveSpeed) {
        if (Math.abs(x - targetX) < moveSpeed && Math.abs(y - targetY) < moveSpeed) {
            x = targetX;
            y = targetY;
            return;
        }
        if (Math.random() > 0.5) {
            if (Math.abs(x - targetX) > moveSpeed) {
                if (x < targetX) {
                    x += moveSpeed;
                    facingRight = true;
                } else {
                    x -= moveSpeed;
                    facingRight = false;
                }
            }
        } else {
            if (Math.abs(y - targetY) > moveSpeed) {
                if (y < targetY) y += moveSpeed;
                else y -= moveSpeed;
            }
        }
        if (!runningToBase) {
            x = Math.max(patrolStartX, Math.min(x, patrolStartX + patrolRange));
            y = Math.max(patrolStartY, Math.min(y, patrolStartY + patrolRange));
        }
    }

    private void followPlayer(long now, double distanceToPlayer) {
        double playerX = gp.player.getX();
        double playerY = gp.player.getY();

        if (distanceToPlayer > 6 * gp.tileSize) {
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
            moveTowardsTarget(playerX, playerY, speed);
            currentRow = 2;
            currentFrame = (int) ((System.nanoTime() / 100_000_000) % totalFramesWalk);
        }
    }

    private void returnToPatrolling() {
        playerInRange = false;
        isFollowingPlayer = false;
        movingToTarget = false;
        patrolStartX = x;
        patrolStartY = y;
        setNewPatrolTarget();
    }

    public void draw(GraphicsContext gc, double camX, double camY, double scale) {
        drawEntity(gc, camX, camY, scale);

        // 🔲 Debug hitbox rectangle
        gc.save();
        gc.setLineWidth(1);
        gc.setStroke(Color.LIME); // Use a bright color for visibility
        double drawX = (x - camX) * scale;
        double drawY = (y - camY) * scale;
        double drawW = width * scale;
        double drawH = height * scale;
        gc.strokeRect(drawX, drawY, drawW, drawH);
        gc.restore();

        if (showingDialogue) drawDialogue(gc, camX, camY, scale);
        if (visibleHealthBar && !isDead) drawHealthBar(gc, camX, camY, scale);
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

    private void drawDialogue(GraphicsContext gc, double camX, double camY, double scale) {
        double screenX = (x - camX) * scale;
        double screenY = (y - camY) * scale - 60;

        gc.setFill(Color.BLACK);
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2);
        double boxWidth = 100;
        double boxHeight = 30;
        gc.fillRoundRect(screenX - boxWidth / 2, screenY - boxHeight, boxWidth, boxHeight, 10, 10);
        gc.strokeRoundRect(screenX - boxWidth / 2, screenY - boxHeight, boxWidth, boxHeight, 10, 10);

        gc.setFill(Color.RED);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        gc.fillText("TRAITOR!", screenX - 30, screenY - 10);

        gc.setFill(Color.BLACK);
        double[] xPoints = {screenX - 5, screenX + 5, screenX};
        double[] yPoints = {screenY, screenY, screenY + 10};
        gc.fillPolygon(xPoints, yPoints, 3);
    }

    public void setPlayerInRange(boolean inRange) {
        this.playerInRange = inRange;
        if (inRange && !isFollowingPlayer && !runningToBase) {
            double distanceToPlayer = Math.sqrt(Math.pow(x - gp.player.getX(), 2) + Math.pow(y - gp.player.getY(), 2));
            if (distanceToPlayer <= 3 * gp.tileSize && !hasSeenPlayer) {
                hasSeenPlayer = true;
                showingDialogue = true;
                dialogueStartTime = System.nanoTime();
            } else if (distanceToPlayer <= 3 * gp.tileSize && hasSeenPlayer && !runningToBase) {
                isFollowingPlayer = true;
            }
        }
    }
}
