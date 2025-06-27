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
    //without gun

    //walk and run
    private final int frontWalkFrame=4;
    private final int frontWalkRow=3;

    private final int backWalkFrame=4;
    private final int backWalkRow=4;

    private final int WalkFrame=6;
    private final int WalkRow=5;

    //idle
    private final int frontIdleFrame=2;
    private final int frontIdleRow=0;

    private final int IdleFrame=2;
    private final int IdleRow=1;

    private final int backIdleFrame=2;
    private final int backIdleRow=2;


    //fist
    private final int FrontFistFrame=2;
    private final int FrontFistRow=6;

    private final int FistFrame=2;
    private final int FistRow=7;

    private final int BackFistFrame=2;
    private final int BackFistRow=8;

    //hurt with no gun
    private final int HitFrame=2;
    private final int HitRow=19;

    private final int FrontHitFrame=2;
    private final int FrontHitRow=20;

    private final int BackHitFrame=2;
    private final int BackHitRow=21;

    //with gun
    //shoot
    private final int FrontShootFrame=2;
    private final int FrontShootRow=9;

    private final int ShootFrame=2;
    private final int ShootRow=10;
    private final int BackShootFrame=2;
    private final int BackShootRow=11;


    //dead
    private final int deadFrame=3;
    private final int deadRow=12;

    //walk and run with gun
    private final int GunFrontWalkFrame=4;
    private final int GunFrontWalkRow=14;

    private final int GunBackWalkFrame=4;
    private final int GunBackWalkRow=15;

    private final int GunWalkFrame=6;
    private final int GunWalkRow=13;

    //idle with gun (NEW: Using gun idle frames)
    private final int GunFrontIdleFrame=2;
    private final int GunFrontIdleRow=16; // Assuming gun front idle is row 16

    private final int GunIdleFrame=2;
    private final int GunIdleRow=17; // Assuming gun idle is row 17

    private final int GunBackIdleFrame=2;
    private final int GunBackIdleRow=18; // Assuming gun back idle is row 18

    //hurt with gun
    private final int GunHitFrame=2;
    private final int GunHitRow=17;

    private final int GunFrontHitFrame=2;
    private final int GunFrontHitRow=16;

    //animation timer
    //fist
    private boolean attackingWithFist = false;
    private long fistAttackStartTime = 0;
    private final long fistFrameDuration = 100_000_000;
    //shoot
    private boolean shooting = false;
    private long shootStartTime = 0;
    private final long shootFrameDuration = 80_000_000;
    // ✅ Shooting mechanics
    private long lastShotTime = 0;
    private final long shootCooldown = 300_000_000; // 300ms between shots
    private List<Bullet> bullets;
    private final double bulletSpeed = 6.0;
    private double lastDirectionX = 1.0; // Default facing right
    private double lastDirectionY = 0.0; // Default horizontal
    // ✅ Death animation (Row 10 → Index 9)
    private boolean isDead = false;
    private long deathStartTime = 0;
    private final long deathFrameDuration = 150_000_000;

    // ✅ Damage cooldown to prevent spam damage
    private long lastDamageTime = 0;
    private final long damageCooldown = 500_000_000; // 0.5 seconds
    // ✅ New: Melee hit reaction
    private boolean reactingToMeleeHit = false;
    private long meleeHitStartTime = 0;
    private final long meleeHitFrameDuration = 100_000_000;


    private final int totalFrames_walk = 5;

    // ✅ NEW: Movement direction tracking
    private int currentDirection = 0; // 0 = right/left, 1 = front (down), 2 = back (up)
    private boolean lastMovedVertically = false;

    // ✅ Explosion reaction
    private boolean reactingToExplosion = false;
    private long explosionReactionStartTime = 0;
    private final int explosionReactionFrames = 4;
    private final int explosionReactionRow = 8;
    private final long explosionFrameDuration = 120_000_000;


    // ✅ Health & Ammo
    public int hp = 10;
    public int maxHp = 10;
    public int ammo = 30; // Start with some ammo


    public Player(double x, double y, double width, double height, double speed, Game gp) {
        super(x, y, width, height, speed, gp);
        imageSet(totalFrames_walk, "/image/main_character.png");
        bullets = new ArrayList<>();
    }

    public void update(Set<KeyCode> keys, Level_1 level1, Game game, long now, long deltaTime) {
        // Update bullets first
        updateBullets(deltaTime, now);

        if (isDead) {
            currentRow = deadRow;
            int frameIndex = (int) ((now - deathStartTime) / deathFrameDuration);
            if (frameIndex < deadFrame) {
                currentFrame = frameIndex;
            } else {
                currentFrame = deadFrame - 1;
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
            // ✅ FIXED: Use directional hit animations with proper frame counts
            int maxFrames;
            if (currentDirection == 1) {
                currentRow = FrontHitRow;
                maxFrames = FrontHitFrame;
            } else if (currentDirection == 2) {
                currentRow = BackHitRow;
                maxFrames = BackHitFrame;
            } else {
                currentRow = HitRow;
                maxFrames = HitFrame;
            }

            int frameIndex = (int) ((now - meleeHitStartTime) / meleeHitFrameDuration);
            if (frameIndex < maxFrames) {
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

        // Handle shooting animation
        if (shooting) {
            // ✅ FIXED: Use directional shooting animations with proper frame counts
            int maxFrames;
            if (currentDirection == 1) {
                currentRow = FrontShootRow;
                maxFrames = FrontShootFrame;
            } else if (currentDirection == 2) {
                currentRow = BackShootRow;
                maxFrames = BackShootFrame;
            } else {
                currentRow = ShootRow;
                maxFrames = ShootFrame;
            }

            int frameIndex = (int) ((now - shootStartTime) / shootFrameDuration);
            if (frameIndex < maxFrames) {
                currentFrame = frameIndex;
            } else {
                shooting = false;
                currentFrame = 0;
                currentRow = 0;
            }
            return; // Don't process other animations while shooting
        }

        if (attackingWithFist) {
            // ✅ FIXED: Use directional fist animations with proper frame counts
            int maxFrames;
            if (currentDirection == 1) {
                currentRow = FrontFistRow;
                maxFrames = FrontFistFrame;
            } else if (currentDirection == 2) {
                currentRow = BackFistRow;
                maxFrames = BackFistFrame;
            } else {
                currentRow = FistRow;
                maxFrames = FistFrame;
            }

            int frameIndex = (int) ((now - fistAttackStartTime) / fistFrameDuration);
            if (frameIndex < maxFrames) {
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

        // ✅ FIXED: Handle shooting input with proper direction tracking
        if (keys.contains(KeyCode.F) && !shooting && !attackingWithFist) {
            // Update shooting direction based on current movement or last direction
            updateShootingDirection(keys);
            shoot(now);
        }

        // Handle fist attack
        if (keys.contains(KeyCode.SPACE) && !attackingWithFist && !shooting) {
            attackingWithFist = true;
            fistAttackStartTime = now;
            currentFrame = 0;

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

        // ✅ FIXED: Movement logic with proper direction tracking
        boolean moved = false;
        double newX = x;
        double newY = y;

        if (game.GameState == game.playState) {
            // ✅ Track movement direction for animations
            if (keys.contains(KeyCode.W)) {
                double testY = y - speed;
                if (!level1.isCollisionRect(x, testY, width, height)
                        && !checkObjectCollisionsAndInteract(x, testY, width, height, game)
                        && !checkNpcCollision(x, testY, game, now)
                        && !checkSoldierCollision(x, testY, game)) {
                    newY = testY;
                    moved = true;
                    currentDirection = 2; // Back direction
                    lastDirectionY = -1.0; // Moving up
                    lastDirectionX = 0.0;
                    lastMovedVertically = true;
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
                    currentDirection = 1; // Front direction
                    lastDirectionY = 1.0; // Moving down
                    lastDirectionX = 0.0;
                    lastMovedVertically = true;
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
                    currentDirection = 0; // Side direction
                    lastDirectionX = -1.0; // Moving left
                    lastDirectionY = 0.0;
                    lastMovedVertically = false;
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
                    currentDirection = 0; // Side direction
                    lastDirectionX = 1.0; // Moving right
                    lastDirectionY = 0.0;
                    lastMovedVertically = false;
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

        // ✅ FIXED: Movement animation with proper directional frames
        if (moved && !reactingToExplosion && !attackingWithFist && !isDead && !reactingToMeleeHit && !shooting) {
            // Use directional walking animations with proper frame counts
            int maxFrames;
            if (currentDirection == 1) { // Front (S key)
                currentRow = GunFrontWalkRow; // Using gun walk frames
                maxFrames = GunFrontWalkFrame;
            } else if (currentDirection == 2) { // Back (W key)
                currentRow = GunBackWalkRow; // Using gun walk frames
                maxFrames = GunBackWalkFrame;
            } else { // Side (A/D keys)
                currentRow = GunWalkRow; // Using gun walk frames
                maxFrames = GunWalkFrame;
            }

            animationTimer += deltaTime;
            if (animationTimer > 100_000_000) {
                nextFrame(maxFrames);
                animationTimer = 0;
            }
        } else if (!reactingToExplosion && !attackingWithFist && !isDead && !reactingToMeleeHit && !shooting) {
            // ✅ FIXED: Idle animation with gun and proper direction
            currentFrame = 0;
            if (currentDirection == 1) { // Front idle
                currentRow = GunFrontIdleRow;
            } else if (currentDirection == 2) { // Back idle
                currentRow = GunBackIdleRow;
            } else { // Side idle
                currentRow = GunIdleRow;
            }
        }
    }

    // ✅ NEW: Update shooting direction based on current movement
    private void updateShootingDirection(Set<KeyCode> keys) {
        // Check current movement keys for shooting direction
        if (keys.contains(KeyCode.W)) {
            lastDirectionX = 0.0;
            lastDirectionY = -1.0; // Shoot up
            currentDirection = 2;
        } else if (keys.contains(KeyCode.S)) {
            lastDirectionX = 0.0;
            lastDirectionY = 1.0; // Shoot down
            currentDirection = 1;
        } else if (keys.contains(KeyCode.A)) {
            lastDirectionX = -1.0; // Shoot left
            lastDirectionY = 0.0;
            currentDirection = 0;
        } else if (keys.contains(KeyCode.D)) {
            lastDirectionX = 1.0; // Shoot right
            lastDirectionY = 0.0;
            currentDirection = 0;
        }
        // If no movement keys are pressed, keep the last direction
    }

    private void shoot(long now) {
        if ((now - lastShotTime) < shootCooldown) {
            return;
        }

        if (ammo <= 0) {
            gp.ui.showMessage("No ammo!");
            return;
        }

        // Start from center of player
        double bulletStartX = x + width / 2;
        double bulletStartY = y + height / 2;
        System.out.println("Start x: "+bulletStartX+", Start Y: "+bulletStartY);
        System.out.println("Last x: "+lastDirectionX+", Last Y: "+lastDirectionY);
        System.out.println("Facing right: "+facingRight+", Current direction: "+currentDirection);

        // Adjust starting position based on shooting direction
        if (currentDirection == 0) { // Horizontal shooting (left/right)
            bulletStartX += lastDirectionX * (width / 2 );
            bulletStartY -= 18;
            System.out.println("Horizontal - After Start x: "+bulletStartX+", After Start Y: "+bulletStartY);
        } else if (currentDirection == 1) { // Front/Down shooting
            bulletStartY += (height / 2 ); // Move to bottom edge
            // For vertical shooting, use the facing direction for X offset
            if (facingRight) {
                bulletStartX -= (width/2 ); // Shoot from right side when facing right
            } else {
                bulletStartX += (width/2
                ); // Shoot from left side when facing left
            }
            System.out.println("Down - After Start x: "+bulletStartX+", After Start Y: "+bulletStartY);
        } else if (currentDirection == 2) { // Back/Up shooting
            bulletStartY -= (height / 2 ); // Move to top edge
            // For vertical shooting, use the facing direction for X offset
            if (facingRight) {
                bulletStartX -= (width/2 ); // Shoot from right side when facing right
            } else {
                bulletStartX += (width/2 ); // Shoot from left side when facing left
            }
            System.out.println("Up - After Start x: "+bulletStartX+", After Start Y: "+bulletStartY);
        }
        System.out.println("Done\n");

        double velX = lastDirectionX * bulletSpeed;
        double velY = lastDirectionY * bulletSpeed;

        Bullet bullet = new Bullet(bulletStartX, bulletStartY, velX, velY, gp);
        bullets.add(bullet);

        shooting = true;
        shootStartTime = now;
        lastShotTime = now;
        ammo--;

        gp.playSoundEffects(4);

        currentFrame = 0;
    }
    // ✅ NEW: Update bullets
    private void updateBullets(long deltaTime, long now) {
        Iterator<Bullet> bulletIterator = bullets.iterator();
        while (bulletIterator.hasNext()) {
            Bullet bullet = bulletIterator.next();
            bullet.update(deltaTime, now);

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

        for (Scout scout : gp.scout) {
            if (scout != null && bulletRect.intersects(scout.getHitbox())) {
                scout.takeDamage();
                bullet.markForRemoval();
                gp.ui.showMessage("Scout hit!");
                return;
            }
        }

        for (Enemy enemy : gp.enemies) {
            if (enemy != null && !enemy.isDead() && bulletRect.intersects(enemy.getHitbox())) {
                enemy.receiveDamage();
                bullet.markForRemoval();
                gp.ui.showMessage("Enemy hit!");
                return;
            }
        }

        for (Soldier soldier : gp.soldiers) {
            if (soldier != null && !soldier.isDead() && bulletRect.intersects(soldier.getHitbox())) {
                soldier.receiveDamage();
                bullet.markForRemoval();
                gp.ui.showMessage("Soldier hit!");
                return;
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