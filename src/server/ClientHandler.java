package server;

import messages.Message;
import messages.MessageType;
import serverServices.AuthService;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

            ExecutorService exs = Executors.newSingleThreadExecutor();
            exs.execute(() -> {
                try {
                    if (doAuth()) {
                        readMessage();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    closeConnection();
                }
            });

            exs.shutdown();

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
                AuthService.Record possibleRecord = server.getAuthService().findRecord(message.getUserFrom(), message.getMessage());
                if (possibleRecord != null) {
                    if (!server.isOccupied(possibleRecord)) {
                        record = possibleRecord;
                        Message msg = new Message(MessageType.AUTH_OK, "server", record.getName(), String.valueOf(record.getId()));
                        System.out.println(msg.toString());
                        sendMessage(msg);
                        server.broadcastMessage(new Message(MessageType.LOGIN, "server", "ALL", record.getName()));
                        server.subscribe(this);
                        return true;
                    } else {
                        sendMessage(new Message(MessageType.AUTH_FAILED, "server", message.getUserFrom(), String.format("Current user [%s] is already occupied", possibleRecord.getName())));
                    }
                } else {
                    sendMessage((new Message(MessageType.AUTH_FAILED, "server", message.getUserFrom(), "User not found")));
                }
            } else if (message.getMessageType() == MessageType.EXIT){
                sendMessage((new Message(MessageType.EXIT, "server", message.getUserFrom(),"Connection closed")));
                return false;
            }
        }
    }

    public void sendMessage(Message msg) {
        try {
            out.writeUTF(msg.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void readMessage() throws IOException {
        while (true){
            Message message = new Message(in.readUTF());
            System.out.printf("Incoming message from %s: %s%n", record.getName(), message);

            switch (message.getMessageType()) {
                case MSG:
                    server.broadcastMessage(message);
                    break;
                case PRVMSG:
                    server.privateMessage(message);
                    break;
                case GET_USERS:
                    sendMessage(new Message(MessageType.USERS, "server", message.getUserFrom(), server.getUsersArray()));
                    break;
                case CHGNAME:
                    if (server.getAuthService().changeName(record, message.getMessage())) {
                        server.broadcastMessage(new Message(MessageType.CHGNAMEOK, "server", message.getUserFrom(), record.getName()));
                    } else {
                        server.broadcastMessage(new Message(MessageType.CHGNAMEFAILED, "server", message.getUserFrom(), record.getName()));
                    }
                    break;
                case EXIT:
                    sendMessage((new Message(MessageType.EXIT, "server", message.getUserFrom(), "Connection closed")));
                    return;
            }
        }
    }

    public void closeConnection() {
        server.unsubscribe(this);
        if (record != null) {
            server.broadcastMessage(new Message(MessageType.LOGOFF, "server", "ALL", record.getName()));
        }
        System.out.printf("Connection /%s closed%n", socket);

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