package platformgame.Event;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.geometry.Rectangle2D;
import platformgame.Game;
import platformgame.ImageLoader;
import platformgame.Entity.Player;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class EventHandler {
    private final List<EventRect> mines = new ArrayList<>();
    private final List<Explosion> activeExplosions = new ArrayList<>();
    private final Image explosionSpriteSheet;

    private final int frameWidth = 32;
    private final int frameHeight = 32;
    private final int totalFrames = 6;

    public EventHandler() {
        explosionSpriteSheet = ImageLoader.load("/image/explosion.png");
    }

    public void addMine(double x, double y, double width, double height, double scaleFactor) {
        EventRect mine = new EventRect(x, y, width, height);
        mine.setScaleFactor(scaleFactor);
        mines.add(mine);
    }

    public void update(Player player, Game game, long now) {
        Rectangle2D playerRect = new Rectangle2D(player.getX(), player.getY(), player.getWidth(), player.getHeight());

        for (EventRect mine : mines) {
            if (!mine.isTriggered() && playerRect.intersects(mine.getBounds())) {
                mine.setTriggered(true);

                double centerX = mine.getX() + mine.getWidth() / 2.0;
                double centerY = mine.getY() + mine.getHeight() / 2.0;

                // ✅ Trigger explosion
                activeExplosions.add(new Explosion(centerX, centerY, now, mine.getScaleFactor()));
                game.playSoundEffects(4);
                player.triggerExplosionReaction(now);

                // ✅ Reduce player health by 10%
                player.takeDamage(0.10, System.nanoTime());

                game.ui.showMessage("Stepped on a mine! -10% HP");
            }
        }

        Iterator<Explosion> iterator = activeExplosions.iterator();
        while (iterator.hasNext()) {
            Explosion explosion = iterator.next();
            explosion.update(now);
            if (explosion.finished()) {
                iterator.remove();
            }
        }
    }

    public void draw(GraphicsContext gc, double camX, double camY, double scale) {
        for (Explosion explosion : activeExplosions) {
            int frame = explosion.getCurrentFrame();
            double explosionScale = scale * explosion.getScaleFactor();
            double drawW = frameWidth * explosionScale;
            double drawH = frameHeight * explosionScale;

            double drawX = (explosion.getX() - camX) * scale - drawW / 2.0;
            double drawY = (explosion.getY() - camY) * scale - drawH / 2.0;

            gc.drawImage(
                    explosionSpriteSheet,
                    frame * frameWidth, 0, frameWidth, frameHeight,
                    drawX, drawY, drawW, drawH
            );
        }
    }

    private static class Explosion {
        private final double x, y;
        private final long startTime;
        private final double scaleFactor;
        private int currentFrame = 0;
        private final long frameDuration = 100_000_000; // 100ms per frame
        private final int totalFrames = 6;

        public Explosion(double x, double y, long startTime, double scaleFactor) {
            this.x = x;
            this.y = y;
            this.startTime = startTime;
            this.scaleFactor = scaleFactor;
        }

        public void update(long now) {
            currentFrame = (int) ((now - startTime) / frameDuration);
        }

        public boolean finished() {
            return currentFrame >= totalFrames;
        }

        public int getCurrentFrame() {
            return Math.min(currentFrame, totalFrames - 1);
        }

        public double getX() { return x; }
        public double getY() { return y; }
        public double getScaleFactor() { return scaleFactor; }
    }
}
