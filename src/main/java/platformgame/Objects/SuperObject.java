package platformgame.Objects;

import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import platformgame.Game;

public class SuperObject {
    public Image image;
    public String name;
    public boolean collision = false;
    public  int worldX, worldY;
    public double scale = 2.5;  // scaling for objects

   public  double screenX, screenY;

    public void draw(GraphicsContext gc, Game gp) {
        if (image == null) return;

        // Calculate camera offset (same as Game.draw method)
        double camX = gp.player.getX() + gp.player.getWidth() / 2 - gp.screenWidth / 2;
        double camY = gp.player.getY() + gp.player.getHeight() / 2 - gp.screenHeight / 2;

        // Clamp camera within the bounds of the map
        camX = Math.max(0, Math.min(camX, gp.tileMap.getWidthInPixels() - gp.screenWidth));
        camY = Math.max(0, Math.min(camY, gp.tileMap.getHeightInPixels() - gp.screenHeight));

        // Calculate object's position on screen
         screenX = (worldX - camX) * gp.scale;
         screenY = (worldY - camY) * gp.scale;

        gc.drawImage(image, screenX, screenY, image.getWidth() * scale, image.getHeight() * scale);
    }

    public Rectangle2D getBoundingBox() {
        double objWidth = image.getWidth() * scale;
        double objHeight = image.getHeight() * scale;
        return new Rectangle2D(worldX, worldY, objWidth, objHeight);
    }


}
