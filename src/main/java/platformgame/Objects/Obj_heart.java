package platformgame.Objects;

import javafx.scene.image.Image;
import platformgame.ImageLoader;

import java.util.Objects;

public class Obj_heart extends SuperObject{
    public Image heartIcon;


    public Obj_heart() {

        name="heart";

        heartIcon = ImageLoader.load("/image/Object/Health-Icon.png");
        collision = false;
    }

}
