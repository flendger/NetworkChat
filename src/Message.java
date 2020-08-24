public final class Message {
    private final static String COM_SPLITTER = ";";
    private final static String USERS_SPLITTER = "&";
    private MessageType messageType = MessageType.NONE;
    private String command = "";
    private String user = "";
    private String message = "";
    private String[] messageArray = {};

    public Message(String msg) {
        String[] msgArr = msg.split(COM_SPLITTER);

        if (msgArr.length == 0){
            return;
        }

        this.messageType = MessageType.getType(msgArr[0]);
        if (this.messageType == MessageType.NONE) {
            return;
        }

        this.command = msgArr[0];

        if (msgArr.length > 1){
            this.user = msgArr[1];
        }

        if (msgArr.length > 2) {
            this.message = msgArr[2];
            setMessageArray();
        }
    }

    public Message(MessageType messageType, String user, String message) {
        this.messageType = messageType;
        if (messageType == MessageType.NONE) {
            return;
        }

        this.command = messageType.toString();
        this.user = user;
        this.message = message;
        setMessageArray();
    }

    public Message(MessageType messageType, String user, String[] messageArray) {
        this.messageType = messageType;
        if (messageType == MessageType.NONE) {
            return;
        }

        this.command = messageType.toString();
        this.user = user;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < messageArray.length; i++) {
            sb.append(messageArray[i]);
            if (i != (messageArray.length -1)) {
                sb.append(USERS_SPLITTER);
            }
        }
        this.message = sb.toString();
        sb.setLength(0);

        setMessageArray();
    }

    private void setMessageArray() {
        this.messageArray = this.message.split(USERS_SPLITTER);
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public String getCommand() {
        return command;
    }

    public String getUser() {
        return user;
    }

    public String getMessage() {
        return message;
    }

    public String[] getMessageArray() {
        return messageArray;
    }

    @Override
    public String toString() {
        return command + COM_SPLITTER + user + COM_SPLITTER + message;
    }
}
