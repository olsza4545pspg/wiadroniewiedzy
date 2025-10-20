import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class ChatClient {
    private static final String HOST = "localhost";
    private static final int PORT = 7070;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;

    private JFrame frame;
    private JTextArea chatArea;
    private JTextField messageField;
    private JList<String> userList;
    private DefaultListModel<String> userListModel;
    private JComboBox<String> recipientComboBox;

    public ChatClient(String username) {
        this.username = username;
        try {
            socket = new Socket(HOST, PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            out.println(username);

            initializeGUI();

            new Thread(this::receiveMessages).start();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Nie można połączyć z serwerem: " + e.getMessage());
            System.exit(1);
        }
    }

    private void initializeGUI() {
        frame = new JFrame("Czat - " + username);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                out.println("/quit");
                System.exit(0);
            }
        });
        frame.setSize(600, 400);
        frame.setLayout(new BorderLayout());

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane chatScroll = new JScrollPane(chatArea);
        frame.add(chatScroll, BorderLayout.CENTER);

        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        JScrollPane userScroll = new JScrollPane(userList);
        userScroll.setPreferredSize(new Dimension(150, 0));
        frame.add(userScroll, BorderLayout.EAST);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        recipientComboBox = new JComboBox<>();
        recipientComboBox.addItem("Wszyscy");
        bottomPanel.add(recipientComboBox, BorderLayout.NORTH);

        messageField = new JTextField();
        JButton sendButton = new JButton("Wyślij");
        sendButton.addActionListener(e -> sendMessage());

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        bottomPanel.add(inputPanel, BorderLayout.CENTER);

        frame.add(bottomPanel, BorderLayout.SOUTH);

        messageField.addActionListener(e -> sendMessage());

        userList.addListSelectionListener(e -> {
            String selected = userList.getSelectedValue();
            if (selected != null && !selected.equals(username)) {
                recipientComboBox.setSelectedItem(selected);
            }
        });

        frame.setVisible(true);
    }

    private void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            String recipient = (String) recipientComboBox.getSelectedItem();
            if (!recipient.equals("Wszyscy")) {
                message = "@" + recipient + " " + message;
            }
            out.println(message);
            messageField.setText("");
        }
    }

    private void receiveMessages() {
        try {
            String received;
            while ((received = in.readLine()) != null) {
                if (received.startsWith("LISTA_UZYTKOWNIKOW:")) {
                    updateUserList(received.substring(19));
                } else if (received.startsWith("ERROR:")) {
                    JOptionPane.showMessageDialog(frame, received);
                    System.exit(1);
                } else {
                    chatArea.append(received + "\n");
                }
            }
        } catch (IOException e) {
            chatArea.append("Połączenie przerwane.\n");
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                // Ignoruj
            }
        }
    }

    private void updateUserList(String userString) {
        userListModel.clear();
        recipientComboBox.removeAllItems();
        recipientComboBox.addItem("Wszyscy");

        String[] users = userString.split(",");
        for (String user : users) {
            if (!user.isEmpty()) {
                userListModel.addElement(user);
                if (!user.equals(username)) {
                    recipientComboBox.addItem(user);
                }
            }
        }
    }

    public static void main(String[] args) {
        String username = JOptionPane.showInputDialog("Podaj login:");
        if (username != null && !username.trim().isEmpty()) {
            new ChatClient(username.trim());
        } else {
            JOptionPane.showMessageDialog(null, "Login nie może być pusty!");
        }
    }
}