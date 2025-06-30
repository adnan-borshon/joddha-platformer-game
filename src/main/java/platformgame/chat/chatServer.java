package platformgame.chat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class chatServer {
    private static final int PORT = 8080;
    private static final String WEBSOCKET_MAGIC_STRING = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    private ServerSocketChannel serverChannel;
    private Selector selector;
    private Map<SocketChannel, ClientConnection> clients;
    private boolean running;

    // Ammo system storage
    private Map<String, Integer> playerAmmo; // username -> ammo count
    private Map<String, List<AmmoRequest>> ammoRequests; // requester -> list of requests
    private Map<String, Long> lastRequestTime; // cooldown for requests

    public chatServer() {
        clients = new ConcurrentHashMap<>();
        playerAmmo = new ConcurrentHashMap<>();
        ammoRequests = new ConcurrentHashMap<>();
        lastRequestTime = new ConcurrentHashMap<>();
    }

    // Inner class for ammo requests
    private static class AmmoRequest {
        String requester;
        int amount;
        long timestamp;
        String message;

        AmmoRequest(String requester, int amount, String message) {
            this.requester = requester;
            this.amount = amount;
            this.timestamp = System.currentTimeMillis();
            this.message = message;
        }
    }

    public void start() throws IOException {
        serverChannel = ServerSocketChannel.open();
        serverChannel.bind(new InetSocketAddress(PORT));
        serverChannel.configureBlocking(false);

        selector = Selector.open();
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        running = true;
        System.out.println("Chat server with ammo system started on port " + PORT);

        while (running) {
            selector.select();
            Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

            while (keys.hasNext()) {
                SelectionKey key = keys.next();
                keys.remove();

                if (key.isAcceptable()) {
                    acceptConnection();
                } else if (key.isReadable()) {
                    readFromClient(key);
                }
            }
        }
    }

    private void acceptConnection() throws IOException {
        SocketChannel clientChannel = serverChannel.accept();
        clientChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_READ);

        ClientConnection client = new ClientConnection(clientChannel);
        clients.put(clientChannel, client);

        System.out.println("New client connected: " + clientChannel.getRemoteAddress());
    }

    private void readFromClient(SelectionKey key) {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        ClientConnection client = clients.get(clientChannel);

        try {
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            int bytesRead = clientChannel.read(buffer);

            if (bytesRead == -1) {
                disconnectClient(clientChannel);
                return;
            }

            buffer.flip();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);

            if (!client.isHandshakeComplete()) {
                handleWebSocketHandshake(client, data);
            } else {
                handleWebSocketFrame(client, data);
            }

        } catch (IOException e) {
            disconnectClient(clientChannel);
        }
    }

    private void handleWebSocketHandshake(ClientConnection client, byte[] data) throws IOException {
        String request = new String(data);
        String webSocketKey = extractWebSocketKey(request);

        if (webSocketKey != null) {
            String response = createWebSocketResponse(webSocketKey);
            client.getChannel().write(ByteBuffer.wrap(response.getBytes()));
            client.setHandshakeComplete(true);
            System.out.println("WebSocket handshake completed for client");
        }
    }

    private String extractWebSocketKey(String request) {
        Pattern pattern = Pattern.compile("Sec-WebSocket-Key: (.+)");
        Matcher matcher = pattern.matcher(request);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private String createWebSocketResponse(String webSocketKey) {
        try {
            String acceptKey = webSocketKey + WEBSOCKET_MAGIC_STRING;
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] hash = md.digest(acceptKey.getBytes());
            String acceptValue = Base64.getEncoder().encodeToString(hash);

            return "HTTP/1.1 101 Switching Protocols\r\n" +
                    "Upgrade: websocket\r\n" +
                    "Connection: Upgrade\r\n" +
                    "Sec-WebSocket-Accept: " + acceptValue + "\r\n\r\n";
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to create WebSocket response", e);
        }
    }

    private void handleWebSocketFrame(ClientConnection client, byte[] data) {
        if (data.length < 2) return;

        // Simple frame parsing (assumes text frames and payload < 126 bytes)
        boolean fin = (data[0] & 0x80) != 0;
        int opcode = data[0] & 0x0F;
        boolean masked = (data[1] & 0x80) != 0;
        int payloadLength = data[1] & 0x7F;

        if (opcode == 0x1 && fin && masked) { // Text frame
            byte[] maskingKey = Arrays.copyOfRange(data, 2, 6);
            byte[] payload = Arrays.copyOfRange(data, 6, 6 + payloadLength);

            // Unmask payload
            for (int i = 0; i < payload.length; i++) {
                payload[i] ^= maskingKey[i % 4];
            }

            String message = new String(payload);
            System.out.println("Received message: " + message);

            // Handle different message types
            if (message.startsWith("[AMMO_UPDATE]")) {
                handleAmmoUpdate(client, message);
            } else if (message.startsWith("[AMMO_REQUEST]")) {
                handleAmmoRequest(client, message);
            } else if (message.startsWith("[AMMO_SEND]")) {
                handleAmmoSend(client, message);
            } else if (message.startsWith("[PLAYER_LIST]")) {
                handlePlayerListRequest(client);
            } else {
                // Regular chat message
                String[] parts = message.split(":", 2);
                if (parts.length == 2) {
                    client.setUsername(parts[0]);
                    broadcastMessage(parts[0], parts[1]);
                }
            }
        }
    }

    private void handleAmmoUpdate(ClientConnection client, String message) {
        // Format: [AMMO_UPDATE]username:ammoCount
        String[] parts = message.substring(13).split(":", 2);
        if (parts.length == 2) {
            String username = parts[0];
            try {
                int ammoCount = Integer.parseInt(parts[1]);
                playerAmmo.put(username, ammoCount);
                client.setUsername(username);
                System.out.println("Updated ammo for " + username + ": " + ammoCount);
            } catch (NumberFormatException e) {
                System.err.println("Invalid ammo count in update: " + parts[1]);
            }
        }
    }

    private void handleAmmoRequest(ClientConnection client, String message) {
        // Format: [AMMO_REQUEST]username:amount:message
        String[] parts = message.substring(15).split(":", 3);
        if (parts.length >= 2) {
            String requester = parts[0];
            try {
                int amount = Integer.parseInt(parts[1]);
                String requestMessage = parts.length > 2 ? parts[2] : "requesting ammo";

                // Check cooldown (5 minutes between requests)
                long currentTime = System.currentTimeMillis();
                Long lastRequest = lastRequestTime.get(requester);
                if (lastRequest != null && (currentTime - lastRequest) < 300000) { // 5 minutes
                    sendToClient(client, "[SYSTEM] You must wait before making another ammo request.");
                    return;
                }

                lastRequestTime.put(requester, currentTime);
                client.setUsername(requester);

                // Broadcast ammo request to all other players
                String requestBroadcast = "[AMMO_REQUEST_BROADCAST]" + requester + ":" + amount + ":" + requestMessage;
                broadcastToOthers(client, "[SYSTEM] " + requester + " is requesting " + amount + " ammo: " + requestMessage);
                broadcastSystemMessage(requestBroadcast);

                System.out.println("Ammo request from " + requester + " for " + amount + " ammo");
            } catch (NumberFormatException e) {
                sendToClient(client, "[SYSTEM] Invalid ammo amount in request.");
            }
        }
    }

    private void handleAmmoSend(ClientConnection client, String message) {
        // Format: [AMMO_SEND]sender:recipient:amount
        String[] parts = message.substring(12).split(":", 3);
        if (parts.length == 3) {
            String sender = parts[0];
            String recipient = parts[1];
            try {
                int amount = Integer.parseInt(parts[2]);

                // Check if sender has enough ammo
                Integer senderAmmo = playerAmmo.get(sender);
                if (senderAmmo == null || senderAmmo < amount) {
                    sendToClient(client, "[SYSTEM] You don't have enough ammo to send.");
                    return;
                }

                // Check if recipient is online
                ClientConnection recipientClient = findClientByUsername(recipient);
                if (recipientClient == null) {
                    sendToClient(client, "[SYSTEM] Player " + recipient + " is not online.");
                    return;
                }

                // Process the transfer
                playerAmmo.put(sender, senderAmmo - amount);
                Integer recipientAmmo = playerAmmo.getOrDefault(recipient, 0);
                playerAmmo.put(recipient, recipientAmmo + amount);

                // Notify both players
                sendToClient(client, "[AMMO_TRANSFER]sent:" + recipient + ":" + amount);
                sendToClient(recipientClient, "[AMMO_TRANSFER]received:" + sender + ":" + amount);

                // Broadcast the transfer
                broadcastMessage("System", sender + " sent " + amount + " ammo to " + recipient);

                System.out.println("Ammo transfer: " + sender + " -> " + recipient + " (" + amount + ")");

            } catch (NumberFormatException e) {
                sendToClient(client, "[SYSTEM] Invalid ammo amount.");
            }
        }
    }

    private void handlePlayerListRequest(ClientConnection client) {
        StringBuilder playerList = new StringBuilder("[PLAYER_LIST]");
        for (ClientConnection c : clients.values()) {
            if (c.isHandshakeComplete() && c.getUsername() != null) {
                String username = c.getUsername();
                int ammo = playerAmmo.getOrDefault(username, 0);
                playerList.append(username).append(":").append(ammo).append(",");
            }
        }
        sendToClient(client, playerList.toString());
    }

    private ClientConnection findClientByUsername(String username) {
        for (ClientConnection client : clients.values()) {
            if (username.equals(client.getUsername())) {
                return client;
            }
        }
        return null;
    }

    private void sendToClient(ClientConnection client, String message) {
        byte[] frame = createWebSocketFrame(message);
        try {
            client.getChannel().write(ByteBuffer.wrap(frame));
        } catch (IOException e) {
            System.err.println("Failed to send message to client: " + e.getMessage());
        }
    }

    private void broadcastToOthers(ClientConnection sender, String message) {
        byte[] frame = createWebSocketFrame(message);
        clients.values().forEach(client -> {
            if (client.isHandshakeComplete() && client != sender) {
                try {
                    client.getChannel().write(ByteBuffer.wrap(frame));
                } catch (IOException e) {
                    System.err.println("Failed to send message to client: " + e.getMessage());
                }
            }
        });
    }

    private void broadcastSystemMessage(String message) {
        byte[] frame = createWebSocketFrame(message);
        clients.values().forEach(client -> {
            if (client.isHandshakeComplete()) {
                try {
                    client.getChannel().write(ByteBuffer.wrap(frame));
                } catch (IOException e) {
                    System.err.println("Failed to send message to client: " + e.getMessage());
                }
            }
        });
    }

    private void broadcastMessage(String username, String message) {
        String fullMessage = username + ": " + message;
        byte[] frame = createWebSocketFrame(fullMessage);

        clients.values().forEach(client -> {
            if (client.isHandshakeComplete()) {
                try {
                    client.getChannel().write(ByteBuffer.wrap(frame));
                } catch (IOException e) {
                    System.err.println("Failed to send message to client: " + e.getMessage());
                }
            }
        });
    }

    private byte[] createWebSocketFrame(String message) {
        byte[] payload = message.getBytes();
        byte[] frame;

        if (payload.length < 126) {
            frame = new byte[2 + payload.length];
            frame[0] = (byte) 0x81; // FIN + text frame
            frame[1] = (byte) payload.length;
            System.arraycopy(payload, 0, frame, 2, payload.length);
        } else {
            // Handle longer payloads if needed
            frame = new byte[4 + payload.length];
            frame[0] = (byte) 0x81;
            frame[1] = 126;
            frame[2] = (byte) (payload.length >> 8);
            frame[3] = (byte) (payload.length & 0xFF);
            System.arraycopy(payload, 0, frame, 4, payload.length);
        }

        return frame;
    }

    private void disconnectClient(SocketChannel clientChannel) {
        try {
            ClientConnection client = clients.remove(clientChannel);
            if (client != null && client.getUsername() != null) {
                String username = client.getUsername();
                playerAmmo.remove(username);
                lastRequestTime.remove(username);
                broadcastMessage("System", username + " has left the game");
            }
            clientChannel.close();
            System.out.println("Client disconnected");
        } catch (IOException e) {
            System.err.println("Error disconnecting client: " + e.getMessage());
        }
    }

    public void stop() {
        running = false;
        try {
            if (selector != null) selector.close();
            if (serverChannel != null) serverChannel.close();
        } catch (IOException e) {
            System.err.println("Error stopping server: " + e.getMessage());
        }
    }

    // Inner class to represent client connections
    private static class ClientConnection {
        private final SocketChannel channel;
        private boolean handshakeComplete;
        private String username;

        public ClientConnection(SocketChannel channel) {
            this.channel = channel;
            this.handshakeComplete = false;
        }

        public SocketChannel getChannel() { return channel; }
        public boolean isHandshakeComplete() { return handshakeComplete; }
        public void setHandshakeComplete(boolean complete) { this.handshakeComplete = complete; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
    }

    public static void main(String[] args) {
        chatServer server = new chatServer();
        try {
            server.start();
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }
}