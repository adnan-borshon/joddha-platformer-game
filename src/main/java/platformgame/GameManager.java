package platformgame;


import platformgame.Objects.SuperObject;

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
        GameState.SavedObject[] savedObjects = new GameState.SavedObject[game.object.length];

        for (int i = 0; i < game.object.length; i++) {
            if (game.object[i] != null) {
                SuperObject obj = game.object[i];
                savedObjects[i] = new GameState.SavedObject(
                        obj.name, obj.worldX, obj.worldY, true
                );
            } else {
                savedObjects[i] = new GameState.SavedObject("", 0, 0, false);
            }
        }

        lastState = new GameState(
                game.player.getX(),
                game.player.getY(),
                game.hasKey,
                game.player.speed,  // ✅ Save speed
                savedObjects
        );
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
