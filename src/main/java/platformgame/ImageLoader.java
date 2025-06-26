package platformgame;

import javafx.scene.image.Image;

public class ImageLoader {

    public static Image load(String path) {
        try {
            return new Image(ImageLoader.class.getResourceAsStream(path));
        } catch (Exception e) {
            System.err.println("Failed to load image: " + path + " - " + e.getMessage());
            return null;
        }
    }
}