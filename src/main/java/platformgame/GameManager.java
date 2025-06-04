package platformgame;



//for saving all the data
public class GameManager {
    private static GameManager instance;
    private GameState lastState;

    private GameManager() {}

    public static GameManager getInstance() {
        if (instance == null) {
            instance = new GameManager();
        }
        return instance;
    }

    public void saveState(Game game) {
        lastState = new GameState(game.player.getX(), game.player.getY(), game.hasKey); // ✅ Fixed constructor
    }

    public GameState getLastState() {
        return lastState;
    }

    public boolean hasSavedState() {
        return lastState != null;
    }

    public void clearState() {
        lastState = null;
    }
}
