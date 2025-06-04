package platformgame.Objects;


import platformgame.ImageLoader;

public class Obj_Door extends SuperObject{
    public Obj_Door(){
        name = "Door";

        try{
            image = ImageLoader.load("/image/Object/door.png");



        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        collision= true;
    }
}
