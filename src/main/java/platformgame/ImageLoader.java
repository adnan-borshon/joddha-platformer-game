package platformgame;

import javafx.scene.image.Image;
import java.util.HashMap;
import java.util.Map;

public class ImageLoader {

    // Cache to store already loaded images
    private static final Map<String, Image> imageCache = new HashMap<>();

    public static Image load(String path) {
        // Check if the image is already in the cache
        if (imageCache.containsKey(path)) {
            return imageCache.get(path);  // Return the cached image
        }

        try {
            // Load image from the resources
            Image image = new Image(ImageLoader.class.getResourceAsStream(path));

            // Add the image to the cache
            imageCache.put(path, image);

            return image;
        } catch (Exception e) {
            System.err.println("Failed to load image: " + path + " - " + e.getMessage());
            return null;
        }
    }

    // Optional: Clear the cache (useful for debugging or freeing memory if needed)
    public static void clearCache() {
        imageCache.clear();
    }
}
