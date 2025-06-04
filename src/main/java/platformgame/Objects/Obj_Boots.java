package platformgame.Objects;

import javafx.scene.image.Image;

public class Obj_Boots extends SuperObject{
    public Obj_Boots(){
        name = "Boots";

        try{
            image = new Image(getClass().getResourceAsStream("/image/Object/boots.png"));


        } catch (Exception e) {
            throw new RuntimeException(e);
        }
collision=false;
    }
}
