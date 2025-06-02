package platformgame.Objects;

import javafx.scene.image.Image;

public class Obj_Door extends SuperObject{
    public Obj_Door(){
        name = "Door";

        try{
            image = new Image(getClass().getResourceAsStream("/image/Object/door.png"));


        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        collision= true;
    }
}
