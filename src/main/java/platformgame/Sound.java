package platformgame;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.net.URL;

public class Sound {

    public MediaPlayer[] mediaPlayers = new MediaPlayer[30];


    public Sound(){
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
            mediaPlayers[index].stop();
            mediaPlayers[index].setCycleCount(MediaPlayer.INDEFINITE);
            mediaPlayers[index].play();
        }
    }

    // ⏹ Stop
    public void stop(int index) {
        if (mediaPlayers[index] != null) {
            mediaPlayers[index].stop();
        }
    }
}
