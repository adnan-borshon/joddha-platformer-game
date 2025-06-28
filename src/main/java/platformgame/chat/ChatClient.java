// src/platformgame/chat/ChatClient.java
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
    private final String serverHost;
    private final int    serverPort;
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

    /**
     * @param serverHost IP or hostname of the chat server
     * @param serverPort port the server is listening on
     * @param username   your desired username
     */
    public ChatClient(String serverHost, int serverPort, String username) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.username   = username;
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
            socketChannel.connect(new InetSocketAddress(serverHost, serverPort));
            socketChannel.configureBlocking(true); // blocking for simplicity

            sendWebSocketHandshake();
            connected.set(true);
            notifyConnectionStatus(true);

            messageListener = new Thread(this::listenForMessages);
            messageListener.setDaemon(true);
            messageListener.start();

            System.out.println("Connected to chat server " +
                    serverHost + ":" + serverPort + " as: " + username);

        } catch (IOException e) {
            connected.set(false);
            if (socketChannel != null && socketChannel.isOpen()) {
                socketChannel.close();
            }
            throw e;
        }
    }

    private void sendWebSocketHandshake() throws IOException {
        String webSocketKey = generateWebSocketKey();
        String handshake =
                "GET /chat HTTP/1.1\r\n" +
                        "Host: " + serverHost + ":" + serverPort + "\r\n" +
                        "Upgrade: websocket\r\n" +
                        "Connection: Upgrade\r\n" +
                        "Sec-WebSocket-Key: " + webSocketKey + "\r\n" +
                        "Sec-WebSocket-Version: 13\r\n\r\n";

        socketChannel.write(ByteBuffer.wrap(handshake.getBytes()));

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

    private String generateWebSocketKey() {
        byte[] key = new byte[16];
        new Random().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }

    public void disconnect() {
        if (!connected.get()) return;
        connected.set(false);
        if (messageListener != null && messageListener.isAlive()) {
            messageListener.interrupt();
        }
        try {
            if (socketChannel != null && socketChannel.isOpen()) {
                socketChannel.close();
            }
        } catch (IOException ignored) {}
        notifyConnectionStatus(false);
        System.out.println("Disconnected from chat server");
    }

    public void sendCustomMessage(String customMessage) throws IOException {
        if (!connected.get() || customMessage == null || customMessage.trim().isEmpty()) {
            return;
        }
        sendMessage(customMessage.trim());
    }

    public void sendMessage(String message) throws IOException {
        if (!connected.get() || message == null || message.trim().isEmpty()) {
            return;
        }
        String fullMessage = username + ": " + message.trim();
        byte[] frame = createWebSocketFrame(fullMessage);
        synchronized (this) {
            if (socketChannel != null && socketChannel.isOpen()) {
                socketChannel.write(ByteBuffer.wrap(frame));
            }
        }
    }

    // === AMMO SYSTEM METHODS ===

    public void updateAmmoCount(int ammoCount) throws IOException {
        if (!connected.get()) return;
        String msg = "[AMMO_UPDATE]" + username + ":" + ammoCount;
        sendRawMessage(msg);
    }

    public void requestAmmo(int amount, String requestMessage) throws IOException {
        if (!connected.get()) return;
        String msg = "[AMMO_REQUEST]" + username + ":" + amount + ":" + requestMessage;
        sendRawMessage(msg);
    }

    public void sendAmmo(String recipient, int amount) throws IOException {
        if (!connected.get()) return;
        String msg = "[AMMO_SEND]" + username + ":" + recipient + ":" + amount;
        sendRawMessage(msg);
    }

    public void requestPlayerList() throws IOException {
        if (!connected.get()) return;
        String msg = "[PLAYER_LIST]" + username;
        sendRawMessage(msg);
    }

    private void sendRawMessage(String message) throws IOException {
        byte[] frame = createWebSocketFrame(message);
        synchronized (this) {
            if (socketChannel != null && socketChannel.isOpen()) {
                socketChannel.write(ByteBuffer.wrap(frame));
            }
        }
    }

    private byte[] createWebSocketFrame(String message) {
        byte[] payload = message.getBytes();
        byte[] maskingKey = new byte[4];
        new Random().nextBytes(maskingKey);

        byte[] masked = new byte[payload.length];
        for (int i = 0; i < payload.length; i++) {
            masked[i] = (byte)(payload[i] ^ maskingKey[i % 4]);
        }

        int length = payload.length;
        if (length < 126) {
            byte[] frame = new byte[6 + length];
            frame[0] = (byte)0x81; // FIN + text
            frame[1] = (byte)(0x80 | length);
            System.arraycopy(maskingKey, 0, frame, 2, 4);
            System.arraycopy(masked, 0, frame, 6, length);
            return frame;
        } else if (length < 65536) {
            byte[] frame = new byte[8 + length];
            frame[0] = (byte)0x81;
            frame[1] = (byte)(0x80 | 126);
            frame[2] = (byte)(length >> 8);
            frame[3] = (byte)(length & 0xFF);
            System.arraycopy(maskingKey, 0, frame, 4, 4);
            System.arraycopy(masked, 0, frame, 8, length);
            return frame;
        } else {
            throw new IllegalArgumentException("Message too long");
        }
    }

    private void listenForMessages() {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        while (connected.get() && !Thread.currentThread().isInterrupted()) {
            try {
                buffer.clear();
                int bytesRead = socketChannel.read(buffer);
                if (bytesRead == -1) break;
                if (bytesRead > 0) {
                    buffer.flip();
                    byte[] data = new byte[buffer.remaining()];
                    buffer.get(data);
                    String msg = parseWebSocketFrame(data);
                    if (msg != null) handleReceivedMessage(msg);
                }
                Thread.sleep(10);
            } catch (Exception e) {
                if (connected.get()) notifyError("Connection error: " + e.getMessage());
                break;
            }
        }
        if (connected.get()) disconnect();
    }

    private void handleReceivedMessage(String message) {
        if (message.startsWith("[AMMO_REQUEST_BROADCAST]")) {
            handleAmmoRequestBroadcast(message.substring(24));
        } else if (message.startsWith("[AMMO_TRANSFER]")) {
            handleAmmoTransfer(message.substring(15));
        } else if (message.startsWith("[PLAYER_LIST]")) {
            handlePlayerList(message.substring(13));
        } else if (message.startsWith("[SYSTEM]")) {
            handleSystemMessage(message.substring(8));
        } else {
            notifyMessageReceived(message);
        }
    }

    private void handleAmmoRequestBroadcast(String data) {
        String[] parts = data.split(":", 3);
        if (parts.length >= 2 && ammoCallback != null) {
            try {
                int amount = Integer.parseInt(parts[1]);
                String msg = parts.length > 2 ? parts[2] : "";
                ammoCallback.onAmmoRequest(parts[0], amount, msg);
            } catch (NumberFormatException ignored) {}
        }
    }

    private void handleAmmoTransfer(String data) {
        String[] parts = data.split(":", 3);
        if (parts.length == 3 && ammoCallback != null) {
            boolean sent = "sent".equals(parts[0]);
            try {
                int amount = Integer.parseInt(parts[2]);
                ammoCallback.onAmmoTransfer(sent, parts[1], amount);
            } catch (NumberFormatException ignored) {}
        }
    }

    private void handlePlayerList(String data) {
        if (ammoCallback != null) ammoCallback.onPlayerListUpdate(data);
    }

    private void handleSystemMessage(String msg) {
        if (ammoCallback != null) ammoCallback.onSystemMessage(msg.trim());
    }

    private String parseWebSocketFrame(byte[] data) {
        if (data.length < 2) return null;
        boolean fin = (data[0] & 0x80) != 0;
        int opcode = data[0] & 0x0F;
        boolean masked = (data[1] & 0x80) != 0;
        int len = data[1] & 0x7F;
        if (opcode == 1 && fin && !masked) {
            int header = 2;
            if (len == 126) {
                len = ((data[2] & 0xFF) << 8) | (data[3] & 0xFF);
                header = 4;
            }
            if (data.length >= header + len) {
                return new String(data, header, len);
            }
        }
        return null;
    }

    private void notifyMessageReceived(String message) {
        if (messageCallback != null) messageCallback.onMessageReceived(message);
    }

    private void notifyConnectionStatus(boolean status) {
        if (messageCallback != null) messageCallback.onConnectionStatusChanged(status);
    }

    private void notifyError(String error) {
        if (messageCallback != null) messageCallback.onError(error);
    }

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
