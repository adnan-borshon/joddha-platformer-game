package platformgame.Event;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
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

    // ✅ Bridge destruction variables
    private boolean bridgeDestructionEnabled = false;
    private boolean playerInTriggerArea = false;
    private boolean showBridgePopup = false;
    private boolean bridgeDestroyed = false;
    private Rectangle2D bridgeTriggerArea;
    private Rectangle2D bridgeExplosionArea;
    private long bridgeExplosionStartTime = 0;
    private boolean bridgeExplosionActive = false;

    // ✅ Mission completion variables
    private boolean missionCompleted = false;
    private boolean showMissionCompletePopup = false;
    private long missionCompleteStartTime = 0;
    private final long missionPopupDuration = 5_000_000_000L; // 5 seconds

    public EventHandler() {
        explosionSpriteSheet = ImageLoader.load("/image/explosion.png");
        initializeBridgeAreas();
    }

    private void initializeBridgeAreas() {
        // Trigger area: tiles 91*42 to 91*45 (4 tiles vertically)
        double triggerX = 91 * 32; // 32 is tileSize
        double triggerY = 42 * 32;
        double triggerWidth = 32; // 1 tile wide
        double triggerHeight = 4 * 32; // 4 tiles tall
        bridgeTriggerArea = new Rectangle2D(triggerX, triggerY, triggerWidth, triggerHeight);

        // Explosion area: tile 102*42
        double explosionX = 102 * 32;
        double explosionY = 42 * 32;
        double explosionWidth = 32;
        double explosionHeight = 32;
        bridgeExplosionArea = new Rectangle2D(explosionX, explosionY, explosionWidth, explosionHeight);
    }

    public void addMine(double x, double y, double width, double height, double scaleFactor) {
        EventRect mine = new EventRect(x, y, width, height);
        mine.setScaleFactor(scaleFactor);
        mines.add(mine);
    }

    // ✅ Call this method when boom is collected to enable bridge destruction
    public void enableBridgeDestruction() {
        bridgeDestructionEnabled = true;
        System.out.println("Bridge destruction enabled!");
    }

    public void update(Player player, Game game, long now) {
        // Handle mine explosions (existing code)
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

        // ✅ Handle bridge destruction sequence
        if (bridgeDestructionEnabled && !bridgeDestroyed) {
            handleBridgeDestruction(player, game, now);
        }

        // ✅ Handle mission completion popup timing
        if (showMissionCompletePopup) {
            if (now - missionCompleteStartTime > missionPopupDuration) {
                showMissionCompletePopup = false;
                // Optionally return to main menu or next level
                // game.returnToMainMenu();
            }
        }

        // Update active explosions
        Iterator<Explosion> iterator = activeExplosions.iterator();
        while (iterator.hasNext()) {
            Explosion explosion = iterator.next();
            explosion.update(now);
            if (explosion.finished()) {
                iterator.remove();
            }
        }

        // ✅ Update bridge explosion if active
        if (bridgeExplosionActive) {
            updateBridgeExplosion(now, game);
        }
    }

    private void handleBridgeDestruction(Player player, Game game, long now) {
        Rectangle2D playerRect = new Rectangle2D(player.getX(), player.getY(), player.getWidth(), player.getHeight());

        // Check if player is in trigger area
        boolean currentlyInArea = playerRect.intersects(bridgeTriggerArea);

        if (currentlyInArea && !playerInTriggerArea) {
            // Player just entered the trigger area
            playerInTriggerArea = true;
            showBridgePopup = true;
            game.GameState = game.dialogueState; // Pause the game
            System.out.println("Player entered bridge trigger area!");
        } else if (!currentlyInArea && playerInTriggerArea) {
            // Player left the trigger area
            playerInTriggerArea = false;
            showBridgePopup = false;
            if (game.GameState == game.dialogueState) {
                game.GameState = game.playState; // Resume game
            }
        }
    }

    // ✅ Call this method when ENTER is pressed while popup is showing
    public void triggerBridgeExplosion(Game game, long now) {
        if (showBridgePopup && !bridgeDestroyed) {
            bridgeDestroyed = true;
            bridgeExplosionActive = true;
            bridgeExplosionStartTime = now;
            showBridgePopup = false;
            game.GameState = game.playState; // Resume game

            // Create explosion at bridge location
            double explosionCenterX = bridgeExplosionArea.getMinX() + bridgeExplosionArea.getWidth() / 2;
            double explosionCenterY = bridgeExplosionArea.getMinY() + bridgeExplosionArea.getHeight() / 2;
            activeExplosions.add(new Explosion(explosionCenterX, explosionCenterY, now, 2.0)); // Bigger explosion

            game.playSoundEffects(4);
            game.ui.showMessage("Bridge destroyed!");

            // ✅ Remove bridge tiles from the map
            if (game.level1 != null) {
                game.level1.removeBridgeTiles(102, 42);
                System.out.println("Bridge tiles removed from map!");
            }

            // ✅ Trigger mission completion after bridge destruction
            completeMission(now);

            System.out.println("Bridge explosion triggered!");
        }
    }

    private void updateBridgeExplosion(long now, Game game) {
        // Bridge explosion lasts for 2 seconds
        if (now - bridgeExplosionStartTime > 2_000_000_000L) {
            bridgeExplosionActive = false;
            System.out.println("Bridge explosion finished!");
        }
    }

    // ✅ New method to trigger mission completion
    private void completeMission(long now) {
        if (!missionCompleted) {
            missionCompleted = true;
            showMissionCompletePopup = true;
            missionCompleteStartTime = now;
            System.out.println("🎉 MISSION COMPLETED! 🎉");
        }
    }

    public void draw(GraphicsContext gc, double camX, double camY, double scale) {
        // Draw mine explosions
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

        // ✅ Draw bridge destruction popup
        if (showBridgePopup) {
            drawBridgePopup(gc);
        }

        // ✅ Draw mission completion popup
        if (showMissionCompletePopup) {
            drawMissionCompletePopup(gc);
        }

        // ✅ Debug: Draw trigger area (remove this in final version)
        if (bridgeDestructionEnabled && !bridgeDestroyed) {
            drawDebugTriggerArea(gc, camX, camY, scale);
        }
    }

    private void drawBridgePopup(GraphicsContext gc) {
        // Semi-transparent overlay
        gc.setFill(Color.color(0, 0, 0, 0.7));
        gc.fillRect(0, 0, 1020, 700); // Use your screen dimensions

        // Popup box
        double boxWidth = 400;
        double boxHeight = 150;
        double boxX = (1020 - boxWidth) / 2;
        double boxY = (700 - boxHeight) / 2;

        gc.setFill(Color.DARKGRAY);
        gc.fillRoundRect(boxX, boxY, boxWidth, boxHeight, 10, 10);

        gc.setStroke(Color.WHITE);
        gc.setLineWidth(3);
        gc.strokeRoundRect(boxX, boxY, boxWidth, boxHeight, 10, 10);

        // Text
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        gc.fillText("Destroy the bridge with the boom?", boxX + 50, boxY + 60);

        gc.setFont(Font.font("Arial", 16));
        gc.fillText("Press ENTER to confirm", boxX + 120, boxY + 100);
    }

    // ✅ New method to draw mission completion popup
    private void drawMissionCompletePopup(GraphicsContext gc) {
        // Semi-transparent overlay with golden tint
        gc.setFill(Color.color(1.0, 0.84, 0.0, 0.3)); // Golden overlay
        gc.fillRect(0, 0, 1020, 700);

        // Large celebration box
        double boxWidth = 600;
        double boxHeight = 300;
        double boxX = (1020 - boxWidth) / 2;
        double boxY = (700 - boxHeight) / 2;

        // Outer glow effect
        gc.setFill(Color.color(1.0, 0.84, 0.0, 0.5));
        gc.fillRoundRect(boxX - 10, boxY - 10, boxWidth + 20, boxHeight + 20, 20, 20);

        // Main box
        gc.setFill(Color.color(0.1, 0.1, 0.1, 0.9));
        gc.fillRoundRect(boxX, boxY, boxWidth, boxHeight, 15, 15);

        // Golden border
        gc.setStroke(Color.GOLD);
        gc.setLineWidth(5);
        gc.strokeRoundRect(boxX, boxY, boxWidth, boxHeight, 15, 15);

        // Mission Complete title
        gc.setFill(Color.GOLD);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 48));
        String title = "MISSION COMPLETE!";
        double titleWidth = title.length() * 28; // Approximate width
        gc.fillText(title, boxX + (boxWidth - titleWidth) / 2, boxY + 80);

        // Success message
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        String message = "You saved the villagers!";
        double messageWidth = message.length() * 14; // Approximate width
        gc.fillText(message, boxX + (boxWidth - messageWidth) / 2, boxY + 130);

        // Bridge destruction confirmation
        gc.setFont(Font.font("Arial", 18));
        String bridgeMsg = "The bridge has been destroyed successfully.";
        double bridgeWidth = bridgeMsg.length() * 10;
        gc.fillText(bridgeMsg, boxX + (boxWidth - bridgeWidth) / 2, boxY + 170);

        // Additional flavor text
        gc.setFill(Color.LIGHTGRAY);
        gc.setFont(Font.font("Arial", 16));
        String flavorText = "The enemy can no longer reach the village!";
        double flavorWidth = flavorText.length() * 9;
        gc.fillText(flavorText, boxX + (boxWidth - flavorWidth) / 2, boxY + 200);

        // Thank you message
        gc.setFill(Color.GOLD);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        String thanksMsg = "Thank you for playing!";
        double thanksWidth = thanksMsg.length() * 12;
        gc.fillText(thanksMsg, boxX + (boxWidth - thanksWidth) / 2, boxY + 250);
    }

    private void drawDebugTriggerArea(GraphicsContext gc, double camX, double camY, double scale) {
        // Draw trigger area outline for debugging
        double drawX = (bridgeTriggerArea.getMinX() - camX) * scale;
        double drawY = (bridgeTriggerArea.getMinY() - camY) * scale;
        double drawW = bridgeTriggerArea.getWidth() * scale;
        double drawH = bridgeTriggerArea.getHeight() * scale;

        gc.setStroke(Color.LIME);
        gc.setLineWidth(2);
        gc.strokeRect(drawX, drawY, drawW, drawH);

        // Draw explosion area
        double expDrawX = (bridgeExplosionArea.getMinX() - camX) * scale;
        double expDrawY = (bridgeExplosionArea.getMinY() - camY) * scale;
        double expDrawW = bridgeExplosionArea.getWidth() * scale;
        double expDrawH = bridgeExplosionArea.getHeight() * scale;

        gc.setStroke(Color.RED);
        gc.strokeRect(expDrawX, expDrawY, expDrawW, expDrawH);
    }

    // ✅ Getters for Game class to check popup state
    public boolean isShowingBridgePopup() {
        return showBridgePopup;
    }

    public boolean isBridgeDestroyed() {
        return bridgeDestroyed;
    }

    public boolean isMissionCompleted() {
        return missionCompleted;
    }

    public boolean isShowingMissionCompletePopup() {
        return showMissionCompletePopup;
    }

    public void update(Main_Tank mainTank, Game_2 game2, long now) {

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