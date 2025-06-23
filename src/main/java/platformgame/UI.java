package platformgame;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import platformgame.Entity.Entity;
import platformgame.Objects.Obj_Key;

public class UI {
    Game game;
    GraphicsContext gc;
    private Image keyIcon;

    //Notification for collecting power item
    public boolean messageOn= false;
    public String message= "";
    public int msgCounter= 0;

    //Text for dialogue
    public String dialogue="";

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
//        if(game.GameState == game.pauseState){
//            drawPauseScreen();
//        }

        //For dialogue section
        if(game.GameState == game.dialogueState){
            drawDialogueScreen();


        }
    }

    //Dialogue screen
    public void drawDialogueScreen(){
        int x=game.tileSize;
        int y=game.tileSize/2;
        int height =  game.tileSize*8;
        int width = (int) (game.screenWidth - (game.tileSize*2));
    subWindows(x,y,height, width);
    x+= game.tileSize;
    y+= game.tileSize;

        gc.setFont(Font.font("Arial", FontWeight.SEMI_BOLD, 30));
        gc.setFill(Color.WHITE);
    gc.fillText(dialogue, x,y+20);
    }

    // Every dialogue screen subwindows
    public void subWindows(int x, int y, int height, int width){

        Color fillColor = Color.rgb(0, 0, 0, 0.85);
        Color borderColor = Color.rgb(255, 255, 255);


        gc.setFill(fillColor);
        gc.fillRoundRect(x, y, width, height, 35, 35);

        // Set border color with transparency
        gc.setStroke(borderColor);
        gc.setLineWidth(5); // Set border thickness
        gc.strokeRoundRect(x+5, y+5, width-10, height-10, 25, 25); // Draw border
    }



    public void drawPauseScreen(){
        String text = "PAUSED";


    }
}
