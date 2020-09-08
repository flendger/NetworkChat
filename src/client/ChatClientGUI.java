package client;

import messages.Message;
import messages.MessageType;
import serverServices.ClientLogService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;

public class ChatClientGUI extends JFrame {
    private static final String SERVER_ADDR = "localhost";
    private static final int SERVER_PORT = 8765;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    private final JTextArea chatText = new JTextArea();
    private final JTextField chatField = new JTextField();
    private final JButton submitBtn = new JButton("Submit");
    private final DefaultListModel usersListModel = new DefaultListModel();
    private final JList userList = new JList(usersListModel);

    private String currentUser = "";
    private int currentId = -1;

    public ChatClientGUI() {
        prepareGUI();
        openConnection();
        openAuth();
    }

    private void prepareGUI() {
        setTitle("Chat client...");
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel contentPane = new JPanel(new BorderLayout());
        setContentPane(contentPane);

        //split panel
        JSplitPane splitPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

        //users list
        JPanel usersPanel = new JPanel();
        usersPanel.setLayout(new BoxLayout(usersPanel, BoxLayout.Y_AXIS));
        usersPanel.add(new JLabel("USERS:"));

        userList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String selectedUser = userList.getSelectedValue().toString();
                    if (!selectedUser.equals(currentUser)) {
                        chatField.setText(String.format("[%s]: ", selectedUser));
                        chatField.grabFocus();
                    }
                } else {
                super.mouseClicked(e);
                }
            }
        });

        JScrollPane usersScroll = new JScrollPane(userList);
        usersPanel.add(usersScroll);

        splitPanel.setLeftComponent(usersPanel);

        //chat panel whit chattext and chatfield
        JPanel chatPanel = new JPanel();
        chatPanel.setLayout(new BorderLayout());

        chatText.setFont(new Font(Font.DIALOG, Font.BOLD, 14));
        chatText.setEditable(false);
        chatPanel.add(chatText, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(chatField, BorderLayout.CENTER);
        bottomPanel.add(submitBtn, BorderLayout.EAST);
        chatPanel.add(bottomPanel, BorderLayout.SOUTH);

        splitPanel.setRightComponent(chatPanel);

        contentPane.add(splitPanel);

        ActionListener submitAction = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String currentText = chatField.getText();
                if (!currentText.isBlank()) {
                    if (socket.isConnected()) {
                        try {
                            int idxPrv = currentText.indexOf("]:");
                            Message msg = null;
                            if (currentText.substring(0, 1).equals("[") && idxPrv != -1) {
                                //private msg
                                String userTo = currentText.substring(1, idxPrv);
                                String msgText = "";
                                if (idxPrv + 2 < currentText.length()) {
                                    msgText = currentText.substring(idxPrv + 2).trim();
                                }
                                if (msgText.isEmpty()) {
                                    return;
                                }
                                msg = new Message(MessageType.PRVMSG, currentUser, userTo, msgText);
                            } else {
                                //broadcast message
                                msg = new Message(MessageType.MSG, currentUser, "ALL", currentText);
                            }

                            sendMsg(msg.toString());
                            chatField.setText("");
                            chatField.grabFocus();
                        } catch (IOException ioException) {
                            ioException.printStackTrace();
                        }
                    }
                }
            }
        };

        chatField.addActionListener(submitAction);
        submitBtn.addActionListener(submitAction);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);

                try {
                    if (socket.isConnected() && !socket.isClosed()) {
                        sendMsg(new Message(MessageType.EXIT, currentUser, "server", "close connection").toString());
                    }
                } catch (IOException exc) {
                    exc.printStackTrace();
                }
            }
        });

        JMenuBar mainMenu = new JMenuBar();

        JMenu menuConnections = new JMenu("Connections");

        JMenuItem itemConnect = new JMenuItem("Connect");
        itemConnect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!socket.isConnected() || socket.isClosed()) {
                    openConnection();
                }
                openAuth();
            }
        });
        menuConnections.add(itemConnect);

        JMenuItem itemChgName = new JMenuItem("Change name");
        itemChgName.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentUser.isEmpty()) {
                    JOptionPane.showMessageDialog(contentPane, "You are not logged in...");
                    return;
                }

                if (!socket.isConnected() || socket.isClosed()) {
                    openConnection();
                }
                changeName();
            }
        });
        menuConnections.add(itemChgName);

        mainMenu.add(menuConnections);
        contentPane.add(mainMenu, BorderLayout.NORTH);

        setVisible(true);
        chatField.setFocusable(true);
        chatField.grabFocus();
    }

    private void changeName() {
        String newName = JOptionPane.showInputDialog(null, "Enter new name");
        if (!newName.isEmpty()) {
            try {
                sendMsg(new Message(MessageType.CHGNAME, currentUser, "server", newName).toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void openAuth() {
        if (socket.isConnected()) {
            AuthFormGUI authFormGUI = new AuthFormGUI();
            String[] msgArr = authFormGUI.showAuth();
            if (!msgArr[0].isEmpty()) {
                Message msg = new Message(MessageType.AUTH, msgArr[0], "server", msgArr[1]);
                System.out.println(msg);
                try {
                    sendMsg(msg.toString());
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        }
    }

    private void listenServer() {
        try {
            while (true) {
                if (socket.isConnected()) {
                    Message msg = new Message(readMsg());
                    handleIncomingMessage(msg);
                    if (msg.getMessageType() == MessageType.EXIT) {
                        break;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        closeConnection();
    }

    synchronized private void handleIncomingMessage(Message msg) {
        String msgTxt = "";
        switch (msg.getMessageType()) {
            case MSG:
                msgTxt = String.format("%s: %s", msg.getUserFrom(), msg.getMessage());
                addMessageToChat(msgTxt);
                ClientLogService.appendLog(currentId, msgTxt);
                return;
            case PRVMSG:
                msgTxt = String.format("[PRIVATE] %s: %s", msg.getUserFrom(), msg.getMessage());
                addMessageToChat(msgTxt);
                ClientLogService.appendLog(currentId, msgTxt);
                return;
            case AUTH_OK:
                currentUser = msg.getUserTo();
                currentId = Integer.valueOf(msg.getMessage());
                clearChat();
                addMessageToChat("Authorization is OK...");
                addMessageToChat(String.format("You are logged in as %s [id: %d] ...", currentUser, currentId));
                try {
                    sendMsg(new Message(MessageType.GET_USERS, currentUser, "server", "").toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                addMessageToChat(ClientLogService.readLog(currentId));
                return;
            case AUTH_FAILED:
                addMessageToChat("Authorization failed: " + msg.getMessage());
                return;
            case EXIT:
                try {
                    sendMsg(new Message(MessageType.EXIT, currentUser, "server", "close connection").toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            case LOGIN:
                addMessageToChat(String.format("User %s entered chat...", msg.getMessage()));
                addUser(msg.getMessage());
                return;
            case LOGOFF:
                addMessageToChat(String.format("User %s left chat...", msg.getMessage()));
                removeUser(msg.getMessage());
                return;
            case USERS:
                updateUsers(msg.getMessageArray());
                return;
            case CHGNAMEOK:
                removeUser(msg.getUserTo());
                addUser(msg.getMessage());
                if (currentUser.equals(msg.getUserTo())) {
                    currentUser = msg.getMessage();
                }
                addMessageToChat(String.format("User %s changed nickname to %s...", msg.getUserTo(), msg.getMessage()));
                return;
            default:
                return;
        }
    }

    private void addUser(String user) {
        int userIdx = usersListModel.indexOf(user);
        if (userIdx == -1) {
            usersListModel.addElement(user);
        } else {
            usersListModel.setElementAt(user, userIdx);
        }
    }

    private void removeUser(String user) {
        int userIdx = usersListModel.indexOf(user);
        if (userIdx != -1) {
            usersListModel.removeElementAt(userIdx);
        }
    }

    private void updateUsers(String[] users) {
        usersListModel.removeAllElements();
        usersListModel.addAll(Arrays.asList(users));
    }

    private void openConnection() {
        try {
            socket = new Socket();
            synchronized (socket) {
                socket.connect(new InetSocketAddress(SERVER_ADDR, SERVER_PORT));
                in = new DataInputStream(socket.getInputStream());
                out = new DataOutputStream(socket.getOutputStream());
                runServerListener();
            }
        } catch (IOException e) {
            e.printStackTrace();
            addMessageToChat("Connection to server failed...");
        }
    }

    private void sendMsg(String msg) throws IOException {
        synchronized (out) {
            out.writeUTF(msg);
        }
    }

    private String readMsg() throws IOException {
        String msg = "";
        synchronized (in) {
            msg = in.readUTF();
        }
        return msg;
    }

    private void runServerListener() {
        if (socket.isConnected() && !socket.isClosed()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    listenServer();
                }
            }).start();
        }
    }

    private void closeConnection() {
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

        currentUser = "";
        addMessageToChat("Connection closed...");
    }

    private void addMessageToChat(String msg) {
        StringBuilder stringBuilder = new StringBuilder(chatText.getText());
        stringBuilder.append(msg + "\n");
        chatText.setText(stringBuilder.toString());
        stringBuilder.setLength(0);
    }

    private void clearChat() {
        chatText.setText("");
    }
}
