package platformgame.Tanks;

import platformgame.Game;
import platformgame.Game_2;
import platformgame.Map.Level_2;
import platformgame.Tank_Bullet;

import javafx.scene.image.Image;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Enemy_Tank extends Tank {
    // Combat properties
    private double attackRange = 250.0;
    private double detectionRange = 300.0;  // Larger range for detection vs attack
    protected double bulletSpeed = 300.0;
    private double turretRotationSpeed = 2.0;
    private double aimTolerance = 0.2;      // How precise aiming needs to be to shoot

    // Position properties
    private final double fixedX;
    private final double fixedY;

    // AI state management
    private boolean playerDetected = false;
    private double lastPlayerX = 0;
    private double lastPlayerY = 0;
    private double lostPlayerTimer = 0;
    private final double lostPlayerTimeout = 3.0; // Time to keep tracking after losing sight

    // Visual feedback
    private double alertLevel = 0.0; // 0.0 to 1.0 for visual alert state
    private final double alertFadeSpeed = 2.0;

    // Health bar properties
    private int maxHealth = 100;
    private boolean showHealthBar = true;
    private double healthBarWidth = 60.0;
    private double healthBarHeight = 8.0;
    private double healthBarOffsetY = -20.0; // How far above the tank to show the health bar
    private double damageFeedbackTimer = 0.0;
    private final double damageFeedbackDuration = 0.5; // Flash duration when taking damage

    public Enemy_Tank(double x, double y, double width, double height, double speed, Game gp, Game_2 gp2) {
        super(x, y, width, height, speed, gp, gp2);
        setCollisionBox(100, 100, -28, -28);

        // Store the fixed position
        this.fixedX = x;
        this.fixedY = y;

        // Set velocity to zero since tank is static
        velocity.x = 0;
        velocity.y = 0;

        // Initialize max health to current health
        this.maxHealth = this.health;
    }

    @Override
    protected void loadTankSprite() {
        loadSprite("/image/Tank_Enemy.png");
    }

    @Override
    public void updateBehavior(Level_2 level2, Game_2 gp2, double deltaTime) {
        if (!alive) return;

        // Always maintain fixed position
        enforceFixedPosition();

        // Update AI behavior
        updateAI(gp2, deltaTime);

        // Update timers
        updateGunTimer(deltaTime);
        updateHealthBarTimers(deltaTime);

        // Update visual states
        updateVisualStates(deltaTime);

        // Check collision with player tank
        checkPlayerCollision(gp2);
    }

    private void updateHealthBarTimers(double deltaTime) {
        if (damageFeedbackTimer > 0) {
            damageFeedbackTimer -= deltaTime;
        }
    }

    private void enforceFixedPosition() {
        this.x = fixedX;
        this.y = fixedY;
        velocity.x = 0;
        velocity.y = 0;
    }

    private void updateAI(Game_2 gp2, double deltaTime) {
        if (gp2.mainTank == null || !gp2.mainTank.alive) {
            // No valid target
            playerDetected = false;
            alertLevel = Math.max(0, alertLevel - alertFadeSpeed * deltaTime);
            return;
        }

        double distanceToPlayer = calculateDistanceToPlayer(gp2);

        if (distanceToPlayer <= detectionRange && hasLineOfSight(gp2, gp2.mainTank)) {
            // Player detected
            playerDetected = true;
            lastPlayerX = gp2.mainTank.getTankX();
            lastPlayerY = gp2.mainTank.getTankY();
            lostPlayerTimer = 0;
            alertLevel = Math.min(1.0, alertLevel + alertFadeSpeed * deltaTime);

            if (distanceToPlayer <= attackRange) {
                trackAndEngagePlayer(gp2, deltaTime);
            } else {
                // Just track, don't shoot
                trackPlayer(gp2.mainTank.getTankX(), gp2.mainTank.getTankY(), deltaTime);
            }
        } else if (playerDetected) {
            // Lost sight of player, but remember last position for a while
            lostPlayerTimer += deltaTime;
            alertLevel = Math.max(0, alertLevel - alertFadeSpeed * deltaTime);

            if (lostPlayerTimer < lostPlayerTimeout) {
                // Continue tracking last known position
                trackPlayer(lastPlayerX, lastPlayerY, deltaTime);
            } else {
                // Give up tracking
                playerDetected = false;
            }
        } else {
            // No player detected, reduce alert level
            alertLevel = Math.max(0, alertLevel - alertFadeSpeed * deltaTime);
        }
    }

    private double calculateDistanceToPlayer(Game_2 gp2) {
        return Math.sqrt(Math.pow(x - gp2.mainTank.getTankX(), 2) +
                Math.pow(y - gp2.mainTank.getTankY(), 2));
    }

    private boolean hasLineOfSight(Game_2 gp2, Tank target) {
        // Simple line of sight check - you might want to implement raycasting here
        // For now, just return true, but this could check for walls between tank and target
        return true;
    }

    private void trackAndEngagePlayer(Game_2 gp2, double deltaTime) {
        double targetX = gp2.mainTank.getTankX();
        double targetY = gp2.mainTank.getTankY();

        double angleDifference = trackPlayer(targetX, targetY, deltaTime);

        // Shoot if aimed well enough
        if (Math.abs(angleDifference) < aimTolerance) {
            shoot();
        }
    }

    private double trackPlayer(double targetX, double targetY, double deltaTime) {
        // Calculate angle to target
        double dx = targetX - x;
        double dy = targetY - y;
        double targetRotation = Math.atan2(dy, dx);

        // Calculate angle difference
        double angleDifference = targetRotation - turretRotation;

        // Normalize angle difference to [-π, π]
        while (angleDifference > Math.PI) angleDifference -= 2 * Math.PI;
        while (angleDifference < -Math.PI) angleDifference += 2 * Math.PI;

        // Rotate turret smoothly toward target
        if (Math.abs(angleDifference) > 0.05) { // Smaller threshold for smoother tracking
            double rotationAmount = Math.signum(angleDifference) * turretRotationSpeed * deltaTime;
            // Don't overshoot the target
            if (Math.abs(rotationAmount) > Math.abs(angleDifference)) {
                turretRotation = targetRotation;
            } else {
                turretRotation += rotationAmount;
            }
        } else {
            turretRotation = targetRotation;
        }

        return angleDifference;
    }

    private void updateVisualStates(double deltaTime) {
        // Update any visual state changes here
        // alertLevel is already updated in updateAI
    }

    private void checkPlayerCollision(Game_2 gp2) {
        if (gp2.mainTank != null && gp2.mainTank.alive && this.alive) {
            double dx = x - gp2.mainTank.getX();
            double dy = y - gp2.mainTank.getY();
            double distance = Math.sqrt(dx * dx + dy * dy);

            double collisionDistance = 80.0;

            if (distance < collisionDistance) {
                System.out.println("Tank collision detected! Distance: " + distance);
                // Optional: Apply damage or push effects
                // handleCollisionEffects(gp2.mainTank);
            }
        }
    }

    @Override
    public void shoot() {
        if (canShoot && alive && playerDetected) {
            canShoot = false;
            gunTimer = gunCooldown;

            Vector2D muzzlePos = getMuzzlePosition();
            double velocityX = bulletSpeed * Math.cos(turretRotation);
            double velocityY = bulletSpeed * Math.sin(turretRotation);

            Tank_Bullet bullet = new Tank_Bullet(muzzlePos.x, muzzlePos.y, velocityX, velocityY, null, gp2, this);

            if (gp2 != null) {
                gp2.addBullet(bullet);
            }

            // Visual/audio feedback for shooting
            onShoot();
        }
    }

    protected void onShoot() {
        // Override this for muzzle flash, sound effects, etc.
        System.out.println("Enemy tank fired!");
    }

    @Override
    protected void createBullet(double x, double y) {
        double velocityX = bulletSpeed * Math.cos(turretRotation);
        double velocityY = bulletSpeed * Math.sin(turretRotation);

        Tank_Bullet bullet = new Tank_Bullet(x, y, velocityX, velocityY, null, gp2, this);

        if (gp2 != null) {
            gp2.addBullet(bullet);
        }
    }

    @Override
    public void createBullet(double x, double y, double velocityX, double velocityY) {
        Tank_Bullet bullet = new Tank_Bullet(x, y, velocityX, velocityY, null, gp2, this);

        if (gp2 != null) {
            gp2.addBullet(bullet);
        }
    }

    @Override
    protected void physicsProcess(Level_2 level2, double deltaTime) {
        enforceFixedPosition();

        // Check for map collisions at fixed position
        if (level2 != null && level2.checkCollisionWithRectangle(
                x + collisionOffsetX, y + collisionOffsetY,
                collisionWidth, collisionHeight)) {
            System.out.println("Warning: Enemy tank placed at colliding position!");
        }
    }

    @Override
    public void draw(GraphicsContext gc, double camX, double camY, double scale) {
        // Only draw if tank is alive
        if (!alive) return;

        // Draw the tank body (assuming parent class handles this)

        // Draw the turret on top
        drawTurret(gc, camX, camY, scale);

        // Draw health bar if enabled and tank is alive
        if (showHealthBar && alive) {
            drawHealthBar(gc, camX, camY, scale);
        }
    }

    private void drawTurret(GraphicsContext gc, double camX, double camY, double scale) {
        if (spriteSheet == null) return;

        // ▶ change these to exactly the rectangle of just your turret graphic ◀
        int turretCol = 2;
        int turretRow = 1;
        int srcX       = turretCol * frameWidth;
        int srcY       = turretRow * frameHeight;
        int srcW       = frameWidth;
        int srcH       = frameHeight;

        double drawW = width * scale;
        double drawH = height * scale;
        double cx    = (x - camX) * scale + drawW/2;
        double cy    = (y - camY) * scale + drawH/2;

        gc.save();
        gc.translate(cx, cy);
        gc.rotate(Math.toDegrees(turretRotation));
        // only the turret pixels, not the whole frame!
        gc.drawImage(
                spriteSheet,
                srcX, srcY, srcW, srcH,
                -drawW/2, -drawH/2,
                drawW, drawH
        );
        gc.restore();
    }

    private void drawHealthBar(GraphicsContext gc, double camX, double camY, double scale) {
        if (health <= 0) return; // Don't draw health bar if tank is dead

        // Calculate health bar position (above the tank)
        double tankCenterX = (x - camX) * scale + (width * scale) / 2;
        double healthBarY = (y - camY) * scale + healthBarOffsetY;
        double healthBarX = tankCenterX - (healthBarWidth * scale) / 2;

        // Scale the health bar size
        double scaledWidth = healthBarWidth * scale;
        double scaledHeight = healthBarHeight * scale;

        // Calculate health percentage
        double healthPercentage = Math.max(0, (double) health / maxHealth);

        // Background (gray bar)
        gc.setFill(Color.DARKGRAY);
        gc.fillRect(healthBarX, healthBarY, scaledWidth, scaledHeight);

        // Health bar outline
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1);
        gc.strokeRect(healthBarX, healthBarY, scaledWidth, scaledHeight);

        // Health fill (changes color based on health level)
        Color healthColor;
        if (healthPercentage > 0.6) {
            healthColor = Color.GREEN;
        } else if (healthPercentage > 0.3) {
            healthColor = Color.YELLOW;
        } else {
            healthColor = Color.RED;
        }

        // Add flashing effect when taking damage
        if (damageFeedbackTimer > 0) {
            // Flash between health color and white
            double flashIntensity = Math.sin(damageFeedbackTimer * 20) * 0.5 + 0.5;
            healthColor = healthColor.interpolate(Color.WHITE, flashIntensity);
        }

        gc.setFill(healthColor);
        gc.fillRect(healthBarX, healthBarY, scaledWidth * healthPercentage, scaledHeight);

        // Optional: Display health numbers
        if (scale > 0.5) { // Only show numbers when zoomed in enough
            gc.setFill(Color.WHITE);
            gc.setFont(javafx.scene.text.Font.font(10 * scale));
            String healthText = health + "/" + maxHealth;
            gc.fillText(healthText, tankCenterX - 15 * scale, healthBarY - 3);
        }
    }

    public void debugDraw(GraphicsContext gc, double camX, double camY, double scale) {
        // Draw a colored rectangle around this tank to identify it
        double drawW = width * scale;
        double drawH = height * scale;
        double drawX = (x - camX) * scale;
        double drawY = (y - camY) * scale;

        // Set a unique color for debugging (you can change this)
        gc.setStroke(Color.YELLOW);
        gc.setLineWidth(2);
        gc.strokeRect(drawX, drawY, drawW, drawH);

        // Draw tank ID or hash code to identify duplicates
        gc.setFill(Color.WHITE);
        gc.fillText("Tank:" + this.hashCode(), drawX, drawY - 5);
    }

    @Override
    protected void onDamageTaken(int damage) {
        super.onDamageTaken(damage);
        System.out.println("Enemy tank took " + damage + " damage! Health: " + health);

        // Trigger damage feedback effect
        damageFeedbackTimer = damageFeedbackDuration;

        // Become instantly alert when damaged
        alertLevel = 1.0;
        playerDetected = true;
        lostPlayerTimer = 0;
    }

    @Override
    protected void onDestroyed() {
        super.onDestroyed();
        System.out.println("Enemy tank destroyed!");
        // Hide health bar when tank is destroyed
        showHealthBar = false;
        // Add explosion effects, score points, drop items, etc.
    }

    // Configuration methods for different enemy types
    public void configureAsLightTank() {
        this.attackRange = 200.0;
        this.detectionRange = 250.0;
        this.bulletSpeed = 250.0;
        this.turretRotationSpeed = 3.0;
        this.health = 50;
        this.maxHealth = 50;
        this.gunCooldown = 1.0;
    }

    public void configureAsHeavyTank() {
        this.attackRange = 300.0;
        this.detectionRange = 350.0;
        this.bulletSpeed = 400.0;
        this.turretRotationSpeed = 1.5;
        this.health = 150;
        this.maxHealth = 150;
        this.gunCooldown = 2.5;
    }

    public void configureAsSniperTank() {
        this.attackRange = 400.0;
        this.detectionRange = 450.0;
        this.bulletSpeed = 500.0;
        this.turretRotationSpeed = 1.0;
        this.health = 75;
        this.maxHealth = 75;
        this.gunCooldown = 3.0;
        this.aimTolerance = 0.1; // More precise aiming required
    }

    // Health bar configuration methods
    public void setHealthBarVisible(boolean visible) {
        this.showHealthBar = visible;
    }

    public void setHealthBarSize(double width, double height) {
        this.healthBarWidth = width;
        this.healthBarHeight = height;
    }

    public void setHealthBarOffset(double offsetY) {
        this.healthBarOffsetY = offsetY;
    }

    // Getters and setters
    public double getAttackRange() { return attackRange; }
    public void setAttackRange(double range) { this.attackRange = range; }

    public double getDetectionRange() { return detectionRange; }
    public void setDetectionRange(double range) { this.detectionRange = range; }

    public double getFixedX() { return fixedX; }
    public double getFixedY() { return fixedY; }

    public boolean isPlayerDetected() { return playerDetected; }
    public double getAlertLevel() { return alertLevel; }

    public int getMaxHealth() { return maxHealth; }
    public double getHealthPercentage() { return (double) health / maxHealth; }
}