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


import platformgame.Entity.Npc;
import platformgame.Entity.Player;
import platformgame.Objects.SuperObject;

import java.util.HashSet;
import java.util.Set;

public class Game extends Pane {
    private final Canvas canvas;
    private final GraphicsContext gc;
    private final Set<KeyCode> keysPressed = new HashSet<>();

    private long lastTime = System.nanoTime(); //  OPTIMIZED: for deltaTime
    //Game state for resume and pause
    public int GameState;
    public final int playState=1;
    public final int pauseState=2;


    //for Asset placement(borshon)
    public AssetSetter aSetter = new AssetSetter(this);
    //for npc store
    public Npc[] npc= new Npc[10];

    //for object store
    public SuperObject object[]= new SuperObject[10];


    public final double scale = 1.15; // tweak this between 1.1 to 1.5 for desired effect


    //for player and tilemap
    public final Player player;
    public final TileMap tileMap;
    public int tileSize = 64;
    public final double screenWidth = 1020;
    public final double screenHeight = 700;

    //For music and sound
    Sound music = Sound.getInstance();
    Sound sound = Sound.getInstance();



    //Objects
    public int hasKey=0;


    //For UI elements and check messages
    public UI ui = new UI(this);

    public Game() {
        this.setPrefSize(screenWidth, screenHeight);
        canvas = new Canvas(screenWidth, screenHeight);
        gc = canvas.getGraphicsContext2D();

        this.getChildren().add(canvas);

         // increased from 64
        tileMap = new TileMap(100, 100, tileSize); // total map size now 3200x3200 pixels

        // Start player in the center of the map
        double startX = (tileMap.getWidthInPixels() - tileSize) / 2;
        double startY = (tileMap.getHeightInPixels() - tileSize) / 2;

        player = new Player(startX, startY, 50, 40, 3, this);

        setUpObject();
        setFocusTraversable(true);
        setOnKeyPressed(this::onKeyPressed);
        setOnKeyReleased(this::onKeyReleased);
    }

        //adding this for set up object before game start (Borshon)
    public void setUpObject() {
    aSetter.setObject();
    aSetter.setNpc();
    playMusic(0);
    GameState= playState;
    }






    private void onKeyPressed(KeyEvent e) {
        keysPressed.add(e.getCode());
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
            player.update(keysPressed, tileMap, this, now, deltaTime);
            //for Npc update
            for (Npc n : npc) {
                if (n != null) {
                    n.update(deltaTime, now);
                }
            }



            if (keysPressed.contains(KeyCode.ESCAPE)) {
                //  Save state and return to menu
                GameManager.getInstance().saveState(this);
                openMainMenu();
                GameState = pauseState;
            }
        }

    }



    //For opening menu
    private void openMainMenu() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FirstPage.fxml"));
            Pane menuRoot = loader.load();
            Scene currentScene = this.getScene();
            currentScene.setRoot(menuRoot); //  Use same scene
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    private void draw() {
        double camX = player.getX() + player.getWidth() / 2 - screenWidth / 2;
        double camY = player.getY() + player.getHeight() / 2 - screenHeight / 2;

        camX = Math.max(0, Math.min(camX, tileMap.getWidthInPixels() - screenWidth));
        camY = Math.max(0, Math.min(camY, tileMap.getHeightInPixels() - screenHeight));

        gc.setFill(Color.DARKGRAY);
        gc.fillRect(0, 0, screenWidth, screenHeight);


        //Debug
        long drawStart=0;
        drawStart= System.nanoTime();
        //for tile draw
        tileMap.draw(gc, camX, camY, scale);
        // draw all objects that exist(Borshon)
        for (SuperObject obj : object) {
            if (obj != null) {
                obj.draw(gc, this);
            }
        }
        //for main player
        player.draw(gc, camX, camY, scale);

        for (Npc npc : npc) {
            if (npc != null) {
                npc.draw(gc,camX,camY,scale);
            }
        }

        // For Ui and messages
        ui.draw(gc);


        long drawEnd = System.nanoTime();
        long passed = (drawEnd - drawStart);
//        System.out.println("Daw time: "+passed);
    }

    public void playMusic(int i){

    music.loop(i);
    }

    public void stopMusic(int i){
        music.stop(i);
    }

    public void playSoundEffects(int i){
        sound.play(i);
    }

}
