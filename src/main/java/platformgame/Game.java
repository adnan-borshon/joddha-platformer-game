package platformgame;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import platformgame.Entity.Enemy;
import platformgame.Entity.Npc;
import platformgame.Entity.Player;
import platformgame.Entity.Scout;
import platformgame.Event.EventHandler;
import platformgame.Map.Level_1;
import platformgame.Map.Level_1_controller;
import platformgame.Objects.SuperObject;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;

public class Game extends Pane {
    private final Canvas canvas;
    private final GraphicsContext gc;
    private final Set<KeyCode> keysPressed = new HashSet<>();

    private long lastTime = System.nanoTime(); //  OPTIMIZED: for deltaTime
    // Game state for resume and pause
    public int GameState;
    public final int playState = 1;
    public final int pauseState = 2;
    public final int dialogueState = 3;

    // for Asset placement (Borshon)
    public AssetSetter aSetter = new AssetSetter(this);
    // for npc store
    public Npc[] npc = new Npc[10];
    //For scout
    public Scout[] scout = new Scout[10];
    // for object store
    public SuperObject object[] = new SuperObject[10];

    //for soldier
    public Enemy[] enemy = new Enemy[40];

    public final double scale = 1.15; // tweak this between 1.1 to 1.5 for desired effect

    // For player and Level_1 class
    public final Player player;
    public Level_1 level1; // Replaced TileMap with Level_1 class
    public int tileSize = 32; // FIXED: Changed from 64 to 32 to match Level_1
    public final double screenWidth = 1020;
    public final double screenHeight = 700;

    // For music and sound
    Sound music = Sound.getInstance();
    Sound sound = Sound.getInstance();

    // Objects
    public int hasKey = 0;

    // For UI elements and check messages
    public UI ui = new UI(this);
//FOr event handeling
public final EventHandler eventHandler = new EventHandler();



    // Camera position - make these accessible to other classes
    public double camX = 0;
    public double camY = 0;

    public Game() {
        this.setPrefSize(screenWidth, screenHeight);
        canvas = new Canvas(screenWidth, screenHeight);
        gc = canvas.getGraphicsContext2D();

        this.getChildren().add(canvas);

        // Load Level_1 FIRST before using it
        loadLevel();

        // Now initialize player using actual map dimensions
        if (level1 != null) {
            double startX = 27*tileSize;  // Use correct tileSize
            double startY = 5*tileSize;
            player = new Player(startX, startY, 50, 40, 3, this);
        } else {
            // Fallback if level1 is null
            player = new Player(500, 350, 50, 40, 3, this);
        }

        setUpObject();
        setFocusTraversable(true);
        setOnKeyPressed(this::onKeyPressed);
        setOnKeyReleased(this::onKeyReleased);
    }

    private void loadLevel() {
        try {

            URL url = getClass().getResource("/Level_1/Level_1.fxml");
            if(url == null){
                System.err.println("FXML resource not found!");
                return;
            } else {
                System.out.println("FXML resource found: " + url.toString());
            }

            FXMLLoader loader = new FXMLLoader(url);
            Pane root = loader.load();
            Level_1_controller controller = loader.getController();

            // Extract logic map
            level1 = controller.getLevelLogic();

            if (level1 != null) {
                System.out.println("Level1 loaded successfully. Map size: " +
                        level1.mapWidth + "x" + level1.mapHeight);
            } else {
                System.err.println("Failed to load Level1 logic");
            }

        } catch (Exception e) {
            System.err.println("Error loading level:");
            e.printStackTrace();
        }
    }

    private void draw() {
        // Clear canvas first
        gc.setFill(Color.BLUE);
        gc.fillRect(0, 0, screenWidth, screenHeight);

        if (level1 == null) {
            System.err.println("Level1 is null, cannot draw map");
            return;
        }

        // Calculate camera position and store it for other classes to use
        camX = player.getX() + player.getWidth() / 2 - screenWidth / 2;
        camY = player.getY() + player.getHeight() / 2 - screenHeight / 2;

        // Use correct tile size for camera bounds
        double mapPixelWidth = level1.mapWidth * tileSize;
        double mapPixelHeight = level1.mapHeight * tileSize;

        camX = Math.max(0, Math.min(camX, mapPixelWidth - screenWidth));
        camY = Math.max(0, Math.min(camY, mapPixelHeight - screenHeight));

        // FIXED: LAYERED RENDERING ORDER:

        // 1. Draw background layers (ground, floor, etc.)
        level1.drawBackground(gc, camX, camY, scale);

        // 2. Draw middle layers (walls, etc.) including "Trees collision"
        level1.drawMiddleground(gc, camX, camY, scale);

        // 3. Draw objects behind player
        for (SuperObject obj : object) {
            if (obj != null && obj.isBehindPlayer(this)) {
                obj.draw(gc, this);
            }
        }
        // Draw the enemies in front of the player or behind the player
        for (Enemy enemyEntity : enemy) {
            if (enemyEntity != null) {
                enemyEntity.draw(gc, camX, camY, scale);
            }
        }
        // 4. Draw NPCs behind player
        for (Npc npcEntity : npc) {
            if (npcEntity != null && npcEntity.isBehindPlayer(this)) {
                npcEntity.draw(gc, camX, camY, scale);
            }
        }

        // 5. Draw Scouts behind player
        for (Scout scoutEntity : scout) {
            if (scoutEntity != null && scoutEntity.isBehindPlayer(this)) {
                scoutEntity.draw(gc, camX, camY, scale);
            }
        }

        // 6. Draw player
        player.draw(gc, camX, camY, scale);


//        Draw Scouts in front of player
        for (Scout scoutEntity : scout) {
            if (scoutEntity != null && !scoutEntity.isBehindPlayer(this)) {
                scoutEntity.draw(gc, camX, camY, scale);
            }
        }
        // . Draw NPCs in front of player
        for (Npc npcEntity : npc) {
            if (npcEntity != null && !npcEntity.isBehindPlayer(this)) {
                npcEntity.draw(gc, camX, camY, scale);
            }
        }

        //enemy in front of player
        for (Enemy enemyEntity : enemy) {
            if (enemyEntity != null && !enemyEntity.isBehindPlayer(this)) {
                enemyEntity.draw(gc, camX, camY, scale);
            }
        }

        // 7. Draw objects in front of player
        for (SuperObject obj : object) {
            if (obj != null && !obj.isBehindPlayer(this)) {
                obj.draw(gc, this);
            }
        }
        eventHandler.draw(gc, camX, camY, scale);


        // 8. Draw foreground layers (tree tops, rooftops, etc.)
        level1.drawForeground(gc, camX, camY, scale);

        // 9. Draw UI (always on top)
        ui.draw(gc);


    }


    // Set up objects before game start (Borshon)
    public void setUpObject() {
        aSetter.setObject();
        aSetter.setNpc();
        aSetter.setScout();
        aSetter.setEnemy();
        aSetter.setExplosion();
        playMusic(0);
        GameState = playState;

    }

    private void onKeyPressed(KeyEvent e) {
        KeyCode key = e.getCode();
        keysPressed.add(key);

        // Dialogue logic
        if (GameState == dialogueState && key == KeyCode.ENTER) {
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

            // For Npc update
            for (Npc n : npc) {
                if (n != null) {
                    n.update(deltaTime, now);
                }
            }
            // For Scout update
            for (Scout scoutEntity : scout) {
                if (scoutEntity != null) {
                    scoutEntity.update(deltaTime, now);  // Update Scout NPCs
                }
            }


            //Enemy update
            // Update enemy behavior (movement, attack, etc.)
            for (Enemy enemyEntity : enemy) {
                if (enemyEntity != null) {
                    enemyEntity.update(deltaTime, now);  // Update enemy state
                }
            }


            if (keysPressed.contains(KeyCode.ESCAPE)) {
                // Save state and return to menu
                GameManager.getInstance().saveState(this);
                openMainMenu();
                GameState = pauseState;
            }
            //for event
            eventHandler.update(player, this, now);

        }
    }

    // For opening menu
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