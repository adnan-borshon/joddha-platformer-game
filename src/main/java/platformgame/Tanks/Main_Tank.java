package platformgame.Tanks;

import javafx.scene.input.KeyCode;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import platformgame.Bullet;
import platformgame.Game;
import platformgame.Game_2;
import platformgame.Map.Level_2;
import platformgame.Tank_Bullet;

import java.util.Set;
import java.util.ArrayList;
import java.util.List;

public class Main_Tank extends Tank {

    double bulletSpeed = 400; // Increased bullet speed for better visibility

    // Track current movement direction for bullet firing
    private double currentMoveDirection = 0; // 0 = right, PI/2 = down, PI = left, 3*PI/2 = up
    private boolean hasMovementDirection = false;

    public Main_Tank(double x, double y, double width, double height, double speed, Game gp, Game_2 gp2) {
        super(x, y, width, height, speed, gp, gp2);

        // Set collision box specific to main tank
        setCollisionBox(100, 100, -28, -28);
        // Enable collision debug by default for main tank (optional)
        showCollisionDebug = false;

        // IMPORTANT: Set default movement direction so tank can fire even before moving
        currentMoveDirection = 0; // Default to firing right
        hasMovementDirection = true; // Allow firing immediately
    }

    @Override
    protected void loadTankSprite() {
        loadSprite("/image/Tank.png");
    }

    // Player-specific update method with mouse and keyboard controls
    public void update(Set<KeyCode> keysPressed, Level_2 level2, Game_2 game, long now, long deltaTime, double mouseX, double mouseY) {
        if (!alive) return;

        double deltaSeconds = deltaTime / 1_000_000_000.0; // Convert to seconds

        // Player-specific control method
        controlWithDirectionalMovement(keysPressed, deltaSeconds);

        // Use parent's physics processing
        physicsProcess(level2, deltaSeconds);

        // Update gun timer
        updateGunTimer(deltaSeconds);

        // Update animations
        updateAnimations(deltaTime);

        // Toggle debug collision rectangle with F1 key
        if (keysPressed.contains(KeyCode.F1)) {
            toggleCollisionDebug();
        }

        // Handle shooting with the space bar
        if (keysPressed.contains(KeyCode.SPACE)) {
            shoot();  // Fire a bullet when spacebar is pressed
        }
    }

    @Override
    protected void updateBehavior(Level_2 level2, Game_2 game, double deltaTime) {
        // Empty as we handle directional behavior in the `update` method
    }

    // Shoot a bullet in the current movement direction
    @Override
    public void shoot() {
        if (canShoot && alive) {
            canShoot = false;
            gunTimer = gunCooldown;

            System.out.println("Main tank shooting in direction: " + Math.toDegrees(currentMoveDirection)); // Debug output

            // Get center position for bullet creation
            double bulletX = x + width / 2;
            double bulletY = y + height / 2;

            // Calculate bullet velocity based on current movement direction
            double velocityX = bulletSpeed * Math.cos(currentMoveDirection);
            double velocityY = bulletSpeed * Math.sin(currentMoveDirection);

            // Create bullet and set the shooter to this tank
            Tank_Bullet bullet = new Tank_Bullet(bulletX, bulletY, velocityX, velocityY, null, gp2, this);

            // Add bullet to the game's bullet list
            if (gp2 != null) {
                gp2.addBullet(bullet);
            }
        }
    }

    // This method creates bullet locally (used by parent class if needed)
    @Override
    protected void createBullet(double x, double y) {
        // Calculate velocity based on current movement direction
        double velocityX = bulletSpeed * Math.cos(currentMoveDirection);
        double velocityY = bulletSpeed * Math.sin(currentMoveDirection);

        // Create bullet and set the shooter
        Tank_Bullet bullet = new Tank_Bullet(x, y, velocityX, velocityY, null, gp2, this);

        // Add bullet to the game's bullet list instead of local list
        if (gp2 != null) {
            gp2.addBullet(bullet);
        }
    }

    // Override the createBullet method with velocity parameters
    @Override
    public void createBullet(double x, double y, double velocityX, double velocityY) {
        Tank_Bullet bullet = new Tank_Bullet(x, y, velocityX, velocityY, null, gp2);

        // Add bullet to the game's bullet list
        if (gp2 != null) {
            gp2.addBullet(bullet);
        }
    }

    private void controlWithDirectionalMovement(Set<KeyCode> keysPressed, double deltaTime) {
        // Reset movement state
        velocity.set(0, 0);
        isMoving = false;
        hasMovementDirection = false;

        // Handle 8-directional movement and track direction for bullet firing
        boolean up = keysPressed.contains(KeyCode.W);
        boolean down = keysPressed.contains(KeyCode.S);
        boolean left = keysPressed.contains(KeyCode.A);
        boolean right = keysPressed.contains(KeyCode.D);

        double moveX = 0;
        double moveY = 0;

        // Calculate movement vector
        if (up) moveY -= 1;
        if (down) moveY += 1;
        if (left) moveX -= 1;
        if (right) moveX += 1;

        // If there's movement, calculate direction and velocity
        if (moveX != 0 || moveY != 0) {
            isMoving = true;
            hasMovementDirection = true;

            // Calculate movement direction
            currentMoveDirection = Math.atan2(moveY, moveX);

            // Normalize diagonal movement to maintain consistent speed
            double length = Math.sqrt(moveX * moveX + moveY * moveY);
            if (length > 0) {
                moveX /= length;
                moveY /= length;
            }

            // Set velocity
            velocity.x = speed * moveX;
            velocity.y = speed * moveY;

            // Update tank rotation to match movement direction
            tankRotation = currentMoveDirection;
        }

        // If no movement keys are pressed but we had a previous direction, keep that direction
        // This allows firing in the last moved direction even when stationary
    }

    @Override
    protected void updateGunTimer(double deltaTime) {
        if (gunTimer > 0) {
            gunTimer -= deltaTime;
            if (gunTimer <= 0) {
                canShoot = true;
            }
        }
    }

    @Override
    protected void onDamageTaken(int damage) {
        super.onDamageTaken(damage);
        // Player-specific damage handling
        // You could add screen shake, damage indicators, etc.
    }

    @Override
    protected void onDestroyed() {
        super.onDestroyed();
        // Player-specific destruction handling
        if (gp2 != null) {
            gp2.GameState = gp2.gameOverState; // Trigger game over
        }
    }

    @Override
    public void speak() {
        System.out.println("Main Tank engine roaring...");
    }

    // Getter methods for position (x and y)
    public double getTankX() {
        return x; // Return the x-coordinate of the tank
    }

    public double getTankY() {
        return y; // Return the y-coordinate of the tank
    }

    // Getter for current movement direction (for debugging or other purposes)
    public double getCurrentMoveDirection() {
        return currentMoveDirection;
    }
}