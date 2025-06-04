package platformgame.Objects;

import javafx.scene.image.Image;
import platformgame.ImageLoader;

public class Obj_Boots extends SuperObject{
    public Obj_Boots(){
        name = "Boots";

        try{
            image = ImageLoader.load("/image/Object/boots.png");



        } catch (Exception e) {
            throw new RuntimeException(e);
        }
collision=false;
    }
}
