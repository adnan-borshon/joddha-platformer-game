package platformgame;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import platformgame.Objects.Obj_Key;

public class UI {
    Game game;
    GraphicsContext gc;
    private Image keyIcon;

    public boolean messageOn= false;
    public String message= "";
    public int msgCounter= 0;


    public UI(Game game) {
        this.game = game;
        try {
            Obj_Key key = new Obj_Key();  // generate key image from existing class
            keyIcon = key.image;
        } catch (Exception e) {
            System.out.println("Key icon not found.");
        }
    }

    public void showMessage(String text){
message= text;
messageOn= true;
    }

    public void draw(GraphicsContext gc) {
        this.gc = gc;
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", 24));

        //For Resume section
        if (game.GameState == game.playState) {
            // Draw key icon
            if (keyIcon != null) {
                gc.drawImage(keyIcon, 20, 20, 32, 32); // top-left corner
            }

            // Draw text showing key count
            gc.fillText("x " + game.hasKey, 60, 45); // next to icon

            //Message
            if (messageOn == true) {
                gc.setFont(Font.font("Arial", 30));
                gc.fillText(message, game.tileSize / 2, game.tileSize * 5);
                msgCounter++;
            }
            if (msgCounter > 120) {
                message = "";
                messageOn = false;
                msgCounter = 0;
            }
        }

        //For pause section
        if(game.GameState == game.pauseState){
            drawPauseScreen();
        }
    }
    public void drawPauseScreen(){
        String text = "PAUSED";


    }
}
