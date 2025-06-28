package platformgame.Tanks;

import platformgame.Game;
import platformgame.Game_2;
import platformgame.Map.Level_2;
import platformgame.Tank_Bullet;

import javafx.scene.image.Image;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Enemy_Tank extends Tank {
    private double attackRange = 250.0;     // Distance at which the enemy tank will attack
    protected double bulletSpeed = 300.0;
    private double turretRotationSpeed = 2.0; // Speed at which turret rotates to track player

    // Fixed position for the tank (it won't move from this position)
    private final double fixedX;
    private final double fixedY;

    public Enemy_Tank(double x, double y, double width, double height, double speed, Game gp, Game_2 gp2) {
        super(x, y, width, height, speed, gp, gp2);
        setCollisionBox(100, 100, -28, -28);

        // Store the fixed position
        this.fixedX = x;
        this.fixedY = y;

        // Set velocity to zero since tank is static
        velocity.x = 0;
        velocity.y = 0;
    }

    @Override
    protected void loadTankSprite() {
        loadSprite("/image/Tank_Enemy.png");  // Load a specific sprite for the enemy tank
    }

    @Override
    public void updateBehavior(Level_2 level2, Game_2 gp2, double deltaTime) {
        if (!alive) return;

        // Always keep the tank at its fixed position
        this.x = fixedX;
        this.y = fixedY;
        velocity.x = 0;
        velocity.y = 0;

        // Calculate the distance to the player's tank
        double distanceToPlayer = Math.sqrt(Math.pow(x - gp2.mainTank.getTankX(), 2) + Math.pow(y - gp2.mainTank.getTankY(), 2));

        // Only track and shoot if player is within attack range
        if (distanceToPlayer <= attackRange) {
            // Calculate angle to player
            double dx = gp2.mainTank.getTankX() - x;
            double dy = gp2.mainTank.getTankY() - y;
            double targetRotation = Math.atan2(dy, dx);

            // Smoothly rotate turret toward player
            double angleDifference = targetRotation - turretRotation;

            // Normalize angle difference to [-π, π]
            while (angleDifference > Math.PI) angleDifference -= 2 * Math.PI;
            while (angleDifference < -Math.PI) angleDifference += 2 * Math.PI;

            // Rotate turret smoothly toward target
            if (Math.abs(angleDifference) > 0.1) { // Small threshold to prevent jittering
                turretRotation += Math.signum(angleDifference) * turretRotationSpeed * deltaTime;
            } else {
                turretRotation = targetRotation; // Snap to target when close enough
            }

            // Shoot at player when turret is approximately aimed
            if (Math.abs(angleDifference) < 0.2) { // Allow some tolerance for shooting
                shoot();
            }
        }

        // Update gun timer
        updateGunTimer(deltaTime);

        // Check collision with player tank
        checkPlayerCollision(gp2);
    }

    // Method to check collision with player tank
    private void checkPlayerCollision(Game_2 gp2) {
        if (gp2.mainTank != null && gp2.mainTank.alive && this.alive) {
            // Calculate distance between tanks
            double dx = x - gp2.mainTank.getX();
            double dy = y - gp2.mainTank.getY();
            double distance = Math.sqrt(dx * dx + dy * dy);

            // Simple circular collision detection
            double collisionDistance = 80.0; // Adjust this value as needed

            if (distance < collisionDistance) {
                // Handle collision - just log for now to avoid damage loops
                System.out.println("Tank collision detected! Distance: " + distance);

                // Optional: Apply small damage over time
                //takeDamage(1);

                //gp2.mainTank.takeDamage(1);
            }
        }
    }

    @Override
    public void shoot() {
        if (canShoot && alive) {
            canShoot = false;
            gunTimer = gunCooldown;

            // Get muzzle position for bullet creation
            Vector2D muzzlePos = getMuzzlePosition();

            // Calculate bullet velocity based on turret rotation
            double velocityX = bulletSpeed * Math.cos(turretRotation);
            double velocityY = bulletSpeed * Math.sin(turretRotation);

            // Create bullet and add it to the game - this bullet should damage the player
            Tank_Bullet bullet = new Tank_Bullet(muzzlePos.x, muzzlePos.y, velocityX, velocityY, null, gp2);

            // Add bullet to the game's bullet list
            if (gp2 != null) {
                gp2.addBullet(bullet);
            }
        }
    }

    @Override
    protected void createBullet(double x, double y) {
        // Calculate velocity based on turret rotation
        double velocityX = bulletSpeed * Math.cos(turretRotation);
        double velocityY = bulletSpeed * Math.sin(turretRotation);

        // Create bullet and add it to the game
        Tank_Bullet bullet = new Tank_Bullet(x, y, velocityX, velocityY, null, gp2);

        // Add bullet to the game's bullet list
        if (gp2 != null) {
            gp2.addBullet(bullet);
        }
    }

    @Override
    public void createBullet(double x, double y, double velocityX, double velocityY) {
        Tank_Bullet bullet = new Tank_Bullet(x, y, velocityX, velocityY, null, gp2, this);

        // Add bullet to the game's bullet list
        if (gp2 != null) {
            gp2.addBullet(bullet);
        }
    }

    // Override physics process to prevent movement but keep collision checking
    @Override
    protected void physicsProcess(Level_2 level2, double deltaTime) {
        // Keep the tank at fixed position
        this.x = fixedX;
        this.y = fixedY;
        velocity.x = 0;
        velocity.y = 0;

        // Still check for map collisions (in case something tries to move the tank)
        if (level2 != null) {
            // Check collision with map objects at current position
            if (level2.checkCollisionWithRectangle(x + collisionOffsetX, y + collisionOffsetY,
                    collisionWidth, collisionHeight)) {
                // If the fixed position collides with map, you might want to log this
                System.out.println("Warning: Enemy tank placed at colliding position!");
            }
        }
    }

    @Override
    public void draw(GraphicsContext gc, double camX, double camY, double scale) {
        super.draw(gc, camX, camY, scale);

        // Draw enemy-specific visuals
        if (alive) {
            gc.setFill(Color.RED);
            gc.fillText("Enemy Tank", (x - camX) * scale, (y - camY) * scale - 10);

            // Optional: Draw attack range circle for debugging
            if (showCollisionDebug) {
                gc.setStroke(Color.RED);
                gc.setGlobalAlpha(0.3);
                gc.strokeOval((x - attackRange - camX) * scale, (y - attackRange - camY) * scale,
                        attackRange * 2 * scale, attackRange * 2 * scale);
                gc.setGlobalAlpha(1.0);
            }
        }
    }

    // Override onDamageTaken to add enemy-specific behavior
    @Override
    protected void onDamageTaken(int damage) {
        super.onDamageTaken(damage);
        System.out.println("Enemy tank took " + damage + " damage! Health: " + health);
    }

    // Override onDestroyed to add enemy-specific behavior
    @Override
    protected void onDestroyed() {
        super.onDestroyed();
        System.out.println("Enemy tank destroyed!");
        // You could add explosion effects, score points, etc.
    }

    // Getter for attack range (useful for game balancing)
    public double getAttackRange() {
        return attackRange;
    }

    // Setter for attack range (useful for different enemy types)
    public void setAttackRange(double range) {
        this.attackRange = range;
    }

    // Get fixed position
    public double getFixedX() { return fixedX; }
    public double getFixedY() { return fixedY; }
}