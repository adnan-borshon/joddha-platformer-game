package platformgame;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.net.URL;

public class Sound {

    private static Sound instance;
    private final MediaPlayer[] mediaPlayers = new MediaPlayer[30];

    private Sound() {
        try {
            loadSound(0, "/sound/BlueBoyAdventure.wav");
            loadSound(1, "/sound/coin.wav");
            loadSound(2, "/sound/powerup.wav");
            loadSound(3, "/sound/unlock.wav");
            loadSound(4, "/sound/fanfare.wav");
            // Add more as needed
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Sound getInstance() {
        if (instance == null) {
            instance = new Sound();
        }
        return instance;
    }

    private void loadSound(int index, String resourcePath) {
        URL resource = getClass().getResource(resourcePath);
        if (resource != null) {
            Media media = new Media(resource.toExternalForm());
            mediaPlayers[index] = new MediaPlayer(media);
        } else {
            System.err.println("Sound not found: " + resourcePath);
        }
    }

    // ▶️ Play once
    public void play(int index) {
        if (mediaPlayers[index] != null) {
            mediaPlayers[index].stop();
            mediaPlayers[index].setCycleCount(1);
            mediaPlayers[index].play();
        }
    }

    // 🔁 Loop continuously
    public void loop(int index) {
        if (mediaPlayers[index] != null) {
            if (!isPlaying(index)) {
                mediaPlayers[index].stop();
                mediaPlayers[index].setCycleCount(MediaPlayer.INDEFINITE);
                mediaPlayers[index].play();
            }
        }
    }

    // ⏹ Stop
    public void stop(int index) {
        if (mediaPlayers[index] != null) {
            mediaPlayers[index].stop();
        }
    }

    // 🔍 Check if a sound is currently playing
    public boolean isPlaying(int index) {
        if (mediaPlayers[index] != null) {
            return mediaPlayers[index].getStatus() == MediaPlayer.Status.PLAYING;
        }
        return false;
    }

    // ⏹ Stop all sounds
    public void stopAll() {
        for (MediaPlayer player : mediaPlayers) {
            if (player != null) {
                player.stop();
            }
        }
    }
}
