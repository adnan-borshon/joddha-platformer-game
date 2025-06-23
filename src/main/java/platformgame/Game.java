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
import platformgame.Entity.Npc;
import platformgame.Entity.Player;
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

    // for object store
    public SuperObject object[] = new SuperObject[10];

    public final double scale = 1.15; // tweak this between 1.1 to 1.5 for desired effect

    // For player and Level_1 class
    public final Player player;
    public Level_1 level1; // Replaced TileMap with Level_1 class
    public int tileSize = 64;
    public final double screenWidth = 1020;
    public final double screenHeight = 700;

    // For music and sound
    Sound music = Sound.getInstance();
    Sound sound = Sound.getInstance();

    // Objects
    public int hasKey = 0;

    // For UI elements and check messages
    public UI ui = new UI(this);

    // Key fixes for the Game class - replace these sections in your Game.java

    // 1. Fix the constructor initialization order
    public Game() {
        this.setPrefSize(screenWidth, screenHeight);
        canvas = new Canvas(screenWidth, screenHeight);
        gc = canvas.getGraphicsContext2D();

        this.getChildren().add(canvas);

        // Load Level_1 FIRST before using it
        loadLevel();

        // Now initialize player using actual map dimensions
        if (level1 != null) {
            double startX = (level1.mapWidth * 32) / 2;  // Use 32 (tile size) not 64
            double startY = (level1.mapHeight * 32) / 2;
            player = new Player(startX, startY, 50, 40, 3, this);
            System.out.println("Player initialized at: " + startX + ", " + startY);
        } else {
            // Fallback if level1 is null
            player = new Player(500, 350, 50, 40, 3, this);
            System.err.println("Level1 is null, using default player position");
        }

        setUpObject();
        setFocusTraversable(true);
        setOnKeyPressed(this::onKeyPressed);
        setOnKeyReleased(this::onKeyReleased);
    }

    // 2. Fix the loadLevel method
    private void loadLevel() {
        try {
            System.out.println("Loading level...");

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

            // Don't add the root to children - we'll draw directly to our canvas

        } catch (Exception e) {
            System.err.println("Error loading level:");
            e.printStackTrace();
        }
    }

    // 3. Fix the draw method camera calculations
    // Replace your draw() method in Game.java with this:

    private void draw() {
        // Clear canvas first
        gc.setFill(Color.BLUE);
        gc.fillRect(0, 0, screenWidth, screenHeight);

        if (level1 == null) {
            System.err.println("Level1 is null, cannot draw map");
            return;
        }

        // Calculate camera position
        double camX = player.getX() + player.getWidth() / 2 - screenWidth / 2;
        double camY = player.getY() + player.getHeight() / 2 - screenHeight / 2;

        // Use correct tile size (32, not 64) for camera bounds
        double mapPixelWidth = level1.mapWidth * 32;
        double mapPixelHeight = level1.mapHeight * 32;

        camX = Math.max(0, Math.min(camX, mapPixelWidth - screenWidth));
        camY = Math.max(0, Math.min(camY, mapPixelHeight - screenHeight));

        // LAYERED RENDERING ORDER:

        // 1. Draw background layers (ground, floor, etc.)
        level1.drawBackground(gc, camX, camY, scale);

        // 2. Draw middle layers (walls, etc.)
        level1.drawMiddleground(gc, camX, camY, scale);

        // 3. Draw objects that should be behind player
        for (SuperObject obj : object) {
            if (obj != null) { // You'll need to add this method
                obj.draw(gc, this);
            }
        }

        // 4. Draw NPCs (if they should be at same level as player)
        for (Npc npc : npc) {
            if (npc != null) {
                npc.draw(gc, camX, camY, scale);
            }
        }

        // 5. Draw the player
        player.draw(gc, camX, camY, scale);

        // 6. Draw objects that should be in front of player
        for (SuperObject obj : object) {
            if (obj != null ) {
                obj.draw(gc, this);
            }
        }

        // 7. Draw foreground layers (tree tops, rooftops, etc.)
        level1.drawForeground(gc, camX, camY, scale);

        // 8. Draw UI (always on top)
        ui.draw(gc);
    }

    // Set up objects before game start (Borshon)
    public void setUpObject() {
        aSetter.setObject();
        aSetter.setNpc();
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
            player.update(keysPressed, level1, this, now, deltaTime); // Update using Level_1
            // For Npc update
            for (Npc n : npc) {
                if (n != null) {
                    n.update(deltaTime, now);
                }
            }

            if (keysPressed.contains(KeyCode.ESCAPE)) {
                // Save state and return to menu
                GameManager.getInstance().saveState(this);
                openMainMenu();
                GameState = pauseState;
            }
        }
    }

    // For opening menu
    private void openMainMenu() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FirstPage.fxml"));
            Pane menuRoot = loader.load();
            Scene currentScene = this.getScene();
            currentScene.setRoot(menuRoot); // Use same scene
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
