package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class AuthFormGUI extends JDialog {
    private final JTextField loginField = new JTextField();
    private final JPasswordField passField = new JPasswordField();
    private final String[] result = {"", ""};

    public AuthFormGUI() {
        prepareGUI();
    }

    public String[] showDialog(){
        setVisible(true);
        return result;
    }

    private void prepareGUI(){
        setTitle("Enter login/password:");
        setSize(300, 140);
        setResizable(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setModal(true);

        //main panel
        JPanel contentPane = new JPanel();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        setContentPane(contentPane);

        Dimension labelDimension = new Dimension(85, 23);
        Dimension fieldDimension = new Dimension(175, 23);

        Font labelFont = new Font(Font.DIALOG, Font.BOLD, 14);
        Font fieldFont = new Font(Font.DIALOG, Font.PLAIN, 14);

        JPanel loginPanel = new JPanel();
        loginPanel.setLayout(new FlowLayout());
        JLabel loginLabel = new JLabel("login: ");
        loginLabel.setFont(labelFont);
        loginLabel.setPreferredSize(labelDimension);
        loginPanel.add(loginLabel, BorderLayout.WEST);

        loginField.setPreferredSize(fieldDimension);
        loginField.setFont(fieldFont);
        loginPanel.add(loginField);
        contentPane.add(loginPanel);


        JPanel passPanel = new JPanel();
        passPanel.setLayout(new FlowLayout());
        JLabel passLabel = new JLabel("password: ");
        passLabel.setFont(labelFont);
        passLabel.setPreferredSize(labelDimension);
        passPanel.add(passLabel, BorderLayout.WEST);

        passField.setPreferredSize(fieldDimension);
        passField.setFont(fieldFont);
        passPanel.add(passField);

        contentPane.add(passPanel, BorderLayout.CENTER);


        JPanel btnPanel = new JPanel();
        btnPanel.setLayout(new FlowLayout());

        JButton btnOK = new JButton("OK");
        btnOK.addActionListener(e -> {
            result[0] = loginField.getText();
            result[1] = new String(passField.getPassword());
            dispose();
        });
        btnPanel.add(btnOK);

        JButton btnCancel = new JButton("Cancel");
        btnCancel.addActionListener(e -> {
            result[0] = "";
            result[1] = "";
            dispose();
        });
        btnPanel.add(btnCancel);

        contentPane.add(btnPanel);
        getRootPane().setDefaultButton(btnOK);
    }
}
