package platformgame.Objects;

import javafx.scene.image.Image;

public class Obj_Key extends SuperObject {
    public Obj_Key(){
        name = "key";

        try{
            image = new Image(getClass().getResourceAsStream("/image/Object/key.png"));


        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
