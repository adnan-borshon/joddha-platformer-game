package platformgame.Map;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Level_1 {

    private final int tileWidth = 32;
    private final int tileHeight = 32;
    public int mapWidth;
    public int mapHeight;

    private static class Tileset {
        int firstGid;
        int tileWidth;
        int tileHeight;
        int columns;
        Image image;
    }

    private static class Layer {
        String name;
        int[][] data;
        boolean isBackground;
        boolean isForeground;
        boolean isCollision;
    }

    private final List<Tileset> tilesets = new ArrayList<>();
    private final List<Layer> layers = new ArrayList<>();

    public Level_1() {

        loadMapData();
    }

    private void loadMapData() {
        try {
            InputStream tmxFileStream = getClass().getResourceAsStream("/Level_1/Level_1.tmx");
            if (tmxFileStream == null) throw new FileNotFoundException("TMX file not found");

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(tmxFileStream);

            Element mapElement = doc.getDocumentElement();
            mapWidth = Integer.parseInt(mapElement.getAttribute("width"));
            mapHeight = Integer.parseInt(mapElement.getAttribute("height"));



            // Load tilesets
            NodeList tilesetList = doc.getElementsByTagName("tileset");
            for (int i = 0; i < tilesetList.getLength(); i++) {
                Element tilesetElem = (Element) tilesetList.item(i);

                // Handle external tilesets (source attribute)
                if (tilesetElem.hasAttribute("source")) {
                    continue;
                }

                int firstGid = Integer.parseInt(tilesetElem.getAttribute("firstgid"));

                // Check if columns attribute exists
                String columnsAttr = tilesetElem.getAttribute("columns");
                int columns = columnsAttr.isEmpty() ? 16 : Integer.parseInt(columnsAttr);

                int tw = Integer.parseInt(tilesetElem.getAttribute("tilewidth"));
                int th = Integer.parseInt(tilesetElem.getAttribute("tileheight"));

                NodeList imageNodes = tilesetElem.getElementsByTagName("image");
                if (imageNodes.getLength() == 0) {
                    continue;
                }

                Element imageElem = (Element) imageNodes.item(0);
                String imagePath = imageElem.getAttribute("source");

                // Extract just the filename from the path
                String fileName = imagePath.substring(imagePath.lastIndexOf("/") + 1);

                InputStream imageStream = getClass().getResourceAsStream("/Level_1/Assets/" + fileName);
                if (imageStream == null) {
                    // Try without Assets folder
                    imageStream = getClass().getResourceAsStream("/Level_1/" + fileName);
                    if (imageStream == null) {
                        continue;
                    }
                }

                Image image = new Image(imageStream);

                Tileset t = new Tileset();
                t.firstGid = firstGid;
                t.columns = columns;
                t.tileWidth = tw;
                t.tileHeight = th;
                t.image = image;

                tilesets.add(t);
            }

            tilesets.sort(Comparator.comparingInt(t -> t.firstGid));

            // Load all layers with classification
            NodeList layerNodes = doc.getElementsByTagName("layer");
            for (int i = 0; i < layerNodes.getLength(); i++) {
                Element layerElem = (Element) layerNodes.item(i);
                String layerName = layerElem.getAttribute("name").toLowerCase();

                Element data = (Element) layerElem.getElementsByTagName("data").item(0);
                String csv = data.getTextContent().trim();

                // Clean up the CSV data
                csv = csv.replaceAll("\\s+", "");

                int[][] grid = parseCSV(csv, mapWidth, mapHeight);

                Layer layer = new Layer();
                layer.name = layerName;
                layer.data = grid;

                // FIXED: Classify layers based on name with corrected logic for trees
                layer.isBackground = layerName.contains("background") ||
                        layerName.contains("ground") ||
                        layerName.contains("floor") ||
                        layerName.contains("base") ||
                        layerName.contains("water") ||
                        i == 0;

                // FIXED: "Trees collision" should NOT be in foreground
                layer.isForeground = layerName.contains("foreground") ||
                        (layerName.contains("tree") && !layerName.contains("collision"));

                // Collision detection remains the same
                layer.isCollision = layerName.contains("boat") ||
                        layerName.contains("army camp objects") ||
                        layerName.contains("army camp 1") ||
                        layerName.contains("fench") ||
                        layerName.contains("khet") ||
                        layerName.contains("river") ||
                        layerName.contains("river paar") ||
                        layerName.contains("trees collisions");

                layers.add(layer);

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // IMPROVED: More precise collision detection
    public boolean isCollisionTile(double x, double y) {
        // Convert world coordinates to tile coordinates
        int tileX = (int) (x / tileWidth);
        int tileY = (int) (y / tileHeight);

        // Check bounds
        if (tileX < 0 || tileX >= mapWidth || tileY < 0 || tileY >= mapHeight) {
            return true; // Out of bounds = collision
        }

        // Check all collision layers
        for (Layer layer : layers) {
            if (layer.isCollision && layer.data[tileY][tileX] != 0) {
                return true;
            }
        }

        return false;
    }

    // IMPROVED: Better rectangle collision with multiple sample points
    public boolean isCollisionRect(double x, double y, double width, double height) {
        // Sample more points for better collision detection
        int samples = 3; // Check 3x3 grid of points
        double stepX = width / (samples - 1);
        double stepY = height / (samples - 1);

        for (int i = 0; i < samples; i++) {
            for (int j = 0; j < samples; j++) {
                double checkX = x + (i * stepX);
                double checkY = y + (j * stepY);

                // Ensure we don't go beyond the rectangle bounds
                checkX = Math.min(checkX, x + width - 1);
                checkY = Math.min(checkY, y + height - 1);

                if (isCollisionTile(checkX, checkY)) {
                    return true;
                }
            }
        }

        return false;
    }

    // NEW: Check collision for next position (useful for movement prediction)
    public boolean wouldCollide(double currentX, double currentY, double newX, double newY, double width, double height) {
        return isCollisionRect(newX, newY, width, height);
    }

    // NEW: Get safe position (moves entity out of collision)
    public double[] getSafePosition(double x, double y, double width, double height) {
        double[] result = {x, y};

        // If not currently colliding, return current position
        if (!isCollisionRect(x, y, width, height)) {
            return result;
        }

        // Try to move to nearby safe positions
        int searchRadius = 3; // tiles to search around
        for (int radius = 1; radius <= searchRadius; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    double testX = x + (dx * tileWidth);
                    double testY = y + (dy * tileHeight);

                    if (!isCollisionRect(testX, testY, width, height)) {
                        result[0] = testX;
                        result[1] = testY;
                        return result;
                    }
                }
            }
        }

        return result; // Return original position if no safe position found
    }

    // Draw only background layers
    public void drawBackground(GraphicsContext gc, double camX, double camY, double scale) {
        for (Layer layer : layers) {
            if (layer.isBackground) {
                drawLayer(gc, layer.data, camX, camY, scale);
            }
        }
    }

    // Draw only foreground layers (trees, rooftops, etc.)
    public void drawForeground(GraphicsContext gc, double camX, double camY, double scale) {
        for (Layer layer : layers) {
            if (layer.isForeground) {
                drawLayer(gc, layer.data, camX, camY, scale);
            }
        }
    }

    // Draw middle layers (neither background nor foreground, and not collision-only)
    public void drawMiddleground(GraphicsContext gc, double camX, double camY, double scale) {
        for (Layer layer : layers) {
            if (!layer.isBackground && !layer.isForeground) {
                drawLayer(gc, layer.data, camX, camY, scale);
            }
        }
    }

    // NEW: Debug method to visualize collision tiles
    public void drawCollisionDebug(GraphicsContext gc, double camX, double camY, double scale) {
        gc.setFill(javafx.scene.paint.Color.RED);
        gc.setGlobalAlpha(0.3);

        for (Layer layer : layers) {
            if (layer.isCollision) {
                for (int y = 0; y < mapHeight; y++) {
                    for (int x = 0; x < mapWidth; x++) {
                        if (layer.data[y][x] != 0) {
                            double drawX = Math.floor((x * tileWidth - camX) * scale);
                            double drawY = Math.floor((y * tileHeight - camY) * scale);
                            double drawWidth = Math.ceil(tileWidth * scale);
                            double drawHeight = Math.ceil(tileHeight * scale);

                            gc.fillRect(drawX, drawY, drawWidth, drawHeight);
                        }
                    }
                }
            }
        }

        gc.setGlobalAlpha(1.0);
    }

    // Original draw method - draws all layers (for backward compatibility)
    public void draw(GraphicsContext gc, double camX, double camY, double scale) {
        for (Layer layer : layers) {
            drawLayer(gc, layer.data, camX, camY, scale);
        }
    }

    private void drawLayer(GraphicsContext gc, int[][] grid, double camX, double camY, double scale) {
        for (int y = 0; y < mapHeight; y++) {
            for (int x = 0; x < mapWidth; x++) {
                int gid = grid[y][x];
                if (gid == 0) continue;

                Tileset selected = null;
                for (int j = tilesets.size() - 1; j >= 0; j--) {
                    if (gid >= tilesets.get(j).firstGid) {
                        selected = tilesets.get(j);
                        break;
                    }
                }

                if (selected == null) continue;

                int localId = gid - selected.firstGid;
                int sx = (localId % selected.columns) * selected.tileWidth;
                int sy = (localId / selected.columns) * selected.tileHeight;

                gc.drawImage(
                        selected.image,
                        sx, sy, selected.tileWidth, selected.tileHeight,
                        Math.floor((x * selected.tileWidth - camX) * scale),
                        Math.floor((y * selected.tileHeight - camY) * scale),
                        Math.ceil(selected.tileWidth * scale),
                        Math.ceil(selected.tileHeight * scale)
                );
            }
        }
    }

    private int[][] parseCSV(String csv, int width, int height) {
        int[][] result = new int[height][width];

        // Remove any trailing commas
        csv = csv.replaceAll(",+$", "");

        String[] tokens = csv.split(",", -1);
        int index = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (index < tokens.length) {
                    String token = tokens[index++].trim();
                    if (token.isEmpty()) {
                        result[y][x] = 0;
                    } else {
                        try {
                            result[y][x] = Integer.parseInt(token);
                        } catch (NumberFormatException e) {
                            result[y][x] = 0;
                        }
                    }
                } else {
                    result[y][x] = 0;
                }
            }
        }
        return result;
    }
}