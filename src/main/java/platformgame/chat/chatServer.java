// src/platformgame/chat/chatServer.java
package platformgame.chat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class chatServer {
    private static final int PORT = 8080;
    private static final String WEBSOCKET_MAGIC_STRING =
            "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    private ServerSocketChannel serverChannel;
    private Selector selector;
    private Map<SocketChannel, ClientConnection> clients = new ConcurrentHashMap<>();
    private Map<String,Integer> playerAmmo      = new ConcurrentHashMap<>();
    private Map<String,List<AmmoRequest>> ammoRequests = new ConcurrentHashMap<>();
    private Map<String,Long> lastRequestTime   = new ConcurrentHashMap<>();

    private static class AmmoRequest {
        String requester;
        int    amount;
        long   timestamp;
        String message;
        AmmoRequest(String r,int a,String m){
            requester=r; amount=a; timestamp=System.currentTimeMillis(); message=m;
        }
    }

    public void start() throws IOException {
        serverChannel = ServerSocketChannel.open();
        // Listen on all interfaces, not just localhost:
        serverChannel.bind(new InetSocketAddress("0.0.0.0", PORT));
        serverChannel.configureBlocking(false);

        selector = Selector.open();
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        System.out.println("Chat server started on 0.0.0.0:" + PORT);

        while (true) {
            selector.select();
            Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
            while (keys.hasNext()) {
                SelectionKey key = keys.next();
                keys.remove();
                if (key.isAcceptable()) acceptConnection();
                else if (key.isReadable()) readFromClient(key);
            }
        }
    }

    private void acceptConnection() throws IOException {
        SocketChannel client = serverChannel.accept();
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ);
        clients.put(client, new ClientConnection(client));
        System.out.println("New client connected: " + client.getRemoteAddress());
    }

    private void readFromClient(SelectionKey key) {
        SocketChannel client = (SocketChannel)key.channel();
        ClientConnection cc = clients.get(client);
        ByteBuffer buf = ByteBuffer.allocate(1024);
        int read;
        try {
            read = client.read(buf);
            if (read == -1) { disconnectClient(client); return; }
            buf.flip();
            byte[] data = new byte[buf.remaining()];
            buf.get(data);
            if (!cc.handshakeComplete) handleHandshake(cc, data);
            else handleWebSocketFrame(cc, data);
        } catch(IOException e) {
            disconnectClient(client);
        }
    }

    private void handleHandshake(ClientConnection cc, byte[] data) throws IOException {
        String req = new String(data);
        String key = extractKey(req);
        if (key != null) {
            String resp = createHandshakeResponse(key);
            cc.channel.write(ByteBuffer.wrap(resp.getBytes()));
            cc.handshakeComplete = true;
            System.out.println("Handshake completed for " + cc.channel.getRemoteAddress());
        }
    }

    private String extractKey(String req) {
        Matcher m = Pattern.compile("Sec-WebSocket-Key: (.+)").matcher(req);
        return m.find() ? m.group(1).trim() : null;
    }

    private String createHandshakeResponse(String key) {
        try {
            String accept = key + WEBSOCKET_MAGIC_STRING;
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] hash = md.digest(accept.getBytes());
            String val = Base64.getEncoder().encodeToString(hash);
            return "HTTP/1.1 101 Switching Protocols\r\n" +
                    "Upgrade: websocket\r\n" +
                    "Connection: Upgrade\r\n" +
                    "Sec-WebSocket-Accept: " + val + "\r\n\r\n";
        } catch(NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void handleWebSocketFrame(ClientConnection cc, byte[] data) throws IOException {
        if (data.length < 2) return;
        boolean fin    = (data[0] & 0x80) != 0;
        int     opcode = data[0] & 0x0F;
        boolean masked = (data[1] & 0x80) != 0;
        int     len    = data[1] & 0x7F;
        if (opcode==1 && fin && masked) {
            int offset = 2;
            if (len==126) { len = ((data[2]&0xFF)<<8)|(data[3]&0xFF); offset=4; }
            byte[] mask   = Arrays.copyOfRange(data, offset, offset+4);
            byte[] payload= Arrays.copyOfRange(data, offset+4, offset+4+len);
            for (int i=0;i<payload.length;i++) payload[i]^=mask[i%4];
            String msg = new String(payload);
            routeMessage(cc, msg);
        }
    }

    private void routeMessage(ClientConnection cc, String msg) throws IOException {
        if      (msg.startsWith("[AMMO_UPDATE]"))  handleAmmoUpdate(cc, msg);
        else if (msg.startsWith("[AMMO_REQUEST]")) handleAmmoRequest(cc, msg);
        else if (msg.startsWith("[AMMO_SEND]"))    handleAmmoSend(cc, msg);
        else if (msg.startsWith("[PLAYER_LIST]"))  handlePlayerListRequest(cc);
        else { // chat
            String[] p = msg.split(":",2);
            if (p.length == 2) {
                String newUsername = p[0].trim();
                // Check if username already exists (other than this connection)
                boolean taken = clients.values().stream()
                        .anyMatch(c -> c != cc && newUsername.equalsIgnoreCase(c.username));
                if (taken) {
                    send(cc, "[SYSTEM] Username '" + newUsername + "' is already in use. Please restart with a different name.");
                    disconnectClient(cc.channel);
                    return;
                }
                cc.username = newUsername;
                broadcast(newUsername, p[1]);
            }


        }
    }

    private void handleAmmoUpdate(ClientConnection cc, String msg) {
        String[] p = msg.substring(13).split(":",2);
        if (p.length==2) {
            try {
                int a = Integer.parseInt(p[1]);
                playerAmmo.put(p[0], a);
                cc.username = p[0];
            } catch(Exception ignored){}
        }
    }

    private void handleAmmoRequest(ClientConnection cc, String msg) throws IOException {
        String[] p = msg.substring(15).split(":",3);
        if (p.length>=2) {
            String requester = p[0];
            int amt = Integer.parseInt(p[1]);
            String m = p.length>2? p[2] : "";
            long now = System.currentTimeMillis();
            Long last = lastRequestTime.get(requester);
            if (last!=null && now-last<300_000) {
                send(cc, "[SYSTEM] Wait before next request.");
                return;
            }
            lastRequestTime.put(requester, now);
            cc.username = requester;
            String bc = "[AMMO_REQUEST_BROADCAST]"+requester+":"+amt+":"+m;
            broadcastSystem("[SYSTEM] " + requester + " requests " + amt + " ammo");
            broadcastSystem(bc);
        }
    }

    private void handleAmmoSend(ClientConnection cc, String msg) throws IOException {
        String[] p = msg.substring(12).split(":",3);
        if (p.length==3) {
            String s=p[0], r=p[1];
            int amt=Integer.parseInt(p[2]);
            Integer have = playerAmmo.getOrDefault(s,0);
            if (have<amt) { send(cc, "[SYSTEM] Not enough ammo."); return; }
            ClientConnection targ = findByUsername(r);
            if (targ==null) { send(cc, "[SYSTEM] "+r+" not online."); return; }
            playerAmmo.put(s, have-amt);
            playerAmmo.put(r, playerAmmo.getOrDefault(r,0)+amt);
            send(cc, "[AMMO_TRANSFER]sent:"+r+":"+amt);
            send(targ, "[AMMO_TRANSFER]received:"+s+":"+amt);
            broadcastSystem("System: "+s+" sent "+amt+" ammo to "+r);
        }
    }

    private void handlePlayerListRequest(ClientConnection cc) throws IOException {
        StringBuilder sb = new StringBuilder("[PLAYER_LIST]");
        for (ClientConnection c: clients.values()) {
            if (c.handshakeComplete && c.username!=null) {
                sb.append(c.username).append(":")
                        .append(playerAmmo.getOrDefault(c.username,0))
                        .append(",");
            }
        }
        send(cc, sb.toString());
    }

    private ClientConnection findByUsername(String u) {
        return clients.values()
                .stream()
                .filter(c->u.equals(c.username))
                .findFirst()
                .orElse(null);
    }

    private void broadcast(String user, String msg) {
        String full = user+": "+msg;
        byte[] frame = makeServerFrame(full);
        clients.values().forEach(c->{
            if (c.handshakeComplete) {
                try { c.channel.write(ByteBuffer.wrap(frame)); }
                catch(IOException ignored){}
            }
        });
    }

    private void broadcastSystem(String msg) {
        byte[] f = makeServerFrame(msg);
        clients.values().forEach(c->{
            if (c.handshakeComplete) {
                try { c.channel.write(ByteBuffer.wrap(f)); }
                catch(IOException ignored){}
            }
        });
    }

    private void send(ClientConnection cc, String msg) throws IOException {
        cc.channel.write(ByteBuffer.wrap(makeServerFrame(msg)));
    }

    private byte[] makeServerFrame(String msg) {
        byte[] p = msg.getBytes();
        if (p.length<126) {
            byte[] f = new byte[2+p.length];
            f[0]=(byte)0x81; f[1]=(byte)p.length;
            System.arraycopy(p,0,f,2,p.length);
            return f;
        } else {
            byte[] f = new byte[4+p.length];
            f[0]=(byte)0x81; f[1]=126;
            f[2]=(byte)(p.length>>8); f[3]=(byte)(p.length&0xFF);
            System.arraycopy(p,0,f,4,p.length);
            return f;
        }
    }

    private void disconnectClient(SocketChannel ch) {
        try {
            ClientConnection cc = clients.remove(ch);
            if (cc!=null && cc.username!=null) {
                playerAmmo.remove(cc.username);
                lastRequestTime.remove(cc.username);
                broadcastSystem("System: "+cc.username+" has left");
            }
            ch.close();
        } catch(IOException ignored){}
    }

    private static class ClientConnection {
        SocketChannel channel;
        boolean handshakeComplete = false;
        String username;
        ClientConnection(SocketChannel ch){ channel=ch; }
    }

    public static void main(String[] args) {
        try {
            new chatServer().start();
        } catch(IOException e) {
            System.err.println("Server error: "+e.getMessage());
        }
    }
}
