import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.util.List;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class YClient {
    private JFrame frame;
    private JTextField usernameField, loginUsernameField, messageField;
    private JPasswordField passwordField, loginPasswordField;
    private JTextField nameField, emailField;
    private Socket socket;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;
    private JPanel postPanel; // Panel for message posting
    private int userId; // User ID of the logged-in user
    private JTextField followUsernameField;
    private JButton followButton, unfollowButton;
    private String LastRefreshed;
    private Timer timer;
    JPanel messagePanel;

    public YClient() {
        connectToServer("localhost", 56300); // Replace with your server's address and port
        initializeUI();
    }

    private void connectToServer(String serverAddress, int port) {
        try {
            socket = new Socket(serverAddress, port);
            outputStream = new ObjectOutputStream(socket.getOutputStream());
            inputStream = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame, "Unable to connect to server: " + e.getMessage(), "Connection Error",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1); // Exit if connection fails
        }
    }

    private void initializeUI() {
        frame = new JFrame("Y Platform Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);
        frame.setLayout(new BorderLayout());

        JTabbedPane tabbedPane = new JTabbedPane();
        JPanel registerPanel = createRegisterPanel();
        JPanel loginPanel = createLoginPanel();
        postPanel = createPostPanel();
        postPanel.setVisible(false); // Initially hide the posting panel

        tabbedPane.addTab("Register", registerPanel);
        tabbedPane.addTab("Login", loginPanel);
        frame.add(tabbedPane, BorderLayout.CENTER);
        frame.add(postPanel, BorderLayout.SOUTH);
        messagePanel = new JPanel();
        frame.setVisible(true);
    }

    private JPanel createRegisterPanel() {
        JPanel registerPanel = new JPanel(new GridLayout(5, 2));
        nameField = new JTextField();
        emailField = new JTextField();
        usernameField = new JTextField();
        passwordField = new JPasswordField();
        JButton registerButton = new JButton("Register");

        registerPanel.add(new JLabel("Name:"));
        registerPanel.add(nameField);
        registerPanel.add(new JLabel("Email:"));
        registerPanel.add(emailField);
        registerPanel.add(new JLabel("Username:"));
        registerPanel.add(usernameField);
        registerPanel.add(new JLabel("Password:"));
        registerPanel.add(passwordField);
        registerPanel.add(registerButton);

        registerButton.addActionListener(e -> handleRegistration());
        return registerPanel;
    }

    private JPanel createLoginPanel() {
        JPanel loginPanel = new JPanel(new GridLayout(3, 2));
        loginUsernameField = new JTextField();
        loginPasswordField = new JPasswordField();
        JButton loginButton = new JButton("Login");

        loginPanel.add(new JLabel("Username:"));
        loginPanel.add(loginUsernameField);
        loginPanel.add(new JLabel("Password:"));
        loginPanel.add(loginPasswordField);
        loginPanel.add(loginButton);

        loginButton.addActionListener(e -> handleLogin());
        return loginPanel;
    }

    private JPanel createPostPanel() {
        JPanel postPanel = new JPanel();
        messageField = new JTextField(20);
        JButton postMessageButton = new JButton("Post Message");

        postPanel.add(messageField);
        postPanel.add(postMessageButton);

        postMessageButton.addActionListener(e -> handlePostMessage());
        return postPanel;
    }

    private JPanel createFollowPanel() {
        JPanel followPanel = new JPanel();
        followUsernameField = new JTextField(10);
        followButton = new JButton("Follow");
        unfollowButton = new JButton("Unfollow");

        followPanel.add(new JLabel("Username:"));
        followPanel.add(followUsernameField);
        followPanel.add(followButton);
        followPanel.add(unfollowButton);

        followButton.addActionListener(e -> handleFollow());
        unfollowButton.addActionListener(e -> handleUnfollow()); // Ensure this line is present

        return followPanel;
    }

    private void handleRegistration() {
        String name = nameField.getText();
        String email = emailField.getText();
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());

        RegistrationRequest request = new RegistrationRequest(username, password, name, email);
        sendRequest(request);
    }

    private void handleLogin() {
        String username = loginUsernameField.getText();
        String password = new String(loginPasswordField.getPassword());

        LoginRequest request = new LoginRequest(username, password);
        sendRequest(request);
    }

    private void handleRefreshFeed() {
        RefreshFeedRequest request = new RefreshFeedRequest(userId, LastRefreshed);
        sendRequest(request);
    }

    private void handlePostMessage() {
        String messageContent = messageField.getText();
        PostMessageRequest request = new PostMessageRequest(userId, messageContent);
        sendRequest(request);
        messageField.setText(""); // Clear the message field after sending
    }

    private void sendRequest(Object request) {
        try {
            outputStream.writeObject(request);
            Object response = inputStream.readObject();

            if (response instanceof RegistrationResponse) {
                handleRegistrationResponse((RegistrationResponse) response);
            } else if (response instanceof LoginResponse) {
                handleLoginResponse((LoginResponse) response);
            } else if (response instanceof PostMessageResponse) {
                handlePostMessageResponse((PostMessageResponse) response);
            } else if (response instanceof FollowResponse) {
                handleFollowResponse((FollowResponse) response);
            } else if (response instanceof RefreshFeedResponse) {
                handleRefreshFeed((RefreshFeedResponse) response);
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame, "Error: " + e.getMessage(), "Communication Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleRegistrationResponse(RegistrationResponse response) {
        JOptionPane.showMessageDialog(frame, response.getMessage(), "Registration", JOptionPane.INFORMATION_MESSAGE);
    }

    private void handleLoginResponse(LoginResponse response) {
        JOptionPane.showMessageDialog(frame, response.getMessage(), "Login", JOptionPane.INFORMATION_MESSAGE);

        if (response.isSuccess()) {
            this.userId = response.getUserId();

            try {
                List<Message> userMessages = (List<Message>) inputStream.readObject();
                List<Message> messagesOfInterest = (List<Message>) inputStream.readObject();
                showPostLoginUI(userMessages, messagesOfInterest);
                if (messagesOfInterest.size() > 0)
                    LastRefreshed = new String(messagesOfInterest.get(messagesOfInterest.size() - 1).getDate());
                else {
                    LastRefreshed = "1900-01-01 00:00:00";
                }
                timer = new Timer(3000, new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        handleRefreshFeed();
                    }
                });
                timer.start();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(frame, "Error retrieving messages: " + e.getMessage(),
                        "Communication Error", JOptionPane.ERROR_MESSAGE);
            }
        }

    }

    private void updateMessagesDisplay(List<Message> userMessages, List<Message> messagesOfInterest) {
        messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS));

        for (Message msg : userMessages) {
            JPanel userMessagePanel = createMessagePanel(msg.getUsername(), msg.getContent(), msg.getDate());
            messagePanel.add(userMessagePanel);
        }

        for (Message msg : messagesOfInterest) {
            JPanel messageItemPanel = createMessagePanel(msg.getUsername(), msg.getContent(), msg.getDate());
            messagePanel.add(messageItemPanel);
        }

        if (userMessages.isEmpty() && messagesOfInterest.isEmpty()) {
            JPanel noMessagePanel = createMessagePanel("No messages to display.", "", "");
            messagePanel.add(noMessagePanel);
        }

        JScrollPane scrollPane = new JScrollPane(messagePanel);
        JPanel containerPanel = new JPanel(new BorderLayout());
        containerPanel.add(scrollPane, BorderLayout.CENTER);

        frame.getContentPane().removeAll();
        frame.getContentPane().add(containerPanel, BorderLayout.CENTER);
        frame.revalidate();
        frame.repaint();
    }

    private JPanel createMessagePanel(String username, String content, String date) {
        JPanel messagePanel = new JPanel(new BorderLayout());
        messagePanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        String htmlMessage = "<html><b>" + username + "</b><br>" + content + "<br><font size=\"2\">" + date
                + "</font></html>";

        JEditorPane editorPane = new JEditorPane();
        editorPane.setContentType("text/html");
        editorPane.setText(htmlMessage);
        editorPane.setEditable(false);
        editorPane.setBackground(messagePanel.getBackground());

        messagePanel.add(editorPane, BorderLayout.CENTER);

        return messagePanel;
    }

    private void handlePostMessageResponse(PostMessageResponse response) {
        JOptionPane.showMessageDialog(frame, response.getMessage(), "Post Message", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showPostLoginUI(List<Message> userMessages, List<Message> messagesOfInterest) {
        frame.getContentPane().removeAll();
        frame.setLayout(new BorderLayout());

        JPanel postLoginPanel = new JPanel(new BorderLayout());
        postLoginPanel.add(createPostPanel(), BorderLayout.NORTH);
        postLoginPanel.add(createFollowPanel(), BorderLayout.SOUTH); // Add follow/unfollow panel

        // Update message display within this method
        updateMessagesDisplay(userMessages, messagesOfInterest);

        frame.getContentPane().add(postLoginPanel, BorderLayout.PAGE_START);
        frame.revalidate();
        frame.repaint();
    }

    private void handleFollow() {
        String usernameToFollow = followUsernameField.getText();
        FollowRequest request = new FollowRequest(userId, usernameToFollow, true);
        sendRequest(request);
        // Handle the response
    }

    private void handleUnfollow() {
        String usernameToUnfollow = followUsernameField.getText();
        FollowRequest request = new FollowRequest(userId, usernameToUnfollow, false);
        sendRequest(request);
        // Handle the response
    }

    private void handleFollowResponse(FollowResponse response) {
        JOptionPane.showMessageDialog(frame, response.getMessage(), "Follow Status", JOptionPane.INFORMATION_MESSAGE);
    }

    private void handleRefreshFeed(RefreshFeedResponse response) {
        if (!response.isSuccess()) {
            JOptionPane.showMessageDialog(frame, "Failed to Refresh Feed.", "Refresh Message",
                    JOptionPane.INFORMATION_MESSAGE);
        } else {

            List<Message> messagesOfInterest = response.getMessages();
            if (messagesOfInterest.size() > 0) {
                LastRefreshed = new String(messagesOfInterest.get(messagesOfInterest.size() - 1).getDate());
            }
            for (Message msg : messagesOfInterest) {
                JPanel messageItemPanel = createMessagePanel(msg.getUsername(), msg.getContent(), msg.getDate());
                messagePanel.add(messageItemPanel);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(YClient::new);
    }
}
