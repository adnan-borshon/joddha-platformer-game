package platformgame.Tanks;

import platformgame.Bullet;
import platformgame.Game;
import platformgame.Game_2;
import platformgame.Map.Level_2;
import platformgame.Tanks.Tank;
import platformgame.Tanks.Vector2D;

public class Enemy_Tank extends Tank {
    // Enemy-specific properties
    private double detectionRange = 300.0;
    private double attackRange = 250.0;
    private double patrolSpeed = 50.0;
    private double chaseSpeed = 80.0;

    // AI state
    private EnemyState currentState = EnemyState.PATROL;
    private Vector2D patrolTarget;
    private Vector2D patrolStartPoint;
    private double patrolRadius = 150.0;
    private double lastPlayerSightTime = 0;
    private double losePlayerDelay = 3.0; // seconds before giving up chase

    // Target tracking
    private Tank targetTank = null;

    public enum EnemyState {
        PATROL,
        CHASE,
        ATTACK,
        SEARCH
    }

    public Enemy_Tank(double x, double y, double width, double height, double speed, Game gp, Game_2 gp2) {
        super(x, y, width, height, speed, gp, gp2);

        // Enemy tank has different stats
        this.maxHealth = 80;
        this.health = maxHealth;
        this.rotationSpeed = 2.0; // Slower rotation than player
        this.gunCooldown = 1.0; // Slower fire rate than player

        // Set collision box for enemy tank
        setCollisionBox(90, 90, -25, -25);

        // Initialize patrol behavior
        patrolStartPoint = new Vector2D(x, y);
        generatePatrolTarget();
    }

    @Override
    protected void loadTankSprite() {
        // Load enemy tank sprite (you'd have a different sprite for enemies)
        loadSprite("/image/Enemy_Tank.png");
        // If you don't have a separate enemy sprite, you could use the same one
        // loadSprite("/image/Tank.png");
    }

    @Override
    protected void updateBehavior(Level_2 level2, Game_2 game, double deltaTime) {
        // Find player tank
        findTargetTank(game);

        // Update AI based on current state
        switch (currentState) {
            case PATROL:
                updatePatrolBehavior(deltaTime);
                checkForPlayer(deltaTime);
                break;
            case CHASE:
                updateChaseBehavior(deltaTime);
                checkPlayerDistance();
                break;
            case ATTACK:
                updateAttackBehavior(deltaTime);
                checkPlayerDistance();
                break;
            case SEARCH:
                updateSearchBehavior(deltaTime);
                break;
        }

        // Update turret to face movement direction or target
        updateTurretRotation(deltaTime);
    }

    @Override
    protected void createBullet(double x, double y) {

    }

    private void findTargetTank(Game_2 game) {
        // In a real implementation, you'd get the player tank from the game
        // For now, this is a placeholder
        // targetTank = game.getMainTank();
    }

    private void updatePatrolBehavior(double deltaTime) {
        // Move towards patrol target
        if (patrolTarget != null) {
            double dx = patrolTarget.x - (x + width/2);
            double dy = patrolTarget.y - (y + height/2);
            double distance = Math.sqrt(dx*dx + dy*dy);

            if (distance < 30) {
                // Reached patrol target, generate new one
                generatePatrolTarget();
            } else {
                // Move towards patrol target
                double angle = Math.atan2(dy, dx);
                tankRotation = angle;

                velocity.x = patrolSpeed * Math.cos(angle);
                velocity.y = patrolSpeed * Math.sin(angle);
                isMoving = true;
            }
        }
    }

    private void updateChaseBehavior(double deltaTime) {
        if (targetTank != null && targetTank.isAlive()) {
            double dx = targetTank.getTankX() - x;
            double dy = targetTank.getTankY() - y;
            double distance = Math.sqrt(dx*dx + dy*dy);

            if (distance <= attackRange) {
                currentState = EnemyState.ATTACK;
            } else {
                // Chase the player
                double angle = Math.atan2(dy, dx);
                tankRotation = angle;

                velocity.x = chaseSpeed * Math.cos(angle);
                velocity.y = chaseSpeed * Math.sin(angle);
                isMoving = true;
            }
        } else {
            // Lost target, go to search mode
            currentState = EnemyState.SEARCH;
        }
    }

    private void updateAttackBehavior(double deltaTime) {
        if (targetTank != null && targetTank.isAlive()) {
            double dx = targetTank.getTankX() - x;
            double dy = targetTank.getTankY() - y;
            double distance = Math.sqrt(dx*dx + dy*dy);

            if (distance > attackRange * 1.2) {
                // Player moved away, chase again
                currentState = EnemyState.CHASE;
            } else {
                // Stop moving and attack
                velocity.set(0, 0);
                isMoving = false;

                // Aim at player
                turretRotation = Math.atan2(dy, dx);

                // Shoot at player
                if (canShoot) {
                    shoot();
                }
            }
        } else {
            currentState = EnemyState.SEARCH;
        }
    }

    private void updateSearchBehavior(double deltaTime) {
        // Search behavior: rotate and occasionally move
        lastPlayerSightTime += deltaTime;

        if (lastPlayerSightTime > losePlayerDelay) {
            // Give up search, return to patrol
            currentState = EnemyState.PATROL;
            lastPlayerSightTime = 0;
        } else {
            // Rotate to search
            tankRotation += rotationSpeed * deltaTime;
        }
    }

    private void checkForPlayer(double deltaTime) {
        if (targetTank != null && targetTank.isAlive()) {
            double dx = targetTank.getTankX() - x;
            double dy = targetTank.getTankY() - y;
            double distance = Math.sqrt(dx*dx + dy*dy);

            if (distance <= detectionRange) {
                currentState = EnemyState.CHASE;
                lastPlayerSightTime = 0;
            }
        }
    }

    private void checkPlayerDistance() {
        if (targetTank != null && targetTank.isAlive()) {
            double dx = targetTank.getTankX() - x;
            double dy = targetTank.getTankY() - y;
            double distance = Math.sqrt(dx*dx + dy*dy);

            if (distance > detectionRange * 1.5) {
                // Player is too far, start searching
                currentState = EnemyState.SEARCH;
                lastPlayerSightTime = 0;
            }
        }
    }

    private void updateTurretRotation(double deltaTime) {
        double targetRotation = tankRotation;

        if (currentState == EnemyState.ATTACK && targetTank != null) {
            // Aim turret at player
            double dx = targetTank.getTankX() - x;
            double dy = targetTank.getTankY() - y;
            targetRotation = Math.atan2(dy, dx);
        }

        // Smooth turret rotation
        double rotationDiff = targetRotation - turretRotation;
        while (rotationDiff > Math.PI) rotationDiff -= 2 * Math.PI;
        while (rotationDiff < -Math.PI) rotationDiff += 2 * Math.PI;

        double maxRotationStep = rotationSpeed * deltaTime;
        if (Math.abs(rotationDiff) <= maxRotationStep) {
            turretRotation = targetRotation;
        } else {
            turretRotation += Math.signum(rotationDiff) * maxRotationStep;
        }
    }

    private void generatePatrolTarget() {
        // Generate a random point within patrol radius
        double angle = Math.random() * 2 * Math.PI;}
    }
