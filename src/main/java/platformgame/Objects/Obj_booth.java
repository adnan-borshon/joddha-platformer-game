package platformgame.Objects;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import platformgame.Game;

import java.util.Objects;

public class Obj_booth extends SuperObject {

    public Obj_booth() {
        name = "Booth";
        image = new Image(Objects.requireNonNull(getClass().getResourceAsStream(
                "/image/Object/Telephone-stand.png"
        )));
        collision = true; // ✅ Enable collision
    }

    @Override
    public void draw(GraphicsContext gc, Game game) {
        if (image != null) {
            double screenX = (worldX - game.camX) * game.scale;
            double screenY = (worldY - game.camY) * game.scale;
            double size = 80 * game.scale;
            gc.drawImage(image, screenX, screenY, size, size);
        }
    }
}
