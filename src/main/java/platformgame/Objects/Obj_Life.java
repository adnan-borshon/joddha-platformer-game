package platformgame.Objects;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import platformgame.Game;
import platformgame.ImageLoader;

import java.util.Objects;

public class Obj_Life extends SuperObject {

    public Obj_Life() {
        name = "life";
        // Instead of directly using new Image(), use ImageLoader:
        image = ImageLoader.load("/image/Object/Life.png");
        collision = false;
    }


    @Override
    public void draw(GraphicsContext gc, Game game) {
        if (image != null) {
            double screenX = (worldX - game.camX) * game.scale;
            double screenY = (worldY - game.camY) * game.scale;
            double size = 40 * game.scale; // slight enlargement
            gc.drawImage(image, screenX, screenY, size, size);
        }
    }
}
