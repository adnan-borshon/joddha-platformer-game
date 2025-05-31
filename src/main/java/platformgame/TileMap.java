package platformgame;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class TileMap {
    private final int cols, rows, tileSize;
    private int[][] map;
    private List<Tile> tiles;

    private static class Tile {
        Image image;
        boolean collision;

        public Tile(Image image, boolean collision) {
            this.image = image;
            this.collision = collision;
        }
    }

    public TileMap(int cols, int rows, int tileSize) {
        this.cols = cols;
        this.rows = rows;
        this.tileSize = tileSize;

        tiles = new ArrayList<>();
        loadTiles();
        loadMap();
    }

    private void loadTiles() {
        try (InputStream is = getClass().getResourceAsStream("/map/NewTileData.txt");
             BufferedReader br = new BufferedReader(new InputStreamReader(is))) {

            String line;
            while ((line = br.readLine()) != null) {
                String fileName = line.trim();
                String collisionLine = br.readLine();
                boolean collision = false;
                if (collisionLine != null) {
                    collision = Boolean.parseBoolean(collisionLine.trim());
                }

                InputStream imgStream = getClass().getResourceAsStream("/image/tiles/" + fileName);
                if (imgStream == null) {
                    System.err.println("Tile image not found: " + fileName);
                    tiles.add(new Tile(null, collision));
                    continue;
                }
                Image img = new Image(imgStream);
                tiles.add(new Tile(img, collision));

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void loadMap() {
        map = new int[rows][cols];

        try (InputStream is = getClass().getResourceAsStream("/map/newmap.txt");
             BufferedReader br = new BufferedReader(new InputStreamReader(is))) {

            for (int row = 0; row < rows; row++) {
                String line = br.readLine();
                if (line == null) break;
                String[] nums = line.trim().split("\\s+");

                for (int col = 0; col < cols; col++) {
                    if (col < nums.length) {
                        try {
                            map[row][col] = Integer.parseInt(nums[col]);
                        } catch (NumberFormatException e) {
                            map[row][col] = 0; // default tile index
                        }
                    } else {
                        map[row][col] = 0;
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void draw(GraphicsContext gc, double camX, double camY, double scale) {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int tileIndex = map[row][col];

                if (tileIndex >= 0 && tileIndex < tiles.size()) {
                    Tile tile = tiles.get(tileIndex);
                    if (tile.image != null) {
                        double x = (col * tileSize - camX) * scale;
                        double y = (row * tileSize - camY) * scale;
                        double size = tileSize * scale;
                        gc.drawImage(tile.image, x, y, size, size);
                    } else {
                        gc.setFill(Color.PINK);
                        double x = (col * tileSize - camX) * scale;
                        double y = (row * tileSize - camY) * scale;
                        double size = tileSize * scale;
                        gc.fillRect(x, y, size, size);
                    }
                }
            }
        }
    }


    public boolean isColliding(double x, double y, double width, double height) {
        int leftCol = (int) (x / tileSize);
        int rightCol = (int) ((x + width) / tileSize);
        int topRow = (int) (y / tileSize);
        int bottomRow = (int) ((y + height) / tileSize);

        for (int row = topRow; row <= bottomRow; row++) {
            for (int col = leftCol; col <= rightCol; col++) {
                if (row < 0 || row >= rows || col < 0 || col >= cols) continue;
                int tileIndex = map[row][col];
                if (tileIndex >= 0 && tileIndex < tiles.size()) {
                    boolean collides = tiles.get(tileIndex).collision;

                    if (collides) {
                        return true;
                    }
                }
            }
        }
        return false;
    }




    public double getWidthInPixels() {
        return cols * tileSize;
    }

    public double getHeightInPixels() {
        return rows * tileSize;
    }
}
