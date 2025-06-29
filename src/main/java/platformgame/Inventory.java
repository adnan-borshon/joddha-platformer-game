package platformgame;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class Inventory {
    private Map<String, Integer> items;
    private File inventoryFile;
    private boolean isVisible = false;
    private VBox inventoryUI;
    private GridPane itemGrid;
    private Label titleLabel;

    // UI dimensions and positioning
    private static final int INVENTORY_WIDTH = 300;
    private static final int INVENTORY_HEIGHT = 400;
    private static final int GRID_COLUMNS = 2;

    public Inventory(String fileName) {
        items = new HashMap<>();
        inventoryFile = new File(fileName);
        loadInventory();
        createInventoryUI();
    }

    // Create the JavaFX UI for the inventory
    private void createInventoryUI() {
        inventoryUI = new VBox(10);
        inventoryUI.setPrefSize(INVENTORY_WIDTH, INVENTORY_HEIGHT);
        inventoryUI.setMaxSize(INVENTORY_WIDTH, INVENTORY_HEIGHT);
        inventoryUI.setAlignment(Pos.TOP_CENTER);
        inventoryUI.setPadding(new Insets(15));

        // Style the inventory background
        inventoryUI.setBackground(new Background(new BackgroundFill(
                Color.color(0.1, 0.1, 0.2, 0.95), // Dark blue with transparency
                new CornerRadii(10),
                Insets.EMPTY
        )));

        // Title
        titleLabel = new Label("INVENTORY");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        titleLabel.setTextFill(Color.WHITE);
        titleLabel.setAlignment(Pos.CENTER);

        // Item grid
        itemGrid = new GridPane();
        itemGrid.setHgap(10);
        itemGrid.setVgap(8);
        itemGrid.setAlignment(Pos.TOP_CENTER);

        inventoryUI.getChildren().addAll(titleLabel, itemGrid);

        // Initially hide the inventory
        inventoryUI.setVisible(false);

        updateInventoryDisplay();
    }

    // Update the visual display of inventory items
    private void updateInventoryDisplay() {
        Platform.runLater(() -> {
            itemGrid.getChildren().clear();

            int row = 0;
            int col = 0;

            for (Map.Entry<String, Integer> entry : items.entrySet()) {
                String itemName = entry.getKey();
                int quantity = entry.getValue();

                if (quantity > 0) { // Only show items with quantity > 0
                    // Create item display
                    VBox itemBox = createItemDisplay(itemName, quantity);
                    itemGrid.add(itemBox, col, row);

                    col++;
                    if (col >= GRID_COLUMNS) {
                        col = 0;
                        row++;
                    }
                }
            }

            // Add empty message if no items
            if (items.isEmpty() || items.values().stream().allMatch(q -> q <= 0)) {
                Label emptyLabel = new Label("Inventory is empty");
                emptyLabel.setTextFill(Color.LIGHTGRAY);
                emptyLabel.setFont(Font.font("Arial", 14));
                itemGrid.add(emptyLabel, 0, 0, GRID_COLUMNS, 1);
            }
        });
    }

    // Create visual representation of an item
    private VBox createItemDisplay(String itemName, int quantity) {
        VBox itemBox = new VBox(3);
        itemBox.setAlignment(Pos.CENTER);
        itemBox.setPadding(new Insets(8));
        itemBox.setBackground(new Background(new BackgroundFill(
                Color.color(0.2, 0.2, 0.3, 0.8),
                new CornerRadii(5),
                Insets.EMPTY
        )));

        // Item icon/name
        Label nameLabel = new Label(getItemDisplayName(itemName));
        nameLabel.setTextFill(Color.WHITE);
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));

        // Quantity
        Label quantityLabel = new Label("x" + quantity);
        quantityLabel.setTextFill(getItemColor(itemName));
        quantityLabel.setFont(Font.font("Arial", FontWeight.BOLD, 11));

        itemBox.getChildren().addAll(nameLabel, quantityLabel);
        return itemBox;
    }

    // Get display name for items
    private String getItemDisplayName(String itemName) {
        switch (itemName.toLowerCase()) {
            case "ammo": return "🔫 Ammo";
            case "key1": return "🗝️ Key 1";
            case "key2": return "🗝️ Key 2";
            case "key3": return "🗝️ Key 3";
            case "boom": return "💣 Boom";
            case "launcher": return "🚀 Launcher";
            default: return itemName;
        }
    }

    // Get color for different item types
    private Color getItemColor(String itemName) {
        switch (itemName.toLowerCase()) {
            case "ammo": return Color.YELLOW;
            case "key1":
            case "key2":
            case "key3": return Color.GOLD;
            case "boom": return Color.RED;
            case "launcher": return Color.ORANGE;
            default: return Color.WHITE;
        }
    }

    // Toggle inventory visibility
    public void toggleVisibility() {
        isVisible = !isVisible;
        Platform.runLater(() -> {
            inventoryUI.setVisible(isVisible);
            if (isVisible) {
                updateInventoryDisplay();
            }
        });
        System.out.println("🎒 Inventory " + (isVisible ? "opened" : "closed"));
    }

    // Show inventory
    public void show() {
        isVisible = true;
        Platform.runLater(() -> {
            inventoryUI.setVisible(true);
            updateInventoryDisplay();
        });
    }

    // Hide inventory
    public void hide() {
        isVisible = false;
        Platform.runLater(() -> inventoryUI.setVisible(false));
    }

    // Load inventory from file
    public void loadInventory() {
        if (!inventoryFile.exists()) {
            // Initialize with default values if file doesn't exist
            items.put("ammo", 0);
            items.put("key1", 0);
            items.put("key2", 0);
            items.put("key3", 0);
            items.put("boom", 0);
            items.put("launcher", 0);
            saveInventory();
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(inventoryFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    String item = parts[0].trim();
                    int quantity = Integer.parseInt(parts[1].trim());
                    items.put(item, quantity);
                }
            }
            System.out.println("📦 Inventory loaded from file: " + inventoryFile.getName());
        } catch (IOException | NumberFormatException e) {
            System.err.println("❌ Error loading inventory: " + e.getMessage());
            // Initialize with defaults if loading fails
            initializeDefaults();
        }
    }

    // Initialize default inventory items
    private void initializeDefaults() {
        items.put("ammo", 0);
        items.put("key1", 0);
        items.put("key2", 0);
        items.put("key3", 0);
        items.put("boom", 0);
        items.put("launcher", 0);
    }

    // Save inventory to file
    public void saveInventory() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(inventoryFile))) {
            for (Map.Entry<String, Integer> entry : items.entrySet()) {
                writer.write(entry.getKey() + ":" + entry.getValue());
                writer.newLine();
            }
            System.out.println("💾 Inventory saved to file: " + inventoryFile.getName());
        } catch (IOException e) {
            System.err.println("❌ Error saving inventory: " + e.getMessage());
        }
    }

    // Add item to inventory
    public void addItem(String item, int quantity) {
        if (quantity <= 0) return;

        int oldQuantity = items.getOrDefault(item, 0);
        items.put(item, oldQuantity + quantity);
        saveInventory();
        updateInventoryDisplay();

        System.out.println("➕ Added " + quantity + " " + item + " to inventory (Total: " + items.get(item) + ")");
    }

    // Use/remove item from inventory
    public boolean useItem(String item, int quantity) {
        if (quantity <= 0) return false;

        int currentQuantity = items.getOrDefault(item, 0);
        if (currentQuantity >= quantity) {
            items.put(item, currentQuantity - quantity);
            saveInventory();
            updateInventoryDisplay();

            System.out.println("➖ Used " + quantity + " " + item + " from inventory (Remaining: " + items.get(item) + ")");
            return true;
        }

        System.out.println("⚠️ Not enough " + item + " in inventory! Current: " + currentQuantity + ", Required: " + quantity);
        return false;
    }

    // Get quantity of specific item
    public int getItemQuantity(String item) {
        return items.getOrDefault(item, 0);
    }

    // Check if item exists in sufficient quantity
    public boolean hasItem(String item, int quantity) {
        return getItemQuantity(item) >= quantity;
    }

    // Get inventory display as string (for debugging)
    public String getInventoryDisplay() {
        StringBuilder inventoryText = new StringBuilder("INVENTORY:\n");
        for (Map.Entry<String, Integer> entry : items.entrySet()) {
            if (entry.getValue() > 0) {
                inventoryText.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
        }
        return inventoryText.toString();
    }

    // Get the UI component for adding to game scene
    public VBox getInventoryUI() {
        return inventoryUI;
    }

    // Check if inventory is currently visible
    public boolean isVisible() {
        return isVisible;
    }

    // Position inventory UI on screen
    public void setPosition(double x, double y) {
        Platform.runLater(() -> {
            inventoryUI.setLayoutX(x);
            inventoryUI.setLayoutY(y);
        });
    }

    // Clear all items (for testing or reset)
    public void clearInventory() {
        items.clear();
        initializeDefaults();
        saveInventory();
        updateInventoryDisplay();
        System.out.println("🗑️ Inventory cleared");
    }

    // Get total number of different item types
    public int getUniqueItemCount() {
        return (int) items.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .count();
    }

    // Method to handle key collection (specific to your game)
    public void collectKey(int keyNumber) {
        addItem("key" + keyNumber, 1);
    }

    // Method to check if specific key is available
    public boolean hasKey(int keyNumber) {
        return hasItem("key" + keyNumber, 1);
    }

    // Method to use a key
    public boolean useKey(int keyNumber) {
        return useItem("key" + keyNumber, 1);
    }
}