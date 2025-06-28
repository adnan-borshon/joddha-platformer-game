package platformgame.Objects;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import platformgame.Game;
import platformgame.ImageLoader;

import java.util.Objects;

public class Obj_RightGate extends SuperObject {

    public Obj_RightGate() {
        name = "right_gate";
        // Instead of directly using new Image(), use ImageLoader:
        image = ImageLoader.load("/image/Gates & Bridges/Right_HostageWall.png");
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
