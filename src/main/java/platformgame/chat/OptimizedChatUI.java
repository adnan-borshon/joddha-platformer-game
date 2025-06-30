// src/platformgame/chat/OptimizedChatUI.java
package platformgame.chat;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import platformgame.Game;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class OptimizedChatUI {
    private final Game game;
    private ChatClient chatClient;
    private boolean isConnected = false;
    private AtomicBoolean chatVisible = new AtomicBoolean(false);

    // UI Components
    private Button toggleButton;
    private VBox chatContainer;
    private TextArea messageArea;
    private TextField inputField;
    private Label ammoLabel;
    private Label statusLabel;
    private Button sendButton;
    private VBox ammoSystemPanel;
    private TextField ammoRequestField;
    private Button requestAmmoButton;
    private ComboBox<String> playerComboBox;
    private TextField sendAmmoField;
    private Button sendAmmoButton;

    // Configuration
    private static final String DEFAULT_USERNAME = "Player" + System.currentTimeMillis() % 1000;

    public OptimizedChatUI(Game game) {
        this.game = game;
        initializeUI();
        setupEventHandlers();
    }

    private void initializeUI() {
        // Create toggle button
        toggleButton = new Button("💬");
        toggleButton.setStyle(
                "-fx-background-color: #4CAF50; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 14px; " +
                        "-fx-min-width: 40px; " +
                        "-fx-min-height: 30px; " +
                        "-fx-background-radius: 5px;"
        );
        toggleButton.setTooltip(new Tooltip("Toggle Chat (T)"));

        // Create main chat container
        chatContainer = new VBox(5);
        chatContainer.setPrefWidth(300);
        chatContainer.setPrefHeight(400);
        chatContainer.setStyle(
                "-fx-background-color: rgba(0, 0, 0, 0.8); " +
                        "-fx-background-radius: 10px; " +
                        "-fx-padding: 10px; " +
                        "-fx-border-color: #333; " +
                        "-fx-border-width: 1px; " +
                        "-fx-border-radius: 10px;"
        );
        chatContainer.setVisible(false);

        // Status label
        statusLabel = new Label("Disconnected");
        statusLabel.setTextFill(Color.RED);
        statusLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));

        // Ammo display
        ammoLabel = new Label("Ammo: 0");
        ammoLabel.setTextFill(Color.YELLOW);
        ammoLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        // Message area
        messageArea = new TextArea();
        messageArea.setPrefHeight(200);
        messageArea.setEditable(false);
        messageArea.setWrapText(true);
        messageArea.setStyle(
                "-fx-control-inner-background: #1a1a1a; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 11px; " +
                        "-fx-background-color: #1a1a1a; " +
                        "-fx-border-color: #333; " +
                        "-fx-border-radius: 5px;"
        );
        messageArea.appendText("=== GAME CHAT ===\n");
        messageArea.appendText("Press T to toggle chat\n");
        messageArea.appendText("Connecting to server...\n\n");

        // Input field and send button
        HBox inputBox = new HBox(5);
        inputField = new TextField();
        inputField.setPrefWidth(200);
        inputField.setPromptText("Type message...");
        inputField.setStyle(
                "-fx-background-color: #2a2a2a; " +
                        "-fx-text-fill: white; " +
                        "-fx-border-color: #555; " +
                        "-fx-border-radius: 3px;"
        );

        sendButton = new Button("Send");
        sendButton.setStyle(
                "-fx-background-color: #2196F3; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 10px;"
        );

        inputBox.getChildren().addAll(inputField, sendButton);
        inputBox.setAlignment(Pos.CENTER_LEFT);

        // Ammo system panel
        createAmmoSystemPanel();

        // Add all components to container
        VBox headerBox = new VBox(2);
        headerBox.getChildren().addAll(statusLabel, ammoLabel);

        chatContainer.getChildren().addAll(
                headerBox,
                new Separator(),
                messageArea,
                inputBox,
                new Separator(),
                ammoSystemPanel
        );
    }

    private void createAmmoSystemPanel() {
        ammoSystemPanel = new VBox(5);
        ammoSystemPanel.setStyle("-fx-padding: 5px; -fx-background-color: rgba(30, 30, 30, 0.9); -fx-background-radius: 5px;");

        Label ammoTitle = new Label("AMMO SYSTEM");
        ammoTitle.setTextFill(Color.ORANGE);
        ammoTitle.setFont(Font.font("Arial", FontWeight.BOLD, 10));

        // Request ammo section
        HBox requestBox = new HBox(3);
        ammoRequestField = new TextField();
        ammoRequestField.setPrefWidth(60);
        ammoRequestField.setPromptText("Amount");
        ammoRequestField.setStyle("-fx-background-color: #2a2a2a; -fx-text-fill: white; -fx-font-size: 9px;");

        requestAmmoButton = new Button("Request");
        requestAmmoButton.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-size: 9px;");

        requestBox.getChildren().addAll(new Label("Request:"), ammoRequestField, requestAmmoButton);
        requestBox.setAlignment(Pos.CENTER_LEFT);

        // Send ammo section
        HBox sendBox = new HBox(3);
        playerComboBox = new ComboBox<>();
        playerComboBox.setPrefWidth(80);
        playerComboBox.setPromptText("Player");
        playerComboBox.setStyle("-fx-background-color: #2a2a2a; -fx-text-fill: white; -fx-font-size: 9px;");

        sendAmmoField = new TextField();
        sendAmmoField.setPrefWidth(50);
        sendAmmoField.setPromptText("Amt");
        sendAmmoField.setStyle("-fx-background-color: #2a2a2a; -fx-text-fill: white; -fx-font-size: 9px;");

        sendAmmoButton = new Button("Send");
        sendAmmoButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 9px;");

        sendBox.getChildren().addAll(new Label("Send:"), playerComboBox, sendAmmoField, sendAmmoButton);
        sendBox.setAlignment(Pos.CENTER_LEFT);

        ammoSystemPanel.getChildren().addAll(ammoTitle, requestBox, sendBox);

        // Style labels in ammo panel
        ammoSystemPanel.getChildren().stream()
                .filter(node -> node instanceof HBox)
                .forEach(hbox -> ((HBox) hbox).getChildren().stream()
                        .filter(node -> node instanceof Label)
                        .forEach(label -> {
                            ((Label) label).setTextFill(Color.LIGHTGRAY);
                            ((Label) label).setFont(Font.font("Arial", 9));
                        }));
    }

    private void setupEventHandlers() {
        // Toggle button
        toggleButton.setOnAction(e -> toggleChat());

        // Send message
        sendButton.setOnAction(e -> sendMessage());
        inputField.setOnAction(e -> sendMessage());

        // Ammo system handlers
        requestAmmoButton.setOnAction(e -> requestAmmo());
        sendAmmoButton.setOnAction(e -> sendAmmo());
    }

    public void connectToServer() {
        if (isConnected) return;

        try {
            // Create ChatClient with the correct constructor (username only)
            chatClient = new ChatClient(DEFAULT_USERNAME);

            // Set up message callback
            chatClient.setMessageCallback(new ChatClient.ChatMessageCallback() {
                @Override
                public void onMessageReceived(String message) {
                    Platform.runLater(() -> {
                        messageArea.appendText(message + "\n");
                        messageArea.setScrollTop(Double.MAX_VALUE);
                    });
                }

                @Override
                public void onConnectionStatusChanged(boolean connected) {
                    Platform.runLater(() -> {
                        isConnected = connected;
                        statusLabel.setText(connected ? "Connected" : "Disconnected");
                        statusLabel.setTextFill(connected ? Color.GREEN : Color.RED);

                        if (connected) {
                            messageArea.appendText("✅ Connected to server!\n");
                            updateAmmo(game.getPlayerAmmo());
                            // Start requesting player list periodically
                            startPlayerListUpdates();
                        } else {
                            messageArea.appendText("❌ Disconnected from server\n");
                            stopPlayerListUpdates();
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    Platform.runLater(() -> {
                        messageArea.appendText("❌ Error: " + error + "\n");
                        statusLabel.setText("Error");
                        statusLabel.setTextFill(Color.RED);
                    });
                }
            });

            // Set up ammo system callback
            chatClient.setAmmoCallback(new ChatClient.AmmoSystemCallback() {
                @Override
                public void onAmmoRequest(String requester, int amount, String message) {
                    Platform.runLater(() -> {
                        String msg = "🔫 " + requester + " requests " + amount + " ammo";
                        if (!message.isEmpty()) msg += ": " + message;
                        messageArea.appendText(msg + "\n");
                    });
                }

                @Override
                public void onAmmoTransfer(boolean sent, String otherPlayer, int amount) {
                    Platform.runLater(() -> {
                        String action = sent ? "sent to" : "received from";
                        messageArea.appendText("🎯 " + action + " " + otherPlayer + ": " + amount + " ammo\n");

                        // Update local ammo count
                        if (sent) {
                            game.onAmmoUsed(amount);
                        } else {
                            game.onAmmoReceived(otherPlayer, amount);
                        }
                        // Update the display
                        updateAmmo(game.getPlayerAmmo());
                    });
                }

                @Override
                public void onPlayerListUpdate(String playerListData) {
                    Platform.runLater(() -> updatePlayerList(playerListData));
                }

                @Override
                public void onSystemMessage(String message) {
                    Platform.runLater(() -> {
                        messageArea.appendText("🔔 " + message + "\n");
                    });
                }
            });

            // Connect to server
            chatClient.connect();

        } catch (Exception e) {
            Platform.runLater(() -> {
                messageArea.appendText("❌ Connection failed: " + e.getMessage() + "\n");
                statusLabel.setText("Connection Failed");
                statusLabel.setTextFill(Color.RED);
                isConnected = false;
            });
        }
    }

    // Player list update management
    private Thread playerListThread;
    private volatile boolean shouldUpdatePlayerList = false;

    private void startPlayerListUpdates() {
        shouldUpdatePlayerList = true;
        playerListThread = new Thread(() -> {
            while (shouldUpdatePlayerList && isConnected) {
                try {
                    if (chatClient != null) {
                        chatClient.requestPlayerList();
                    }
                    Thread.sleep(5000); // Update every 5 seconds
                } catch (InterruptedException e) {
                    break;
                } catch (IOException e) {
                    System.err.println("Failed to request player list: " + e.getMessage());
                }
            }
        });
        playerListThread.setDaemon(true);
        playerListThread.start();
    }

    private void stopPlayerListUpdates() {
        shouldUpdatePlayerList = false;
        if (playerListThread != null) {
            playerListThread.interrupt();
        }
    }

    public void disconnect() {
        stopPlayerListUpdates();
        if (chatClient != null && isConnected) {
            chatClient.disconnect();
            isConnected = false;
        }
    }

    public void toggleChat() {
        boolean visible = chatVisible.get();
        chatVisible.set(!visible);
        chatContainer.setVisible(!visible);

        if (!visible) {
            // Opening chat
            if (!isConnected) {
                connectToServer();
            }
            Platform.runLater(() -> {
                inputField.requestFocus();
                inputField.positionCaret(inputField.getText().length());
            });
        } else {
            // Closing chat
            Platform.runLater(() -> {
                inputField.clear();
                // Return focus to the game
                if (game != null) {
                    game.requestFocus();
                }
            });
        }
    }

    private void sendMessage() {
        String message = inputField.getText().trim();
        if (message.isEmpty() || !isConnected || chatClient == null) return;

        try {
            chatClient.sendMessage(message);
            inputField.clear();
        } catch (Exception e) {
            messageArea.appendText("❌ Failed to send message: " + e.getMessage() + "\n");
        }
    }

    public void sendGameEvent(String event) {
        if (!isConnected || chatClient == null) return;

        try {
            chatClient.sendCustomMessage("🎮 " + event);
        } catch (Exception e) {
            System.err.println("Failed to send game event: " + e.getMessage());
        }
    }

    public void updateAmmo(int ammoCount) {
        Platform.runLater(() -> {
            ammoLabel.setText("Ammo: " + ammoCount);

            if (isConnected && chatClient != null) {
                try {
                    chatClient.updateAmmoCount(ammoCount);
                } catch (Exception e) {
                    System.err.println("Failed to update ammo on server: " + e.getMessage());
                }
            }
        });
    }

    private void requestAmmo() {
        String amountStr = ammoRequestField.getText().trim();
        if (amountStr.isEmpty() || !isConnected || chatClient == null) return;

        try {
            int amount = Integer.parseInt(amountStr);
            if (amount > 0) {
                chatClient.requestAmmo(amount, "Need ammo please!");
                ammoRequestField.clear();
                messageArea.appendText("📤 Requested " + amount + " ammo from other players\n");
            }
        } catch (NumberFormatException e) {
            messageArea.appendText("❌ Invalid ammo amount\n");
        } catch (Exception e) {
            messageArea.appendText("❌ Request failed: " + e.getMessage() + "\n");
        }
    }

    private void sendAmmo() {
        String recipient = playerComboBox.getValue();
        String amountStr = sendAmmoField.getText().trim();

        if (recipient == null || amountStr.isEmpty() || !isConnected || chatClient == null) return;

        try {
            int amount = Integer.parseInt(amountStr);
            if (amount > 0 && game.getPlayerAmmo() >= amount) {
                chatClient.sendAmmo(recipient, amount);
                sendAmmoField.clear();
                messageArea.appendText("📤 Sent " + amount + " ammo to " + recipient + "\n");
            } else {
                messageArea.appendText("❌ Not enough ammo to send\n");
            }
        } catch (NumberFormatException e) {
            messageArea.appendText("❌ Invalid ammo amount\n");
        } catch (Exception e) {
            messageArea.appendText("❌ Send failed: " + e.getMessage() + "\n");
        }
    }

    private void updatePlayerList(String playerListData) {
        playerComboBox.getItems().clear();

        if (playerListData == null || playerListData.isEmpty()) return;

        String[] players = playerListData.split(",");
        for (String playerData : players) {
            if (!playerData.trim().isEmpty()) {
                String[] parts = playerData.split(":");
                if (parts.length >= 1) {
                    String playerName = parts[0].trim();
                    if (!playerName.equals(chatClient.getUsername())) {
                        playerComboBox.getItems().add(playerName);
                    }
                }
            }
        }
    }

    // Key handling - only handle specific keys when chat is visible
    public boolean handleKeyEvent(KeyCode keyCode) {
        // If chat is not visible, only handle the toggle key
        if (!chatVisible.get() &&
                keyCode != KeyCode.PLUS &&
                keyCode != KeyCode.EQUALS &&
                keyCode != KeyCode.MINUS &&
                keyCode != KeyCode.UNDERSCORE) {
            return false; // Let the game handle all other keys when chat is closed
        }

        // Chat is visible - handle specific keys
        switch (keyCode) {
            case ESCAPE:
                toggleChat(); // Close chat
                return true; // Consumed

            case ENTER:
                if (inputField.isFocused()) {
                    sendMessage();
                    return true; // Consumed
                }
                return false; // Not consumed - let game handle it

            case TAB:
                // Handle tab navigation within chat
                if (inputField.isFocused()) {
                    // You can add tab handling logic here if needed
                    return true;
                }
                return false;
            case PLUS:
            case EQUALS:
                game.onAmmoCollected(10);
                break;

            case MINUS:
                game.onAmmoUsed(10);
                break;
            case UNDERSCORE:
            default:
                // Only consume alphanumeric and special keys if input field is focused
                if (inputField.isFocused()) {
                    return isTypingKey(keyCode);
                }
        }
                return false; // Not consumed

    }

    // Helper method to determine if a key is for typing
    private boolean isTypingKey(KeyCode keyCode) {
        // Return true for keys that should be handled by the text input
        return keyCode.isLetterKey() ||
                keyCode.isDigitKey() ||
                keyCode == KeyCode.SPACE ||
                keyCode == KeyCode.BACK_SPACE ||
                keyCode == KeyCode.DELETE ||
                keyCode == KeyCode.LEFT ||
                keyCode == KeyCode.RIGHT ||
                keyCode == KeyCode.HOME ||
                keyCode == KeyCode.END ||
                keyCode == KeyCode.SHIFT ||
                keyCode == KeyCode.CONTROL ||
                keyCode.getName().length() == 1; // Single character keys
    }

    public boolean isChatInputFocused() {
        return inputField != null && inputField.isFocused();
    }

    // Getters for Game class integration
    public Button getToggleButton() {
        return toggleButton;
    }

    public VBox getChatContainer() {
        return chatContainer;
    }

    public boolean isVisible() {
        return chatVisible.get();
    }

    public boolean isConnected() {
        return isConnected;
    }

    public String getUsername() {
        return chatClient != null ? chatClient.getUsername() : DEFAULT_USERNAME;
    }
}