package server;

import messages.Message;
import serverServices.AuthService;
import serverServices.DBAuthService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

public class ChatServer {
    private final static int SERVER_PORT = 8765;
    private Scanner scanner;
    private AuthService authService;
    private Set<ClientHandler> clientHandlers;

    public ChatServer() {
        startServer();
    }

    private void sendMsg() {
        while (true) {
                System.out.println("Enter msg:");
                String msg = scanner.nextLine();
                broadcastMessage(new Message(msg));
        }
    }

    private void startServer(){
        try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)){
            System.out.printf("Server is running on port: %d%n", serverSocket.getLocalPort());

            authService = new DBAuthService();
            clientHandlers = new HashSet<>();

            scanner = new Scanner(System.in);
            Thread inputThread = new Thread(this::sendMsg);
            inputThread.setDaemon(true);
            inputThread.start();

            while (true){
                System.out.println("Waiting for connections...");
                Socket socket = serverSocket.accept();
                System.out.println("Connection established with " + socket);
                new ClientHandler(this, socket);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Server stopped...");
    }
    public AuthService getAuthService() {
        return authService;
    }

    public synchronized boolean isOccupied(AuthService.Record record) {
        for (ClientHandler ch : clientHandlers) {
            if (ch.getRecord().equals(record)) {
                return true;
            }
        }
        return false;
    }

    public synchronized void subscribe(ClientHandler ch) {
        clientHandlers.add(ch);
    }

    public synchronized void unsubscribe(ClientHandler ch) {
        clientHandlers.remove(ch);
    }

    public synchronized void broadcastMessage(Message message) {
        for (ClientHandler ch : clientHandlers) {
            ch.sendMessage(message);
        }
    }

    public synchronized void privateMessage(Message message) {
        for (ClientHandler ch : clientHandlers) {
            if (ch.getRecord().getName().equals(message.getUserFrom()) || ch.getRecord().getName().equals(message.getUserTo())){
                ch.sendMessage(message);
            }
        }
    }

    public synchronized String[] getUsersArray(){
        String[] usersArr = new String[clientHandlers.size()];
        int i = 0;
        for (ClientHandler ch: clientHandlers
             ) {
            usersArr[i] = ch.getRecord().getName();
            i++;
        }

        return usersArr;
    }

}
