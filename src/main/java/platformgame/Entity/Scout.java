package platformgame.Entity;

import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import platformgame.Game;

public class Scout extends Entity {
    // Animation frame constants
    protected final int frontWalkFrame = 4;
    protected final int frontWalkRow = 3;

    protected final int backWalkFrame = 4;
    protected final int backWalkRow = 4;

    protected final int walkFrame = 6;
    protected final int walkRow = 5;

    protected final int frontIdleFrame = 1;
    protected final int frontIdleRow = 0;

    protected final int idleFrame = 1;
    protected final int idleRow = 1;

    protected final int backIdleFrame = 1;
    protected final int backIdleRow = 2;

    protected final int frontFistFrame = 2;
    protected final int frontFistRow = 6;

    protected final int fistFrame = 2;
    protected final int fistRow = 7;

    protected final int backFistFrame = 2;
    protected final int backFistRow = 8;

    protected final int hitFrame = 2;
    protected final int hitRow = 19;

    protected final int frontHitFrame = 2;
    protected final int frontHitRow = 20;

    protected final int backHitFrame = 2;
    protected final int backHitRow = 21;

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
    private final double runSpeed = speed * 1.2;

    private int health = 3;
    private boolean isDead = false;
    private boolean visibleHealthBar = false;
    private long deathStartTime;
    private final int totalDeathFrames = 5;
    private final int deathAnimationRow = 22;
    private final long deathFrameDuration = 200_000_000; // Death animation duration

    // Hit reaction animation
    private boolean reactingToHit = false;
    private long hitReactionStartTime;

    private final long hitFrameDuration = 100_000_000;

    // Movement direction tracking
    private enum MovementDirection {
        FRONT,  // Moving down
        BACK,   // Moving up
        RIGHT,
        LEFT,
        IDLE
    }

    private MovementDirection currentDirection = MovementDirection.IDLE;
    private MovementDirection lastDirection = MovementDirection.FRONT;
    protected boolean deathAnimationComplete = false;
    private boolean canBeAttacked = true;
    private boolean inCombat = false;
    private boolean isAggressive = false;
    private long lastAttackTime = 0;
    private final long attackCooldown = 1_500_000_000L;
    private final double combatRange = 2 * 32;
    private final double aggroRange = 3 * 32;
    private final double combatSpeed = speed * 1.3;

    private final double touchDetectionRange = 5 * 32;

    private String customDialogue = null;
    private Game gp;
    private boolean hasDealtDamageThisAttack = false;

    // Attack animation timing fix
    private final long attackFrameDuration = 150_000_000L; // Consistent attack frame timing

    public Scout(double x, double y, double width, double height, double speed, Game gp) {
        super(x, y, width, height, speed, gp, null);
        this.gp = gp;
        imageSet(walkFrame, "/image/Scout.png");
        patrolStartX = x;
        patrolStartY = y;
        baseCampX = 53 * gp.tileSize;
        baseCampY = 23 * gp.tileSize;
        setNewPatrolTarget();

        // Set initial idle animation
        setIdleAnimation();
    }

    // ✅ NEW: Comprehensive collision detection method similar to Player
    private boolean checkAllCollisions(double testX, double testY) {
        // Check map collision first
        if (gp.level1.isCollisionRect(testX, testY, width, height)) {
            return true;
        }

        // Check object collisions
        if (checkObjectCollisions(testX, testY)) {
            return true;
        }

        // Check player collision (if scout shouldn't overlap player)
        if (checkPlayerCollision(testX, testY)) {
            return true;
        }

        // Check other enemy collisions (prevent overlap)
        if (checkOtherEnemyCollisions(testX, testY)) {
            return true;
        }

        return false;
    }

    // ✅ NEW: Check object collisions similar to Player's method
    private boolean checkObjectCollisions(double testX, double testY) {
        Rectangle2D scoutRect = new Rectangle2D(testX, testY, width, height);

        for (int i = 0; i < gp.object.length; i++) {
            if (gp.object[i] != null) {
                double dx = Math.abs(gp.object[i].worldX - testX);
                double dy = Math.abs(gp.object[i].worldY - testY);

                // Only check objects within reasonable distance
                if (dx < 128 && dy < 128) {
                    Rectangle2D objRect = gp.object[i].getBoundingBox();
                    if (scoutRect.intersects(objRect) && gp.object[i].collision) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // ✅ NEW: Check player collision
    private boolean checkPlayerCollision(double testX, double testY) {
        if (gp.player == null) return false;

        Rectangle2D scoutRect = new Rectangle2D(testX, testY, width, height);
        Rectangle2D playerRect = new Rectangle2D(
                gp.player.getX(), gp.player.getY(),
                gp.player.getWidth(), gp.player.getHeight()
        );

        // Only block movement if not in combat/aggressive mode
        if (!isAggressive && !inCombat && !attacking) {
            return scoutRect.intersects(playerRect);
        }

        return false;
    }

    // ✅ NEW: Check collision with other enemies/NPCs
    private boolean checkOtherEnemyCollisions(double testX, double testY) {
        Rectangle2D scoutRect = new Rectangle2D(testX, testY, width, height);

        // Check other scouts
        if (gp.scout != null) {
            for (Scout otherScout : gp.scout) {
                if (otherScout != null && otherScout != this && !otherScout.isDead()) {
                    Rectangle2D otherRect = new Rectangle2D(
                            otherScout.getX(), otherScout.getY(),
                            otherScout.getWidth(), otherScout.getHeight()
                    );
                    if (scoutRect.intersects(otherRect)) {
                        return true;
                    }
                }
            }
        }

        // Check enemies
        if (gp.enemies != null) {
            for (Enemy enemy : gp.enemies) {
                if (enemy != null && !enemy.isDead()) {
                    Rectangle2D enemyRect = new Rectangle2D(
                            enemy.getX(), enemy.getY(),
                            enemy.getWidth(), enemy.getHeight()
                    );
                    if (scoutRect.intersects(enemyRect)) {
                        return true;
                    }
                }
            }
        }

        // Check soldiers
        if (gp.soldiers != null) {
            for (Soldier soldier : gp.soldiers) {
                if (soldier != null && !soldier.isDead()) {
                    Rectangle2D soldierRect = new Rectangle2D(
                            soldier.getX(), soldier.getY(),
                            soldier.getWidth(), soldier.getHeight()
                    );
                    if (scoutRect.intersects(soldierRect)) {
                        return true;
                    }
                }
            }
        }

        // Check NPCs
        if (gp.npc != null) {
            for (Npc npc : gp.npc) {
                if (npc != null) {
                    Rectangle2D npcRect = new Rectangle2D(
                            npc.getX(), npc.getY(),
                            npc.getWidth(), npc.getHeight()
                    );
                    if (scoutRect.intersects(npcRect)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    // ✅ UPDATED: Replace the old isColliding method
    public boolean isColliding(double testX, double testY) {
        return checkAllCollisions(testX, testY);
    }

    // ✅ UPDATED: Improved movement with proper collision handling
    private void moveTowardsTargetWithCollision(double targetX, double targetY, double moveSpeed) {
        double dx = targetX - x;
        double dy = targetY - y;
        double distance = Math.hypot(dx, dy);

        if (distance > moveSpeed) {
            // Normalize direction
            dx = (dx / distance) * moveSpeed;
            dy = (dy / distance) * moveSpeed;

            double newX = x + dx;
            double newY = y + dy;

            // Check collisions for each axis separately
            boolean xCollision = checkAllCollisions(newX, y);
            boolean yCollision = checkAllCollisions(x, newY);

            // Move if no collision
            if (!xCollision) {
                x = newX;
            }
            if (!yCollision) {
                y = newY;
            }

            // If both axes blocked, try alternative movement
            if (xCollision && yCollision) {
                // Try diagonal movement with reduced speed
                double altDx = dx * 0.7;
                double altDy = dy * 0.7;

                if (!checkAllCollisions(x + altDx, y)) {
                    x += altDx;
                } else if (!checkAllCollisions(x, y + altDy)) {
                    y += altDy;
                } else {
                    // Try moving around obstacle
                    tryMoveAround(targetX, targetY, moveSpeed);
                }
            }

            // Update facing direction
            if (!xCollision || (!xCollision && !yCollision)) {
                facingRight = dx > 0;
            }
        }
    }

    // ✅ NEW: Try to move around obstacles
    private void tryMoveAround(double targetX, double targetY, double moveSpeed) {
        // Try different angles to move around obstacles
        double[] angles = {45, -45, 90, -90, 135, -135};

        for (double angle : angles) {
            double radians = Math.toRadians(angle);
            double testDx = moveSpeed * Math.cos(radians);
            double testDy = moveSpeed * Math.sin(radians);

            if (!checkAllCollisions(x + testDx, y + testDy)) {
                x += testDx;
                y += testDy;
                facingRight = testDx > 0;
                break;
            }
        }
    }

    // ✅ UPDATED: Improved moveTowardsPlayer with better collision handling
    private void moveTowardsPlayer(double moveSpeed) {
        double playerX = gp.player.getX();
        double playerY = gp.player.getY();

        double dx = playerX - x;
        double dy = playerY - y;
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance > moveSpeed) {
            dx = (dx / distance) * moveSpeed;
            dy = (dy / distance) * moveSpeed;

            double newX = x + dx;
            double newY = y + dy;

            // Check collisions but allow player overlap in combat
            boolean xCollision = gp.level1.isCollisionRect(newX, y, width, height) ||
                    checkObjectCollisions(newX, y) ||
                    checkOtherEnemyCollisions(newX, y);
            boolean yCollision = gp.level1.isCollisionRect(x, newY, width, height) ||
                    checkObjectCollisions(x, newY) ||
                    checkOtherEnemyCollisions(x, newY);

            if (!xCollision) {
                x = newX;
            } else {
                // Try to move around X obstacle
                facingRight = !facingRight;
            }

            if (!yCollision) {
                y = newY;
            } else {
                // Try alternative Y movement
                setNewPatrolTarget();
            }

            if (!xCollision || !yCollision) {
                facingRight = dx > 0;
                setMovementAnimation(dx, dy);
            } else {
                // Both directions blocked, find new approach
                tryMoveAround(playerX, playerY, moveSpeed);
                setIdleAnimation();
            }
        } else {
            setIdleAnimation();
        }
    }

    private void setIdleAnimation() {
        currentDirection = MovementDirection.IDLE;

        switch (lastDirection) {
            case FRONT:
                currentRow = frontIdleRow;
                break;
            case BACK:
                currentRow = backIdleRow;
                break;
            case LEFT:
            case RIGHT:
                currentRow = walkRow;
                break;
            default:
                currentRow = IdleRow;
                break;
        }
        currentFrame = 0;
    }

    private void setMovementAnimation(double dx, double dy) {
        // Determine primary movement direction
        if (Math.abs(dy) > Math.abs(dx)) {
            if (dy < 0) {
                // Moving up (back)
                currentDirection = MovementDirection.BACK;
                lastDirection = MovementDirection.BACK;
                currentRow = backWalkRow;
            } else {
                // Moving down (front)
                currentDirection = MovementDirection.FRONT;
                lastDirection = MovementDirection.FRONT;
                currentRow = frontWalkRow;
            }
        } else if (dx > 0) {
            // Moving right
            currentDirection = MovementDirection.RIGHT;
            lastDirection = MovementDirection.RIGHT;
            currentRow = walkRow;
        } else if (dx < 0) {
            // Moving left
            currentDirection = MovementDirection.LEFT;
            lastDirection = MovementDirection.LEFT;
            currentRow = walkRow;
        } else {
            // Idle
            currentDirection = MovementDirection.IDLE;
            lastDirection = MovementDirection.IDLE;
            currentRow = idleRow;
        }
    }

    private void setAttackAnimation() {
        switch (lastDirection) {
            case FRONT:
                currentRow = frontFistRow;
                break;
            case BACK:
                currentRow = backFistRow;
                break;
            case LEFT:
            case RIGHT:
            default:
                currentRow = fistRow;
                break;
        }
        currentFrame = 0; // Reset frame to start attack animation properly
    }

    private void setHitAnimation() {
        switch (lastDirection) {
            case FRONT:
                currentRow = frontHitRow;
                break;
            case BACK:
                currentRow = backHitRow;
                break;
            case LEFT:
            case RIGHT:
            default:
                currentRow = hitRow;
                break;
        }
    }

    public void takeDamage() {
        if (isDead || reactingToHit) return;

        health--;
        visibleHealthBar = true;

        // Trigger hit animation
        reactingToHit = true;
        hitReactionStartTime = System.nanoTime();
        currentFrame = 0;
        setHitAnimation();

        // Stop any current attack when hit
        attacking = false;
        hasDealtDamageThisAttack = false;

        if (!isAggressive && health > 0) {
            isAggressive = true;
            inCombat = true;
            isFollowingPlayer = true;
            runningToBase = false;
            facingRight = gp.player.getX() > x;
        }
    }

    public Rectangle2D getHitbox() {
        return new Rectangle2D(x - 10, y + 4, width + 25, height);
    }

    public boolean canDamagePlayer() {
        if (!attacking || isDead) return false;
        long now = System.nanoTime();
        int frameIndex = (int) ((now - attackStartTime) / attackFrameDuration);

        // Damage frames depend on attack type - using updated frame counts
        switch (lastDirection) {
            case FRONT:
                return frameIndex >= 0 && frameIndex < frontFistFrame ;
            case BACK:
                return frameIndex >= 0 && frameIndex < backFistFrame ;
            case LEFT:
            case RIGHT:
            default:
                return frameIndex >= 0 && frameIndex < fistFrame ;
        }
    }

    private void handleCombat(long now, double distanceToPlayer) {
        double dx = gp.player.getX() - x;
        facingRight = dx > 0;

        if (distanceToPlayer > aggroRange * 2.5) {
            isAggressive = false;
            inCombat = false;
            isFollowingPlayer = false;
            runningToBase = true;
            return;
        }

        if (distanceToPlayer > combatRange && !attacking && !runningToBase) {
            moveTowardsPlayer(combatSpeed);
        }

        if (distanceToPlayer <= combatRange && !attacking && (now - lastAttackTime) >= attackCooldown && !runningToBase) {
            attackStartTime = now;
            attacking = true;
            hasDealtDamageThisAttack = false;
            setAttackAnimation();
        }
    }

    private void handleAttack(long now) {
        if (!attacking) return;

        int frameIndex = (int) ((now - attackStartTime) / attackFrameDuration);
        int maxFrames = (lastDirection == MovementDirection.FRONT) ? frontFistFrame :
                (lastDirection == MovementDirection.BACK) ? backFistFrame : fistFrame;

        if (frameIndex < maxFrames) {
            currentFrame = frameIndex;

            double distanceToPlayer = Math.hypot(x - gp.player.getX(), y - gp.player.getY());

            if (canDamagePlayer() && !hasDealtDamageThisAttack && distanceToPlayer <= combatRange) {
                Rectangle2D playerHitbox = new Rectangle2D(gp.player.getX(), gp.player.getY(), gp.player.getWidth(), gp.player.getHeight());
                Rectangle2D scoutHitbox = getHitbox();

                if (playerHitbox.intersects(scoutHitbox)) {
                    punchSound();
                    gp.player.takeMeleeDamageFromEnemy(1, now);
                    hasDealtDamageThisAttack = true;
                    System.out.println("Scout dealt damage to player!");
                }
            }

        } else {
            currentFrame = 0;
            attacking = false;
            hasDealtDamageThisAttack = false;
            lastAttackTime = now;
            if (!isAggressive) returnToPatrolling();
            else setIdleAnimation();
        }
    }

    public void update(long deltaTime, long now) {
        // Handle hit reaction animation first
        if (reactingToHit) {
            int frameIndex = (int) ((now - hitReactionStartTime) / hitFrameDuration);
            int maxHitFrames = (lastDirection == MovementDirection.FRONT) ? frontHitFrame :
                    (lastDirection == MovementDirection.BACK) ? backHitFrame : hitFrame;

            if (frameIndex < maxHitFrames) {
                currentFrame = frameIndex;
            } else {
                reactingToHit = false;
                currentFrame = 0;

                if (health <= 0) {
                    isDead = true;
                    currentRow = deathAnimationRow;
                    deathStartTime = now;
                    currentFrame = 0;
                    gp.scout[1] = null;
                    attacking = false;
                    runningToBase = false;
                    inCombat = false;
                    isAggressive = false;
                    isFollowingPlayer = false;
                    showingDialogue = false;
                } else {
                    setIdleAnimation();
                }
            }
            return;
        }

        // Handle death animation - remove entity after animation completes
        if (isDead) {
            if (deathStartTime == 0) {
                deathStartTime = now;
            }
            currentRow = deathAnimationRow;
            int frameIndex = (int) ((now - deathStartTime) / 200_000_000);
            if (frameIndex < totalDeathFrames) {
                currentFrame = frameIndex;
            } else {
                currentFrame = totalDeathFrames - 1; // Stay on last frame briefly
                // Mark animation as complete after a short delay on the last frame
                if ((now - deathStartTime) > (totalDeathFrames * 200_000_000L + 400_000_000L)) { // Extra 0.5 seconds on last frame
                    deathAnimationComplete = true;
                }
            }

            gp.scout[0] = null;
            return;
        }

        if (idleAtBase) {
            setIdleAnimation();
            animateFrames(now, 200_000_000);
            return;
        }

        if (showingDialogue) {
            if (now - dialogueStartTime >= dialogueDuration) {
                showingDialogue = false;
                customDialogue = null;
                if (!isAggressive) runningToBase = true;
                canBeAttacked = true;
            }
            setIdleAnimation();
            animateFrames(now, 200_000_000);
            return;
        }

        double distanceToPlayer = Math.hypot(x - gp.player.getX(), y - gp.player.getY());

        if (runningToBase && distanceToPlayer <= aggroRange) {
            runningToBase = false;
            isAggressive = true;
            inCombat = true;
            isFollowingPlayer = true;
            facingRight = gp.player.getX() > x;
        }

        if (attacking) {
            handleAttack(now);
            return;
        }

        if (isAggressive && !isDead) {
            handleCombat(now, distanceToPlayer);
            if (!attacking) animateFrames(now, 100_000_000);
            return;
        }

        if (runningToBase) {
            Rectangle2D playerHitbox = new Rectangle2D(gp.player.getX(), gp.player.getY(), gp.player.getWidth(), gp.player.getHeight());
            Rectangle2D scoutHitbox = getHitbox();
            if (playerHitbox.intersects(scoutHitbox)) {
                // Player can attack scout by punching
            }
            runToBase(now);
            return;
        }

        if (distanceToPlayer <= touchDetectionRange && !hasSeenPlayer) {
            hasSeenPlayer = true;
            showingDialogue = true;
            dialogueStartTime = now;
            return;
        }

        if (distanceToPlayer <= touchDetectionRange && !isFollowingPlayer && hasSeenPlayer && !runningToBase) {
            isFollowingPlayer = true;
        }

        if (isFollowingPlayer && !runningToBase) followPlayer(now, distanceToPlayer);
        else if (!runningToBase) patrol(now);
    }

    private void animateFrames(long now, long frameDuration) {
        int maxFrames;
        switch (currentDirection) {
            case FRONT:
                maxFrames = (currentRow == frontIdleRow) ? frontIdleFrame : frontWalkFrame;
                break;
            case BACK:
                maxFrames = (currentRow == backIdleRow) ? backIdleFrame : backWalkFrame;
                break;
            case LEFT:
            case RIGHT:
                maxFrames = (currentRow == IdleRow) ? IdleFrame : WalkFrame;
                break;
            case IDLE:
                if (currentRow == frontIdleRow) maxFrames = frontIdleFrame;
                else if (currentRow == backIdleRow) maxFrames = backIdleFrame;
                else maxFrames = IdleFrame;
                break;
            default:
                maxFrames = 4;
                break;
        }

        currentFrame = (int) ((now / frameDuration) % maxFrames);
    }

    private void runToBase(long now) {
        double distanceToBase = Math.hypot(x - baseCampX, y - baseCampY);
        if (distanceToBase < gp.tileSize) {
            x = baseCampX;
            y = baseCampY;
            runningToBase = false;
            idleAtBase = true;
            setIdleAnimation();

            showingDialogue = true;
            dialogueStartTime = now;
            customDialogue = "Alert! We've got an enemy inside our territory. Everyone, be on high alert!";
            return;
        }

        double dx = baseCampX - x;
        double dy = baseCampY - y;
        moveTowardsTargetWithCollision(baseCampX, baseCampY, runSpeed);
        setMovementAnimation(dx, dy);
        animateFrames(now, 100_000_000);
    }

    private void patrol(long now) {
        if (!movingToTarget || hasReachedTarget()) setNewPatrolTarget();

        double dx = patrolTargetX - x;
        double dy = patrolTargetY - y;

        if (Math.hypot(dx, dy) > speed * 0.5) {
            moveTowardsTargetWithCollision(patrolTargetX, patrolTargetY, speed * 0.5);
            setMovementAnimation(dx, dy);
            animateFrames(now, 150_000_000);
        } else {
            setIdleAnimation();
            animateFrames(now, 200_000_000);
        }
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

        if (distanceToPlayer > 6 * gp.tileSize && !isAggressive) {
            isFollowingPlayer = false;
            returnToPatrolling();
            return;
        }

        if (distanceToPlayer < 1.5 * gp.tileSize && !attacking) {
            attackStartTime = now;
            attacking = true;
            hasDealtDamageThisAttack = false;
            setAttackAnimation();
            return;
        }

        if (!attacking) {
            double dy = gp.player.getY() - y;
            if (Math.hypot(dx, dy) > speed) {
                moveTowardsTargetWithCollision(gp.player.getX(), gp.player.getY(), speed);
                setMovementAnimation(dx, dy);
                animateFrames(now, 100_000_000);
            } else {
                setIdleAnimation();
                animateFrames(now, 200_000_000);
            }
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
        setIdleAnimation();
    }

    public void draw(GraphicsContext gc, double camX, double camY, double scale) {
        // Only draw if not dead - this makes the scout disappear after death
        if (!isDead || (isDead && !deathAnimationComplete)) {
            drawEntity(gc, camX, camY, scale);
        }

        if (!isDead ) {

            if (showingDialogue) drawDialogue(gc, camX, camY, scale);
            if (visibleHealthBar) drawHealthBar(gc, camX, camY, scale);
            if (isAggressive) drawCombatIndicator(gc, camX, camY, scale);
        }
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

    public boolean isDead() {
        return isDead;
    }
}
