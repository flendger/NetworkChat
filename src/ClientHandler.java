import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Objects;

public class ClientHandler {
    private AuthService.Record record;
    private final ChatServer server;
    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;

    public ClientHandler(ChatServer server, Socket socket) {
        try {
            this.server = server;
            this.socket = socket;
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (doAuth()) {
                            readMessage();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        closeConnection();
                    }
                }
            })
                    .start();

        } catch (IOException e) {
            throw new RuntimeException("Client handler was not created");
        }
    }

    public AuthService.Record getRecord() {
        return record;
    }

    public boolean doAuth() throws IOException {
        while (true) {
            System.out.println("Waiting for auth...");
            Message message = new Message(in.readUTF());
            if (message.getMessageType() == MessageType.AUTH) {
                AuthService.Record possibleRecord = server.getAuthService().findRecord(message.getUser(), message.getMessage());
                if (possibleRecord != null) {
                    if (!server.isOccupied(possibleRecord)) {
                        record = possibleRecord;
                        Message msg = new Message(MessageType.AUTH_OK, "server", record.getName());
                        System.out.println(msg.toString());
                        sendMessage(msg.toString());
                        server.broadcastMessage(new Message(MessageType.LOGIN, "server", record.getName()).toString());
                        server.subscribe(this);
                        return true;
                    } else {
                        sendMessage(new Message(MessageType.AUTH_FAILED, "server", String.format("Current user [%s] is already occupied", possibleRecord.getName())).toString());
                    }
                } else {
                    sendMessage((new Message(MessageType.AUTH_FAILED, "server", "User not found")).toString());
                }
            } else if (message.getMessageType() == MessageType.EXIT){
                sendMessage((new Message(MessageType.EXIT, "server", "Connection closed")).toString());
                return false;
            }
        }
    }

    public void sendMessage(String message) {
        try {
            out.writeUTF(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void readMessage() throws IOException {
        while (true){
            Message message = new Message(in.readUTF());
            System.out.println(String.format("Incoming message from %s: %s", record.getName(), message));

            switch (message.getMessageType()) {
                case MSG:
                    server.broadcastMessage(message.toString());
                    break;
                case GET_USERS:
                    sendMessage(new Message(MessageType.USERS, "server", server.getUsersArray()).toString());
                    break;
                case CHGNAME:
                    if (server.getAuthService().changeName(record, message.getMessage())) {
                        server.broadcastMessage(new Message(MessageType.CHGNAMEOK, message.getUser(), record.getName()).toString());
                    }
                    break;
                case EXIT:
                    sendMessage((new Message(MessageType.EXIT, "server", "Connection closed")).toString());
                    return;
            }
        }
    }

    public void closeConnection() {
        server.unsubscribe(this);
        if (record != null) {
            server.broadcastMessage(new Message(MessageType.LOGOFF, "server", record.getName()).toString());
        }
        System.out.println(String.format("Connection /%s closed", socket));

        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClientHandler that = (ClientHandler) o;
        return record.equals(that.record) &&
                server.equals(that.server) &&
                socket.equals(that.socket) &&
                in.equals(that.in) &&
                out.equals(that.out);
    }

    @Override
    public int hashCode() {
        return Objects.hash(record, server, socket, in, out);
    }
}