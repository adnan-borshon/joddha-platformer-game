package platformgame.Map;

import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;

public class Level_2_controller {

    @FXML
    private Pane mapPane;

    private Canvas canvas;
    private GraphicsContext gc;
    private Level_2 level;

    @FXML
    public void initialize() {

        level = new Level_2();

        // Use actual map dimensions from the TMX file
        double canvasWidth = level.mapWidth * 32;
        double canvasHeight = level.mapHeight * 32;


        canvas = new Canvas(canvasWidth, canvasHeight);
        gc = canvas.getGraphicsContext2D();

        mapPane.setPrefSize(canvasWidth, canvasHeight);
        mapPane.getChildren().add(canvas);

//        // Initial render to test if map loads
//        renderMap();
    }

//    private void renderMap() {
//        // Render the map once to test
//        level.draw(gc, 0, 0, 1.0);
//    }

    public Level_2 getLevelLogic() {
        return level;
    }

    public Canvas getCanvas() {
        return canvas;
    }

    public GraphicsContext getGraphicsContext() {
        return gc;
    }
}
