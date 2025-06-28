package platformgame;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import platformgame.chat.ChatClient;
import java.util.HashMap;
import java.util.Map;

public class ChatUI {
    private VBox mainContainer; // Container to hold both button and chat
    private VBox chatContainer;
    private ScrollPane chatScrollPane;
    private VBox chatMessages;
    private TextField messageInput;
    private Button sendButton;
    private Button toggleButton;
    private Label connectionStatus;
    private ChatClient chatClient;
    private boolean isVisible = false;
    private Game game;

    // Ammo system UI components
    private VBox ammoPanel;
    private Label ammoCountLabel;
    private TextField ammoRequestField;
    private TextField ammoRequestMessageField;
    private Button requestAmmoButton;
    private ComboBox<String> playerSelector;
    private TextField ammoSendField;
    private Button sendAmmoButton;
    private Button refreshPlayersButton;
    private Map<String, Integer> onlinePlayers;

    // Chat dimensions
    private final double CHAT_WIDTH = 320;
    private final double CHAT_HEIGHT = 500;
    private final double BUTTON_OFFSET_X = -50;

    // Player's current ammo count (displayed in UI, real value in Game)
    private int currentAmmo = 0;

    public ChatUI(Game game) {
        this.game = game;
        this.onlinePlayers = new HashMap<>();
        initializeUI();
        initializeChatClient();
    }

    private void initializeUI() {
        mainContainer = new VBox(5);
        mainContainer.setAlignment(Pos.TOP_LEFT);

        toggleButton = new Button("Chat/Ammo");
        toggleButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold;");
        toggleButton.setPrefSize(80, 30);

        // NEW toggle button container (top-right)
        HBox buttonContainer = new HBox();
        buttonContainer.setAlignment(Pos.TOP_RIGHT);
// 10px padding from top and right edges:
        buttonContainer.setPadding(new Insets(40, 10, 0, 0));
        buttonContainer.getChildren().add(toggleButton);


        chatContainer = new VBox(5);
        chatContainer.setPrefSize(CHAT_WIDTH, CHAT_HEIGHT);
        chatContainer.setMaxSize(CHAT_WIDTH, CHAT_HEIGHT);
        chatContainer.setStyle("-fx-background-color: rgba(0, 0, 0, 0.9); -fx-background-radius: 10;");
        chatContainer.setPadding(new Insets(10));
        chatContainer.setVisible(false);

        HBox chatContainerWrapper = new HBox();
        chatContainerWrapper.setAlignment(Pos.CENTER_LEFT);
        chatContainerWrapper.setPadding(new Insets(0, 0, 0, Math.abs(BUTTON_OFFSET_X)));
        chatContainerWrapper.getChildren().add(chatContainer);

        HBox header = createHeader();
        ammoPanel = createAmmoPanel();
        chatMessages = new VBox(3);
        chatMessages.setPadding(new Insets(5));

        chatScrollPane = new ScrollPane(chatMessages);
        chatScrollPane.setPrefSize(CHAT_WIDTH - 20, 200);
        chatScrollPane.setStyle("-fx-background: transparent; -fx-background-color: rgba(255, 255, 255, 0.1);");
        chatScrollPane.setFitToWidth(true);
        chatScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        chatScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        HBox inputArea = createInputArea();

        chatContainer.getChildren().addAll(header, ammoPanel, new Separator(), chatScrollPane, inputArea);
        mainContainer.getChildren().addAll(buttonContainer, chatContainerWrapper);

        setupEventHandlers();
    }

    private HBox createHeader() {
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label chatTitle = new Label("Game Chat & Ammo");
        chatTitle.setTextFill(Color.WHITE);
        chatTitle.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        connectionStatus = new Label("Disconnected");
        connectionStatus.setTextFill(Color.RED);
        connectionStatus.setFont(Font.font("Arial", 10));

        header.getChildren().addAll(chatTitle, connectionStatus);
        return header;
    }

    private VBox createAmmoPanel() {
        VBox panel = new VBox(5);
        panel.setStyle("-fx-background-color: rgba(255, 255, 255, 0.1); -fx-background-radius: 5;");
        panel.setPadding(new Insets(8));

        HBox ammoDisplay = new HBox(10);
        ammoDisplay.setAlignment(Pos.CENTER_LEFT);
        Label ammoLabel = new Label("Current Ammo:");
        ammoLabel.setTextFill(Color.WHITE);
        ammoLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));

        ammoCountLabel = new Label("0");
        ammoCountLabel.setTextFill(Color.YELLOW);
        ammoCountLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        ammoDisplay.getChildren().addAll(ammoLabel, ammoCountLabel);

        Label requestLabel = new Label("Request Ammo:");
        requestLabel.setTextFill(Color.WHITE);
        requestLabel.setFont(Font.font("Arial", FontWeight.BOLD, 11));

        HBox requestRow1 = new HBox(5);
        requestRow1.setAlignment(Pos.CENTER_LEFT);

        ammoRequestField = new TextField();
        ammoRequestField.setPromptText("Amount");
        ammoRequestField.setPrefWidth(60);

        ammoRequestMessageField = new TextField();
        ammoRequestMessageField.setPromptText("Reason (optional)");
        ammoRequestMessageField.setPrefWidth(120);

        requestAmmoButton = new Button("Request");
        requestAmmoButton.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white;");
        requestAmmoButton.setPrefWidth(70);

        requestRow1.getChildren().addAll(ammoRequestField, ammoRequestMessageField, requestAmmoButton);

        Label sendLabel = new Label("Send Ammo:");
        sendLabel.setTextFill(Color.WHITE);
        sendLabel.setFont(Font.font("Arial", FontWeight.BOLD, 11));

        HBox sendRow1 = new HBox(5);
        sendRow1.setAlignment(Pos.CENTER_LEFT);

        playerSelector = new ComboBox<>();
        playerSelector.setPromptText("Select Player");
        playerSelector.setPrefWidth(120);

        refreshPlayersButton = new Button("↻");
        refreshPlayersButton.setStyle("-fx-background-color: #607D8B; -fx-text-fill: white;");
        refreshPlayersButton.setPrefWidth(25);
        refreshPlayersButton.setTooltip(new Tooltip("Refresh player list"));

        sendRow1.getChildren().addAll(playerSelector, refreshPlayersButton);

        HBox sendRow2 = new HBox(5);
        sendRow2.setAlignment(Pos.CENTER_LEFT);

        ammoSendField = new TextField();
        ammoSendField.setPromptText("Amount");
        ammoSendField.setPrefWidth(80);

        sendAmmoButton = new Button("Send Ammo");
        sendAmmoButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        sendAmmoButton.setPrefWidth(90);

        sendRow2.getChildren().addAll(ammoSendField, sendAmmoButton);

        panel.getChildren().addAll(
                ammoDisplay,
                new Separator(),
                requestLabel,
                requestRow1,
                new Separator(),
                sendLabel,
                sendRow1,
                sendRow2
        );

        return panel;
    }

    private HBox createInputArea() {
        HBox inputArea = new HBox(5);
        inputArea.setAlignment(Pos.CENTER);

        messageInput = new TextField();
        messageInput.setPrefWidth(CHAT_WIDTH - 70);
        messageInput.setPromptText("Type your message...");
        messageInput.setStyle("-fx-background-color: rgba(255, 255, 255, 0.9);");

        sendButton = new Button("Send");
        sendButton.setPrefWidth(50);
        sendButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");

        inputArea.getChildren().addAll(messageInput, sendButton);
        return inputArea;
    }

    private void setupEventHandlers() {
        toggleButton.setOnAction(e -> toggleChatVisibility());
        sendButton.setOnAction(e -> sendMessage());
        messageInput.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) sendMessage();
        });
        requestAmmoButton.setOnAction(e -> requestAmmo());
        sendAmmoButton.setOnAction(e -> sendAmmo());
        refreshPlayersButton.setOnAction(e -> refreshPlayerList());

        ammoRequestField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) requestAmmo();
        });
        ammoSendField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) sendAmmo();
        });
    }

    private void initializeChatClient() {
        String username = "Player" + (int)(Math.random() * 1000);
        chatClient = new ChatClient("192.168.0.110", 8080, username);

        chatClient.setMessageCallback(new ChatClient.ChatMessageCallback() {
            @Override
            public void onMessageReceived(String message) {
                Platform.runLater(() -> addMessage(message, false));
            }
            @Override
            public void onConnectionStatusChanged(boolean connected) {
                Platform.runLater(() -> {
                    if (connected) {
                        connectionStatus.setText("Connected");
                        connectionStatus.setTextFill(Color.GREEN);
                        addMessage("Connected to chat server", true);
                        updateAmmoOnServer();
                        refreshPlayerList();
                    } else {
                        connectionStatus.setText("Disconnected");
                        connectionStatus.setTextFill(Color.RED);
                        addMessage("Disconnected from chat server", true);
                    }
                });
            }
            @Override
            public void onError(String error) {
                Platform.runLater(() -> {
                    addMessage("Error: " + error, true);
                    connectionStatus.setText("Error");
                    connectionStatus.setTextFill(Color.ORANGE);
                });
            }
        });

        // --- Ammo system callbacks integration ---
        chatClient.setAmmoCallback(new ChatClient.AmmoSystemCallback() {
            @Override
            public void onAmmoRequest(String requester, int amount, String message) {
                Platform.runLater(() -> {
                    // Build a friendly request message
                    String requestMsg = requester + " requests " + amount + " ammo";
                    if (!message.isEmpty()) {
                        requestMsg += " (“" + message + "”)";
                    }

                    // Create label + grant button
                    Label reqLabel = new Label("🔫 " + requestMsg);
                    reqLabel.setWrapText(true);
                    reqLabel.setStyle("-fx-text-fill: white;");

                    Button grantButton = new Button("Grant");
                    grantButton.setStyle(
                            "-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 10;"
                    );
                    // When clicked, send ammo and disable the button
                    grantButton.setOnAction(evt -> {
                        try {
                            chatClient.sendAmmo(requester, amount);
                            grantButton.setText("Granted");
                            grantButton.setDisable(true);
                        } catch (Exception e) {
                            addMessage("❌ Failed to grant ammo: " + e.getMessage(), true);
                        }
                    });

                    HBox requestRow = new HBox(8, reqLabel, grantButton);
                    requestRow.setAlignment(Pos.CENTER_LEFT);
                    requestRow.setPadding(new Insets(4, 0, 4, 0));

                    chatMessages.getChildren().add(requestRow);
                    scrollToBottom();
                });
            }

            @Override
            public void onAmmoTransfer(boolean sent, String otherPlayer, int amount) {
                Platform.runLater(() -> {
                    if (sent) {
                        addMessage("✅ You sent " + amount + " ammo to " + otherPlayer, true);
                        // Use your existing method:
                        game.onAmmoUsed(amount);
                        updateAmmo(game.getPlayerAmmo());
                    } else {
                        addMessage("🎁 You received " + amount + " ammo from " + otherPlayer, true);
                        game.onAmmoCollected(amount);
                        updateAmmo(game.getPlayerAmmo());
                    }
                    updateAmmoOnServer();
                });
            }


            @Override
            public void onPlayerListUpdate(String playerListData) {
                Platform.runLater(() -> updatePlayerList(playerListData));
            }

            @Override
            public void onSystemMessage(String message) {
                Platform.runLater(() -> addMessage("🔔 " + message, true));
            }
        });

        connectToChat();
    }

    // --- Ammo System UI logic ---
    private void requestAmmo() {
        String amountText = ammoRequestField.getText().trim();
        String message = ammoRequestMessageField.getText().trim();

        if (amountText.isEmpty()) {
            addMessage("Please enter ammo amount to request", true);
            return;
        }

        try {
            int amount = Integer.parseInt(amountText);
            if (amount <= 0) {
                addMessage("Ammo amount must be positive", true);
                return;
            }

            chatClient.requestAmmo(amount, message.isEmpty() ? "Please help!" : message);
            ammoRequestField.clear();
            ammoRequestMessageField.clear();
            addMessage("📢 You requested " + amount + " ammo from other players", true);

        } catch (NumberFormatException e) {
            addMessage("Invalid ammo amount", true);
        } catch (Exception e) {
            addMessage("Failed to send ammo request: " + e.getMessage(), true);
        }
    }

    private void sendAmmo() {
        String selectedPlayer = playerSelector.getValue();
        String amountText = ammoSendField.getText().trim();

        if (selectedPlayer == null || selectedPlayer.isEmpty()) {
            addMessage("Please select a player", true);
            return;
        }
        if (amountText.isEmpty()) {
            addMessage("Please enter ammo amount to send", true);
            return;
        }

        try {
            int amount = Integer.parseInt(amountText);
            if (amount <= 0) {
                addMessage("Ammo amount must be positive", true);
                return;
            }
            if (amount > game.getPlayerAmmo()) {
                addMessage("You don't have enough ammo (Current: " + game.getPlayerAmmo() + ")", true);
                return;
            }
            String recipient = selectedPlayer.split(" ")[0];
            chatClient.sendAmmo(recipient, amount);
            ammoSendField.clear();
            addMessage("📤 Sending " + amount + " ammo to " + recipient + "...", true);
        } catch (NumberFormatException e) {
            addMessage("Invalid ammo amount", true);
        } catch (Exception e) {
            addMessage("Failed to send ammo: " + e.getMessage(), true);
        }
    }

    private void refreshPlayerList() {
        try {
            chatClient.requestPlayerList();
        } catch (Exception e) {
            addMessage("Failed to refresh player list: " + e.getMessage(), true);
        }
    }

    private void updatePlayerList(String playerListData) {
        onlinePlayers.clear();
        playerSelector.getItems().clear();

        if (!playerListData.isEmpty()) {
            String[] players = playerListData.split(",");
            for (String playerData : players) {
                if (!playerData.trim().isEmpty()) {
                    String[] parts = playerData.split(":");
                    if (parts.length == 2) {
                        String playerName = parts[0];
                        try {
                            int ammo = Integer.parseInt(parts[1]);
                            onlinePlayers.put(playerName, ammo);

                            if (!playerName.equals(chatClient.getUsername())) {
                                playerSelector.getItems().add(playerName + " (" + ammo + " ammo)");
                            }
                        } catch (NumberFormatException e) {
                            // skip invalid entry
                        }
                    }
                }
            }
        }
    }

    // Called from Game to update chat UI ammo display (after pickup, used, sent, received)
    public void updateAmmo(int newAmmoCount) {
        currentAmmo = newAmmoCount;
        Platform.runLater(() -> {
            updateAmmoDisplay();
            updateAmmoOnServer();
        });
    }

    private void updateAmmoDisplay() {
        ammoCountLabel.setText(String.valueOf(currentAmmo));
    }

    private void updateAmmoOnServer() {
        if (chatClient != null && chatClient.isConnected()) {
            try {
                chatClient.updateAmmoCount(currentAmmo);
            } catch (Exception e) {
                System.err.println("Failed to update ammo on server: " + e.getMessage());
            }
        }
    }

    public void connectToChat() {
        if (!chatClient.isConnected()) {
            try {
                chatClient.connect();
            } catch (Exception e) {
                Platform.runLater(() -> {
                    addMessage("Failed to connect: " + e.getMessage(), true);
                    System.err.println("Chat connection failed: " + e.getMessage());
                });
            }
        }
    }

    private void sendMessage() {
        String message = messageInput.getText().trim();
        if (!message.isEmpty() && chatClient.isConnected()) {
            try {
                chatClient.sendMessage(message);
                messageInput.clear();
            } catch (Exception e) {
                addMessage("Failed to send message: " + e.getMessage(), true);
            }
        } else if (!chatClient.isConnected()) {
            addMessage("Not connected to chat server", true);
        }
    }

    private void addMessage(String message, boolean isSystemMessage) {
        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(CHAT_WIDTH - 40);

        if (isSystemMessage) {
            messageLabel.setStyle("-fx-text-fill: #FFD700; -fx-font-style: italic;");
        } else {
            messageLabel.setStyle("-fx-text-fill: white;");
        }

        chatMessages.getChildren().add(messageLabel);
        scrollToBottom();

        if (chatMessages.getChildren().size() > 50) {
            chatMessages.getChildren().remove(0);
        }
    }

    private void scrollToBottom() {
        Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
    }

    private void toggleChatVisibility() {
        isVisible = !isVisible;
        chatContainer.setVisible(isVisible);

        if (isVisible) {
            toggleButton.setText("Hide");
            messageInput.requestFocus();
            refreshPlayerList();
        } else {
            toggleButton.setText("Chat/Ammo");
            game.requestFocus();
        }
    }

    public boolean handleKeyEvent(KeyCode key) {
        if (isVisible) {
            return key == KeyCode.ENTER || key == KeyCode.TAB || key == KeyCode.ESCAPE;
        }
        return false;
    }

    public VBox getMainContainer() {
        return mainContainer;
    }

    public VBox getChatContainer() {
        return chatContainer;
    }

    public Button getToggleButton() {
        return toggleButton;
    }

    public boolean isChatVisible() {
        return isVisible;
    }

    public int getCurrentAmmo() {
        return currentAmmo;
    }

    public void disconnect() {
        if (chatClient != null && chatClient.isConnected()) {
            chatClient.disconnect();
        }
    }

    public void sendGameEvent(String event) {
        if (chatClient != null && chatClient.isConnected()) {
            try {
                chatClient.sendCustomMessage("[GAME] " + event);
            } catch (Exception e) {
                System.err.println("Failed to send game event: " + e.getMessage());
            }
        }
    }
}
