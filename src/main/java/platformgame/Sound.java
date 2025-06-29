package platformgame;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.net.URL;

public class Sound {
    private static Sound instance;
    private final MediaPlayer[] mediaPlayers = new MediaPlayer[30];

    // GLOBAL flag for whether music (menu or game) should play
    private boolean musicEnabled = true;

    private Sound() {
        try {
            loadSound(0, "/sounds/BGM(loop).WAV");  // game BGM
            loadSound(6, "/sounds/Menu.wav");             // menu music
            loadSound(3, "/sounds/Punch.WAV");
            loadSound(4, "/sound/bomb.mp3");
            loadSound(1, "/sounds/GunShot.WAV");
            loadSound(5, "/sounds/click.mp3");
            loadSound(7, "/sounds/Level_1_Mission_Complete_Narration.m4a");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Sound getInstance() {
        if (instance == null) instance = new Sound();
        return instance;
    }

    /** Turn music on/off globally */
    public void setMusicEnabled(boolean on) {
        musicEnabled = on;
    }
    public boolean isMusicEnabled() {
        return musicEnabled;
    }

    private void loadSound(int index, String resourcePath) {
        URL res = getClass().getResource(resourcePath);
        if (res != null) {
            mediaPlayers[index] = new MediaPlayer(new Media(res.toExternalForm()));
        } else {
            System.err.println("Sound not found: " + resourcePath);
        }
    }

    public void play(int idx) {
        if (mediaPlayers[idx] != null) {
            mediaPlayers[idx].stop();
            mediaPlayers[idx].setCycleCount(1);
            mediaPlayers[idx].play();
        }
    }

    public void loop(int idx) {
        if (!musicEnabled) return;       // respect global flag
        if (mediaPlayers[idx] != null && !isPlaying(idx)) {
            mediaPlayers[idx].stop();
            mediaPlayers[idx].setCycleCount(MediaPlayer.INDEFINITE);
            mediaPlayers[idx].play();
        }
    }

    public void stop(int idx) {
        if (mediaPlayers[idx] != null) {
            mediaPlayers[idx].stop();
        }
    }

    public boolean isPlaying(int idx) {
        return mediaPlayers[idx] != null
                && mediaPlayers[idx].getStatus() == MediaPlayer.Status.PLAYING;
    }

    public void stopAll() {
        for (MediaPlayer mp : mediaPlayers) {
            if (mp != null) mp.stop();
        }
    }
}
