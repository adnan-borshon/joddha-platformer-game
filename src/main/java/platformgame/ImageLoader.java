package platformgame;

import javafx.scene.image.Image;
import java.util.HashMap;
import java.util.Map;

public class ImageLoader {

    private static final Map<String, Image> cache = new HashMap<>();

    public static Image load(String path) {
        if (cache.containsKey(path)) {
            return cache.get(path); // ✅ Return cached image
        }

        Image image = new Image(ImageLoader.class.getResourceAsStream(path));
        cache.put(path, image);     // ✅ Store in cache
        return image;
    }
}
