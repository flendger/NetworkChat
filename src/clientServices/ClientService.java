package clientServices;

import client.chatClient;
import messages.Message;
import messages.MessageType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class ClientService {
    private final chatClient clientView;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    public ClientService(chatClient clientView) {
        this.clientView = clientView;
    }

    public boolean isActive() {
        return (!socket.isClosed() && socket.isConnected());
    }

    public void openConnection(String srvAddress, int srvPort) throws IOException{
        socket = new Socket();
        synchronized (socket) {
            socket.connect(new InetSocketAddress(srvAddress, srvPort));
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            runServerListener();
        }
    }

    private void runServerListener() {
        if (isActive()) {
            new Thread(this::listenServer).start();
        }
    }

    private void listenServer() {
        while (true) {
            if (isActive()) {
                Message msg;
                try {
                    msg = new Message(readMsg());
                } catch (IOException e) {
                    e.printStackTrace();
                    clientView.handleLogInfo("Some issues with the connection...");
                    break;
                }
                clientView.handleIncomingMessage(msg);
                if (msg.getMessageType() == MessageType.EXIT) {
                    break;
                }
            }
        }

        closeConnection();
    }

    public void closeConnection() {
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

        clientView.connectionClosed();
    }

    public void sendMsg(Message msg) {
        try {
            synchronized (out) {
                out.writeUTF(msg.toString());
            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    private String readMsg() throws IOException{
        String msg;
        synchronized (in) {
            msg = in.readUTF();
        }
        return msg;
    }
}
