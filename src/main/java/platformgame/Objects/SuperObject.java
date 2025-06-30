package platformgame.Objects;

import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import platformgame.Game;
import platformgame.Game_2;

public class SuperObject {
    public Image image;
    public String name;
    public boolean collision = false;
    public int worldX, worldY;
    public double scale = 2.5;  // scaling for objects
    public double screenX, screenY;

    public void draw(GraphicsContext gc, Game gp) {
        if (image == null) {
            System.err.println("Image is null for object: " + name);
            return;
        }

        // Use the camera position calculated in Game.draw()
        double camX = gp.camX;
        double camY = gp.camY;

        // Calculate object's position on screen
        screenX = (worldX - camX) * gp.scale;
        screenY = (worldY - camY) * gp.scale;

        // Only draw if object is on screen (optimization)
        double objWidth = image.getWidth() * scale * gp.scale;
        double objHeight = image.getHeight() * scale * gp.scale;

        if (screenX + objWidth > 0 && screenX < gp.screenWidth &&
                screenY + objHeight > 0 && screenY < gp.screenHeight) {

            gc.drawImage(image, screenX, screenY, objWidth, objHeight);

            // Debug: Draw bounding box
            gc.save();
            gc.restore();


        }
    }
    public void draw(GraphicsContext gc, Game_2 gp2) {
        if (image == null) {
            System.err.println("Image is null for object: " + name);
            return;
        }

        // Use the camera position calculated in Game.draw()
        double camX = gp2.camX;
        double camY = gp2.camY;

        // Calculate object's position on screen
        screenX = (worldX - camX) * gp2.scale;
        screenY = (worldY - camY) * gp2.scale;

        // Only draw if object is on screen (optimization)
        double objWidth = image.getWidth()  * gp2.scale;
        double objHeight = image.getHeight()  * gp2.scale;

        if (screenX + objWidth > 0 && screenX < gp2.screenWidth &&
                screenY + objHeight > 0 && screenY < gp2.screenHeight) {

            gc.drawImage(image, screenX, screenY, objWidth, objHeight);

            // Debug: Draw bounding box
            gc.save();
            gc.restore();


        }
    }

    public Rectangle2D getBoundingBox() {
        double objWidth = image.getWidth() * scale;
        double objHeight = image.getHeight() * scale;
        return new Rectangle2D(worldX, worldY, objWidth, objHeight);
    }

    // Check if object should be drawn behind player based on Y position
    public boolean isBehindPlayer(Game gp) {
        // Objects with lower Y values (higher on screen) are behind the player
        // Add some buffer to prevent flickering
        return this.worldY + (image != null ? image.getHeight() * scale : 0) < gp.player.getY() + gp.player.getHeight();
    }
}