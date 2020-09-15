package client;

import clientServices.ClientService;
import messages.Message;
import messages.MessageType;
import clientServices.ClientLogService;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

//TODO: 23) add text colors to chat text field
//TODO: 24) add users registration
public class ChatClientGUI extends JFrame implements chatClient {
    private String serverAddr = "localhost";
    private int serverPort = 8765;

    private final ClientService clientService = new ClientService(this);

    private final JTextArea chatText = new JTextArea();
    private final JTextField chatField = new JTextField();
    private final JButton submitBtn = new JButton("Submit");
    private final DefaultListModel<String> usersListModel = new DefaultListModel<>();
    private final JList<String> userList = new JList<>(usersListModel);

    private String currentUser = "";
    private int currentId = -1;

    public ChatClientGUI() {
        readSettings();
        prepareGUI();
        connect();
        openAuth();
    }

    private void readSettings() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new File("clsettings.xml"));

            NodeList serverNodes = document.getDocumentElement().getElementsByTagName("server");
            if (serverNodes.getLength() > 0) {
                Node item = serverNodes.item(0);
                NamedNodeMap namedNodeMap = item.getAttributes();
                serverAddr = namedNodeMap.getNamedItem("value").getNodeValue();
            }

            NodeList portNodes = document.getDocumentElement().getElementsByTagName("port");
            if (portNodes.getLength() > 0) {
                Node item = portNodes.item(0);
                NamedNodeMap namedNodeMap = item.getAttributes();
                serverPort = Integer.parseInt(namedNodeMap.getNamedItem("value").getNodeValue());
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }

        System.out.println(serverAddr);
        System.out.println(serverPort);
    }

    private void saveSettings() {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            //root elements
            Document doc = docBuilder.newDocument();

            Element rootElement = doc.createElement("network-settings");
            doc.appendChild(rootElement);

            //server element
            Element serverElement = doc.createElement("server");
            rootElement.appendChild(serverElement);
            //set attribute to server element
            serverElement.setAttribute("value", serverAddr);

            //server element
            Element portElement = doc.createElement("port");
            rootElement.appendChild(portElement);
            //set attribute to server element
            portElement.setAttribute("value", String.valueOf(serverPort));

            //write the content into xml file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);

            StreamResult result = new StreamResult(new File("clsettings.xml"));
            transformer.transform(source, result);

            System.out.println("Settings are up to date...");

        } catch (ParserConfigurationException | TransformerException e) {
            e.printStackTrace();
        }
    }

    private void prepareGUI() {
        setChatTitle();
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
        JLabel usersLabel = new JLabel("USERS:");
        usersLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    if (clientService.isActive()) {
                        clientService.sendMsg(new Message(MessageType.GET_USERS, currentUser, "server", ""));
                    }
                } else {
                    super.mouseClicked(e);
                }
            }
        });
        usersPanel.add(usersLabel);

        userList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String selectedUser = userList.getSelectedValue();
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
        DefaultCaret caret = (DefaultCaret) chatText.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        JScrollPane chatScroll = new JScrollPane(chatText);
        chatPanel.add(chatScroll, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(chatField, BorderLayout.CENTER);
        bottomPanel.add(submitBtn, BorderLayout.EAST);
        chatPanel.add(bottomPanel, BorderLayout.SOUTH);

        splitPanel.setRightComponent(chatPanel);

        contentPane.add(splitPanel);

        ActionListener submitAction = e -> {
            String currentText = chatField.getText();
            if (currentText.isBlank()) {
                return;
            }

            if (currentUser.isEmpty()) {
                JOptionPane.showMessageDialog(contentPane, "You are not logged in...");
                return;
            }

            if (clientService.isActive()) {
                int idxPrv = currentText.indexOf("]:");
                Message msg;
                if (currentText.charAt(0) == '[' && idxPrv != -1) {
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

                clientService.sendMsg(msg);
                chatField.setText("");
                chatField.grabFocus();
            }
        };

        chatField.addActionListener(submitAction);
        submitBtn.addActionListener(submitAction);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);

                if (clientService.isActive()) {
                    clientService.sendMsg(new Message(MessageType.EXIT, currentUser, "server", "close connection"));
                }
            }
        });

        JMenuBar mainMenu = new JMenuBar();

        JMenu menuConnections = new JMenu("Connections");

        JMenuItem itemConnect = new JMenuItem("Connect");
        itemConnect.addActionListener(e -> {
            if (!currentUser.isEmpty() && clientService.isActive()) {
                JOptionPane.showMessageDialog(contentPane, "You are already logged in...");
                return;
            }

            if (!clientService.isActive()) {
                connect();
            }
            openAuth();
        });
        menuConnections.add(itemConnect);

        JMenuItem itemChgName = new JMenuItem("Change name");
        itemChgName.addActionListener(e -> {
            if (currentUser.isEmpty()) {
                JOptionPane.showMessageDialog(contentPane, "You are not logged in...");
                return;
            }

            if (!clientService.isActive()) {
                connect();
            }
            changeName();
        });
        menuConnections.add(itemChgName);

        JMenuItem itemSettings = new JMenuItem("Settings");
        itemSettings.addActionListener(this::actionPerformed);
        menuConnections.add(itemSettings);

        JMenuItem itemDisconnect = new JMenuItem("Disconnect");
        itemDisconnect.addActionListener(e -> {
            if (clientService.isActive() && !currentUser.isEmpty()) {
                clientService.sendMsg(new Message(MessageType.EXIT, currentUser, "server", "close connection"));
            }
        });
        menuConnections.add(itemDisconnect);

        mainMenu.add(menuConnections);
        contentPane.add(mainMenu, BorderLayout.NORTH);

        setVisible(true);
        chatField.setFocusable(true);
        chatField.grabFocus();
    }

    private void changeName() {
        String newName = JOptionPane.showInputDialog(null, "Enter new name");
        if (!newName.isEmpty() && !newName.equals(currentUser)) {
            clientService.sendMsg(new Message(MessageType.CHGNAME, currentUser, "server", newName));
        }
    }

    private void openAuth() {
        if (clientService.isActive()) {
            AuthFormGUI authFormGUI = new AuthFormGUI();
            String[] msgArr = authFormGUI.showDialog();
            if (!msgArr[0].isEmpty()) {
                Message msg = new Message(MessageType.AUTH, msgArr[0], "server", msgArr[1]);
                System.out.println(msg);
                clientService.sendMsg(msg);
            }
        }
    }

    private void openSettings() {
        HashMap<String, String> setMapEssential = new HashMap<>();
        setMapEssential.put("server", serverAddr);
        setMapEssential.put("port", (String.valueOf(serverPort)));

        ClientSettingsGUI setForm = new ClientSettingsGUI(setMapEssential);
        HashMap<String, String> setMapTarget = setForm.showDialog();

        boolean applyChanges = false;
        if (setMapTarget.containsKey("server") && !serverAddr.equals(setMapTarget.get("server"))) {
            serverAddr = setMapTarget.get("server");
            applyChanges = true;
        }
        if (setMapTarget.containsKey("port") && serverPort != Integer.parseInt(setMapTarget.get("port"))) {
            serverPort = Integer.parseInt(setMapTarget.get("port"));
            applyChanges = true;
        }

        if (applyChanges) {
            saveSettings();

            if (!clientService.isActive()) {
                connect();
            } else {
                addMessageToChat("To apply new settings, please reconnect to server...");
            }
        }
    }

    private void connect() {
        try {
            clientService.openConnection(serverAddr, serverPort);
        } catch (IOException e) {
            e.printStackTrace();
            addMessageToChat("Connection to server failed...");
        }
    }

    private void addUser(String user) {
        int userIdx = usersListModel.indexOf(user);
        if (userIdx == -1) {
            usersListModel.addElement(user);
            List namesList = Arrays.asList(usersListModel.toArray());
            Collections.sort(namesList);
            usersListModel.removeAllElements();
            usersListModel.addAll(namesList);
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
        List namesList = Arrays.asList(users);
        Collections.sort(namesList);
        usersListModel.addAll(namesList);
    }

    private void addMessageToChat(String msg) {
        if (msg.isEmpty()) {
            return;
        }

        StringBuilder stringBuilder = new StringBuilder(chatText.getText());
        stringBuilder.append(msg).append("\n");
        chatText.setText(stringBuilder.toString());
        stringBuilder.setLength(0);
    }

    private void clearChat() {
        chatText.setText("");
    }

    private void setChatTitle() {
        setTitle("Simple chat... " + ((currentUser.isEmpty()) ? "" : ("[" + currentUser + "]")));
    }

    @Override
    public synchronized void handleIncomingMessage(Message msg) {
        String msgTxt;
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
                currentId = Integer.parseInt(msg.getMessage());
                clearChat();
                addMessageToChat("Authorization is OK...");
                addMessageToChat(String.format("You are logged in as %s [id: %d] ...", currentUser, currentId));
                clientService.sendMsg(new Message(MessageType.GET_USERS, currentUser, "server", ""));
                addMessageToChat(ClientLogService.readLog(currentId));
                setChatTitle();
                return;
            case AUTH_FAILED:
                addMessageToChat("Authorization failed: " + msg.getMessage());
                return;
            case EXIT:
                clientService.sendMsg(new Message(MessageType.EXIT, currentUser, "server", "close connection"));
                addMessageToChat("You left chat...");
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
                setChatTitle();
                return;
            case CHGNAMEFAILED:
                addMessageToChat(String.format("Can't change nickname to %s. Possibly the name has already occupied...", msg.getUserTo()));
        }
    }

    @Override
    public void connectionClosed() {
        removeUser(currentUser);
        currentUser = "";
        currentId = -1;
        addMessageToChat("Connection closed...");
        setChatTitle();
    }

    @Override
    public void handleLogInfo(String msg) {
        addMessageToChat(msg);
    }

    private void actionPerformed(ActionEvent e) {
        openSettings();
    }
}
