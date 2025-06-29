package platformgame.Map;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import platformgame.ImageLoader;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.FileNotFoundException;

public class Level_2 {

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

    public Level_2() {
        loadMapData();
    }

    private void loadMapData() {
        try {
            InputStream tmxFileStream = getClass().getResourceAsStream("/Level_2/Level_2.tmx");
            if (tmxFileStream == null) throw new FileNotFoundException("TMX file not found");

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(tmxFileStream);

            Element mapElement = doc.getDocumentElement();
            mapWidth = Integer.parseInt(mapElement.getAttribute("width"));
            mapHeight = Integer.parseInt(mapElement.getAttribute("height"));

            NodeList tilesetList = doc.getElementsByTagName("tileset");
            for (int i = 0; i < tilesetList.getLength(); i++) {
                Element tilesetElem = (Element) tilesetList.item(i);
                if (tilesetElem.hasAttribute("source")) continue;

                int firstGid = Integer.parseInt(tilesetElem.getAttribute("firstgid"));
                String columnsAttr = tilesetElem.getAttribute("columns");
                int columns = columnsAttr.isEmpty() ? 16 : Integer.parseInt(columnsAttr);

                int tw = Integer.parseInt(tilesetElem.getAttribute("tilewidth"));
                int th = Integer.parseInt(tilesetElem.getAttribute("tileheight"));

                NodeList imageNodes = tilesetElem.getElementsByTagName("image");
                if (imageNodes.getLength() == 0) continue;

                Element imageElem = (Element) imageNodes.item(0);
                String imagePath = imageElem.getAttribute("source");
                String fileName = imagePath.substring(imagePath.lastIndexOf("/") + 1);

//                InputStream imageStream = getClass().getResourceAsStream("/Assets/" + fileName);
//                if (imageStream == null) {
//                    imageStream = getClass().getResourceAsStream("/Level_1/" + fileName);
//                    if (imageStream == null) continue;
//                }

                Image image = ImageLoader.load("/Assets/" + fileName);
                if (image == null) {
                    image = ImageLoader.load("/Level_1/" + fileName);
                    if (image == null) continue;
                }

                Tileset t = new Tileset();
                t.firstGid = firstGid;
                t.columns = columns;
                t.tileWidth = tw;
                t.tileHeight = th;
                t.image = image;

                tilesets.add(t);
            }

            tilesets.sort(Comparator.comparingInt(t -> t.firstGid));

            NodeList layerNodes = doc.getElementsByTagName("layer");
            for (int i = 0; i < layerNodes.getLength(); i++) {
                Element layerElem = (Element) layerNodes.item(i);
                String layerName = layerElem.getAttribute("name").toLowerCase();

                Element data = (Element) layerElem.getElementsByTagName("data").item(0);
                String csv = data.getTextContent().trim().replaceAll("\\s+", "");

                int[][] grid = parseCSV(csv, mapWidth, mapHeight);

                Layer layer = new Layer();
                layer.name = layerName;
                layer.data = grid;

                layer.isBackground = layerName.contains("background") ||
                        layerName.contains("ground") ||
                        layerName.contains("floor") ||
                        layerName.contains("base") ||
                        layerName.contains("water") ||
                        i == 0;

                layer.isForeground = layerName.contains("foreground") ||
                        (layerName.contains("tree") && !layerName.contains("collision"));

                layer.isCollision = layerName.contains("paar") ||
                        layerName.contains("armycamp") ||
                        layerName.contains("grassleft") ||
                        layerName.contains("chour") ||
                        layerName.contains("maati") ||
                        layerName.contains("port");

                layers.add(layer);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int[][] parseCSV(String csv, int width, int height) {
        int[][] result = new int[height][width];
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

    public boolean isCollisionTile(double x, double y) {
        int tileX = (int) (x / tileWidth);
        int tileY = (int) (y / tileHeight);
        if (tileX < 0 || tileX >= mapWidth || tileY < 0 || tileY >= mapHeight) {
            return true;
        }

        for (Layer layer : layers) {
            if (layer.isCollision && layer.data[tileY][tileX] != 0) {
                return true;
            }
        }
        return false;
    }




    // NEW COLLISION SYSTEM: Rectangle-based collision detection
    public boolean checkCollisionWithRectangle(double x, double y, double width, double height) {
        // Create a smaller collision rectangle
        double margin = 20.0; // Adjust this to make collision box smaller/larger
        double collisionX = x + margin;
        double collisionY = y + margin;
        double collisionWidth = width - (2 * margin);
        double collisionHeight = height - (2 * margin);

        // Ensure collision box isn't too small
        if (collisionWidth <= 0) collisionWidth = width * 0.6;
        if (collisionHeight <= 0) collisionHeight = height * 0.6;

        // Check if any part of the collision rectangle overlaps with collision tiles
        return isRectangleCollidingWithTiles(collisionX, collisionY, collisionWidth, collisionHeight);
    }


    // Helper method to check if rectangle collides with tiles
// Helper method to check if rectangle collides with tiles
    private boolean isRectangleCollidingWithTiles(double rectX, double rectY, double rectWidth, double rectHeight) {
        // Get the tile bounds of the rectangle
        int startTileX = (int) Math.floor(rectX / tileWidth);
        int endTileX = (int) Math.floor((rectX + rectWidth - 1) / tileWidth);
        int startTileY = (int) Math.floor(rectY / tileHeight);
        int endTileY = (int) Math.floor((rectY + rectHeight - 1) / tileHeight);

        // Check all tiles that the rectangle overlaps
        for (int tileY = startTileY; tileY <= endTileY; tileY++) {
            for (int tileX = startTileX; tileX <= endTileX; tileX++) {
                // Check for collision at the center of each tile
                if (isCollisionTile(tileX * tileWidth + tileWidth / 2, tileY * tileHeight + tileHeight / 2)) {
                    return true;
                }
            }
        }
        return false;
    }



    public void drawBackground(GraphicsContext gc, double camX, double camY, double scale) {
        for (Layer layer : layers) {
            if (layer.isBackground) {
                drawLayer(gc, layer.data, camX, camY, scale);
            }
        }
    }

    public void drawMiddleground(GraphicsContext gc, double camX, double camY, double scale) {
        for (Layer layer : layers) {
            if (!layer.isBackground && !layer.isForeground) {
                drawLayer(gc, layer.data, camX, camY, scale);
            }
        }
    }

    public void drawForeground(GraphicsContext gc, double camX, double camY, double scale) {
        for (Layer layer : layers) {
            if (layer.isForeground) {
                drawLayer(gc, layer.data, camX, camY, scale);
            }
        }
    }

    // Helper function to draw a single layer based on tile data
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
}