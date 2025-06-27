package platformgame.Entity;

import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import platformgame.Bullet;
import platformgame.Game;
import platformgame.Map.Level_1;
import platformgame.Objects.Obj_Boom;
import platformgame.Objects.SuperObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class Player extends Entity {

    private final int totalFrames_walk = 5;

    // ✅ Explosion reaction
    private boolean reactingToExplosion = false;
    private long explosionReactionStartTime = 0;
    private final int explosionReactionFrames = 4;
    private final int explosionReactionRow = 8;
    private final long explosionFrameDuration = 120_000_000;

    // ✅ Fist attack
    private boolean attackingWithFist = false;
    private long fistAttackStartTime = 0;
    private final int totalFistFrames = 3;
    private final int fistAttackRow = 3;
    private final long fistFrameDuration = 100_000_000;

    // ✅ NEW: Shooting animation
    private boolean shooting = false;
    private long shootStartTime = 0;
    private final int totalShootFrames = 4;
    private final int shootAnimationRow = 3; // You mentioned 4th row, but arrays are 0-indexed so row 3
    private final long shootFrameDuration = 80_000_000; // 80ms per frame for quick shooting animation

    // ✅ Shooting mechanics
    private long lastShotTime = 0;
    private final long shootCooldown = 300_000_000; // 300ms between shots
    private List<Bullet> bullets;
    private final double bulletSpeed = 4.0;
    private double lastDirectionX = 1.0; // Default facing right
    private double lastDirectionY = 0.0; // Default horizontal
    // ✅ Health & Ammo
    public int hp = 10;
    public int maxHp = 10;
    public int ammo = 30; // Start with some ammo

    // ✅ Death animation (Row 10 → Index 9)
    private boolean isDead = false;
    private long deathStartTime = 0;
    private final int deathAnimationRow = 9;
    private final int totalDeathFrames = 6;
    private final long deathFrameDuration = 150_000_000;

    // ✅ Damage cooldown to prevent spam damage
    private long lastDamageTime = 0;
    private final long damageCooldown = 500_000_000; // 0.5 seconds

    // ✅ New: Melee hit reaction
    private boolean reactingToMeleeHit = false;
    private long meleeHitStartTime = 0;
    private final int meleeHitFrames = 3;
    private final int meleeHitRow = 0;
    private final long meleeHitFrameDuration = 100_000_000;

    public Player(double x, double y, double width, double height, double speed, Game gp) {
        super(x, y, width, height, speed, gp);
        imageSet(totalFrames_walk, "/image/main_character.png");
        bullets = new ArrayList<>();
    }

    public void update(Set<KeyCode> keys, Level_1 level1, Game game, long now, long deltaTime) {

        // Update bullets first
        updateBullets(deltaTime, now);

        if (isDead) {
            currentRow = deathAnimationRow;
            int frameIndex = (int) ((now - deathStartTime) / deathFrameDuration);
            if (frameIndex < totalDeathFrames) {
                currentFrame = frameIndex;
            } else {
                currentFrame = totalDeathFrames - 1;
                game.GameState = game.gameOverState;
            }
            return;
        }

        if (reactingToExplosion) {
            currentRow = explosionReactionRow;
            int frameIndex = (int) ((now - explosionReactionStartTime) / explosionFrameDuration);
            if (frameIndex < explosionReactionFrames) {
                currentFrame = frameIndex;
            } else {
                currentFrame = 0;
                currentRow = 0;
                reactingToExplosion = false;

                if (hp <= 0 && !isDead) {
                    isDead = true;
                    deathStartTime = now;
                    gp.ui.showMessage("You Died!");
                    gp.playSoundEffects(2);
                }
            }
            return;
        }

        if (reactingToMeleeHit) {
            currentRow = meleeHitRow;
            int frameIndex = (int) ((now - meleeHitStartTime) / meleeHitFrameDuration);
            if (frameIndex < meleeHitFrames) {
                currentFrame = frameIndex;
            } else {
                currentFrame = 0;
                currentRow = 0;
                reactingToMeleeHit = false;

                if (hp <= 0 && !isDead) {
                    isDead = true;
                    deathStartTime = now;
                    gp.ui.showMessage("You Died!");
                    gp.playSoundEffects(2);
                }
            }
            return;
        }

        // ✅ NEW: Handle shooting animation
        if (shooting) {
            currentRow = shootAnimationRow;
            int frameIndex = (int) ((now - shootStartTime) / shootFrameDuration);
            if (frameIndex < totalShootFrames) {
                currentFrame = frameIndex;
            } else {
                shooting = false;
                currentFrame = 0;
                currentRow = 0;
            }
            return; // Don't process other animations while shooting
        }

        if (attackingWithFist) {
            currentRow = fistAttackRow;
            int frameIndex = (int) ((now - fistAttackStartTime) / fistFrameDuration);
            if (frameIndex < totalFistFrames) {
                currentFrame = frameIndex;
            } else {
                attackingWithFist = false;
                currentFrame = 0;
                currentRow = 0;
            }
            return;
        }

        if (keys.contains(KeyCode.L)) {
            hp = 10;
        }

        // ✅ NEW: Handle shooting input (using Q key like many games)
        if (keys.contains(KeyCode.F) && !shooting && !attackingWithFist) {
            shoot(now);
        }

        // Handle fist attack
        if (keys.contains(KeyCode.SPACE) && !attackingWithFist && !shooting) {
            attackingWithFist = true;
            fistAttackStartTime = now;
            currentFrame = 0;
            currentRow = fistAttackRow;

            Rectangle2D punchBox = facingRight
                    ? new Rectangle2D(x + width, y, width * 0.6, height)
                    : new Rectangle2D(x - width * 0.6, y, width * 0.6, height);

            for (Scout scoutEntity : game.scout) {
                if (scoutEntity != null && punchBox.intersects(scoutEntity.getHitbox())) {
                    scoutEntity.takeDamage();
                }
            }

            for (Enemy enemyEntity : game.enemies) {
                if (enemyEntity != null && !enemyEntity.isDead()) {
                    Rectangle2D enemyHitbox = new Rectangle2D(
                            enemyEntity.getX(), enemyEntity.getY(),
                            enemyEntity.getWidth(), enemyEntity.getHeight()
                    );
                    if (punchBox.intersects(enemyHitbox)) {
                        enemyEntity.receiveDamage();
                    }
                }
            }

            for (Soldier soldierEntity : game.soldiers) {
                if (soldierEntity != null && !soldierEntity.isDead()) {
                    Rectangle2D soldierHitbox = new Rectangle2D(
                            soldierEntity.getX(), soldierEntity.getY(),
                            soldierEntity.getWidth(), soldierEntity.getHeight()
                    );
                    if (punchBox.intersects(soldierHitbox)) {
                        soldierEntity.receiveDamage();
                    }
                }
            }

            return;
        }

        boolean moved = false;
        double newX = x;
        double newY = y;

        // Updated movement section in your update() method - replace the movement handling part
        if (game.GameState == game.playState) {
            if (keys.contains(KeyCode.W)) {
                double testY = y - speed;
                if (!level1.isCollisionRect(x, testY, width, height)
                        && !checkObjectCollisionsAndInteract(x, testY, width, height, game)
                        && !checkNpcCollision(x, testY, game, now)
                        && !checkSoldierCollision(x, testY, game)) {
                    newY = testY;
                    moved = true;
                    // Update shooting direction
                    if (keys.contains(KeyCode.A)) {
                        lastDirectionX = -0.707; // Diagonal up-left
                        lastDirectionY = -0.707;
                        facingRight = false;
                    } else if (keys.contains(KeyCode.D)) {
                        lastDirectionX = 0.707;  // Diagonal up-right
                        lastDirectionY = -0.707;
                        facingRight = true;
                    } else {
                        lastDirectionX = 0;      // Straight up
                        lastDirectionY = -1;
                    }
                }
            }
            if (keys.contains(KeyCode.S)) {
                double testY = y + speed;
                if (!level1.isCollisionRect(x, testY, width, height)
                        && !checkObjectCollisionsAndInteract(x, testY, width, height, game)
                        && !checkNpcCollision(x, testY, game, now)
                        && !checkSoldierCollision(x, testY, game)) {
                    newY = testY;
                    moved = true;
                    // Update shooting direction
                    if (keys.contains(KeyCode.A)) {
                        lastDirectionX = -0.707; // Diagonal down-left
                        lastDirectionY = 0.707;
                        facingRight = false;
                    } else if (keys.contains(KeyCode.D)) {
                        lastDirectionX = 0.707;  // Diagonal down-right
                        lastDirectionY = 0.707;
                        facingRight = true;
                    } else {
                        lastDirectionX = 0;      // Straight down
                        lastDirectionY = 1;
                    }
                }
            }
            if (keys.contains(KeyCode.A)) {
                double testX = x - speed;
                if (!level1.isCollisionRect(testX, y, width, height)
                        && !checkObjectCollisionsAndInteract(testX, y, width, height, game)
                        && !checkNpcCollision(testX, y, game, now)
                        && !checkSoldierCollision(testX, y, game)) {
                    newX = testX;
                    moved = true;
                    facingRight = false;
                    // Update shooting direction (only if not moving vertically)
                    if (!keys.contains(KeyCode.W) && !keys.contains(KeyCode.S)) {
                        lastDirectionX = -1;     // Straight left
                        lastDirectionY = 0;
                    }
                }
            }
            if (keys.contains(KeyCode.D)) {
                double testX = x + speed;
                if (!level1.isCollisionRect(testX, y, width, height)
                        && !checkObjectCollisionsAndInteract(testX, y, width, height, game)
                        && !checkNpcCollision(testX, y, game, now)
                        && !checkSoldierCollision(testX, y, game)) {
                    newX = testX;
                    moved = true;
                    facingRight = true;
                    // Update shooting direction (only if not moving vertically)
                    if (!keys.contains(KeyCode.W) && !keys.contains(KeyCode.S)) {
                        lastDirectionX = 1;      // Straight right
                        lastDirectionY = 0;
                    }
                }
            }
        }


        checkEnemyCollisions(game, now);
        checkSoldierCollisions(game, now);

        for (Npc npcEntity : game.npc) {
            if (npcEntity != null && npcEntity.playerIsTouching) {
                game.GameState = game.dialogueState;
                npcEntity.speak();
                break;
            }
        }

        x = newX;
        y = newY;

        if (moved && !reactingToExplosion && !attackingWithFist && !isDead && !reactingToMeleeHit && !shooting) {
            currentRow = 4;
            animationTimer += deltaTime;
            if (animationTimer > 100_000_000) {
                nextFrame(totalFrames_walk);
                animationTimer = 0;
            }
        } else if (!reactingToExplosion && !attackingWithFist && !isDead && !reactingToMeleeHit && !shooting) {
            currentFrame = 0;
            currentRow = 0;
        }
    }



    // Updated shoot method - replace your existing shoot() method
    private void shoot(long now) {
        // Check cooldown and ammo
        if ((now - lastShotTime) < shootCooldown) {
            return; // Still in cooldown
        }

        if (ammo <= 0) {
            gp.ui.showMessage("No ammo!");
            return;
        }

        // Calculate bullet starting position (outside player hitbox based on facing direction)
        double bulletStartX = x + width / 2;  // Start from center
        double bulletStartY = y + height / 2;

        // Offset the starting position slightly outside the player based on direction
        bulletStartX += lastDirectionX * (width / 2 + 5);
        bulletStartY += lastDirectionY * (height / 2 + 5);

        // Calculate bullet velocity based on last movement direction
        double velX = lastDirectionX * bulletSpeed;
        double velY = lastDirectionY * bulletSpeed;

        // Create and add bullet
        Bullet bullet = new Bullet(bulletStartX, bulletStartY, velX, velY, gp);
        bullets.add(bullet);

        // Update shooting state
        shooting = true;
        shootStartTime = now;
        lastShotTime = now;
        ammo--;

        // Play shooting sound effect
        gp.playSoundEffects(4);

        currentFrame = 0;
        currentRow = shootAnimationRow;

        System.out.println("Player shot in direction (" + lastDirectionX + ", " + lastDirectionY + ")! Ammo remaining: " + ammo);
    }









    // ✅ NEW: Update bullets
    private void updateBullets(long deltaTime, long now) {
        Iterator<Bullet> bulletIterator = bullets.iterator();
        while (bulletIterator.hasNext()) {
            Bullet bullet = bulletIterator.next();
            bullet.update(deltaTime, now);

            // Check if bullet hit any enemies
            checkBulletEnemyCollisions(bullet);

            if (bullet.shouldRemove()) {
                bulletIterator.remove();
            }
        }
    }

    // ✅ NEW: Check bullet collisions with enemies
    private void checkBulletEnemyCollisions(Bullet bullet) {
        Rectangle2D bulletRect = new Rectangle2D(
                bullet.getX() - bullet.getWidth() / 2,
                bullet.getY() - bullet.getHeight() / 2,
                bullet.getWidth(),
                bullet.getHeight()
        );

        // Check collision with scouts
        for (Scout scout : gp.scout) {
            if (scout != null) {
                Rectangle2D scoutRect = scout.getHitbox();
                if (bulletRect.intersects(scoutRect)) {
                    scout.takeDamage();
                    bullet.markForRemoval();
                    gp.ui.showMessage("Scout hit!");
                    return;
                }
            }
        }

        // Check collision with enemies
        for (Enemy enemy : gp.enemies) {
            if (enemy != null && !enemy.isDead()) {
                Rectangle2D enemyRect = new Rectangle2D(
                        enemy.getX(), enemy.getY(),
                        enemy.getWidth(), enemy.getHeight()
                );
                if (bulletRect.intersects(enemyRect)) {
                    enemy.receiveDamage();
                    bullet.markForRemoval();
                    gp.ui.showMessage("Enemy hit!");
                    return;
                }
            }
        }

        // Check collision with soldiers
        for (Soldier soldier : gp.soldiers) {
            if (soldier != null && !soldier.isDead()) {
                Rectangle2D soldierRect = new Rectangle2D(
                        soldier.getX(), soldier.getY(),
                        soldier.getWidth(), soldier.getHeight()
                );
                if (bulletRect.intersects(soldierRect)) {
                    soldier.receiveDamage();
                    bullet.markForRemoval();
                    gp.ui.showMessage("Soldier hit!");
                    return;
                }
            }
        }
    }

    private void checkEnemyCollisions(Game game, long now) {
        double playerCenterX = x + width / 2;
        double playerCenterY = y + height / 2;

        for (Enemy enemyEntity : game.enemies) {
            if (enemyEntity != null && !enemyEntity.isDead()) {
                double enemyCenterX = enemyEntity.getX() + enemyEntity.getWidth() / 2;
                double enemyCenterY = enemyEntity.getY() + enemyEntity.getHeight() / 2;

                double dx = playerCenterX - enemyCenterX;
                double dy = playerCenterY - enemyCenterY;
                double distance = Math.sqrt(dx * dx + dy * dy);

                if (distance <= 25) {
                    if (!reactingToExplosion && !reactingToMeleeHit && !isDead && (now - lastDamageTime) > damageCooldown) {
                        takeDamage(0.05, now);
                        lastDamageTime = now;
                    }
                }
            }
        }
    }

    private void checkSoldierCollisions(Game game, long now) {
        if (game.soldiers == null) return;

        Rectangle2D playerRect = new Rectangle2D(x, y, width, height);

        for (Soldier soldierEntity : game.soldiers) {
            if (soldierEntity != null && !soldierEntity.isDead()) {
                Rectangle2D soldierRect = new Rectangle2D(
                        soldierEntity.getX(), soldierEntity.getY(),
                        soldierEntity.getWidth(), soldierEntity.getHeight()
                );

                if (playerRect.intersects(soldierRect)) {
                    if (!reactingToExplosion && !reactingToMeleeHit && !isDead && (now - lastDamageTime) > damageCooldown) {
                        takeMeleeDamageFromEnemy(1, now);
                        lastDamageTime = now;
                    }
                }
            }
        }
    }

    private boolean checkNpcCollision(double playerX, double playerY, Game game, long now) {
        Rectangle2D playerRect = new Rectangle2D(playerX, playerY, width, height);

        for (Npc npcEntity : game.npc) {
            if (npcEntity != null) {
                Rectangle2D npcRect = new Rectangle2D(
                        npcEntity.getX(), npcEntity.getY(),
                        npcEntity.getWidth(), npcEntity.getHeight()
                );
                if (playerRect.intersects(npcRect)) {
                    npcEntity.notifyPlayerCollision();
                    return true;
                }
            }
        }

        for (Scout scoutEntity : game.scout) {
            if (scoutEntity != null) {
                Rectangle2D scoutRect = new Rectangle2D(
                        scoutEntity.getX(), scoutEntity.getY(),
                        scoutEntity.getWidth(), scoutEntity.getHeight()
                );
                if (playerRect.intersects(scoutRect)) {
                    scoutEntity.setPlayerInRange(true);
                    return true;
                }
            }
        }

        for (Enemy enemyEntity : game.enemies) {
            if (enemyEntity != null && !enemyEntity.isDead()) {
                Rectangle2D enemyRect = new Rectangle2D(
                        enemyEntity.getX(), enemyEntity.getY(),
                        enemyEntity.getWidth(), enemyEntity.getHeight()
                );
                if (playerRect.intersects(enemyRect)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean checkSoldierCollision(double playerX, double playerY, Game game) {
        if (game.soldiers == null) return false;

        Rectangle2D playerRect = new Rectangle2D(playerX, playerY, width, height);

        for (Soldier soldierEntity : game.soldiers) {
            if (soldierEntity != null && !soldierEntity.isDead()) {
                Rectangle2D soldierRect = new Rectangle2D(
                        soldierEntity.getX(), soldierEntity.getY(),
                        soldierEntity.getWidth(), soldierEntity.getHeight()
                );

                if (playerRect.intersects(soldierRect)) {
                    double overlapX = Math.min(playerRect.getMaxX(), soldierRect.getMaxX()) -
                            Math.max(playerRect.getMinX(), soldierRect.getMinX());
                    double overlapY = Math.min(playerRect.getMaxY(), soldierRect.getMaxY()) -
                            Math.max(playerRect.getMinY(), soldierRect.getMinY());

                    if (overlapX >= 5 && overlapY >= 5) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void triggerExplosionReaction(long now) {
        reactingToExplosion = true;
        explosionReactionStartTime = now;
        currentFrame = 0;
    }

    public void takeDamage(double percentage, long now) {
        if (isDead) return;

        int damage = (int) (maxHp * percentage);
        hp -= damage;
        if (hp < 0) {
            hp = 0;
        }

        triggerExplosionReaction(now);
        gp.ui.showMessage("You took damage -" + damage + " HP");
        gp.playSoundEffects(2);
    }

    public void takeMeleeDamageFromEnemy(int rawDamage, long now) {
        if (isDead) return;

        if ((now - lastDamageTime) < damageCooldown) return;

        this.hp -= rawDamage;
        if (hp < 0) hp = 0;

        lastDamageTime = now;

        reactingToMeleeHit = true;
        meleeHitStartTime = now;
        currentFrame = 0;

        gp.ui.showMessage("Enemy attacked! -" + rawDamage + " HP");
        gp.playSoundEffects(2);

        System.out.println("Player took melee damage: " + rawDamage + ", HP now: " + hp);
    }

    public void draw(GraphicsContext gc, double camX, double camY, double scale) {
        drawEntity(gc, camX, camY, scale);

        // ✅ NEW: Draw bullets
        for (Bullet bullet : bullets) {
            bullet.draw(gc, camX, camY, scale, facingRight);
        }
    }

    public boolean checkObjectCollisionsAndInteract(double nextX, double nextY, double width, double height, Game game) {
        Rectangle2D playerRect = new Rectangle2D(nextX, nextY, width, height);

        for (int i = 0; i < game.object.length; i++) {
            SuperObject obj = game.object[i];
            if (obj != null) {
                double dx = Math.abs(obj.worldX - nextX);
                double dy = Math.abs(obj.worldY - nextY);

                if (dx < 128 && dy < 128) {
                    Rectangle2D objRect = obj.getBoundingBox();
                    if (playerRect.intersects(objRect)) {
                        switch (obj.name.toLowerCase()) {
                            case "key":
                                game.hasKey++;
                                game.object[i] = null;
                                game.playSoundEffects(1);
                                game.ui.showMessage("You got a key");
                                break;
                            case "door":
                                if (game.hasKey > 0) {
                                    game.hasKey--;
                                    game.object[i] = null;
                                    game.playSoundEffects(3);
                                    game.ui.showMessage("Door has opened");
                                } else {
                                    game.ui.showMessage("You need a key to open");
                                    return true;
                                }
                                break;
                            case "boots":
                                speed += 10;
                                game.object[i] = null;
                                game.playSoundEffects(2);
                                game.ui.showMessage("You got speed up +2");
                                break;
                            case "ammo":
                                ammo += 10;
                                game.object[i] = null;
                                game.playSoundEffects(1);
                                game.ui.showMessage("Picked up 10 ammo");
                                break;
                            case "life":
                            case "obj":
                                if (hp < maxHp) {
                                    int healAmount = (int)(maxHp * 0.2);
                                    hp += healAmount;
                                    if (hp > maxHp) hp = maxHp;

                                    System.out.println("Picking up life, removing object: " + obj.name);
                                    System.out.println("Healed for: " + healAmount + ", current HP: " + hp + "/" + maxHp);

                                    game.object[i] = null;
                                    game.playSoundEffects(1);
                                    game.ui.showMessage("Life restored +" + healAmount + " HP");
                                } else {
                                    game.ui.showMessage("Health already full");
                                }
                                break;
                            case "mine":
                                takeDamage(0.10, System.nanoTime());
                                game.playSoundEffects(4);
                                game.ui.showMessage("Stepped on a mine! -10% HP");
                                game.object[i] = null;
                                break;
                            case "boom":
                                if (obj instanceof Obj_Boom) {
                                    Obj_Boom boomObj = (Obj_Boom) obj;
                                    if (boomObj.shouldAppear() && !boomObj.isCollected()) {
                                        boomObj.collect();
                                        game.object[i] = null;
                                        game.playSoundEffects(1);
                                        game.ui.showMessage("You collected the mysterious boom!");

                                        System.out.println("Player collected the boom!");

                                        game.eventHandler.enableBridgeDestruction();
                                        game.ui.showMessage("You can now destroy the bridge!");
                                    } else {
                                        game.ui.showMessage("This boom seems inactive...");
                                    }
                                }
                                break;
                            default:
                                if (obj.collision) return true;
                                break;
                        }
                    }
                }
            }
        }

        return false;
    }

    // ✅ NEW: Getter methods for bullets (useful for debugging)
    public List<Bullet> getBullets() {
        return bullets;
    }

    public int getAmmo() {
        return ammo;
    }

    public void addAmmo(int amount) {
        this.ammo += amount;
    }
}