package platformgame.chat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Base64;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChatClient {
    private static final String SERVER_HOST = "10.15.28.50";
    private static final int SERVER_PORT = 8080;

    private SocketChannel socketChannel;
    private String username;
    private AtomicBoolean connected = new AtomicBoolean(false);
    private Thread messageListener;
    private ChatMessageCallback messageCallback;
    private AmmoSystemCallback ammoCallback;

    // Interface for handling received messages
    public interface ChatMessageCallback {
        void onMessageReceived(String message);
        void onConnectionStatusChanged(boolean connected);
        void onError(String error);
    }

    // Interface for handling ammo system events
    public interface AmmoSystemCallback {
        void onAmmoRequest(String requester, int amount, String message);
        void onAmmoTransfer(boolean sent, String otherPlayer, int amount);
        void onPlayerListUpdate(String playerListData);
        void onSystemMessage(String message);
    }

    public ChatClient() {
        this.username = "Player" + (new Random().nextInt(1000));
    }

    public ChatClient(String username) {
        this.username = username;
    }

    public void setMessageCallback(ChatMessageCallback callback) {
        this.messageCallback = callback;
    }

    public void setAmmoCallback(AmmoSystemCallback callback) {
        this.ammoCallback = callback;
    }

    public void connect() throws IOException {
        if (connected.get()) {
            throw new IllegalStateException("Already connected");
        }

        try {
            socketChannel = SocketChannel.open();
            socketChannel.connect(new InetSocketAddress(SERVER_HOST, SERVER_PORT));
            socketChannel.configureBlocking(true); // Use blocking mode for simplicity

            // Send WebSocket handshake
            sendWebSocketHandshake();

            connected.set(true);
            notifyConnectionStatus(true);

            // Start message listener thread
            messageListener = new Thread(this::listenForMessages);
            messageListener.setDaemon(true);
            messageListener.start();

            System.out.println("Connected to chat server as: " + username);

        } catch (IOException e) {
            connected.set(false);
            if (socketChannel != null) {
                try {
                    socketChannel.close();
                } catch (IOException closeEx) {
                    // Ignore close exception
                }
            }
            throw e;
        }
    }

    // Send WebSocket handshake request to the server
    private void sendWebSocketHandshake() throws IOException {
        String webSocketKey = generateWebSocketKey();

        String handshake = "GET /chat HTTP/1.1\r\n" +
                "Host: " + SERVER_HOST + ":" + SERVER_PORT + "\r\n" +
                "Upgrade: websocket\r\n" +
                "Connection: Upgrade\r\n" +
                "Sec-WebSocket-Key: " + webSocketKey + "\r\n" +
                "Sec-WebSocket-Version: 13\r\n\r\n";

        socketChannel.write(ByteBuffer.wrap(handshake.getBytes()));

        // Read handshake response
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int bytesRead = socketChannel.read(buffer);

        if (bytesRead <= 0) {
            throw new IOException("No handshake response received");
        }

        buffer.flip();
        String response = new String(buffer.array(), 0, buffer.remaining());

        if (!response.contains("101 Switching Protocols")) {
            throw new IOException("WebSocket handshake failed: " + response);
        }
    }

    // Generate the WebSocket Key for handshake
    private String generateWebSocketKey() {
        byte[] key = new byte[16];
        new Random().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }

    public void disconnect() {
        if (!connected.get()) {
            return;
        }

        connected.set(false);

        if (messageListener != null && messageListener.isAlive()) {
            messageListener.interrupt();
        }

        if (socketChannel != null && socketChannel.isOpen()) {
            try {
                socketChannel.close();
            } catch (IOException e) {
                System.err.println("Error closing socket: " + e.getMessage());
            }
        }

        notifyConnectionStatus(false);
        System.out.println("Disconnected from chat server");
    }

    // Send any custom message (plain text) to the server
    public void sendCustomMessage(String customMessage) throws IOException {
        if (!connected.get() || customMessage == null || customMessage.trim().isEmpty()) {
            return;
        }

        // Send the custom message directly, no prefix
        sendMessage(customMessage.trim());
    }

    // Send regular chat message
    public void sendMessage(String message) throws IOException {
        if (!connected.get() || message == null || message.trim().isEmpty()) {
            return;
        }

        try {
            String fullMessage = username + ": " + message.trim();
            byte[] frame = createWebSocketFrame(fullMessage);

            synchronized (this) { // Synchronize writes
                if (socketChannel != null && socketChannel.isOpen()) {
                    socketChannel.write(ByteBuffer.wrap(frame));
                }
            }

        } catch (IOException e) {
            notifyError("Error sending message: " + e.getMessage());
            disconnect();
            throw e;
        }
    }

    // === AMMO SYSTEM METHODS ===

    // Update player's ammo count on server
    public void updateAmmoCount(int ammoCount) throws IOException {
        if (!connected.get()) return;

        String message = "[AMMO_UPDATE]" + username + ":" + ammoCount;
        sendRawMessage(message);
    }

    // Request ammo from other players
    public void requestAmmo(int amount, String requestMessage) throws IOException {
        if (!connected.get()) return;

        String message = "[AMMO_REQUEST]" + username + ":" + amount + ":" + requestMessage;
        sendRawMessage(message);
    }

    // Send ammo to another player
    public void sendAmmo(String recipient, int amount) throws IOException {
        if (!connected.get()) return;

        String message = "[AMMO_SEND]" + username + ":" + recipient + ":" + amount;
        sendRawMessage(message);
    }

    // Request list of online players and their ammo
    public void requestPlayerList() throws IOException {
        if (!connected.get()) return;

        String message = "[PLAYER_LIST]" + username;
        sendRawMessage(message);
    }

    // Send raw message without username prefix
    private void sendRawMessage(String message) throws IOException {
        try {
            byte[] frame = createWebSocketFrame(message);

            synchronized (this) {
                if (socketChannel != null && socketChannel.isOpen()) {
                    socketChannel.write(ByteBuffer.wrap(frame));
                }
            }

        } catch (IOException e) {
            notifyError("Error sending raw message: " + e.getMessage());
            disconnect();
            throw e;
        }
    }

    // Create WebSocket frame for sending message
    private byte[] createWebSocketFrame(String message) {
        byte[] payload = message.getBytes();
        byte[] maskingKey = new byte[4];
        new Random().nextBytes(maskingKey);

        // Create masked payload
        byte[] maskedPayload = new byte[payload.length];
        for (int i = 0; i < payload.length; i++) {
            maskedPayload[i] = (byte) (payload[i] ^ maskingKey[i % 4]);
        }

        byte[] frame;
        if (payload.length < 126) {
            frame = new byte[6 + payload.length];
            frame[0] = (byte) 0x81; // FIN + text frame
            frame[1] = (byte) (0x80 | payload.length); // MASK + payload length
            System.arraycopy(maskingKey, 0, frame, 2, 4);
            System.arraycopy(maskedPayload, 0, frame, 6, maskedPayload.length);
        } else if (payload.length < 65536) {
            frame = new byte[8 + payload.length];
            frame[0] = (byte) 0x81;
            frame[1] = (byte) (0x80 | 126);
            frame[2] = (byte) (payload.length >> 8);
            frame[3] = (byte) (payload.length & 0xFF);
            System.arraycopy(maskingKey, 0, frame, 4, 4);
            System.arraycopy(maskedPayload, 0, frame, 8, maskedPayload.length);
        } else {
            throw new IllegalArgumentException("Message too long");
        }

        return frame;
    }

    // Listen for messages from the server
    private void listenForMessages() {
        ByteBuffer buffer = ByteBuffer.allocate(1024);

        while (connected.get() && !Thread.currentThread().isInterrupted()) {
            try {
                buffer.clear();
                int bytesRead = socketChannel.read(buffer);

                if (bytesRead == -1) {
                    // Connection closed by server
                    break;
                }

                if (bytesRead > 0) {
                    buffer.flip();
                    byte[] data = new byte[buffer.remaining()];
                    buffer.get(data);

                    String message = parseWebSocketFrame(data);
                    if (message != null) {
                        handleReceivedMessage(message);
                    }
                }

                // Small delay to prevent busy waiting
                Thread.sleep(10);

            } catch (IOException e) {
                if (connected.get()) {
                    notifyError("Connection error: " + e.getMessage());
                }
                break;
            } catch (InterruptedException e) {
                break;
            }
        }

        if (connected.get()) {
            disconnect();
        }
    }

    private void handleReceivedMessage(String message) {
        // Handle different types of messages
        if (message.startsWith("[AMMO_REQUEST_BROADCAST]")) {
            handleAmmoRequestBroadcast(message.substring(24));
        } else if (message.startsWith("[AMMO_TRANSFER]")) {
            handleAmmoTransfer(message.substring(15));
        } else if (message.startsWith("[PLAYER_LIST]")) {
            handlePlayerList(message.substring(13));
        } else if (message.startsWith("[SYSTEM]")) {
            handleSystemMessage(message.substring(8));
        } else {
            // Regular chat message
            notifyMessageReceived(message);
        }
    }

    private void handleAmmoRequestBroadcast(String data) {
        // Format: requester:amount:message
        String[] parts = data.split(":", 3);
        if (parts.length >= 2 && ammoCallback != null) {
            String requester = parts[0];
            try {
                int amount = Integer.parseInt(parts[1]);
                String requestMessage = parts.length > 2 ? parts[2] : "";
                ammoCallback.onAmmoRequest(requester, amount, requestMessage);
            } catch (NumberFormatException e) {
                System.err.println("Invalid ammo amount in request broadcast: " + parts[1]);
            }
        }
    }

    private void handleAmmoTransfer(String data) {
        // Format: sent:recipient:amount OR received:sender:amount
        String[] parts = data.split(":", 3);
        if (parts.length == 3 && ammoCallback != null) {
            boolean sent = "sent".equals(parts[0]);
            String otherPlayer = parts[1];
            try {
                int amount = Integer.parseInt(parts[2]);
                ammoCallback.onAmmoTransfer(sent, otherPlayer, amount);
            } catch (NumberFormatException e) {
                System.err.println("Invalid ammo amount in transfer: " + parts[2]);
            }
        }
    }

    private void handlePlayerList(String data) {
        if (ammoCallback != null) {
            ammoCallback.onPlayerListUpdate(data);
        }
    }

    private void handleSystemMessage(String message) {
        if (ammoCallback != null) {
            ammoCallback.onSystemMessage(message.trim());
        }
    }

    private String parseWebSocketFrame(byte[] data) {
        if (data.length < 2) return null;

        boolean fin = (data[0] & 0x80) != 0;
        int opcode = data[0] & 0x0F;
        boolean masked = (data[1] & 0x80) != 0;
        int payloadLength = data[1] & 0x7F;

        if (opcode == 0x1 && fin && !masked) { // Text frame from server (not masked)
            int headerLength = 2;
            int actualPayloadLength = payloadLength;

            if (payloadLength == 126) {
                if (data.length < 4) return null;
                actualPayloadLength = ((data[2] & 0xFF) << 8) | (data[3] & 0xFF);
                headerLength = 4;
            } else if (payloadLength == 127) {
                // Handle 64-bit length if needed
                return null; // Not implemented for simplicity
            }

            if (data.length >= headerLength + actualPayloadLength) {
                byte[] payload = Arrays.copyOfRange(data, headerLength, headerLength + actualPayloadLength);
                return new String(payload);
            }
        }

        return null;
    }

    private void notifyMessageReceived(String message) {
        if (messageCallback != null) {
            messageCallback.onMessageReceived(message);
        }
    }

    private void notifyConnectionStatus(boolean isConnected) {
        if (messageCallback != null) {
            messageCallback.onConnectionStatusChanged(isConnected);
        }
    }

    private void notifyError(String error) {
        if (messageCallback != null) {
            messageCallback.onError(error);
        }
    }

    // Getter methods
    public boolean isConnected() {
        return connected.get();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}