package platformgame;

public class GameState {
    public double playerX;
    public double playerY;
    public int hasKey;


    public GameState(double playerX, double playerY, int hasKey) {
        this.playerX = playerX;
        this.playerY = playerY;
        this.hasKey = hasKey;
    }
}
