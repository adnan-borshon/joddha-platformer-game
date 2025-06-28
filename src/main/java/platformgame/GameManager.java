package platformgame;

import platformgame.Objects.SuperObject;

public class GameManager {
    private static GameManager instance;
    private GameState lastState;
    private boolean musicEnabled = true;
    private GameManager() {}

    public static GameManager getInstance() {
        if (instance == null) instance = new GameManager();
        return instance;
    }
    public void setMusicEnabled(boolean on) {
        this.musicEnabled = on;
    }
    public boolean isMusicEnabled() {
        return musicEnabled;
    }
    public void saveState(Game game) {
        // 1) gather object states
        GameState.SavedObject[] savedObjects = new GameState.SavedObject[game.object.length];
        for (int i = 0; i < game.object.length; i++) {
            SuperObject obj = game.object[i];
            if (obj != null) {
                savedObjects[i] = new GameState.SavedObject(
                        obj.name, obj.worldX, obj.worldY, true
                );
            } else {
                savedObjects[i] = new GameState.SavedObject("", 0, 0, false);
            }
        }

        // 2) capture whether music is playing (index 0)
        lastState = new GameState(
                game.player.getX(),
                game.player.getY(),
                game.hasKey,                    // boolean now
                game.player.speed,
                savedObjects,
                Sound.getInstance().isPlaying(0)
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

    // (empty stub for Game_2)
    public void saveState(Game_2 game2) { }
}
