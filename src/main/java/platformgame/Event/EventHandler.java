package platformgame.Event;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

import javafx.geometry.Rectangle2D;
import platformgame.Tanks.Main_Tank;
import platformgame.Game;
import platformgame.Game_2;
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

    private boolean bridgeDestructionEnabled = false;
    private boolean playerInTriggerArea = false;
    private boolean showBridgePopup = false;
    private boolean bridgeDestroyed = false;
    private Rectangle2D bridgeTriggerArea;
    private Rectangle2D bridgeExplosionArea;
    private long bridgeExplosionStartTime = 0;
    private boolean bridgeExplosionActive = false;

    // ✅ Mission completion delay
    private boolean missionCompleted = false;
    private boolean waitingForMissionComplete = false;
    private long missionCompleteDelayStart = 0;
    private final long MISSION_COMPLETE_DELAY = 4_000_000_000L; // 4 seconds in nanoseconds

    public EventHandler() {
        explosionSpriteSheet = ImageLoader.load("/image/explosion.png");
        initializeBridgeAreas();
    }

    private void initializeBridgeAreas() {
        double triggerX = 91 * 32;
        double triggerY = 42 * 32;
        double triggerWidth = 32;
        double triggerHeight = 4 * 32;
        bridgeTriggerArea = new Rectangle2D(triggerX, triggerY, triggerWidth, triggerHeight);

        double explosionX = 102 * 32;
        double explosionY = 42 * 32;
        double explosionWidth = 32*7;
        double explosionHeight = 32*7;
        bridgeExplosionArea = new Rectangle2D(explosionX, explosionY, explosionWidth, explosionHeight);
    }


    public void addMine(double x, double y, double width, double height, double scaleFactor) {
        EventRect mine = new EventRect(x, y, width, height);
        mine.setScaleFactor(scaleFactor);
        mines.add(mine);
    }

    public void enableBridgeDestruction() {
        bridgeDestructionEnabled = true;

    }

    public void update(Player player, Game game, long now) {
        Rectangle2D playerRect = new Rectangle2D(player.getX(), player.getY(), player.getWidth(), player.getHeight());

        for (EventRect mine : mines) {
            if (!mine.isTriggered() && playerRect.intersects(mine.getBounds())) {
                mine.setTriggered(true);

                double centerX = mine.getX() + mine.getWidth() / 2.0;
                double centerY = mine.getY() + mine.getHeight() / 2.0;

                activeExplosions.add(new Explosion(centerX, centerY, now, mine.getScaleFactor()));
                game.playSoundEffects(4);
                player.triggerExplosionReaction(now);
                player.takeDamage(0.10, System.nanoTime());
                game.ui.showMessage("Stepped on a mine! -10% HP");
            }
        }

        if (bridgeDestructionEnabled && !bridgeDestroyed) {
            handleBridgeDestruction(player, game, now);
        }

        Iterator<Explosion> iterator = activeExplosions.iterator();
        while (iterator.hasNext()) {
            Explosion explosion = iterator.next();
            explosion.update(now);
            if (explosion.finished()) {
                iterator.remove();
            }
        }

        if (bridgeExplosionActive) {
            updateBridgeExplosion(now);
        }

        // ✅ Handle mission completion delay (short delay after explosions end)
        if (waitingForMissionComplete && !missionCompleted) {
            if (now - missionCompleteDelayStart >= 1_000_000_000L) { // 1 second delay after explosions
                completeMission(now, game);
                waitingForMissionComplete = false;
            }
        }
    }

    private void handleBridgeDestruction(Player player, Game game, long now) {
        Rectangle2D playerRect = new Rectangle2D(player.getX(), player.getY(), player.getWidth(), player.getHeight());

        boolean currentlyInArea = playerRect.intersects(bridgeTriggerArea);

        if (currentlyInArea && !playerInTriggerArea) {
            playerInTriggerArea = true;
            showBridgePopup = true;
            // ✅ Set the popup message in UI dialogue system
            game.ui.dialogue = "Press Enter to destroy the bridge and save villagers from pakistan military";
            game.GameState = game.dialogueState;

        } else if (!currentlyInArea && playerInTriggerArea) {
            playerInTriggerArea = false;
            showBridgePopup = false;
            if (game.GameState == game.dialogueState) {
                game.GameState = game.playState;
            }
        }
    }

    public void triggerBridgeExplosion(Game game, long now) {
        if (showBridgePopup && !bridgeDestroyed) {
            bridgeDestroyed = true;
            bridgeExplosionActive = true;
            bridgeExplosionStartTime = now;
            showBridgePopup = false;
            game.GameState = game.playState;

            // ✅ Create multiple explosions for better visibility
            double explosionCenterX = bridgeExplosionArea.getMinX();
            double explosionCenterY = bridgeExplosionArea.getMinY() ;

            // Main explosion at center
            activeExplosions.add(new Explosion(explosionCenterX, explosionCenterY, now, 3.0));

// Additional explosions for better effect
// Generate explosions along the x-axis with increment of 16
            for (int i = -180; i <= -60; i += 16) {
                activeExplosions.add(new Explosion(explosionCenterX + i, explosionCenterY, now + 200_000_000L, 2.0));
                activeExplosions.add(new Explosion(explosionCenterX + i, explosionCenterY + 30, now + 300_000_000L, 2.0));
                activeExplosions.add(new Explosion(explosionCenterX + i, explosionCenterY - 30, now + 400_000_000L, 2.0));
            }

// Additional explosions at specific points
            activeExplosions.add(new Explosion(explosionCenterX - 120, explosionCenterY - 30, now + 500_000_000L, 2.0));
            activeExplosions.add(new Explosion(explosionCenterX - 142, explosionCenterY, now + 500_000_000L, 2.0));

// Explosions at both the edges of the bridge
            activeExplosions.add(new Explosion(explosionCenterX - 180, explosionCenterY, now + 500_000_000L, 2.0));
            activeExplosions.add(new Explosion(explosionCenterX - 180, explosionCenterY + 60, now + 500_000_000L, 2.0));
            activeExplosions.add(new Explosion(explosionCenterX - 180, explosionCenterY - 60, now + 500_000_000L, 2.0));

            game.playSoundEffects(4);
            game.ui.showMessage("Bridge destroyed!");


        }
    }

    private void updateBridgeExplosion(long now) {
        // ✅ Check if all explosions are finished, then show mission complete
        if (bridgeExplosionActive && activeExplosions.isEmpty()) {
            bridgeExplosionActive = false;

            // ✅ Start mission completion after explosions end
            if (!waitingForMissionComplete && !missionCompleted) {
                waitingForMissionComplete = true;
                missionCompleteDelayStart = now;
            }
        }
    }

    private void completeMission(long now, Game game) {
        if (!missionCompleted) {
            missionCompleted = true;

            // ✅ Use dialogue box system
            game.ui.dialogue = "🎉 MISSION COMPLETE! 🎉\n\nYou saved the villagers!\n\nThe bridge has been destroyed successfully.\nThe enemy can no longer reach the village!\n\nThank you for playing!";
            game.GameState = game.dialogueState;

        }
    }

    public void draw(GraphicsContext gc, double camX, double camY, double scale) {
        // ✅ Draw explosions with debug info
        for (Explosion explosion : activeExplosions) {
            int frame = explosion.getCurrentFrame();
            double explosionScale = scale * explosion.getScaleFactor();
            double drawW = frameWidth * explosionScale;
            double drawH = frameHeight * explosionScale;

            double drawX = (explosion.getX() - camX) * scale - drawW / 2.0;
            double drawY = (explosion.getY() - camY) * scale - drawH / 2.0;

            // ✅ Debug: Check if explosion is in view
            if (drawX > -drawW && drawX < 1020 && drawY > -drawH && drawY < 700) {
                System.out.println("Drawing explosion at screen pos: " + drawX + ", " + drawY + " frame: " + frame);
            }

            gc.drawImage(
                    explosionSpriteSheet,
                    frame * frameWidth, 0, frameWidth, frameHeight,
                    drawX, drawY, drawW, drawH
            );
        }

        // ✅ Remove the custom popup drawing since we're using the UI dialogue system
    }

    public boolean isShowingBridgePopup() {
        return showBridgePopup;
    }

    public boolean isBridgeDestroyed() {
        return bridgeDestroyed;
    }

    public boolean isMissionCompleted() {
        return missionCompleted;
    }

    public void update(Main_Tank mainTank, Game_2 game2, long now) {
        // Empty implementation for Game_2
    }

    private static class Explosion {
        private final double x, y;
        private final long startTime;
        private final double scaleFactor;
        private int currentFrame = 0;
        private final long frameDuration = 150_000_000; // ✅ Slower animation (150ms per frame)
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