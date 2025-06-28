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
    private final int totalFramesAttack = 5;
    private final int attackRow = 7;

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
    private final int deathAnimationRow = 8;
    private final long deathFrameDuration = 100_000_000;

    // ✅ FIXED: Hit reaction animation
    private boolean reactingToHit = false;
    private long hitReactionStartTime;
    private final int hitReactionFrames = 4;
    private final int hitReactionRow = 8; // 9th row (0-indexed)
    private final long hitFrameDuration = 70_000_000;

    private boolean canBeAttacked = true; // ✅ FIXED: Default to true so player can hit scout
    private boolean inCombat = false;
    private boolean isAggressive = false;
    private long lastAttackTime = 0;
    private final long attackCooldown = 1_500_000_000L;
    private final double combatRange = 2 * 32;
    private final double aggroRange = 3 * 32;
    private final double combatSpeed = speed * 1.3;

    // ✅ UPDATED: Much smaller detection range (touching distance)
    private final double touchDetectionRange = 5 * 32; // About 1.2 tiles instead of 3

    private String customDialogue = null;
    private Game gp;
    private boolean hasDealtDamageThisAttack = false;

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
        if (isDead || reactingToHit) return; // ✅ FIXED: Removed canBeAttacked check

        health--;
        visibleHealthBar = true;

        // ✅ FIXED: Always trigger hit animation when taking damage
        reactingToHit = true;
        hitReactionStartTime = System.nanoTime();
        currentFrame = 0;
        currentRow = hitReactionRow;

        // ✅ FIXED: Stop any current attack when hit
        attacking = false;
        hasDealtDamageThisAttack = false;

        if (!isAggressive && health > 0) { // ✅ FIXED: Only become aggressive if still alive
            isAggressive = true;
            inCombat = true;
            isFollowingPlayer = true;
            runningToBase = false;
            facingRight = gp.player.getX() > x;
        }

        // ✅ FIXED: Death is handled in update() after hit animation finishes
        System.out.println("Scout took damage! Health: " + health); // Debug
    }

    public Rectangle2D getHitbox() {
        return new Rectangle2D(x - 10, y + 4, width+25, height);
    }

    public boolean canDamagePlayer() {
        if (!attacking || isDead) return false;
        long now = System.nanoTime();
        int frameIndex = (int) ((now - attackStartTime) / 100_000_000);
        return frameIndex >= 2 && frameIndex <= 3;
    }

    private void handleCombat(long now, double distanceToPlayer) {
        double dx = gp.player.getX() - x;
        facingRight = dx > 0;

        // If player is too far, stop being aggressive
        if (distanceToPlayer > aggroRange * 2.5) {
            isAggressive = false;
            inCombat = false;
            isFollowingPlayer = false;
            runningToBase = true;
            return;
        }

        // Move towards player if not in range and not attacking
        if (distanceToPlayer > combatRange && !attacking && !runningToBase) {
            moveTowardsPlayer(combatSpeed);
            currentRow = 3;
            currentFrame = (int) ((System.nanoTime() / 80_000_000) % totalFramesRun);
        }

        // Start attack if in range and cooldown is over, but only if not running to base
        if (distanceToPlayer <= combatRange && !attacking && (now - lastAttackTime) >= attackCooldown && !runningToBase) {
            attackStartTime = now;
            attacking = true;
            hasDealtDamageThisAttack = false;
            currentRow = attackRow;
            currentFrame = 0;
        }
    }

    private void handleAttack(long now) {
        if (!attacking) return;

        currentRow = attackRow;
        int frameIndex = (int) ((now - attackStartTime) / 100_000_000);

        // Deal damage during specific frames, but only if the player is within range
        if (frameIndex < totalFramesAttack) {
            currentFrame = frameIndex;

            // Check if the player is within range to be damaged
            double distanceToPlayer = Math.hypot(x - gp.player.getX(), y - gp.player.getY());

            // Adjusted: Check if the player is within combat range or close enough
            if (canDamagePlayer() && !hasDealtDamageThisAttack && distanceToPlayer <= combatRange) {
                Rectangle2D playerHitbox = new Rectangle2D(gp.player.getX(), gp.player.getY(), gp.player.getWidth(), gp.player.getHeight());
                Rectangle2D scoutHitbox = getHitbox();

                if (playerHitbox.intersects(scoutHitbox)) {
                    gp.player.takeMeleeDamageFromEnemy(1, now); // Apply damage to player
                    hasDealtDamageThisAttack = true;
                    System.out.println("Scout dealt damage to player!"); // Debug line
                }
            }
        } else {
            // Attack animation finished
            currentFrame = 0;
            attacking = false;
            hasDealtDamageThisAttack = false;
            lastAttackTime = now;
            if (!isAggressive) returnToPatrolling();
        }
    }

    public void update(long deltaTime, long now) {
        // ✅ FIXED: Handle hit reaction animation first - PRIORITY OVER ALL OTHER ANIMATIONS
        if (reactingToHit) {
            currentRow = hitReactionRow;
            int frameIndex = (int) ((now - hitReactionStartTime) / hitFrameDuration);
            if (frameIndex < hitReactionFrames) {
                currentFrame = frameIndex;
            } else {
                // Hit animation finished
                reactingToHit = false;
                currentFrame = 0;

                // ✅ FIXED: Check if should transition to death AFTER hit animation
                if (health <= 0) {
                    isDead = true;
                    currentRow = deathAnimationRow;
                    deathStartTime = now;
                    currentFrame = 0;

                    // Stop all other behaviors when dead
                    attacking = false;
                    runningToBase = false;
                    inCombat = false;
                    isAggressive = false;
                    isFollowingPlayer = false;
                    showingDialogue = false;

                }
            }
            return; // Exit early, don't process other updates during hit reaction
        }

        // ✅ FIXED: Handle death animation
        if (isDead) {
            int frameIndex = (int) ((now - deathStartTime) / deathFrameDuration);
            if (frameIndex < totalDeathFrames) {
                currentRow = deathAnimationRow;
                currentFrame = frameIndex;
            } else {
                currentFrame = totalDeathFrames - 1; // Stay on last death frame
                gp.scout[0] = null;
            }
            return; // Exit early, don't process other updates when dead
        }

        if (idleAtBase) return;

        if (showingDialogue) {
            if (now - dialogueStartTime >= dialogueDuration) {
                showingDialogue = false;
                customDialogue = null;
                if (!isAggressive) runningToBase = true;
                canBeAttacked = true; // ✅ FIXED: Set to true after dialogue
            }
            return;
        }

        double distanceToPlayer = Math.hypot(x - gp.player.getX(), y - gp.player.getY());

        // If running to base and player gets close, stop and attack
        if (runningToBase && distanceToPlayer <= aggroRange) {
            runningToBase = false;
            isAggressive = true;
            inCombat = true;
            isFollowingPlayer = true;
            facingRight = gp.player.getX() > x;
        }

        // Handle attack animation and damage
        if (attacking) {
            handleAttack(now);
            return;
        }

        // Handle combat behavior
        if (isAggressive && !isDead) {
            handleCombat(now, distanceToPlayer);
            return;
        }

        if (runningToBase) {
            Rectangle2D playerHitbox = new Rectangle2D(gp.player.getX(), gp.player.getY(), gp.player.getWidth(), gp.player.getHeight());
            Rectangle2D scoutHitbox = getHitbox();
            if (playerHitbox.intersects(scoutHitbox)) {
                // ✅ FIXED: Player can always attack scout by punching
                // No automatic damage here, damage is handled by player's punch
            }
            runToBase(now);
            return;
        }

        // ✅ UPDATED: Much smaller detection range for initial contact
        if (distanceToPlayer <= touchDetectionRange && !hasSeenPlayer) {
            hasSeenPlayer = true;
            showingDialogue = true;
            dialogueStartTime = now;
            return;
        }

        // ✅ UPDATED: Smaller range for following behavior
        if (distanceToPlayer <= touchDetectionRange && !isFollowingPlayer && hasSeenPlayer && !runningToBase) {
            isFollowingPlayer = true;
        }

        if (isFollowingPlayer && !runningToBase) followPlayer(now, distanceToPlayer);
        else if (!runningToBase) patrol(now);
    }

    private void moveTowardsPlayer(double moveSpeed) {
        double dx = gp.player.getX() - x;
        double dy = gp.player.getY() - y;
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance > moveSpeed) {
            dx = (dx / distance) * moveSpeed;
            dy = (dy / distance) * moveSpeed;

            double newX = x + dx;
            double newY = y + dy;

            if (!isColliding(newX, y)) {
                x = newX;
            }
            if (!isColliding(x, newY)) {
                y = newY;
            }

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

            showingDialogue = true;
            dialogueStartTime = now;
            customDialogue = "Alert! We've got an enemy inside our territory. Everyone, be on high alert!";
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

            double newX = x + dx;
            double newY = y + dy;

            if (!isColliding(newX, y)) {
                x = newX;
            }
            if (!isColliding(x, newY)) {
                y = newY;
            }

            facingRight = dx > 0;
        }
    }

    private void followPlayer(long now, double distanceToPlayer) {
        double dx = gp.player.getX() - x;
        facingRight = dx > 0;

        // ✅ UPDATED: Increased follow range but still reasonable
        if (distanceToPlayer > 6 * gp.tileSize && !isAggressive) {
            isFollowingPlayer = false;
            returnToPatrolling();
            return;
        }

        if (distanceToPlayer < 1.5 * gp.tileSize && !attacking) {
            attackStartTime = now;
            attacking = true;
            hasDealtDamageThisAttack = false;
            currentRow = attackRow;
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
        drawEntity(gc, camX, camY, scale, !facingRight);

        gc.save();
        gc.setLineWidth(1);
        gc.setStroke(isAggressive ? Color.RED : Color.LIME);
        double drawX = (x - camX) * scale;
        double drawY = (y - camY) * scale;
        gc.strokeRect(drawX + 2, drawY + 2, width * scale, height * scale);
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

        String dialogueText = (customDialogue != null) ? customDialogue : "TRAITOR!";

        double boxWidth = 280;
        double boxHeight = 70;
        double textX = screenX - boxWidth / 2 + 10;
        double textY = screenY - boxHeight / 2 + 25;

        gc.setFill(Color.BLACK);
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2);
        gc.fillRoundRect(screenX - boxWidth / 2, screenY - boxHeight / 2, boxWidth, boxHeight, 10, 10);
        gc.strokeRoundRect(screenX - boxWidth / 2, screenY - boxHeight / 2, boxWidth, boxHeight, 10, 10);

        gc.setFill(Color.RED);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        drawWrappedText(gc, dialogueText, textX, textY, boxWidth - 20);

        gc.setFill(Color.BLACK);
        gc.fillPolygon(new double[]{screenX - 5, screenX + 5, screenX}, new double[]{screenY, screenY, screenY + 10}, 3);
    }

    private void drawWrappedText(GraphicsContext gc, String text, double x, double y, double maxWidth) {
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        double lineHeight = gc.getFont().getSize() + 4;
        double currentY = y;

        for (String word : words) {
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
            double testWidth = gc.getFont().getSize() * testLine.length() * 0.55;

            if (testWidth > maxWidth && currentLine.length() > 0) {
                gc.fillText(currentLine.toString(), x, currentY);
                currentLine = new StringBuilder(word);
                currentY += lineHeight;
            } else {
                currentLine = new StringBuilder(testLine);
            }
        }

        if (currentLine.length() > 0) {
            gc.fillText(currentLine.toString(), x, currentY);
        }
    }

    public void setPlayerInRange(boolean inRange) {
        this.playerInRange = inRange;
        if (inRange && !isFollowingPlayer && !runningToBase) {
            double distanceToPlayer = Math.hypot(x - gp.player.getX(), y - gp.player.getY());
            // ✅ UPDATED: Use touch detection range instead of 3 tiles
            if (distanceToPlayer <= touchDetectionRange && !hasSeenPlayer) {
                hasSeenPlayer = true;
                showingDialogue = true;
                dialogueStartTime = System.nanoTime();
            } else if (distanceToPlayer <= touchDetectionRange && hasSeenPlayer && !runningToBase) {
                isFollowingPlayer = true;
            }
        }
    }

    public boolean isAggressive() {
        return isAggressive;
    }

    public boolean isInCombat() {
        return inCombat;
    }


    // Add this method to your Scout class
    public boolean isDead() {
        return isDead;
    }


}