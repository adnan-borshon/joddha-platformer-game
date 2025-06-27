package platformgame;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import platformgame.Tanks.Main_Tank;
import platformgame.Event.EventHandler;
import platformgame.Map.Level_2;
import platformgame.Map.Level_2_controller;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;

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
    private Main_Tank mainTank;
    Sound music = Sound.getInstance();
    Sound sound = Sound.getInstance();

    public final EventHandler eventHandler = new EventHandler();

    public double camX = 0;
    public double camY = 0;

    // Mouse tracking for turret control
    private double mouseX = 0;
    private double mouseY = 0;

    public Game_2() {
        this.setPrefSize(screenWidth, screenHeight);
        canvas = new Canvas(screenWidth, screenHeight);
        gc = canvas.getGraphicsContext2D();
        this.getChildren().add(canvas);

        // Initialize the main tank with better positioning
        double tankStartX = 15 * tileSize;
        double tankStartY = 35 * tileSize;
        mainTank = new Main_Tank(tankStartX, tankStartY, 128, 128, 200.0, null, this);


        loadLevel();

        setFocusTraversable(true);
        setOnKeyPressed(this::onKeyPressed);
        setOnKeyReleased(this::onKeyReleased);
        setOnMouseMoved(this::onMouseMoved);
        setOnMouseClicked(this::onMouseClicked);

        // Request focus immediately
        requestFocus();
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

        if (level2 != null) {
            level2.drawForeground(gc, camX, camY, 1.15);
        }

        // Draw crosshair at mouse position
        drawCrosshair();

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

    private void drawCrosshair() {
        gc.setStroke(Color.RED);
        gc.setLineWidth(2);
        double size = 15;
        gc.strokeLine(mouseX - size, mouseY, mouseX + size, mouseY);
        gc.strokeLine(mouseX, mouseY - size, mouseX, mouseY + size);
    }

    private void update(long now, long deltaTime) {
        if (GameState == playState) {
            if (mainTank != null) {
                // Calculate world mouse position (accounting for camera)
                double worldMouseX = mouseX + camX;
                double worldMouseY = mouseY + camY;

                mainTank.update(keysPressed, level2, this, now, deltaTime, worldMouseX, worldMouseY);

                // Update camera to follow tank
                updateCamera();
            }

            if (eventHandler != null) {
                eventHandler.update(mainTank, this, now);
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

    private void onMouseMoved(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
    }

    private void onMouseClicked(MouseEvent e) {
        if (mainTank != null) {
            mainTank.shoot();
        }
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