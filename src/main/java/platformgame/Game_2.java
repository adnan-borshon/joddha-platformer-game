package platformgame;

import javafx.animation.AnimationTimer;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import platformgame.Tanks.*;
import platformgame.Event.EventHandler;
import platformgame.Map.Level_2;
import platformgame.Map.Level_2_controller;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.Iterator;

public class Game_2 extends Pane {
    private final Canvas canvas;
    private final GraphicsContext gc;
    private final Set<KeyCode> keysPressed = new HashSet<>();
    public final int gameOverState = 4;

    private long lastTime = System.nanoTime();
    public int GameState = 1; // Initialize to playState
    public final int playState = 1;
    public final int pauseState = 2;
    public final int dialogueState = 3;

    private Image healthIconImg;
    private Image healthLineImg;
    private Image healthBarImg;

    public Level_2 level2;
    public int tileSize = 32;
    public final double screenWidth = 1020;
    public final double screenHeight = 700;
    public Main_Tank mainTank;
    Sound music = Sound.getInstance();
    Sound sound = Sound.getInstance();

    // FIXED: Use ArrayList instead of array for better management
    public ArrayList<Enemy_Tank> enemyTanks = new ArrayList<>();
    public ArrayList<Tank2> Tanks = new ArrayList<>();

    // Keep the array for backward compatibility but don't use it for collision
    Enemy_Tank[] enemyTank = new Enemy_Tank[10];
    Tank2[] Tanks2 = new Tank2[10];

    // List to store all bullets
    private ArrayList<Tank_Bullet> bullets = new ArrayList<>();

    public final EventHandler eventHandler = new EventHandler();

    public double camX = 0;
    public double camY = 0;
    AssetSetter assetSetter = new AssetSetter(this);

    public Game_2() {
        this.setPrefSize(screenWidth, screenHeight);
        canvas = new Canvas(screenWidth, screenHeight);
        gc = canvas.getGraphicsContext2D();
        this.getChildren().add(canvas);

        // Initialize the main tank with better positioning
        double tankStartX = 15 * tileSize;
        double tankStartY = 35 * tileSize;
        mainTank = new Main_Tank(tankStartX, tankStartY, 128, 128, 200.0, null, this);

        // Initialize enemy tanks - REMOVED from constructor
        // initializeEnemyTanks(); // This was causing issues

        healthIconImg = new Image(getClass().getResourceAsStream("/image/Object/Health-Icon.png"));
        healthLineImg = new Image(getClass().getResourceAsStream("/image/Object/Health-line.png"));
        healthBarImg  = new Image(getClass().getResourceAsStream("/image/object/Health-Bar.png"));

        loadLevel();

        setFocusTraversable(true);
        setOnKeyPressed(this::onKeyPressed);
        setOnKeyReleased(this::onKeyReleased);

        setUpObject(); // This will properly set up the enemy tanks

        // Request focus immediately
        requestFocus();
    }

    // REMOVED: This method was creating dummy enemy tanks
    // private void initializeEnemyTanks() { ... }

    private void setUpObject() {
        assetSetter.setTank();
        assetSetter.setTank2();
    }

    // Method to return all enemy tanks - UPDATED
    public Enemy_Tank[] getEnemyTanks() {
        return enemyTank;  // Return the enemy tank array for backward compatibility
    }

    // NEW: Method to get enemy tanks as ArrayList
    public ArrayList<Enemy_Tank> getEnemyTanksList() {
        return enemyTanks;
    }

    // Method to add bullets to the game
    public void addBullet(Tank_Bullet bullet) {
        bullets.add(bullet);
        System.out.println("Bullet added! Total bullets: " + bullets.size()); // Debug output
    }

    private void loadLevel() {
        try {
            URL url = getClass().getResource("/Level_2/Level_2.fxml");
            if (url == null) {
                System.err.println("FXML resource not found!");
                return;
            }

            FXMLLoader loader = new FXMLLoader(url);
            Pane root = loader.load();
            Level_2_controller controller = loader.getController();
            level2 = controller.getLevelLogic();

            if (level2 != null) {
                System.out.println("level2 loaded: " + level2.mapWidth + "x" + level2.mapHeight);
            } else {
                System.err.println("level2 logic load failed");
            }

        } catch (Exception e) {
            System.err.println("Error loading level:");
            e.printStackTrace();
        }
    }

    private void draw() {
        // 1) Clear the world
        gc.setFill(Color.DARKGREEN);
        gc.fillRect(0, 0, screenWidth, screenHeight);

        // 2) Draw the tilemap layers
        if (level2 == null) {
            gc.setFill(Color.RED);
            gc.fillText("Level2 not loaded", 10, 20);
        } else {
            level2.drawBackground(gc, camX, camY, 1.15);
            level2.drawMiddleground(gc, camX, camY, 1.15);
        }

        // 3) Draw the main tank
        if (mainTank != null) {
            mainTank.draw(gc, camX, camY, 1);
        }

        // 4) Draw all enemy tanks
        for (int i = 0; i < enemyTank.length; i++) {
            if (enemyTank[i] != null) {
                enemyTank[i].draw(gc, camX, camY, 1);
            }
        }




        //array list
        for (Enemy_Tank enemy : enemyTanks) {
            if (enemy != null && enemy.isAlive()) {
                enemy.draw(gc, camX, camY, 1);
            }
        }




        for (int i = 0; i < Tanks2.length; i++) {
            if (Tanks2[i] != null) {
                Tanks2[i].draw(gc, camX, camY, 1);
            }
        }




        //array list
        for (Tank2 tank2 : Tanks) {
            if (tank2 != null && tank2.isAlive()) {
                tank2.draw(gc, camX, camY, 1);
            }
        }

        // 5) Draw all bullets
        for (Tank_Bullet bullet : bullets) {
            if (bullet != null) {
                bullet.draw(gc, camX, camY, 1);
            }
        }

        // 6) Draw the foreground
        if (level2 != null) {
            level2.drawForeground(gc, camX, camY, 1.15);
        }

        // ─── HEALTH BAR OVERLAY ────────────────────────────────────
        // Scale factors for health bar elements
        double iconScale = 0.3; // Make icon 40% of original size
        double barScale = 0.45;  // Keep bar at 60% of original size

        // fixed icon at (10,10)
        double iconX = 10, iconY = 10;
        double iconW = healthIconImg.getWidth() * iconScale;
        double iconH = healthIconImg.getHeight() * iconScale;

        // bar immediately right of icon, vertically centered with icon
        double barX = iconX + iconW + 2;
        double barW = healthBarImg.getWidth() * barScale;
        double barH = healthBarImg.getHeight() * barScale;
        double barY = iconY + (iconH - barH) / 2; // Center bar vertically with icon

        // percent full (0.0–1.0)
        double pct = (double) mainTank.getHealth() / mainTank.getMaxHealth();

        // width of the track (outline) image scaled
        double trackW = healthLineImg.getWidth() * barScale;
        double trackH = healthLineImg.getHeight() * barScale;

        // compute how many pixels of the line to draw
        double currW = trackW * pct;

        // a) draw the **bar** (fill) first, full width
        gc.drawImage(healthBarImg, barX, barY, barW, barH);

        // b) draw the **line** (track/outline) on top, clipped to currW
        gc.drawImage(
                healthLineImg,
                0, 0,                      // srcX, srcY
                currW / barScale, healthLineImg.getHeight(),  // srcW, srcH (adjust source width for scaling)
                barX, barY,                // destX, destY
                currW, trackH              // destW, destH
        );

        // c) draw the **icon** with scaling
        gc.drawImage(healthIconImg, iconX, iconY, iconW, iconH);

        // ─── GAME OVER SCREEN ──────────────────────────────────────
        if (GameState == gameOverState) {
            gc.setFill(Color.color(0, 0, 0, 0.6));
            gc.fillRect(0, 0, screenWidth, screenHeight);

            gc.setFill(Color.RED);
            gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 60));
            gc.fillText("GAME OVER", screenWidth / 2 - 200, screenHeight / 2 - 20);

            gc.setFill(Color.WHITE);
            gc.setFont(javafx.scene.text.Font.font("Arial", 24));
            gc.fillText("Press ENTER to Menu", screenWidth / 2 - 130, screenHeight / 2 + 40);
        }
    }


    public void update(long now, long deltaTime) {
        if (GameState != playState) return;

        // 1) move player
        mainTank.update(keysPressed, level2, this, now, deltaTime, 0, 0);
        updateCamera();

        // 2) move Enemy_Tank enemies
        double dtSec = deltaTime / 1_000_000_000.0;
        for (Enemy_Tank enemy : enemyTanks) {
            if (enemy != null && enemy.isAlive()) {
                enemy.updateBehavior(level2, this, dtSec);
            }
        }

        // FIXED: Also update Tank2 enemies
        for (Tank2 tank2 : Tanks) {
            if (tank2 != null && tank2.isAlive()) {
                tank2.updateBehavior(level2, this, dtSec);
            }
        }

        // FIXED: Also update array-based tanks for backward compatibility
        for (int i = 0; i < enemyTank.length; i++) {
            if (enemyTank[i] != null && enemyTank[i].isAlive()) {
                enemyTank[i].updateBehavior(level2, this, dtSec);
            }
        }

        for (int i = 0; i < Tanks2.length; i++) {
            if (Tanks2[i] != null && Tanks2[i].isAlive()) {
                Tanks2[i].updateBehavior(level2, this, dtSec);
            }
        }

        // ─── tank-to-tank collision ─────────────────────────────────
        Rectangle2D playerBox = mainTank.getBounds();

        // Check collision with Enemy_Tank ArrayList
        for (Enemy_Tank enemy : enemyTanks) {
            if (enemy != null && enemy.isAlive()) {
                Rectangle2D enemyBox = enemy.getBounds();
                if (playerBox.intersects(enemyBox)) {
                    // undo the player's last movement
                    Vector2D v = mainTank.getVelocity();
                    mainTank.setX(mainTank.getX() - v.x * dtSec);
                    mainTank.setY(mainTank.getY() - v.y * dtSec);
                    mainTank.onCollision();
                    break;
                }
            }
        }

        // FIXED: Check collision with Tank2 ArrayList
        for (Tank2 tank2 : Tanks) {
            if (tank2 != null && tank2.isAlive()) {
                Rectangle2D tank2Box = tank2.getBounds();
                if (playerBox.intersects(tank2Box)) {
                    // undo the player's last movement
                    Vector2D v = mainTank.getVelocity();
                    mainTank.setX(mainTank.getX() - v.x * dtSec);
                    mainTank.setY(mainTank.getY() - v.y * dtSec);
                    mainTank.onCollision();
                    break;
                }
            }
        }

        // 3) bullets, events, etc.
        updateBullets(deltaTime);
        if (eventHandler != null) eventHandler.update(mainTank, this, now);
    }

    private void updateBullets(long deltaTime) {
        Iterator<Tank_Bullet> iterator = bullets.iterator();
        while (iterator.hasNext()) {
            Tank_Bullet bullet = iterator.next();
            bullet.update(deltaTime);
            bullet.checkCollisionWithTanks(); // Use the fixed method

            if (bullet.shouldRemove()) {
                iterator.remove();
                System.out.println("Bullet removed. Remaining bullets: " + bullets.size());
            }
        }
    }

    private void updateCamera() {
        // Center camera on tank
        double targetCamX = mainTank.getTankX() - screenWidth / 2;
        double targetCamY = mainTank.getTankY() - screenHeight / 2;

        // Smooth camera movement
        camX += (targetCamX - camX) * 0.1;
        camY += (targetCamY - camY) * 0.1;

        // Clamp camera to level boundaries
        camX = Math.max(0, Math.min(camX, level2.mapWidth * tileSize - screenWidth));
        camY = Math.max(0, Math.min(camY, level2.mapHeight * tileSize - screenHeight));

        // ← Insert snapping here to eliminate sub‐pixel jitter:
        camX = Math.floor(camX);
        camY = Math.floor(camY);
    }



    private void onKeyPressed(KeyEvent e) {
        KeyCode key = e.getCode();
        keysPressed.add(key);

        if (GameState == gameOverState && key == KeyCode.ENTER) {
            openMainMenu();
        }

        // Shooting with SPACE
        if (key == KeyCode.SPACE && mainTank != null) {
            mainTank.shoot();
        }
    }

    private void onKeyReleased(KeyEvent e) {
        keysPressed.remove(e.getCode());
    }

    public void startGameLoop() {
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                long delta = now - lastTime;
                update(now, delta);
                draw();
                lastTime = now;
            }
        };
        timer.start();
    }

    private void openMainMenu() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FirstPage.fxml"));
            Pane menuRoot = loader.load();
            Scene currentScene = this.getScene();
            currentScene.setRoot(menuRoot);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }




    private boolean checkTankToTankCollision(Tank tank1, Tank tank2) {
        // Get tank centers
        double tank1CenterX = tank1.getX() + tank1.getTankWidth() / 2;
        double tank1CenterY = tank1.getY() + tank1.getTankHeight() / 2;
        double tank2CenterX = tank2.getX() + tank2.getTankWidth() / 2;
        double tank2CenterY = tank2.getY() + tank2.getTankHeight() / 2;

        // Calculate distance between centers
        double dx = tank1CenterX - tank2CenterX;
        double dy = tank1CenterY - tank2CenterY;
        double distance = Math.sqrt(dx * dx + dy * dy);

        // Define collision radius (adjust as needed)
        double collisionRadius = 80.0;

        return distance < collisionRadius;
    }

    public void playMusic(int i) {
        music.loop(i);
    }

    public void stopMusic(int i) {
        music.stop(i);
    }

    public void playSoundEffects(int i) {
        sound.play(i);
    }

    // Getters for screen dimensions (useful for tank boundary checks)
    public double getScreenWidth() { return screenWidth; }
    public double getScreenHeight() { return screenHeight; }

    private void checkVictoryCondition() {
        // Check if all enemy tanks and tank2 are dead
        boolean allEnemiesDefeated = true;

        for (Enemy_Tank enemy : enemyTanks) {
            if (enemy != null && enemy.isAlive()) {
                allEnemiesDefeated = false;
                break;
            }
        }

        for (Tank2 tank2 : Tanks) {
            if (tank2 != null && tank2.isAlive()) {
                allEnemiesDefeated = false;
                break;
            }
        }

        if (!allEnemiesDefeated) return;

        // Check if player is at 108,29 tile
        int playerTileX = (int)(mainTank.getX() / tileSize);
        int playerTileY = (int)(mainTank.getY() / tileSize);

        if (playerTileX == 108 && playerTileY == 29) {
            if (GameState != dialogueState) {
                GameState = dialogueState;

                mainTank.setDialogue(new String[]{
                        "Congratulations! You have saved the villagers!"
                });
                mainTank.startDialogue();
            }
        }
    }

}