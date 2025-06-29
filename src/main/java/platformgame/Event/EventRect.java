package platformgame.Event;

import javafx.geometry.Rectangle2D;

public class EventRect {
    private double x, y, width, height;
    private boolean triggered = false;
    private double scaleFactor = 3.0; // default explosion scale

    public EventRect(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public Rectangle2D getBounds() {
        return new Rectangle2D(x, y, width, height);
    }

    public boolean isTriggered() {
        return triggered;
    }

    public void setTriggered(boolean triggered) {
        this.triggered = triggered;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }

    public void setScaleFactor(double scaleFactor) {
        this.scaleFactor = scaleFactor;
    }

    public double getScaleFactor() {
        return scaleFactor;
    }
}
