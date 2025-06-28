package platformgame;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import platformgame.Tanks.Enemy_Tank;
import platformgame.Tanks.Main_Tank;
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

    public Level_2 level2;
    public int tileSize = 32;
    public final double screenWidth = 1020;
    public final double screenHeight = 700;
    public Main_Tank mainTank;
    Sound music = Sound.getInstance();
    Sound sound = Sound.getInstance();

    // FIXED: Use ArrayList instead of array for better management
    public ArrayList<Enemy_Tank> enemyTanks = new ArrayList<>();

    // Keep the array for backward compatibility but don't use it for collision
    Enemy_Tank[] enemyTank = new Enemy_Tank[10];

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
        // Clear canvas
        gc.setFill(Color.DARKGREEN);
        gc.fillRect(0, 0, screenWidth, screenHeight);

        if (level2 == null) {
            // Draw without level for testing
            gc.setFill(Color.RED);
            gc.fillText("Level2 not loaded", 10, 20);
        } else {
            level2.drawBackground(gc, camX, camY, 1.15);
            level2.drawMiddleground(gc, camX, camY, 1.15);
        }

        // Always draw tank (even if level2 is null)
        if (mainTank != null) {
            mainTank.draw(gc, camX, camY, 1);
        } else {
            System.err.println("mainTank is null!");
        }

        // UPDATED: Draw enemy tanks using both methods for compatibility
        // Draw from array (backward compatibility)
        for (int i = 0; i < enemyTank.length; i++) {
            if (enemyTank[i] != null) {
                enemyTank[i].draw(gc, camX, camY, 1);
            }
        }

        // Draw from ArrayList (new method)
        for (Enemy_Tank enemy : enemyTanks) {
            if (enemy != null && enemy.isAlive()) {
                enemy.draw(gc, camX, camY, 1);
            }
        }

        // Draw all bullets
        for (Tank_Bullet bullet : bullets) {
            if (bullet != null) {
                bullet.draw(gc, camX, camY, 1);
            }
        }

        if (level2 != null) {
            level2.drawForeground(gc, camX, camY, 1.15);
        }

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
        if (GameState == playState) {
            if (mainTank != null) {
                mainTank.update(keysPressed, level2, this, now, deltaTime, 0, 0);
                updateCamera();
            }

            // Convert deltaTime from nanoseconds to seconds for enemy tanks
            double deltaSeconds = deltaTime / 1_000_000_000.0;

            // UPDATED: Update all enemy tanks from both sources
            // Update from array
            for (Enemy_Tank enemy : enemyTank) {
                if (enemy != null && enemy.isAlive()) {
                    enemy.updateBehavior(level2, this, deltaSeconds);
                }
            }

            // Update from ArrayList
            for (Enemy_Tank enemy : enemyTanks) {
                if (enemy != null && enemy.isAlive()) {
                    enemy.updateBehavior(level2, this, deltaSeconds);
                }
            }

            updateBullets(deltaTime);

            if (eventHandler != null) {
                eventHandler.update(mainTank, this, now);
            }
        }
    }

    private void updateBullets(long deltaTime) {
        Iterator<Tank_Bullet> iterator = bullets.iterator();
        while (iterator.hasNext()) {
            Tank_Bullet bullet = iterator.next();
            bullet.update(deltaTime);
            bullet.checkCollisionWithEnemies();

            if (bullet.shouldRemove()) {
                iterator.remove();
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

        // Clamp camera to level boundaries if level2 exists
        if (level2 != null) {
            camX = Math.max(0, Math.min(camX, level2.mapWidth * tileSize - screenWidth));
            camY = Math.max(0, Math.min(camY, level2.mapHeight * tileSize - screenHeight));
        }
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
}