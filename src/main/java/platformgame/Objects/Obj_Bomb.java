package platformgame.Objects;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import platformgame.Game;
import platformgame.ImageLoader;

import java.util.Objects;

public class Obj_Bomb extends SuperObject {

    public Obj_Bomb() {
        name = "bomb";
        // Instead of directly using new Image(), use ImageLoader:
        image = ImageLoader.load("/Popups/New/bomb.png");
        collision = true; // ✅ Enable collision
    }

}
