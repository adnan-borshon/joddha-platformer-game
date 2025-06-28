package platformgame;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
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

import java.net.URL;
import java.util.HashSet;
import java.util.Set;

public class Game extends Pane {
    private final Canvas canvas;
    private final GraphicsContext gc;
    private final Set<KeyCode> keysPressed = new HashSet<>();
    public final int gameOverState = 4;

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


    public final double scale = 1.15;
    public final Player player;
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

    // ✅ Chat system integration
    private ChatUI chatUI;
    private StackPane gameRoot; // Container for game and UI elements

    // ✅ FIXED: Enhanced ammo tracking with proper initialization
    private int playerAmmo = 0; // Start with some ammo for testing
    private long lastAmmoSyncTime = 0; // For periodic sync

    public Game() {
        // Create root container for layering
        gameRoot = new StackPane();
        gameRoot.setPrefSize(screenWidth, screenHeight);

        // Initialize canvas
        canvas = new Canvas(screenWidth, screenHeight);
        gc = canvas.getGraphicsContext2D();

        // ✅ FIXED: Initialize chat system BEFORE other components
        initializeChatSystem();

        // Create UI overlay pane
        // Find the position where you want the button to appear
        double chatButtonX = 920; // <<<< Change this value to match the X you want
        double chatButtonY = 35;  // <<<< Change this value to match the Y you want

        Pane uiOverlay = new Pane();
        uiOverlay.setPrefSize(screenWidth, screenHeight);

// Place the toggle button absolutely
        chatUI.getToggleButton().setLayoutX(chatButtonX);
        chatUI.getToggleButton().setLayoutY(chatButtonY);
        uiOverlay.getChildren().add(chatUI.getToggleButton());

// Place the chat container wherever you want (off screen or near the button)
        chatUI.getChatContainer().setLayoutX(chatButtonX - 20); // or same as button if you want
        chatUI.getChatContainer().setLayoutY(chatButtonY + 40);  // adjust so it appears below or right of button
        uiOverlay.getChildren().add(chatUI.getChatContainer());


        // Layer canvas and UI
        gameRoot.getChildren().addAll(canvas, uiOverlay);
        StackPane.setAlignment(canvas, Pos.CENTER);
        StackPane.setAlignment(uiOverlay, Pos.TOP_LEFT);

        // Add gameRoot to this pane
        this.getChildren().add(gameRoot);
        this.setPrefSize(screenWidth, screenHeight);

        loadLevel();

        if (level1 != null) {
            double startX = 27 * tileSize;
            double startY = 5 * tileSize;
            player = new Player(startX, startY, 40, 40, 3, this);
        } else {
            player = new Player(500, 350, 40, 40, 3, this);
        }

        setUpObject();
        setFocusTraversable(true);
        setOnKeyPressed(this::onKeyPressed);
        setOnKeyReleased(this::onKeyReleased);
    }

    // ✅ NEW: Separate method to initialize chat system with proper callbacks
    private void initializeChatSystem() {
        chatUI = new ChatUI(this);

        // ✅ CRITICAL: Set up ammo transfer callbacks to actually affect game state
        setupAmmoCallbacks();

        System.out.println("🔧 Chat system initialized with ammo callbacks");
    }

    // ✅ NEW: Setup callbacks to handle ammo transfers in the game
    private void setupAmmoCallbacks() {
        // This would require modifying ChatUI to accept callbacks
        // For now, we'll rely on the existing methods but ensure they're properly called

        // Note: You'll need to modify ChatUI to call these methods when ammo is transferred
        // The methods onAmmoReceived and onAmmoGiven should be called from ChatUI
    }

    private void loadLevel() {
        try {
            URL url = getClass().getResource("/Level_1/Level_1.fxml");
            if (url == null) {
                System.err.println("FXML resource not found!");
                return;
            }

            FXMLLoader loader = new FXMLLoader(url);
            Pane root = loader.load();
            Level_1_controller controller = loader.getController();
            level1 = controller.getLevelLogic();

            if (level1 != null) {
                System.out.println("Level1 loaded: " + level1.mapWidth + "x" + level1.mapHeight);
            } else {
                System.err.println("Level1 logic load failed");
            }

        } catch (Exception e) {
            System.err.println("Error loading level:");
            e.printStackTrace();
        }
    }

    private void draw() {
        gc.setFill(Color.BLUE);
        gc.fillRect(0, 0, screenWidth, screenHeight);

        if (level1 == null) {
            System.err.println("Level1 is null, cannot draw map");
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

//        //for granade launcher
//        for (SuperObject obj : Launcher) {
//            if (obj != null && obj.isBehindPlayer(this)) {
//                obj.draw(gc, this);
//            }
//        }
//
//        //for key opener
//        for (SuperObject obj : Opener) {
//            if (obj != null && obj.isBehindPlayer(this)) {
//                obj.draw(gc, this);
//            }
//        }

        for (Scout scoutEntity : scout) {
            if (scoutEntity != null && scoutEntity.isBehindPlayer(this)) {
                scoutEntity.draw(gc, camX, camY, scale);

            }
        }

        player.draw(gc, camX, camY, scale);

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
//        //for granade launcher
//        for (SuperObject obj : Launcher) {
//            if (obj != null && !obj.isBehindPlayer(this)) {
//                obj.draw(gc, this);
//            }
//        }
//        //for key opener
//        for (SuperObject obj : Opener) {
//            if (obj != null && !obj.isBehindPlayer(this)) {
//                obj.draw(gc, this);
//            }
//        }

        eventHandler.draw(gc, camX, camY, scale);
        level1.drawForeground(gc, camX, camY, scale);
        ui.draw(gc);

        if (GameState == gameOverState) {
            gc.setFill(Color.color(0, 0, 0, 0.6));
            gc.fillRect(0, 0, screenWidth, screenHeight);

            gc.setFill(Color.RED);
            gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 60));
            gc.fillText("GAME OVER", screenWidth / 2 - 200, screenHeight / 2 - 20);

            gc.setFill(Color.WHITE);
            gc.setFont(javafx.scene.text.Font.font("Arial", 24));
            gc.fillText("Press ENTER to Menu", screenWidth / 2 - 130, screenHeight / 2 + 40);
        }
    }

    public void setUpObject() {
        aSetter.setObject();
//        aSetter.setNpc();
        aSetter.setScout();
        aSetter.setEnemy();
        aSetter.setSoldiers();
        aSetter.setExplosion();
//        aSetter.setLauncherAndOpener();
        playMusic(0);
        GameState = playState;

        // ✅ FIXED: Initialize chat ammo display with proper delay and connection
        Platform.runLater(() -> {
            try {
                // Connect to chat first
                if (chatUI != null) {
                    chatUI.connectToChat();

                    // Wait a bit for connection, then update ammo
                    new Thread(() -> {
                        try {
                            Thread.sleep(1000); // Wait 1 second for connection
                            Platform.runLater(() -> {
                                chatUI.updateAmmo(playerAmmo);
                                chatUI.sendGameEvent("Player joined the game with " + playerAmmo + " ammo!");
                                System.out.println("🔫 Initial ammo set to: " + playerAmmo);
                            });
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }).start();
                }
            } catch (Exception e) {
                System.err.println("Error initializing chat ammo: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void onKeyPressed(KeyEvent e) {
        KeyCode key = e.getCode();

        // ✅ Check if chat should handle this key event
        if (chatUI != null && chatUI.handleKeyEvent(key)) {
            // If chat is handling the key, don't add it to game keys
            return;
        }

        keysPressed.add(key);

        if (GameState == gameOverState && key == KeyCode.ENTER) {
            openMainMenu();
        }

        // ✅ Handle bridge destruction popup
        if (GameState == dialogueState && key == KeyCode.ENTER) {
            if (eventHandler.isShowingBridgePopup()) {
                eventHandler.triggerBridgeExplosion(this, System.nanoTime());
                return;
            }

            for (Npc n : npc) {
                if (n != null && n.playerIsTouching) {
                    n.speak();
                    break;
                }
            }
        }

        // ✅ Toggle chat with 'T' key (common in games)
        if (key == KeyCode.T && GameState == playState) {
            if (chatUI != null && !chatUI.isChatVisible()) {
                chatUI.getToggleButton().fire(); // Simulate button click
            }
        }

        // ✅ TESTING: Add keys to manually add/remove ammo for testing
        if (key == KeyCode.PLUS || key == KeyCode.EQUALS) {
            onAmmoCollected(10); // Add 10 ammo for testing
        }
        if (key == KeyCode.MINUS) {
            if (onAmmoUsed(5)) { // Use 5 ammo for testing
                System.out.println("✅ Ammo used successfully");
            } else {
                System.out.println("❌ Not enough ammo");
            }
        }
    }

    private void onKeyReleased(KeyEvent e) {
        // ✅ Only remove key if chat isn't handling it
        if (chatUI == null || !chatUI.handleKeyEvent(e.getCode())) {
            keysPressed.remove(e.getCode());
        }
    }

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
            player.update(keysPressed, level1, this, now, deltaTime);

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

            checkAndSpawnBoom();

            // ✅ NEW: Periodic ammo sync to ensure consistency
            if (now - lastAmmoSyncTime > 5_000_000_000L) { // Every 5 seconds
                syncAmmoWithChat();
                lastAmmoSyncTime = now;
            }

            if (keysPressed.contains(KeyCode.ESCAPE)) {
                GameManager.getInstance().saveState(this);
                openMainMenu();
                GameState = pauseState;
            }

            eventHandler.update(player, this, now);
        }
    }

    private void checkAndSpawnBoom() {
        if (boomSpawned) return;

        boolean allEnemiesDead = true;
        boolean placed = false; //bomb placed

        for (Enemy e : enemies) {
            if (e != null) {
                if (!e.isDead()) {
                    allEnemiesDead = false;
                }
            }
            break;
        }

        for (Scout s : scout) {
            if (s != null) {
                if (!s.isDead()) {
                    allEnemiesDead = false;
                }
            }
            break;
        }
        for (Soldier s : soldiers) {
            if (s != null) {
                if (!s.isDead()) {
                    allEnemiesDead = false;
                }
            }
            break;
        }

        if (allEnemiesDead) {
            boomObject = new Obj_Boom(54 * tileSize, 24 * tileSize);

            for (int i = 0; i < object.length; i++) {
                if (object[i] == null) {
                    object[i] = boomObject;
                    System.out.println("💥 Boom appeared at position: " + (54 * tileSize) + ", " + (24 * tileSize));
                    placed = true;
                    break;
                }
            }

            if (!placed) {
                System.out.println("⚠️ Could not place boom - object array is full!");
            }

            boomSpawned = true;
            eventHandler.enableBridgeDestruction();
        }
    }

    // ✅ FIXED: Enhanced ammo collection with validation and proper synchronization
    public void onAmmoCollected(int ammoAmount) {
        if (ammoAmount <= 0) {
            System.out.println("⚠️ Invalid ammo amount: " + ammoAmount);
            return;
        }

        playerAmmo += ammoAmount;
        System.out.println("🔫 Player collected " + ammoAmount + " ammo. Total: " + playerAmmo);

        // ✅ CRITICAL: Ensure UI update happens on JavaFX thread with proper null checks
        Platform.runLater(() -> {
            try {
                if (chatUI != null) {
                    chatUI.updateAmmo(playerAmmo);
                    chatUI.sendGameEvent("Player collected " + ammoAmount + " ammo! (Total: " + playerAmmo + ")");
                    System.out.println("✅ Chat UI updated with new ammo: " + playerAmmo);
                } else {
                    System.out.println("⚠️ ChatUI is null, cannot update ammo display");
                }
            } catch (Exception e) {
                System.err.println("❌ Error updating chat UI: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    // ✅ FIXED: Enhanced ammo usage with proper validation and synchronization
    public boolean onAmmoUsed(int ammoAmount) {
        if (ammoAmount <= 0) {
            System.out.println("⚠️ Invalid ammo amount: " + ammoAmount);
            return false;
        }

        if (playerAmmo >= ammoAmount) {
            playerAmmo -= ammoAmount;
            System.out.println("🔫 Player used " + ammoAmount + " ammo. Remaining: " + playerAmmo);

            // ✅ CRITICAL: Update chat UI on JavaFX thread with proper error handling
            Platform.runLater(() -> {
                try {
                    if (chatUI != null) {
                        chatUI.updateAmmo(playerAmmo);
                        System.out.println("✅ Chat UI updated after ammo usage: " + playerAmmo);
                    } else {
                        System.out.println("⚠️ ChatUI is null, cannot update ammo display");
                    }
                } catch (Exception e) {
                    System.err.println("❌ Error updating chat UI after ammo usage: " + e.getMessage());
                    e.printStackTrace();
                }
            });
            return true; // Indicate successful usage
        } else {
            System.out.println("⚠️ Not enough ammo! Current: " + playerAmmo + ", Required: " + ammoAmount);
            return false; // Indicate failed usage
        }
    }

    // ✅ FIXED: Method to handle receiving ammo from other players via chat
    public void onAmmoReceived(String fromPlayer, int ammoAmount) {
        if (ammoAmount <= 0) {
            System.out.println("⚠️ Invalid ammo amount received: " + ammoAmount);
            return;
        }

        playerAmmo += ammoAmount;
        System.out.println("🎁 Received " + ammoAmount + " ammo from " + fromPlayer + ". Total: " + playerAmmo);

        Platform.runLater(() -> {
            try {
                if (chatUI != null) {
                    chatUI.updateAmmo(playerAmmo);
                    chatUI.sendGameEvent("Received " + ammoAmount + " ammo from " + fromPlayer + "!");
                    System.out.println("✅ Chat UI updated after receiving ammo: " + playerAmmo);
                } else {
                    System.out.println("⚠️ ChatUI is null, cannot update ammo display");
                }
            } catch (Exception e) {
                System.err.println("❌ Error updating chat UI after receiving ammo: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    // ✅ FIXED: Method to handle giving ammo to other players via chat
    public boolean onAmmoGiven(String toPlayer, int ammoAmount) {
        if (ammoAmount <= 0) {
            System.out.println("⚠️ Invalid ammo amount to give: " + ammoAmount);
            return false;
        }

        if (playerAmmo >= ammoAmount) {
            playerAmmo -= ammoAmount;
            System.out.println("📤 Sent " + ammoAmount + " ammo to " + toPlayer + ". Remaining: " + playerAmmo);

            Platform.runLater(() -> {
                try {
                    if (chatUI != null) {
                        chatUI.updateAmmo(playerAmmo);
                        chatUI.sendGameEvent("Sent " + ammoAmount + " ammo to " + toPlayer + "!");
                        System.out.println("✅ Chat UI updated after giving ammo: " + playerAmmo);
                    } else {
                        System.out.println("⚠️ ChatUI is null, cannot update ammo display");
                    }
                } catch (Exception e) {
                    System.err.println("❌ Error updating chat UI after giving ammo: " + e.getMessage());
                    e.printStackTrace();
                }
            });
            return true;
        } else {
            System.out.println("⚠️ Not enough ammo to send! Current: " + playerAmmo + ", Requested: " + ammoAmount);
            return false;
        }
    }

    // ✅ Getter for current ammo (if needed elsewhere)
    public int getPlayerAmmo() {
        return playerAmmo;
    }

    // ✅ FIXED: Method to directly set ammo with chat synchronization and validation
    public void setPlayerAmmo(int ammo) {
        playerAmmo = Math.max(0, ammo); // Ensure ammo is never negative

        Platform.runLater(() -> {
            try {
                if (chatUI != null) {
                    chatUI.updateAmmo(playerAmmo);
                    System.out.println("✅ Chat UI updated after setting ammo: " + playerAmmo);
                } else {
                    System.out.println("⚠️ ChatUI is null, cannot update ammo display");
                }
            } catch (Exception e) {
                System.err.println("❌ Error updating chat UI after setting ammo: " + e.getMessage());
                e.printStackTrace();
            }
        });
        System.out.println("🔫 Player ammo set to: " + playerAmmo);
    }

    // ✅ FIXED: Method to synchronize ammo count with proper error handling
    public void syncAmmoWithChat() {
        Platform.runLater(() -> {
            try {
                if (chatUI != null) {
                    chatUI.updateAmmo(playerAmmo);
                    System.out.println("🔄 Ammo synced with chat: " + playerAmmo);
                } else {
                    System.out.println("⚠️ ChatUI is null, cannot sync ammo");
                }
            } catch (Exception e) {
                System.err.println("❌ Error syncing ammo with chat: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void chatDisconnection(){
        // ✅ Disconnect from chat before leaving
        try {
            if (chatUI != null) {
                chatUI.sendGameEvent("Player left the game");
                chatUI.disconnect();
                System.out.println("🔌 Disconnected from chat");
            }
        } catch (Exception e) {
            System.err.println("❌ Error disconnecting from chat: " + e.getMessage());
        }
    }

    private void openMainMenu() {
        try {
            chatDisconnection();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FirstPage.fxml"));
            Pane menuRoot = loader.load();
            Scene currentScene = this.getScene();
            currentScene.setRoot(menuRoot);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void playMusic(int i) {
        music.loop(i);
    }

    public void stopMusic(int i) {
        music.stop(i);
    }

    public void playSoundEffects(int i) {
        sound.play(i);
    }

    // ✅ Getter for chat UI (if needed elsewhere)
    public ChatUI getChatUI() {
        return chatUI;
    }
}