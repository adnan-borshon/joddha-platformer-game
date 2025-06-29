package platformgame.Objects;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import platformgame.Game;
import platformgame.ImageLoader;

import java.util.Objects;

public class Obj_ammo extends SuperObject {

    public Obj_ammo() {
        name = "Ammo";
        // Instead of directly using new Image(), use ImageLoader:
        image = ImageLoader.load("/image/Object/Ammo.png");
        collision = false; // Ammo doesn't block player
    }


    @Override
    public void draw(GraphicsContext gc, Game game) {
        if (image != null) {
            double screenX = (worldX - game.camX) * game.scale;
            double screenY = (worldY - game.camY) * game.scale;
            double size = 40 * game.scale; // ✅ Slightly bigger than default size
            gc.drawImage(image, screenX, screenY, size, size);
        }
    }
}
