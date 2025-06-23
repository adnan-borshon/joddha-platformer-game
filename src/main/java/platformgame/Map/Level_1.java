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
        boolean isCollision; // Add collision flag
    }

    private final List<Tileset> tilesets = new ArrayList<>();
    private final List<Layer> layers = new ArrayList<>();

    public Level_1() {
        System.out.println("Level_1 logic class created...");
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

            System.out.println("Map dimensions: " + mapWidth + "x" + mapHeight);

            // Load tilesets
            NodeList tilesetList = doc.getElementsByTagName("tileset");
            for (int i = 0; i < tilesetList.getLength(); i++) {
                Element tilesetElem = (Element) tilesetList.item(i);
                int firstGid = Integer.parseInt(tilesetElem.getAttribute("firstgid"));
                int columns = Integer.parseInt(tilesetElem.getAttribute("columns"));
                int tw = Integer.parseInt(tilesetElem.getAttribute("tilewidth"));
                int th = Integer.parseInt(tilesetElem.getAttribute("tileheight"));

                Element imageElem = (Element) tilesetElem.getElementsByTagName("image").item(0);
                String imagePath = imageElem.getAttribute("source");
                System.out.println("Loading tileset image: " + imagePath);

                InputStream imageStream = getClass().getResourceAsStream("/Level_1/" + imagePath);
                if (imageStream == null) {
                    System.err.println("Tileset image not found: " + imagePath);
                    continue;
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
            System.out.println("Tilesets loaded: " + tilesets.size());

            // Load all layers with classification
            NodeList layerNodes = doc.getElementsByTagName("layer");
            for (int i = 0; i < layerNodes.getLength(); i++) {
                Element layerElem = (Element) layerNodes.item(i);
                String layerName = layerElem.getAttribute("name").toLowerCase();

                Element data = (Element) layerElem.getElementsByTagName("data").item(0);
                String csv = data.getTextContent().trim().replace("\n", "").replace("\r", "");
                int[][] grid = parseCSV(csv, mapWidth, mapHeight);

                Layer layer = new Layer();
                layer.name = layerName;
                layer.data = grid;

                // Classify layers based on name
                layer.isBackground = layerName.contains("background") ||
                        layerName.contains("ground") ||
                        layerName.contains("floor") ||
                        layerName.contains("base") ||
                        i == 0; // First layer is usually background

                layer.isForeground = layerName.contains("foreground") ||
                        layerName.contains("tree") ||
                        layerName.contains("roof") ||
                        layerName.contains("top") ||
                        layerName.contains("over");

                // NEW: Set collision layers based on your specified layer names
                layer.isCollision = layerName.contains("boat") ||
                        layerName.contains("army camp objects") ||
                        layerName.contains("army camp 1") ||
                        layerName.contains("fench") ||
                        layerName.contains("khet") ||
                        layerName.contains("river") ||
                        layerName.contains("river paar");

                layers.add(layer);
                System.out.println("Layer loaded: " + layerName +
                        " (Background: " + layer.isBackground +
                        ", Foreground: " + layer.isForeground +
                        ", Collision: " + layer.isCollision + ")");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // NEW: Collision detection method
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
                return true; // Found a collision tile
            }
        }

        return false; // No collision
    }

    // NEW: Check collision for a rectangle (player/NPC bounding box)
    public boolean isCollisionRect(double x, double y, double width, double height) {
        // Check all four corners of the rectangle
        return isCollisionTile(x, y) ||
                isCollisionTile(x + width, y) ||
                isCollisionTile(x, y + height) ||
                isCollisionTile(x + width, y + height);
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
        String[] tokens = csv.split(",");
        int index = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (index < tokens.length) {
                    String token = tokens[index++].trim();
                    result[y][x] = token.isEmpty() ? 0 : Integer.parseInt(token);
                } else {
                    result[y][x] = 0;
                }
            }
        }
        return result;
    }
}