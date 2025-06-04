package platformgame.Entity;

import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import platformgame.Game;
import platformgame.Objects.SuperObject;

import java.util.Random;

public class Npc extends Entity {
    private int totalFrames_walk= 8;
    private boolean collision = true;
    private String direction = "down";
    private final int totalFrames_idle = 11;  // frames in row 1 (2nd row)

    protected boolean playerIsTouching = false;

    public Npc(double x, double y, double width, double height, double speed, Game gp){
        super(x,y,width, height, speed, gp);
        imageSet(totalFrames_walk, "/image/npc.png");
    }

    public void draw(GraphicsContext gc, double camX, double camY, double scale) {
        drawEntity(gc, camX, camY, scale);


        //Debug
        // Draw collision rectangle (bounding box)
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

    public void setAction() {

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
        double newX = x;
        double newY = y;

        // Check collision with player
        if (playerIsTouching) {
            System.out.println("Collision happening");
            direction = "stop";
            actionCounter++;
            if(actionCounter>120){
                playerIsTouching=false;
                actionCounter=0;
            }
        }
    else {
        setAction();
        }




        // Apply movement
        switch (direction) {
            case "up": newY -= speed; break;
            case "down": newY += speed; break;
            case "left": newX -= speed; facingRight = false; break;
            case "right": newX += speed; facingRight = true; break;
            case "stop": break;
        }

        // Tile collision check
        boolean canMoveX = !gp.tileMap.isColliding(newX, y, width, height);
        boolean canMoveY = !gp.tileMap.isColliding(x, newY, width, height);

        // Check object collision (all objects block NPCs)
        Rectangle2D futureXRect = new Rectangle2D(newX, y, width, height);
        Rectangle2D futureYRect = new Rectangle2D(x, newY, width, height);
        for (SuperObject obj : gp.object) {
            if (obj != null) {
                Rectangle2D objRect = obj.getBoundingBox();
                if (futureXRect.intersects(objRect)) canMoveX = false;
                if (futureYRect.intersects(objRect)) canMoveY = false;
            }
        }
        if (canMoveX) x = newX;
        if (canMoveY) y = newY;

        // Animate
        animationTimer += deltaTime;

        if (direction.equals("stop")) {
            currentRow = 1;
            if (animationTimer > 130_000_000) {
                nextFrame(totalFrames_idle);
                animationTimer = 0;
            }
        } else {
            currentRow = 3;
            if (animationTimer > 100_000_000) {
                nextFrame(totalFrames_walk);
                animationTimer = 0;
            }
        }
    }
    public void notifyPlayerCollision() {
        playerIsTouching = true;
    }


    public boolean isCollision() {
        return collision;
    }

    public void setCollision(boolean collision) {
        this.collision = collision;
    }
}
