package platformgame;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

import java.util.HashSet;
import java.util.Set;

public class Game extends Pane {
    private final Canvas canvas;
    private final GraphicsContext gc;
    private final Set<KeyCode> keysPressed = new HashSet<>();

    private final Player player;
    private final TileMap tileMap;

    private final double screenWidth = 1020;
    private final double screenHeight = 800;

    public Game() {
        this.setPrefSize(screenWidth, screenHeight);
        canvas = new Canvas(screenWidth, screenHeight);
        gc = canvas.getGraphicsContext2D();

        this.getChildren().add(canvas);

        int tileSize = 64;  // increased from 16 to 32
        tileMap = new TileMap(100, 100, tileSize); // total map size now 3200x3200 pixels

        // Start player in the center of the map
        double startX = (tileMap.getWidthInPixels() - tileSize) / 2;
        double startY = (tileMap.getHeightInPixels() - tileSize) / 2;

        player = new Player(startX, startY, 32, 32, 3);

        setFocusTraversable(true);
        setOnKeyPressed(this::onKeyPressed);
        setOnKeyReleased(this::onKeyReleased);
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
                update();
                draw();
            }
        };
        timer.start();
    }

    private void update() {
        player.update(keysPressed, tileMap, System.nanoTime());
    }


    private final double scale = 1.05; // tweak this between 1.1 to 1.5 for desired effect

    private void draw() {
        double camX = player.getX() + player.getWidth() / 2 - screenWidth / 2;
        double camY = player.getY() + player.getHeight() / 2 - screenHeight / 2;

        camX = Math.max(0, Math.min(camX, tileMap.getWidthInPixels() - screenWidth));
        camY = Math.max(0, Math.min(camY, tileMap.getHeightInPixels() - screenHeight));

        gc.setFill(Color.DARKGRAY);
        gc.fillRect(0, 0, screenWidth, screenHeight);

        tileMap.draw(gc, camX, camY, scale);
        player.draw(gc, camX, camY, scale);
        

    }

}
