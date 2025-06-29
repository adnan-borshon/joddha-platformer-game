package platformgame;

public class GameState {
    public double playerX;
    public double playerY;
    public int hasKey;
    public double playerSpeed;

    // for saving the objects
    public SavedObject[] savedObjects;

    // new: save whether music was on
    public boolean musicOn;

    // updated constructor to include musicOn
    public GameState(double playerX,
                     double playerY,
                     int hasKey,
                     double playerSpeed,
                     SavedObject[] savedObjects,
                     boolean musicOn) {
        this.playerX     = playerX;
        this.playerY     = playerY;
        this.hasKey      = hasKey;
        this.playerSpeed = playerSpeed;
        this.savedObjects= savedObjects;
        this.musicOn     = musicOn;
    }

    public static class SavedObject {
        public String  type;
        public int     worldX, worldY;
        public boolean exists;

        public SavedObject(String type, int worldX, int worldY, boolean exists) {
            this.type    = type;
            this.worldX  = worldX;
            this.worldY  = worldY;
            this.exists  = exists;
        }
    }
}
