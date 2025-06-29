package platformgame.Tanks;

// Fixed Vector2D class
public class Vector2D {
    public double x, y;

    public Vector2D() {
        this(0, 0);
    }

    public Vector2D(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void set(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double length() {
        return Math.sqrt(x * x + y * y);
    }

    public double lengthSquared() {
        return x * x + y * y;
    }

    public void normalize() {
        double len = length();
        if (len > 0) {
            x /= len;
            y /= len;
        }
    }

    public Vector2D normalized() {
        double len = length();
        if (len > 0) {
            return new Vector2D(x / len, y / len);
        }
        return new Vector2D(0, 0);
    }

    public Vector2D rotated(double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Vector2D(x * cos - y * sin, x * sin + y * cos);
    }

    public void rotate(double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double newX = x * cos - y * sin;
        double newY = x * sin + y * cos;
        x = newX;
        y = newY;
    }

    public Vector2D add(Vector2D other) {
        return new Vector2D(x + other.x, y + other.y);
    }

    public Vector2D subtract(Vector2D other) {
        return new Vector2D(x - other.x, y - other.y);
    }

    public Vector2D multiply(double scalar) {
        return new Vector2D(x * scalar, y * scalar);
    }

    public double dot(Vector2D other) {
        return x * other.x + y * other.y;
    }

    public double distanceTo(Vector2D other) {
        double dx = x - other.x;
        double dy = y - other.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    // Position getters/setters
    public double getX() { return x; }
    public double getY() { return y; }
    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }

    @Override
    public String toString() {
        return String.format("Vector2D(%.2f, %.2f)", x, y);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Vector2D vector2D = (Vector2D) obj;
        return Double.compare(vector2D.x, x) == 0 && Double.compare(vector2D.y, y) == 0;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(x, y);
    }


}
