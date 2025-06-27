package platformgame.Tanks;

import javafx.scene.input.KeyCode;
import platformgame.Bullet;
import platformgame.Game;
import platformgame.Game_2;
import platformgame.Map.Level_2;

import java.util.Set;

public class Main_Tank extends Tank {


    public Main_Tank(double x, double y, double width, double height, double speed, Game gp, Game_2 gp2) {
        super(x, y, width, height, speed, gp, gp2);
        // Set collision box specific to main tank
        setCollisionBox(100, 100, -28, -28);
        // Enable collision debug by default for main tank (optional)
        showCollisionDebug = false;
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
        controlWithMouse(keysPressed, mouseX, mouseY, deltaSeconds);

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
    }

    @Override
    protected void updateBehavior(Level_2 level2, Game_2 game, double deltaTime) {
        // This method is called by parent's update, but for Main_Tank we use the mouse-controlled version above
        // So this can be empty or used for additional main tank specific behavior
    }

    // Player-specific control method with mouse control for turret
    private void controlWithMouse(Set<KeyCode> keysPressed, double mouseX, double mouseY, double deltaTime) {
        // Turret follows mouse (equivalent to $Turret.look_at(get_global_mouse_position()))
        double dx = mouseX - (x + width/2);
        double dy = mouseY - (y + height/2);
        turretRotation = Math.atan2(dy, dx);

        // Tank rotation input
        int rotationDirection = 0;
        if (keysPressed.contains(KeyCode.A)) {
            rotationDirection -= 1;
        }
        if (keysPressed.contains(KeyCode.D)) {
            rotationDirection += 1;
        }

        // Apply rotation
        tankRotation += rotationSpeed * rotationDirection * deltaTime;

        // Tank movement input
        velocity.set(0, 0);
        isMoving = false;

        if (keysPressed.contains(KeyCode.W)) {
            // Forward movement
            velocity.x = speed * Math.cos(tankRotation);
            velocity.y = speed * Math.sin(tankRotation);
            isMoving = true;
        } else if (keysPressed.contains(KeyCode.S)) {
            // Backward movement (half speed)
            velocity.x = -(speed/2) * Math.cos(tankRotation);
            velocity.y = -(speed/2) * Math.sin(tankRotation);
            isMoving = true;
        }
    }

    @Override
    protected void createBullet(double x, double y) {
        // Create player bullet
//        Bullet bullet = new Bullet(x, y, bulletSpeed);
        // You might want to add the bullet to a bullet manager or list here
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

    // Additional player-specific methods can be added here
    public void upgradeSpeed(double multiplier) {
        speed *= multiplier;
        System.out.println("Tank speed upgraded! New speed: " + speed);
    }

    public void upgradeFireRate(double multiplier) {
        gunCooldown *= multiplier;
        System.out.println("Tank fire rate upgraded! New cooldown: " + gunCooldown);
    }

    public void upgradeHealth(int additionalHealth) {
        setMaxHealth(maxHealth + additionalHealth);
        heal(additionalHealth);
        System.out.println("Tank health upgraded! New max health: " + maxHealth);
    }
}