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
import platformgame.Entity.*;
import platformgame.Event.EventHandler;
import platformgame.Map.Level_1;
import platformgame.Map.Level_1_controller;
import platformgame.Objects.Obj_Boom;
import platformgame.Objects.SuperObject;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;

public class Game extends Pane {
    private final Canvas canvas;
    private final GraphicsContext gc;
    private final Set<KeyCode> keysPressed = new HashSet<>();
    public final int gameOverState = 4;

    private long lastTime = System.nanoTime();
    public int GameState;
    public final int playState = 1;
    public final int pauseState = 2;
    public final int dialogueState = 3;

    public AssetSetter aSetter = new AssetSetter(this);
    public Npc[] npc = new Npc[10];
    public Scout[] scout = new Scout[10];
    public SuperObject[] object = new SuperObject[15];
    public Enemy[] enemies = new Enemy[40];
    public Soldier[] soldiers = new Soldier[10];


    public final double scale = 1.15;
    public final Player player;
    public Level_1 level1;
    public int tileSize = 32;
    public final double screenWidth = 1020;
    public final double screenHeight = 700;

    Sound music = Sound.getInstance();
    Sound sound = Sound.getInstance();

    public int hasKey = 0;
    public UI ui = new UI(this);
    public final EventHandler eventHandler = new EventHandler();

    public double camX = 0;
    public double camY = 0;

    // ✅ Boom spawn logic
    private boolean boomSpawned = false;
    private Obj_Boom boomObject;

    public Game() {
        this.setPrefSize(screenWidth, screenHeight);
        canvas = new Canvas(screenWidth, screenHeight);
        gc = canvas.getGraphicsContext2D();
        this.getChildren().add(canvas);

        loadLevel();

        if (level1 != null) {
            double startX = 27 * tileSize;
            double startY = 5 * tileSize;
            player = new Player(startX, startY, 40, 40, 3, this);
        } else {
            player = new Player(500, 350, 40, 40, 3, this);
        }

        setUpObject();
        setFocusTraversable(true);
        setOnKeyPressed(this::onKeyPressed);
        setOnKeyReleased(this::onKeyReleased);
    }

    private void loadLevel() {
        try {
            URL url = getClass().getResource("/Level_1/Level_1.fxml");
            if (url == null) {
                System.err.println("FXML resource not found!");
                return;
            }

            FXMLLoader loader = new FXMLLoader(url);
            Pane root = loader.load();
            Level_1_controller controller = loader.getController();
            level1 = controller.getLevelLogic();

            if (level1 != null) {
                System.out.println("Level1 loaded: " + level1.mapWidth + "x" + level1.mapHeight);
            } else {
                System.err.println("Level1 logic load failed");
            }

        } catch (Exception e) {
            System.err.println("Error loading level:");
            e.printStackTrace();
        }
    }

    private void draw() {
        gc.setFill(Color.BLUE);
        gc.fillRect(0, 0, screenWidth, screenHeight);

        if (level1 == null) {
            System.err.println("Level1 is null, cannot draw map");
            return;
        }

        camX = player.getX() + player.getWidth() / 2 - screenWidth / 2;
        camY = player.getY() + player.getHeight() / 2 - screenHeight / 2;

        double mapPixelWidth = level1.mapWidth * tileSize;
        double mapPixelHeight = level1.mapHeight * tileSize;

        camX = Math.max(0, Math.min(camX, mapPixelWidth - screenWidth));
        camY = Math.max(0, Math.min(camY, mapPixelHeight - screenHeight));

        level1.drawBackground(gc, camX, camY, scale);
        level1.drawMiddleground(gc, camX, camY, scale);

        for (SuperObject obj : object) {
            if (obj != null && obj.isBehindPlayer(this)) {
                obj.draw(gc, this);
            }
        }

        for (Enemy enemyEntity : enemies) {
            if (enemyEntity != null && enemyEntity.isBehindPlayer(this)) {
                enemyEntity.draw(gc, camX, camY, scale);
            }
        }

        for (Npc npcEntity : npc) {
            if (npcEntity != null && npcEntity.isBehindPlayer(this)) {
                npcEntity.draw(gc, camX, camY, scale);
            }
        }

        for (Scout scoutEntity : scout) {
            if (scoutEntity != null && scoutEntity.isBehindPlayer(this)) {
                scoutEntity.draw(gc, camX, camY, scale);
            }
        }

        player.draw(gc, camX, camY, scale);

        for (Scout scoutEntity : scout) {
            if (scoutEntity != null && !scoutEntity.isBehindPlayer(this)) {
                scoutEntity.draw(gc, camX, camY, scale);
            }
        }

        for (Npc npcEntity : npc) {
            if (npcEntity != null && !npcEntity.isBehindPlayer(this)) {
                npcEntity.draw(gc, camX, camY, scale);
            }
        }

        for (Enemy enemyEntity : enemies) {
            if (enemyEntity != null && !enemyEntity.isBehindPlayer(this)) {
                enemyEntity.draw(gc, camX, camY, scale);
            }
        }
        // Draw soldiers (make sure to draw them in the right order for depth)
        for (Soldier soldier : soldiers) {
            if (soldier != null) {
                soldier.draw(gc, camX, camY, scale);
            }
        }

        for (SuperObject obj : object) {
            if (obj != null && !obj.isBehindPlayer(this)) {
                obj.draw(gc, this);
            }
        }

        eventHandler.draw(gc, camX, camY, scale);
        level1.drawForeground(gc, camX, camY, scale);
        ui.draw(gc);

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

    public void setUpObject() {
        aSetter.setObject();
        aSetter.setNpc();
        aSetter.setScout();
        aSetter.setEnemy();
        aSetter.setSoldiers();
        aSetter.setExplosion();
        playMusic(0);
        GameState = playState;
    }

    private void onKeyPressed(KeyEvent e) {
        KeyCode key = e.getCode();
        keysPressed.add(key);

        if (GameState == gameOverState && key == KeyCode.ENTER) {
            openMainMenu();
        }

        // ✅ Handle bridge destruction popup
        if (GameState == dialogueState && key == KeyCode.ENTER) {
            // Check if we're showing bridge popup
            if (eventHandler.isShowingBridgePopup()) {
                eventHandler.triggerBridgeExplosion(this, System.nanoTime());
                return; // Don't process NPC dialogue
            }

            // Handle NPC dialogue (existing code)
            for (Npc n : npc) {
                if (n != null && n.playerIsTouching) {
                    n.speak();
                    break;
                }
            }
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

    private void update(long now, long deltaTime) {
        if (GameState == playState) {
            player.update(keysPressed, level1, this, now, deltaTime);

            for (Npc n : npc) {
                if (n != null) {
                    n.update(deltaTime, now);
                }
            }

            for (Scout scoutEntity : scout) {
                if (scoutEntity != null) {
                    scoutEntity.update(deltaTime, now);
                }
            }

            for (Enemy enemyEntity : enemies) {
                if (enemyEntity != null) {
                    enemyEntity.update(deltaTime, now);
                }
            }
            for (Soldier soldier : soldiers) {
                if (soldier != null) {
                    soldier.update(deltaTime, now);
                }
            }

            checkAndSpawnBoom();  // ✅ Call boom spawn check

            if (keysPressed.contains(KeyCode.ESCAPE)) {
                GameManager.getInstance().saveState(this);
                openMainMenu();
                GameState = pauseState;
            }


            eventHandler.update(player, this, now);
        }
    }

    // ✅ Check if all enemies and scouts are dead → spawn boom
    private void checkAndSpawnBoom() {
        if (boomSpawned) return;

        boolean allEnemiesDead = true;
        boolean foundAnyEnemies = false;
        boolean foundAnyScouts = false;

        // Check enemies
        for (Enemy e : enemies) {
            if (e != null) {
                foundAnyEnemies = true;
                if (!e.isDead()) {
                    allEnemiesDead = false;
                    break;
                }
            }
        }
        // Check scouts
        for (Scout s : scout) {
            if (s != null) {
                foundAnyScouts = true;
                if (!s.isDead()) {
                    allEnemiesDead = false;
                    break;
                }
            }
        }

        // Only spawn boom if we actually had enemies/scouts to kill AND they're all dead

        if (allEnemiesDead && (foundAnyEnemies || foundAnyScouts)) {
            boomObject = new Obj_Boom(54 * tileSize, 24 * tileSize);

            boolean placed = false;
            for (int i = 0; i < object.length; i++) {
                if (object[i] == null) {
                    object[i] = boomObject;
                    System.out.println("💥 Boom appeared at position: " + (54 * tileSize) + ", " + (24 * tileSize));
                    placed = true;
                    break;
                }
            }

            if (!placed) {
                System.out.println("⚠️ Could not place boom - object array is full!");
            }

            boomSpawned = true;

            // ✅ Enable bridge destruction when boom spawns
            eventHandler.enableBridgeDestruction();
        } else if (!foundAnyEnemies && !foundAnyScouts) {
            System.out.println("⚠️ No enemies or scouts found - spawning boom for testing");
            boomObject = new Obj_Boom(54 * tileSize, 24 * tileSize);

            for (int i = 0; i < object.length; i++) {
                if (object[i] == null) {
                    object[i] = boomObject;
                    System.out.println("💥 Boom appeared (no enemies/scouts mode)");
                    break;
                }
            }
            boomSpawned = true;

            // ✅ Enable bridge destruction for testing mode too
            eventHandler.enableBridgeDestruction();
        }
    }
    // ✅ Also add this method to help debug boom spawning
    public void debugBoomStatus() {
        System.out.println("=== BOOM DEBUG INFO ===");
        System.out.println("Boom spawned: " + boomSpawned);

        int aliveEnemies = 0;
        int totalEnemies = 0;
        for (Enemy e : enemies) {
            if (e != null) {
                totalEnemies++;
                if (!e.isDead()) {
                    aliveEnemies++;
                }
            }
        }

        int aliveScouts = 0;
        int totalScouts = 0;
        for (Scout s : scout) {
            if (s != null) {
                totalScouts++;
                if (!s.isDead()) {
                    aliveScouts++;
                }
            }
        }

        System.out.println("Enemies: " + aliveEnemies + "/" + totalEnemies + " alive");
        System.out.println("Scouts: " + aliveScouts + "/" + totalScouts + " alive");
        System.out.println("=======================");
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
}
