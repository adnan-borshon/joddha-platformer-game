package platformgame.Objects;


import platformgame.ImageLoader;

public class Obj_Key extends SuperObject {
    public Obj_Key(){
        name = "key";

        try{
            image = ImageLoader.load("/image/Object/key.png");



        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
