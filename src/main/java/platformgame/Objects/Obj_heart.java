package platformgame.Objects;

import javafx.scene.image.Image;

import java.util.Objects;

public class Obj_heart {
    public Image heartIcon;
    public Image healthFullBar;
    public Image healthEmptyBar;

    public Obj_heart() {
        heartIcon = new Image(Objects.requireNonNull(getClass().getResourceAsStream(
                "/image/Object/Health-Icon.png")));
        healthFullBar = new Image(Objects.requireNonNull(getClass().getResourceAsStream(
                "/image/Object/Health-Bar.png")));
        healthEmptyBar = new Image(Objects.requireNonNull(getClass().getResourceAsStream(
                "/image/Object/Health-line.png")));
    }
}
