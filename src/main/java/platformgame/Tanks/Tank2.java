package platformgame.Tanks;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import platformgame.Game;
import platformgame.Game_2;
import platformgame.Map.Level_2;
import platformgame.Tank_Bullet;

public class Tank2 extends Tank {
    // Combat properties
    private double attackRange = 640.0;
    private double detectionRange =640.0;
    protected double bulletSpeed = 150.0;
    double turretRotationSpeed = 1.0;
    double aimTolerance = 0.2;

    // Position properties
    private final double fixedX;
    private final double fixedY;

    // AI state
    private boolean playerDetected = false;
    private double lastPlayerX = 0;
    private double lastPlayerY = 0;
    private double lostPlayerTimer = 0;
    private final double lostPlayerTimeout = 3.0;

    // Visual feedback
    private double alertLevel = 0.0;
    private final double alertFadeSpeed = 2.0;

    // Health bar - FIXED: Set to 50 health (5 hits to destroy with 10 damage per hit)
    private int maxHealth = 50;  // CHANGED: Was 100, now 50 (5 hits to destroy)
    boolean showHealthBar = true;
    private double healthBarWidth = 60.0;
    private double healthBarHeight = 8.0;
    private double healthBarOffsetY = -20.0;
    private double damageFeedbackTimer = 0.0;
    private final double damageFeedbackDuration = 0.5;

    public Tank2(double x, double y, double width, double height, double speed, Game gp, Game_2 gp2) {
        super(x, y, width, height, speed, gp, gp2);
        setCollisionBox(100, 100, -28, -28);

        this.fixedX = x;
        this.fixedY = y;
        velocity.x = 0;
        velocity.y = 0;

        // FIXED: Set health to 50 (5 hits to destroy)
        this.health = 50;
        this.maxHealth = 50;
        this.gunCooldown = 1.5;

        // FIXED: Initialize canShoot to true and gunTimer to 0
        this.canShoot = true;
        this.gunTimer = 0.0;

        System.out.println("Tank2 created with " + this.health + " health (5 hits to destroy)");
    }

    @Override
    protected void loadTankSprite() {
        loadSprite("/image/Tank2_Enemy.png");
    }

    @Override
    public void updateBehavior(Level_2 level2, Game_2 gp2, double deltaTime) {
        if (!alive) return;

        enforceFixedPosition();
        updateAI(gp2, deltaTime);
        updateGunTimer(deltaTime);
        updateHealthBarTimers(deltaTime);
        updateVisualStates(deltaTime);
        checkPlayerCollision(gp2);
    }

    private void updateHealthBarTimers(double deltaTime) {
        if (damageFeedbackTimer > 0) {
            damageFeedbackTimer -= deltaTime;
        }
    }

    void enforceFixedPosition() {
        this.x = fixedX;
        this.y = fixedY;
        velocity.x = 0;
        velocity.y = 0;
    }

    private void updateAI(Game_2 gp2, double deltaTime) {
        if (gp2.mainTank == null || !gp2.mainTank.alive) {
            playerDetected = false;
            alertLevel = Math.max(0, alertLevel - alertFadeSpeed * deltaTime);
            return;
        }

        double distanceToPlayer = calculateDistanceToPlayer(gp2);

        if (distanceToPlayer <= detectionRange && hasLineOfSight(gp2, gp2.mainTank)) {
            playerDetected = true;
            lastPlayerX = gp2.mainTank.getTankX();
            lastPlayerY = gp2.mainTank.getTankY();
            lostPlayerTimer = 0;
            alertLevel = Math.min(1.0, alertLevel + alertFadeSpeed * deltaTime);

            if (distanceToPlayer <= attackRange) {
                trackAndEngagePlayer(gp2, deltaTime);
            } else {
                trackPlayer(gp2.mainTank.getTankX(), gp2.mainTank.getTankY(), deltaTime);
            }
        } else if (playerDetected) {
            lostPlayerTimer += deltaTime;
            alertLevel = Math.max(0, alertLevel - alertFadeSpeed * deltaTime);

            if (lostPlayerTimer < lostPlayerTimeout) {
                trackPlayer(lastPlayerX, lastPlayerY, deltaTime);
            } else {
                playerDetected = false;
            }
        } else {
            alertLevel = Math.max(0, alertLevel - alertFadeSpeed * deltaTime);
        }
    }

    private double calculateDistanceToPlayer(Game_2 gp2) {
        return Math.sqrt(Math.pow(x - gp2.mainTank.getTankX(), 2) + Math.pow(y - gp2.mainTank.getTankY(), 2));
    }

    private boolean hasLineOfSight(Game_2 gp2, Tank target) {
        return true;
    }

    private void trackAndEngagePlayer(Game_2 gp2, double deltaTime) {
        double targetX = gp2.mainTank.getTankX();
        double targetY = gp2.mainTank.getTankY();

        double angleDifference = trackPlayer(targetX, targetY, deltaTime);

        if (Math.abs(angleDifference) < aimTolerance) {
            shoot();
        }
    }

    private double trackPlayer(double targetX, double targetY, double deltaTime) {
        double dx = targetX - x;
        double dy = targetY - y;
        double targetRotation = Math.atan2(dy, dx);

        double angleDifference = targetRotation - turretRotation;
        while (angleDifference > Math.PI) angleDifference -= 2 * Math.PI;
        while (angleDifference < -Math.PI) angleDifference += 2 * Math.PI;

        if (Math.abs(angleDifference) > 0.05) {
            double rotationAmount = Math.signum(angleDifference) * turretRotationSpeed * deltaTime;
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
        // Already handled in updateAI
    }

    private void checkPlayerCollision(Game_2 gp2) {
        if (gp2.mainTank != null && gp2.mainTank.alive && this.alive) {
            double dx = x - gp2.mainTank.getX();
            double dy = y - gp2.mainTank.getY();
            double distance = Math.sqrt(dx * dx + dy * dy);

            if (distance < 80.0) {
                System.out.println("Tank collision detected! Distance: " + distance);
            }
        }
    }

    /** FIXED: Damage-taking method with proper feedback **/
    @Override
    public void takeDamage(int damage) {
        if (!alive) return;

        System.out.println("Tank2 got hit! Taking " + damage + " damage. Health: " + health + " -> " + (health - damage));

        this.health -= damage;
        if (this.health <= 0) {
            this.health = 0;
            this.alive = false;
            System.out.println("Tank2 destroyed after taking enough damage!");
            onDestroyed();
        } else {
            int hitsRemaining = (int)Math.ceil((double)health / 10.0);
            System.out.println("Tank2 health remaining: " + health + " (" + hitsRemaining + " hits to destroy)");
            onDamageTaken(damage);
        }

        // Play hit sound if available
        if (gp2 != null) {
            gp2.playSoundEffects(2);
        }
    }

    /** FIXED: Shooting method with proper muzzle position **/
    @Override
    public void shoot() {
        if (canShoot && alive && playerDetected) {
            canShoot = false;
            gunTimer = gunCooldown;

            // FIXED: Calculate proper muzzle position
            double muzzleOffsetX = 50.0 * Math.cos(turretRotation); // Offset from tank center
            double muzzleOffsetY = 50.0 * Math.sin(turretRotation);
            double muzzleX = x + width/2 + muzzleOffsetX;
            double muzzleY = y + height/2 + muzzleOffsetY;

            double velocityX = bulletSpeed * Math.cos(turretRotation);
            double velocityY = bulletSpeed * Math.sin(turretRotation);

            Tank_Bullet bullet = new Tank_Bullet(
                    muzzleX, muzzleY,
                    velocityX, velocityY,
                    null, gp2, this // IMPORTANT: set shooter = this
            );

            if (gp2 != null) {
                gp2.addBullet(bullet);
                gp2.playSoundEffects(1);
            }

            onShoot();
        }
    }

    protected void onShoot() {
        System.out.println("Tank2 fired! Position: (" + x + ", " + y + ") Angle: " + Math.toDegrees(turretRotation));
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

        if (level2 != null && level2.checkCollisionWithRectangle(
                x + collisionOffsetX, y + collisionOffsetY,
                collisionWidth, collisionHeight)) {
            System.out.println("Warning: Tank2 placed at colliding position!");
        }
    }

    @Override
    public void draw(GraphicsContext gc, double camX, double camY, double scale) {
        if (!alive) return;

        drawTurret(gc, camX, camY, scale);

        if (showHealthBar && alive) {
            drawHealthBar(gc, camX, camY, scale);
        }

        // FIXED: Draw collision box for debugging
        if (false) { // Set to true to enable debug drawing
            drawCollisionBox(gc, camX, camY, scale);
        }

        // FIXED: Draw center point for collision debugging
//        if (true) { // Set to false to disable center point debugging
//            drawCenterPoint(gc, camX, camY, scale);
//        }
    }

    // FIXED: Add method to draw collision box for debugging
    private void drawCollisionBox(GraphicsContext gc, double camX, double camY, double scale) {
        double collisionX = (x + collisionOffsetX - camX) * scale;
        double collisionY = (y + collisionOffsetY - camY) * scale;
        double collisionW = collisionWidth * scale;
        double collisionH = collisionHeight * scale;

        gc.setStroke(Color.RED);
        gc.setLineWidth(2);
        gc.strokeRect(collisionX, collisionY, collisionW, collisionH);
    }

    // NEW: Draw center point and collision radius for debugging
    private void drawCenterPoint(GraphicsContext gc, double camX, double camY, double scale) {
        double centerX = (x + width/2 - camX) * scale;
        double centerY = (y + height/2 - camY) * scale;
        double radius = 40.0 * scale; // Same as collision radius in bullet code

        // Draw center point
        gc.setFill(Color.BLUE);
        gc.fillOval(centerX - 3, centerY - 3, 6, 6);

        // Draw collision radius circle
        gc.setStroke(Color.BLUE);
        gc.setLineWidth(1);
        gc.strokeOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
    }

    private void drawTurret(GraphicsContext gc, double camX, double camY, double scale) {
        if (spriteSheet == null) return;

        int turretCol = 2;
        int turretRow = 1;
        int srcX = turretCol * frameWidth;
        int srcY = turretRow * frameHeight;
        int srcW = frameWidth;
        int srcH = frameHeight;

        double drawW = width * scale;
        double drawH = height * scale;
        double cx = (x - camX) * scale + drawW / 2;
        double cy = (y - camY) * scale + drawH / 2;

        gc.save();
        gc.translate(cx, cy);
        gc.rotate(Math.toDegrees(turretRotation));
        gc.drawImage(
                spriteSheet,
                srcX, srcY, srcW, srcH,
                -drawW / 2, -drawH / 2,
                drawW, drawH
        );
        gc.restore();
    }

    void drawHealthBar(GraphicsContext gc, double camX, double camY, double scale) {
        if (health <= 0) return;

        double tankCenterX = (x - camX) * scale + (width * scale) / 2;
        double healthBarY = (y - camY) * scale + healthBarOffsetY;
        double healthBarX = tankCenterX - (healthBarWidth * scale) / 2;

        double scaledWidth = healthBarWidth * scale;
        double scaledHeight = healthBarHeight * scale;
        double healthPercentage = Math.max(0, (double) health / maxHealth);

        gc.setFill(Color.DARKGRAY);
        gc.fillRect(healthBarX, healthBarY, scaledWidth, scaledHeight);

        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1);
        gc.strokeRect(healthBarX, healthBarY, scaledWidth, scaledHeight);

        Color healthColor;
        if (healthPercentage > 0.6) {
            healthColor = Color.GREEN;
        } else if (healthPercentage > 0.3) {
            healthColor = Color.YELLOW;
        } else {
            healthColor = Color.RED;
        }

        if (damageFeedbackTimer > 0) {
            double flashIntensity = Math.sin(damageFeedbackTimer * 20) * 0.5 + 0.5;
            healthColor = healthColor.interpolate(Color.WHITE, flashIntensity);
        }

        gc.setFill(healthColor);
        gc.fillRect(healthBarX, healthBarY, scaledWidth * healthPercentage, scaledHeight);

        if (scale > 0.5) {
            gc.setFill(Color.WHITE);
            gc.setFont(javafx.scene.text.Font.font(10 * scale));
            String healthText = health + "/" + maxHealth;
            gc.fillText(healthText, tankCenterX - 15 * scale, healthBarY - 3);
        }
    }

    @Override
    protected void onDamageTaken(int damage) {
        super.onDamageTaken(damage);
        System.out.println("Tank2 took " + damage + " damage! Health: " + health);

        damageFeedbackTimer = damageFeedbackDuration;
        alertLevel = 1.0;
        playerDetected = true;
        lostPlayerTimer = 0;
    }

    @Override
    protected void onDestroyed() {
        super.onDestroyed();
        System.out.println("Tank2 destroyed!");
        showHealthBar = false;
    }

    // FIXED: Add getter methods for collision detection
    @Override
    public double getX() { return x; }

    @Override
    public double getY() { return y; }

    @Override
    public double getCollisionOffsetX() { return collisionOffsetX; }

    @Override
    public double getCollisionOffsetY() { return collisionOffsetY; }

    @Override
    public double getCollisionWidth() { return collisionWidth; }

    @Override
    public double getCollisionHeight() { return collisionHeight; }

    @Override
    public boolean isAlive() { return alive; }

    // NEW: Getter methods for tank dimensions (used in centered collision)
    public double getTankWidth() { return width; }
    public double getTankHeight() { return height; }
}