package platformgame.Map;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import org.w3c.dom.*;
import platformgame.ImageLoader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.io.FileNotFoundException;
import java.util.*;

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
    private final Map<Integer, Image> tileImages = new HashMap<>();
    public Level_1() {
        loadMapData();

        preSliceTiles();
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

                layer.isCollision = layerName.contains("boat") ||
                        layerName.contains("army camp objects") ||
                        layerName.contains("army camp 1") ||
                        layerName.contains("fench") ||
                        layerName.contains("fenchgate") ||
                        layerName.contains("containergate") ||
                        layerName.contains("hostagewall02") ||
                        layerName.contains("hostagewall01") ||
                        layerName.contains("bridgefront") ||
                        layerName.contains("bridgeback") ||
                        layerName.contains("khet") ||
                        layerName.contains("river") ||
                        layerName.contains("river paar") ||
                        layerName.contains("people") ||
                        layerName.contains("trees collisions");

                layers.add(layer);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void preSliceTiles() {
        for (Tileset ts : tilesets) {
            int tilesPerRow = ts.columns;
            int rows = (int) (ts.image.getHeight() / ts.tileHeight);
            for (int localId = 0; localId < tilesPerRow * rows; localId++) {
                int gid = ts.firstGid + localId;
                int sx = (localId % tilesPerRow) * ts.tileWidth;
                int sy = (localId / tilesPerRow) * ts.tileHeight;
                Image tileImg = new WritableImage(
                        ts.image.getPixelReader(),
                        sx, sy, ts.tileWidth, ts.tileHeight
                );
                tileImages.put(gid, tileImg);
            }
        }
    }

    private void removeLayerByName(String targetName) {
        for (Layer layer : layers) {
            if (layer.name.equalsIgnoreCase(targetName)) {
                for (int y = 0; y < mapHeight; y++) {
                    for (int x = 0; x < mapWidth; x++) {
                        layer.data[y][x] = 0;
                    }
                }
                System.out.println("✅ Layer \"" + targetName + "\" cleared.");
                break;
            }
        }
    }

    /** Public entry‐points for each of your flags: */
    public void removeBridgeLayer() {
        removeLayerByName("bridgefront");
    }
    public void removeBridgeBackLayer() {
        removeLayerByName("bridgeback");
    }

    public void removeContainerGateLayer() {
        removeLayerByName("containergate");
    }

    public void removeFenchGateLayer() {
        removeLayerByName("fenchgate");
    }

    public void removeLeftWallLayer() {
        removeLayerByName("hostagegate01");
    }

    public void removeRightWallLayer() {
        removeLayerByName("hostagegate02");
    }



    public boolean isCollisionTile(double x, double y) {
        int tileX = (int) (x / tileWidth);
        int tileY = (int) (y / tileHeight);
        if (tileX < 0 || tileX >= mapWidth || tileY < 0 || tileY >= mapHeight) return true;

        for (Layer layer : layers) {
            if (layer.isCollision && layer.data[tileY][tileX] != 0) {
                return true;
            }
        }
        return false;
    }

    public boolean isCollisionRect(double x, double y, double width, double height) {
        int samples = 3;
        double stepX = width / (samples - 1);
        double stepY = height / (samples - 1);

        for (int i = 0; i < samples; i++) {
            for (int j = 0; j < samples; j++) {
                double checkX = x + (i * stepX);
                double checkY = y + (j * stepY);
                checkX = Math.min(checkX, x + width - 1);
                checkY = Math.min(checkY, y + height - 1);

                if (isCollisionTile(checkX, checkY)) return true;
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

    public void drawForeground(GraphicsContext gc, double camX, double camY, double scale) {
        for (Layer layer : layers) {
            if (layer.isForeground) {
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

    public void draw(GraphicsContext gc, double camX, double camY, double scale) {
        for (Layer layer : layers) {
            drawLayer(gc, layer.data, camX, camY, scale);
        }
    }

    private void drawLayer(GraphicsContext gc, int[][] grid, double camX, double camY, double scale) {
        for (int y = 0; y < mapHeight; y++) {
            for (int x = 0; x < mapWidth; x++) {
                Image tile = tileImages.get(grid[y][x]);
                if (tile != null) {
                    double dx = Math.floor((x * tileWidth - camX) * scale);
                    double dy = Math.floor((y * tileHeight - camY) * scale);
                    gc.drawImage(tile, dx, dy, tileWidth * scale, tileHeight * scale);
                }
            }
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
}
