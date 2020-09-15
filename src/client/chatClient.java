package client;

import messages.Message;

public interface chatClient {
    void handleIncomingMessage(Message msg);
    void connectionClosed();
    void handleLogInfo(String msg);
}
