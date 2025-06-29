package platformgame.Entity;

import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import platformgame.Game;

public class Npc extends Entity {
    private final int totalFrames_idle = 11;  // frames in idle animation (used here)
    private int dialogueIndex = 0;
    public boolean playerIsTouching = false;

    // Constructor with custom dialogue
    public Npc(double x, double y, double width, double height, double speed, Game gp, Image[] customDialogue) {
        super(x, y, width, height, speed, gp);
        imageSet(totalFrames_idle, "/image/npc.png");  // Set to idle sprite sheet
        setDialogue(customDialogue);
    }

    // Constructor with default dialogue (for backward compatibility)
    public Npc(double x, double y, double width, double height, double speed, Game gp) {
        super(x, y, width, height, speed, gp);
        imageSet(totalFrames_idle, "/image/npc.png");  // Set to idle sprite sheet
//        setDialogue();
    }

    public void draw(GraphicsContext gc, double camX, double camY, double scale) {
        drawEntity(gc, camX, camY, scale);

        // Draw collision rectangle (bounding box) for debugging
        gc.save();
        gc.setLineWidth(1);
        gc.setStroke(javafx.scene.paint.Color.RED); // Choose any visible color
        double drawX = (x - camX) * scale;
        double drawY = (y - camY) * scale;
        double drawW = width * scale;
        double drawH = height * scale;
        gc.strokeRect(drawX, drawY, drawW, drawH); // Draw red rectangle
        gc.restore();
    }

    // Simplified setAction method, now it doesn't change direction (since NPC doesn't move)
    public void setAction() {
        actionCounter++;
        if (actionCounter == 120) {
            actionCounter = 0;  // Just reset the counter; no need to change direction now
        }
    }

    public void update(long deltaTime, long now) {
        if (gp.GameState == gp.dialogueState) return;  // Skip update if in dialogue state

        // Animate the idle sprite
        animationTimer += deltaTime;
        currentRow = 1;  // Use row 1 for idle animation
        if (animationTimer > 180_000_000) {
            nextFrame(totalFrames_idle);  // Go to the next idle frame
            animationTimer = 0;
        }
    }

    // Method to set custom dialogue
    public void setDialogue(Image[] customDialogue) {
        for (int i = 0; i < customDialogue.length && i < dialogues.length; i++) {
            dialogues[i] = customDialogue[i];
        }
    }

    // Default dialogue method
//    public void setDialogue() {
//        dialogues[0] = "Rohan please be careful. There are many soldiers with weapons and there are hostages please save them.";
//        dialogues[1] = "Stay alert and watch your back.";
//        dialogues[2] = "The enemies are heavily armed.";
//        dialogues[3] = "Save the hostages at all costs.";
//    }

    public void speak() {
        if (dialogues[dialogueIndex] == null) {
            dialogueIndex = 0;
            gp.GameState = gp.playState;
            playerIsTouching = false; // prevent instant re-trigger
            return;
        }

        // ✅ Set image-based dialogue
        gp.ui.setImageDialogue(dialogues[dialogueIndex]);
        gp.GameState = gp.dialogueState;
        dialogueIndex++;
    }

    public void notifyPlayerCollision() {
        playerIsTouching = true;  // Player has touched the NPC
    }
}