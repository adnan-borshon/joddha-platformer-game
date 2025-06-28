package platformgame.Objects;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import platformgame.Game;
import platformgame.ImageLoader;

import java.util.Objects;

public class Obj_LeftGate extends SuperObject {

    public Obj_LeftGate() {
        name = "left_gate";
        // Instead of directly using new Image(), use ImageLoader:
        image = ImageLoader.load("/image/Gates & Bridges/Left_HostageWall.png");
        collision = true; // ✅ Enable collision
    }

//    @Override
//    public void draw(GraphicsContext gc, Game game) {
//        if (image != null) {
//            double screenX = (worldX - game.camX) * game.scale;
//            double screenY = (worldY - game.camY) * game.scale;
//            double size = 50 * game.scale;
//            gc.drawImage(image, screenX, screenY, size, size);
//        }
//    }
}
