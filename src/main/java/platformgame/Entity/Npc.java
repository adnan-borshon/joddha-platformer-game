package platformgame.Entity;

import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import platformgame.Game;

import java.util.Random;

public class Npc extends Entity {
    private int totalFrames_walk= 8;
    private boolean collision = true;
    private String direction = "down";
    private final int totalFrames_idle = 11;  // frames in row 1 (2nd row)


    public Npc(double x, double y, double width, double height, double speed, Game gp){
        super(x,y,width, height, speed, gp);
        imageSet(totalFrames_walk, "/image/npc.png");
    }

    public void draw(GraphicsContext gc, double camX, double camY, double scale) {
        drawEntity(gc, camX, camY, scale);
    }

    public void setAction() {
        if (direction.equals("stop")) {
            actionCounter++;

        if (actionCounter > 240) { // stop for 6 second (~60 frames)
            direction = "down";   // or set random direction again
            actionCounter = 0;
        }
    }
        actionCounter++;

//        move for 2 sec in any direction
        if(actionCounter == 120){
        Random random = new Random();
        int i = random.nextInt(100)+1;
        if (i <= 25) {
            direction = "up";
        } else if (i <= 50) {
            direction = "down";
        } else if (i <= 75) {
            direction = "left";
        } else {
            direction = "right";
        }
        actionCounter=0;
        }
    }

    public void update(long deltaTime, long now) {
        // Move NPC based on direction
        double newX = x;
        double newY = y;
        setAction(); // choose new direction occasionally
        switch (direction) {
            case "up": newY -= speed; break;
            case "down": newY += speed; break;
            case "left": newX -= speed; facingRight = false; break;
            case "right": newX += speed; facingRight = true; break;
            case "stop": break;
        }

        // Basic tile collision
        boolean canMoveX = !gp.tileMap.isColliding(newX, y, width, height);
        boolean canMoveY = !gp.tileMap.isColliding(x, newY, width, height);

        if (canMoveX) x = newX;
        if (canMoveY) y = newY;

        if (checkPlayerCollision()) {
            direction = "stop"; // Or: reverseDirection();
        }
        // Animate
        animationTimer += deltaTime;

        if (direction.equals("stop")) {
            currentRow = 1; // second row for idle
            if (animationTimer > 130_000_000) {
                nextFrame(totalFrames_idle); // cycle through idle frames
                animationTimer = 0;
            }
        } else {
            currentRow = 3; // third row for walking
            if (animationTimer > 100_000_000) {
                nextFrame(totalFrames_walk); // cycle through walk frames
                animationTimer = 0;
            }
        }


    }
        //checking Npc to player collision
    public boolean checkPlayerCollision() {
        Rectangle2D npcRect = new Rectangle2D(x, y, width, height);
        Rectangle2D playerRect = new Rectangle2D(gp.player.getX(), gp.player.getY(), gp.player.getWidth(), gp.player.getHeight());

        return npcRect.intersects(playerRect);
    }

    public boolean isCollision() {
        return collision;
    }

    public void setCollision(boolean collision) {
        this.collision = collision;
    }
}
