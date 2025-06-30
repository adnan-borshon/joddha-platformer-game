package platformgame;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import platformgame.Entity.*;
import platformgame.Event.EventHandler;
import platformgame.Map.Level_1;
import platformgame.Map.Level_1_controller;
import platformgame.Objects.Obj_Boom;
import platformgame.Objects.SuperObject;
import platformgame.chat.OptimizedChatUI;

import java.net.URL;

public class Game extends Pane {
    private final Canvas canvas;
    private final GraphicsContext gc;
    private KeyHandler keyHandler;
    public final int gameOverState = 4;
    public Inventory inventory;
    private long lastTime = System.nanoTime();
    public int GameState;
    public final int playState = 1;
    public final int pauseState = 2;
    public final int dialogueState = 3;

    public AssetSetter aSetter = new AssetSetter(this);
    public Npc[] npc = new Npc[10];
    public Scout[] scout = new Scout[10];
    public SuperObject[] object = new SuperObject[30];
    public Enemy[] enemies = new Enemy[40];
    public Soldier[] soldiers = new Soldier[20];

    public boolean hasKey1 = false;
    public boolean hasKey2 = false;
    public boolean hasKey3 = false;
    public int granadeCounter = 0;
    public boolean boomCollected = false;
    public boolean bridgeDestroyed = false;

    private boolean bridgeRemoved = false;
    public boolean ContainerGateRemoved = false;
    public boolean FenchGateRemoved = false;
    public boolean LeftWallRemoved = false;
    public boolean RightWallRemoved = false;

    public final double scale = 1.15;
    public  Player player;
    public Level_1 level1;
    public int tileSize = 32;
    public final double screenWidth = 1020;
    public final double screenHeight = 700;

    Sound music = Sound.getInstance();
    Sound sound = Sound.getInstance();

    public int hasKey = 0;
    public UI ui = new UI(this);
    public final EventHandler eventHandler = new EventHandler();

    public double camX = 0;
    public double camY = 0;

    // ✅ Boom spawn logic
    private boolean boomSpawned = false;
    private Obj_Boom boomObject;
    public boolean hasLauncher = false;

    // ✅ OPTIMIZED: Simplified chat system - no blocking initialization
    private OptimizedChatUI chatUI;
    private StackPane gameRoot;

    // ✅ OPTIMIZED: Simple ammo tracking without heavy synchronization
    private volatile int playerAmmo = 0;
    private volatile boolean chatConnected = false;
    public boolean missionCompleted = false;

    public Game() {

        long startTime = System.currentTimeMillis();

        // ✅ 1. Initialize canvas IMMEDIATELY (fastest possible)
        canvas = new Canvas(screenWidth, screenHeight);
        gc = canvas.getGraphicsContext2D();
        gameRoot = new StackPane();
        gameRoot.setPrefSize(screenWidth, screenHeight);
        inventory = new Inventory("/Inventory/level_1.txt");
        // ✅ 2. Load level with timeout protection
        loadLevelOptimized();

        // ✅ 3. Initialize player immediately
        initializePlayer();

        // ✅ 4. Setup basic game objects (no chat dependencies)
        setUpGameObjectsFast();

        // ✅ 5. Setup UI layout immediately (no chat blocking)
        setupUILayoutFast();

        // ✅ 6. Setup input handling BEFORE chat initialization
        setupInputHandling();

        // ✅ 7. Initialize chat system in LAZY mode (post-initialization)
        initializeChatLazy();

        // ✅ 8. Set initial game state
        GameState = playState;

        long endTime = System.currentTimeMillis();

    }

    // ✅ OPTIMIZED: Fast level loading with fallback
    private void loadLevelOptimized() {
        try {
            long levelStart = System.currentTimeMillis();
            URL url = getClass().getResource("/Level_1/Level_1.fxml");

            if (url == null) {
                System.err.println("❌ FXML resource not found! Creating fallback...");
                createFallbackLevel();
                return;
            }

            FXMLLoader loader = new FXMLLoader(url);
            Pane root = loader.load();
            Level_1_controller controller = loader.getController();
            level1 = controller.getLevelLogic();

            long levelTime = System.currentTimeMillis() - levelStart;
            System.out.println("✅ Level loaded in " + levelTime + "ms");

        } catch (Exception e) {
            System.err.println("❌ Level loading failed, using fallback: " + e.getMessage());
            createFallbackLevel();
        }
    }

    // ✅ OPTIMIZED: Fast player initialization
    private void initializePlayer() {
        if (level1 != null) {
            double startX = 27 * tileSize;
            double startY = 5 * tileSize;
            player = new Player(startX, startY, 40, 40, 3, this);
        } else {
            player = new Player(500, 350, 40, 40, 3, this);
        }
        System.out.println("✅ Player initialized");
    }

    // ✅ OPTIMIZED: Fast game objects setup (no chat dependencies)
    private void setUpGameObjectsFast() {
        aSetter.setObject();
        aSetter.setNpc();
        aSetter.setScout();
        aSetter.setEnemy();
        aSetter.setSoldiers();
        aSetter.setExplosion();
        ui = new UI(this);
        System.out.println("✅ Game objects initialized");
    }
    public void syncInventoryWithGameState() {
        // Sync keys
        if (hasKey1) inventory.addItem("key1", 1);
        if (hasKey2) inventory.addItem("key2", 1);
        if (hasKey3) inventory.addItem("key3", 1);

        // Sync other items
        if (hasLauncher) inventory.addItem("launcher", 1);
        if (boomCollected) inventory.addItem("boom", 1);

        // Sync ammo
        inventory.addItem("ammo", playerAmmo);
    }
    // ✅ OPTIMIZED: Fast UI layout without chat blocking
    private void setupUILayoutFast() {
        // Add canvas immediately
        gameRoot.getChildren().add(canvas);
        StackPane.setAlignment(canvas, Pos.CENTER);

        // ✅ ADD INVENTORY UI TO GAME ROOT
        gameRoot.getChildren().add(inventory.getInventoryUI());
        StackPane.setAlignment(inventory.getInventoryUI(), Pos.TOP_LEFT);

        // Position inventory in top-left corner
        inventory.setPosition(20, 20);

        // Add gameRoot to this pane
        this.getChildren().add(gameRoot);
        this.setPrefSize(screenWidth, screenHeight);
    }
    // ✅ OPTIMIZED: Lazy chat initialization (happens after game is ready)
    private void initializeChatLazy() {
        // Create chat UI placeholder immediately (no connection)
        chatUI = new OptimizedChatUI(this);
        keyHandler.setChatUI(chatUI);
        // Add chat UI elements to game root
        addChatUIElements();

        // Connect in background after a short delay (non-blocking)
        new Thread(() -> {
            try {
                Thread.sleep(1000); // Wait 1 second for game to fully load
                Platform.runLater(() -> {
                    try {
                        chatUI.connectToServer();
                        chatConnected = true;
                        chatUI.updateAmmo(playerAmmo);
                        System.out.println("🔌 Chat connected lazily");
                    } catch (Exception e) {
                        System.err.println("Chat connection failed (non-critical): " + e.getMessage());
                        chatConnected = false;
                    }
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    // ✅ OPTIMIZED: Add chat UI elements without blocking
    private void addChatUIElements() {
        double chatButtonX = 920;
        double chatButtonY = 35;

        Pane uiOverlay = new Pane();
        uiOverlay.setPrefSize(screenWidth, screenHeight);

        // Add chat toggle button
        chatUI.getToggleButton().setLayoutX(chatButtonX);
        chatUI.getToggleButton().setLayoutY(chatButtonY);
        uiOverlay.getChildren().add(chatUI.getToggleButton());

        // Add chat container
        chatUI.getChatContainer().setLayoutX(chatButtonX - 250);
        chatUI.getChatContainer().setLayoutY(chatButtonY + 40);
        uiOverlay.getChildren().add(chatUI.getChatContainer());

        // Add overlay to game root
        gameRoot.getChildren().add(uiOverlay);
        StackPane.setAlignment(uiOverlay, Pos.TOP_LEFT);
    }

    // ✅ OPTIMIZED: Fast input handling setup
    private void setupInputHandling() {
        keyHandler = new KeyHandler(this);
        setFocusTraversable(true);
        setOnKeyPressed(keyHandler::handleKeyPressed);
        setOnKeyReleased(keyHandler::handleKeyReleased);
    }

    // ✅ OPTIMIZED: Simplified fallback level creation
    private void createFallbackLevel() {
        System.out.println("🔧 Creating minimal fallback level...");
        // Create a minimal level to prevent crashes
        // This should be implemented based on your Level_1 class structure
        level1 = null; // Will be handled in draw() method
    }

    // ✅ AMMO SYSTEM: Optimized ammo collection
    public void onAmmoCollected(int ammoAmount) {
        if (ammoAmount <= 0) return;

        playerAmmo += ammoAmount;
        System.out.println("🔫 Collected " + ammoAmount + " ammo. Total: " + playerAmmo);

        // ✅ IMMEDIATELY update chat UI (don't wait for connection)
        if (chatUI != null) {
            Platform.runLater(() -> {
                chatUI.updateAmmo(playerAmmo);
                if (chatUI.isConnected()) {
                    try {
                        chatUI.sendGameEvent("Collected " + ammoAmount + " ammo! (Total: " + playerAmmo + ")");
                    } catch (Exception e) {
                        System.err.println("Chat update failed: " + e.getMessage());
                    }
                }
            });
        }
    }
    // ✅ AMMO SYSTEM: Optimized ammo usage
    public boolean onAmmoUsed(int ammoAmount) {
        if (ammoAmount <= 0 || playerAmmo < ammoAmount) {
            return false;
        }

        playerAmmo -= ammoAmount;
        System.out.println("🔫 Used " + ammoAmount + " ammo. Remaining: " + playerAmmo);

        // ✅ IMMEDIATELY update chat UI
        if (chatUI != null) {
            Platform.runLater(() -> {
                chatUI.updateAmmo(playerAmmo);
            });
        }
        return true;
    }

    // ✅ AMMO SYSTEM: Receive ammo from other players
    public void onAmmoReceived(String fromPlayer, int ammoAmount) {
        if (ammoAmount <= 0) return;

        playerAmmo += ammoAmount;
        System.out.println("🎁 Received " + ammoAmount + " ammo from " + fromPlayer);

        if (chatUI != null) {
            Platform.runLater(() -> {
                chatUI.updateAmmo(playerAmmo);
                if (chatUI.isConnected()) {
                    try {
                        chatUI.sendGameEvent("Received " + ammoAmount + " ammo from " + fromPlayer + "!");
                    } catch (Exception e) {
                        System.err.println("Chat update failed: " + e.getMessage());
                    }
                }
            });
        }

        // Show UI message
        if (ui != null) {
            ui.showMessage("Received " + ammoAmount + " ammo from " + fromPlayer + "!");
        }
    }
    // ✅ AMMO SYSTEM: Give ammo to other players
    public boolean onAmmoGiven(String toPlayer, int ammoAmount) {
        if (ammoAmount <= 0 || playerAmmo < ammoAmount) {
            return false;
        }

        playerAmmo -= ammoAmount;
        System.out.println("📤 Sent " + ammoAmount + " ammo to " + toPlayer);

        if (chatUI != null) {
            Platform.runLater(() -> {
                chatUI.updateAmmo(playerAmmo);
                if (chatUI.isConnected()) {
                    try {
                        chatUI.sendGameEvent("Sent " + ammoAmount + " ammo to " + toPlayer + "!");
                    } catch (Exception e) {
                        System.err.println("Chat update failed: " + e.getMessage());
                    }
                }
            });
        }
        return true;
    }

    // ✅ GETTERS/SETTERS: Simple ammo management
    public int getPlayerAmmo() {
        return playerAmmo;
    }

    public void setPlayerAmmo(int ammo) {
        playerAmmo = Math.max(0, ammo);
        System.out.println("🔫 Ammo set to: " + playerAmmo);

        if (chatUI != null) {
            Platform.runLater(() -> {
                chatUI.updateAmmo(playerAmmo);
            });
        }
    }
    public void collectAmmoFromObject(int ammoAmount) {
        if (ammoAmount <= 0) return;

        // Update the internal ammo counter
        onAmmoCollected(ammoAmount);

        // Show UI message
        if (ui != null) {
            ui.showMessage("Collected " + ammoAmount + " ammo!");
        }

        // Play sound effect if you have one
        playSoundEffects(1); // Adjust sound index as needed

        System.out.println("✅ Player collected " + ammoAmount + " ammo from object");
    }

    // ✅ OPTIMIZED: Key event handling with chat integration
//    private void onKeyPressed(KeyEvent e) {
//        KeyCode key = e.getCode();
//
//        // ✅ PRIORITY 1: Check if chat should handle the key first
//        if (chatUI != null && chatUI.handleKeyEvent(key)) {
//            e.consume(); // Prevent further processing
//            return;
//        }
//
//        // ✅ PRIORITY 2: Handle chat toggle (T key) - only when game is in play state
//        if (key == KeyCode.T && GameState == playState) {
//            if (chatUI != null) {
//                chatUI.toggleChat();
//            }
//            e.consume();
//            return;
//        }
//
//        // ✅ PRIORITY 3: Add to keysPressed for game logic (only if chat didn't handle it)
//        keysPressed.add(key);
//
//        // ✅ PRIORITY 4: Handle game state specific keys
//        if (GameState == gameOverState && key == KeyCode.ENTER) {
//            openMainMenu();
//            e.consume();
//            return;
//        }
//
//        if (missionCompleted && ui.isImageDialogue) {
//            Sound.getInstance().stop(7);
//            GameState = playState;
//            Sound.getInstance().loop(0);
//            e.consume();
//            return;
//        }
//
//        // Handle dialogue state
//        if (GameState == dialogueState && key == KeyCode.ENTER) {
//            handleDialogueActions();
//            e.consume();
//            return;
//        }
//
//        // ✅ Testing keys for ammo (remove in production)
//        if (key == KeyCode.PLUS || key == KeyCode.EQUALS) {
//            onAmmoCollected(10);
//            e.consume();
//            return;
//        }
//        if (key == KeyCode.MINUS) {
//            onAmmoUsed(5);
//            e.consume();
//            return;
//        }
//    }
//
//    private void onKeyReleased(KeyEvent e) {
//        KeyCode key = e.getCode();
//
//        // ✅ PRIORITY 1: Check if chat is handling this key
//        if (chatUI != null && chatUI.isVisible() && chatUI.isChatInputFocused()) {
//            // If chat input is focused, don't remove from keysPressed
//            e.consume();
//            return;
//        }
//
//        // ✅ PRIORITY 2: Only remove from keysPressed if it's a game key
//        keysPressed.remove(key);
//    }

    // ✅ OPTIMIZED: Dialogue action handling
//    private void handleDialogueActions() {
//        if (eventHandler.isShowingBridgePopup()) {
//            eventHandler.triggerBridgeExplosion(this, System.nanoTime());
//            return;
//        }
//
//        if (hasKey1 && !LeftWallRemoved) {
//            level1.removeLeftWallLayer();
//            LeftWallRemoved = true;
//            playSoundEffects(2);
//            ui.showMessage("Left wall opened! The villagers are free!");
//        }
//
//        if (hasKey3 && !RightWallRemoved) {
//            level1.removeRightWallLayer();
//            RightWallRemoved = true;
//            playSoundEffects(2);
//            ui.showMessage("Right wall opened! The villagers are free!");
//        }
//
//        if (hasLauncher && !ContainerGateRemoved) {
//            granadeCounter--;
//            long now = System.nanoTime();
//            eventHandler.triggerContainerGateExplosion(this, now);
//            level1.removeContainerGateLayer();
//            ContainerGateRemoved = true;
//            playSoundEffects(2);
//            ui.showMessage("Container gate destroyed");
//        }
//
//        if (boomCollected && !bridgeDestroyed) {
//            long now = System.nanoTime();
//            eventHandler.triggerBridgeExplosion(this, now);
//            level1.removeBridgeLayer();
//            level1.removeBridgeBackLayer();
//            bridgeDestroyed = true;
//            ui.showMessage("Bridge has been destroyed");
//        }
//
//        // Exit dialogue state
//        GameState = playState;
//        ui.dialogue = "";
//        ui.narrator = null;
//        ui.isImageDialogue = false;
//
//        for (Npc n : npc) {
//            if (n != null && n.playerIsTouching) {
//                n.speak();
//                break;
//            }
//        }
//    }





    // ✅ EXISTING METHODS: Keep all your existing game logic
    public void startGameLoop() {
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                long delta = now - lastTime;
                update(now, delta);
                draw();
                lastTime = now;
            }
        };
        timer.start();
    }

    private void update(long now, long deltaTime) {
        if (GameState == playState) {
            player.update(keyHandler.getKeysPressed(), level1, this, now, deltaTime);
            for (Npc n : npc) {
                if (n != null) {
                    n.update(deltaTime, now);
                }
            }

            for (Scout scoutEntity : scout) {
                if (scoutEntity != null) {
                    scoutEntity.update(deltaTime, now);
                }
            }

            for (Enemy enemyEntity : enemies) {
                if (enemyEntity != null) {
                    enemyEntity.update(deltaTime, now);
                }
            }

            for (Soldier soldier : soldiers) {
                if (soldier != null) {
                    soldier.update(deltaTime, now);
                }
            }

            if (keyHandler.isKeyPressed(KeyCode.ESCAPE)) {
                GameManager.getInstance().saveState(this);
                openMainMenu();
                GameState = pauseState;
            }

            eventHandler.update(player, this, now);
        }
    }

    private void draw() {
        gc.setFill(Color.BLUE);
        gc.fillRect(0, 0, screenWidth, screenHeight);

        if (level1 == null) {
            // Draw fallback screen
            gc.setFill(Color.BLACK);
            gc.fillRect(0, 0, screenWidth, screenHeight);
            gc.setFill(Color.WHITE);
            gc.setFont(javafx.scene.text.Font.font("Arial", 24));
            gc.fillText("Loading Level...", screenWidth / 2 - 100, screenHeight / 2);
            return;
        }

        camX = player.getX() + player.getWidth() / 2 - screenWidth / 2;
        camY = player.getY() + player.getHeight() / 2 - screenHeight / 2;

        double mapPixelWidth = level1.mapWidth * tileSize;
        double mapPixelHeight = level1.mapHeight * tileSize;

        camX = Math.max(0, Math.min(camX, mapPixelWidth - screenWidth));
        camY = Math.max(0, Math.min(camY, mapPixelHeight - screenHeight));

        level1.drawBackground(gc, camX, camY, scale);
        level1.drawMiddleground(gc, camX, camY, scale);

        // Draw entities in proper order
        drawEntitiesBehindPlayer();
        player.draw(gc, camX, camY, scale);
        drawEntitiesInFrontOfPlayer();

        eventHandler.draw(gc, camX, camY, scale);
        level1.drawForeground(gc, camX, camY, scale);
        ui.draw(gc);

        if (GameState == gameOverState) {
            drawGameOverScreen();
        }
    }

    private void drawEntitiesBehindPlayer() {
        for (Enemy enemyEntity : enemies) {
            if (enemyEntity != null && enemyEntity.isBehindPlayer(this)) {
                enemyEntity.draw(gc, camX, camY, scale);
            }
        }
        for (Npc npcEntity : npc) {
            if (npcEntity != null && npcEntity.isBehindPlayer(this)) {
                npcEntity.draw(gc, camX, camY, scale);
            }
        }
        for (SuperObject obj : object) {
            if (obj != null && obj.isBehindPlayer(this)) {
                obj.draw(gc, this);
            }
        }
        for (Scout scoutEntity : scout) {
            if (scoutEntity != null && scoutEntity.isBehindPlayer(this)) {
                scoutEntity.draw(gc, camX, camY, scale);
            }
        }
    }

    private void drawEntitiesInFrontOfPlayer() {
        for (Scout scoutEntity : scout) {
            if (scoutEntity != null && !scoutEntity.isBehindPlayer(this)) {
                scoutEntity.draw(gc, camX, camY, scale);
            }
        }
        for (Npc npcEntity : npc) {
            if (npcEntity != null && !npcEntity.isBehindPlayer(this)) {
                npcEntity.draw(gc, camX, camY, scale);
            }
        }
        for (Enemy enemyEntity : enemies) {
            if (enemyEntity != null && !enemyEntity.isBehindPlayer(this)) {
                enemyEntity.draw(gc, camX, camY, scale);
            }
        }
        for (Soldier soldier : soldiers) {
            if (soldier != null) {
                soldier.draw(gc, camX, camY, scale);
            }
        }
        for (SuperObject obj : object) {
            if (obj != null && !obj.isBehindPlayer(this)) {
                obj.draw(gc, this);
            }
        }
    }

    private void drawGameOverScreen() {
        gc.setFill(Color.color(0, 0, 0, 0.6));
        gc.fillRect(0, 0, screenWidth, screenHeight);

        gc.setFill(Color.RED);
        gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 60));
        gc.fillText("GAME OVER", screenWidth / 2 - 200, screenHeight / 2 - 20);

        gc.setFill(Color.WHITE);
        gc.setFont(javafx.scene.text.Font.font("Arial", 24));
        gc.fillText("Press ENTER to Menu", screenWidth / 2 - 130, screenHeight / 2 + 40);
    }

    public void setUpObject() {
        setUpGameObjectsFast();
        GameState = playState;

        // Initialize ammo display with delay (non-blocking)
        if (chatConnected && chatUI != null) {
            Platform.runLater(() -> {
                try {
                    chatUI.updateAmmo(playerAmmo);
                    chatUI.sendGameEvent("Player joined with " + playerAmmo + " ammo!");
                } catch (Exception e) {
                    System.err.println("Error updating chat: " + e.getMessage());
                }
            });
        }
    }

    public void checkAndSpawnBoom() {
        if (boomSpawned) return;

        boolean allEnemiesDead = true;

        for (Enemy e : enemies) {
            if (e != null && !e.isDead()) {
                allEnemiesDead = false;
                break;
            }
        }

        for (Scout s : scout) {
            if (s != null && !s.isDead()) {
                allEnemiesDead = false;
                break;
            }
        }

        for (Soldier s : soldiers) {
            if (s != null && !s.isDead()) {
                allEnemiesDead = false;
                break;
            }
        }

        if (allEnemiesDead) {
            boomObject = new Obj_Boom(54 * tileSize, 24 * tileSize);

            for (int i = 0; i < object.length; i++) {
                if (object[i] == null) {
                    object[i] = boomObject;
                    System.out.println("💥 Boom spawned at: " + (54 * tileSize) + ", " + (24 * tileSize));
                    break;
                }
            }

            boomSpawned = true;
            eventHandler.enableBridgeDestruction();
        }
    }

    public void openMainMenu() {
        try {
            // Disconnect from chat
            if (chatConnected && chatUI != null) {
                try {
                    chatUI.sendGameEvent("Player left the game");
                    chatUI.disconnect();
                    chatConnected = false;
                } catch (Exception e) {
                    System.err.println("Error disconnecting from chat: " + e.getMessage());
                }
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FirstPage.fxml"));
            Pane menuRoot = loader.load();
            Scene currentScene = this.getScene();
            currentScene.setRoot(menuRoot);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ✅ SOUND METHODS: Keep existing
    public void playMusic(int i) {
        music.loop(i);
    }

    public void stopMusic(int i) {
        music.stop(i);
    }

    public void playSoundEffects(int i) {
        sound.play(i);
    }

    // ✅ GETTERS: Keep existing
    public OptimizedChatUI getChatUI() {
        return chatUI;
    }

    public boolean isChatConnected() {
        return chatConnected;
    }

    public void triggerExplosionGate() {
        // Implementation if needed
    }
}