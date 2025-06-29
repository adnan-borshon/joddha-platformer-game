package platformgame.Objects;

import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import platformgame.Game;

public class Obj_Boom extends SuperObject {
    private boolean collected = false;
    private boolean appear = true; // assume it should appear when placed

    public Obj_Boom(int worldX, int worldY) {
        this.name = "Boom";
        this.image = new Image(getClass().getResourceAsStream("/image/Object/Granade.png"));
        this.worldX = worldX;
        this.worldY = worldY;
        this.collision = true;
    }

    public boolean shouldAppear() {
        return appear;
    }

    public boolean isCollected() {
        return collected;
    }

    public void collect() {
        collected = true;
    }

    @Override
    public void draw(GraphicsContext gc, Game gp) {
        if (!collected) {
            super.draw(gc, gp);
        }
    }

    @Override
    public Rectangle2D getBoundingBox() {
        return new Rectangle2D(worldX, worldY, 32, 32);
    }
}
