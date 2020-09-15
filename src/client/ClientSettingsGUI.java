package client;

import javax.swing.*;
import java.awt.event.*;
import java.util.HashMap;

public class ClientSettingsGUI extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField serverField;
    private JSpinner portSpinner;

    private final HashMap<String, String> settings;

    public ClientSettingsGUI(HashMap<String, String> settings) {
        this.settings = settings;
        serverField.setText(getServer());
        portSpinner.setValue(Integer.parseInt(getPort()));
        JSpinner.DefaultEditor s = (JSpinner.DefaultEditor) portSpinner.getEditor();
        s.getTextField().setHorizontalAlignment(JTextField.LEFT);

        setResizable(false);
        setModal(true);

        setContentPane(contentPane);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(e -> onOK());

        buttonCancel.addActionListener(e -> onCancel());

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(e -> onCancel(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void onOK() {
        settings.put("server", serverField.getText());
        settings.put("port", String.valueOf(portSpinner.getValue()));
        dispose();
    }

    private void onCancel() {
        dispose();
    }

    private String getServer() {
        if (settings.containsKey("server")) {
            return settings.get("server");
        }
        return "";
    }

    private String getPort() {
        if (settings.containsKey("port")) {
            return settings.get("port");
        }
        return "";
    }

    public HashMap<String, String> showDialog() {
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
        return settings;
    }
}
