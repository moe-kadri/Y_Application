// import javax.swing.*;
// import java.awt.*;
// import java.io.*;
// import java.net.Socket;
// import java.util.List;

// public class YClient {
//     private JFrame frame;
//     private JTextField usernameField;
//     private JPasswordField passwordField;
//     private JTextField nameField;
//     private JTextField emailField;
//     private JTextField loginUsernameField;
//     private JPasswordField loginPasswordField;
//     private Socket socket;
//     private ObjectOutputStream outputStream;
//     private ObjectInputStream inputStream;

//     public YClient() {
//         connectToServer("localhost", 56300); // Replace with your server's address and port
//         initializeUI();
//     }

//     private void connectToServer(String serverAddress, int port) {
//         try {
//             socket = new Socket(serverAddress, port);
//             outputStream = new ObjectOutputStream(socket.getOutputStream());
//             inputStream = new ObjectInputStream(socket.getInputStream());
//         } catch (IOException e) {
//             e.printStackTrace();
//             JOptionPane.showMessageDialog(frame, "Unable to connect to server: " + e.getMessage(), "Connection Error", JOptionPane.ERROR_MESSAGE);
//         }
//     }

//     private void initializeUI() {
//         frame = new JFrame("Y Platform Client");
//         frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//         frame.setSize(400, 300);
//         frame.setLayout(new BorderLayout());

//         JTabbedPane tabbedPane = new JTabbedPane();

//         // Registration panel
//         JPanel registerPanel = new JPanel(new GridLayout(5, 2));
//         nameField = new JTextField();
//         emailField = new JTextField();
//         usernameField = new JTextField();
//         passwordField = new JPasswordField();
//         JButton registerButton = new JButton("Register");
//         registerPanel.add(new JLabel("Name:"));
//         registerPanel.add(nameField);
//         registerPanel.add(new JLabel("Email:"));
//         registerPanel.add(emailField);
//         registerPanel.add(new JLabel("Username:"));
//         registerPanel.add(usernameField);
//         registerPanel.add(new JLabel("Password:"));
//         registerPanel.add(passwordField);
//         registerPanel.add(registerButton);

//         // Login panel
//         JPanel loginPanel = new JPanel(new GridLayout(3, 2));
//         loginUsernameField = new JTextField();
//         loginPasswordField = new JPasswordField();
//         JButton loginButton = new JButton("Login");
//         loginPanel.add(new JLabel("Username:"));
//         loginPanel.add(loginUsernameField);
//         loginPanel.add(new JLabel("Password:"));
//         loginPanel.add(loginPasswordField);
//         loginPanel.add(loginButton);

//         // Add tabs
//         tabbedPane.addTab("Register", registerPanel);
//         tabbedPane.addTab("Login", loginPanel);
//         frame.add(tabbedPane, BorderLayout.CENTER);

//         // Register button action
//         registerButton.addActionListener(e -> handleRegistration());

//         // Login button action
//         loginButton.addActionListener(e -> handleLogin());

//         frame.setVisible(true);
//     }

//     private void handleRegistration() {
//         String name = nameField.getText();
//         String email = emailField.getText();
//         String username = usernameField.getText();
//         String password = new String(passwordField.getPassword());
        
//         RegistrationRequest request = new RegistrationRequest(username, password, name, email);
//         sendRequest(request);
//     }

//     private void handleLogin() {
//         String username = loginUsernameField.getText();
//         String password = new String(loginPasswordField.getPassword());
        
//         LoginRequest request = new LoginRequest(username, password);
//         sendRequest(request);
//     }

//     private void sendRequest(Object request) {
//         try {
//             outputStream.writeObject(request);
//             Object response = inputStream.readObject();
//             // Handle the response based on its type
//             if (response instanceof RegistrationResponse) {
//                 handleRegistrationResponse((RegistrationResponse) response);
//             } else if (response instanceof LoginResponse) {
//                 handleLoginResponse((LoginResponse) response);
//             }
//         } catch (IOException | ClassNotFoundException e) {
//             e.printStackTrace();
//             JOptionPane.showMessageDialog(frame, "Error: " + e.getMessage(), "Communication Error", JOptionPane.ERROR_MESSAGE);
//         }
//     }

//     private void handleRegistrationResponse(RegistrationResponse response) {
//         JOptionPane.showMessageDialog(frame, response.getMessage(), "Registration", JOptionPane.INFORMATION_MESSAGE);
//     }

//     private void handleLoginResponse(LoginResponse response) {
//         JOptionPane.showMessageDialog(frame, response.getMessage(), "Login", JOptionPane.INFORMATION_MESSAGE);
    
//         if (response.isSuccess()) {
//             displayMessages();
//         }
//     }

//     private void displayMessages() {
//         try {
//             // Assuming messages are sent as List<String>
//             List<String> userMessages = (List<String>) inputStream.readObject();
//             List<String> messagesOfInterest = (List<String>) inputStream.readObject();
    
//             // Display these messages in the GUI
//             updateMessagesPanel(userMessages, messagesOfInterest);
//         } catch (IOException | ClassNotFoundException e) {
//             e.printStackTrace();
//             JOptionPane.showMessageDialog(frame, "Error retrieving messages: " + e.getMessage(), "Communication Error", JOptionPane.ERROR_MESSAGE);
//         }
//     }

//     private void updateMessagesPanel(List<String> userMessages, List<String> messagesOfInterest) {
//         JPanel messagesPanel = new JPanel();
//         messagesPanel.setLayout(new BoxLayout(messagesPanel, BoxLayout.Y_AXIS));
    
//         messagesPanel.add(new JLabel("Your Messages:"));
//         for (String message : userMessages) {
//             messagesPanel.add(new JLabel(message));
//         }
    
//         messagesPanel.add(new JLabel("Messages of Interest:"));
//         for (String message : messagesOfInterest) {
//             messagesPanel.add(new JLabel(message));
//         }
    
//         // Update the frame to display this new panel
//         frame.getContentPane().removeAll();
//         frame.getContentPane().add(messagesPanel, BorderLayout.CENTER);
//         frame.revalidate();
//         frame.repaint();
//     }
    
//     public static void main(String[] args) {
//         new YClient();
//     }
// }

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.util.List;

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
            JOptionPane.showMessageDialog(frame, "Unable to connect to server: " + e.getMessage(), "Connection Error", JOptionPane.ERROR_MESSAGE);
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
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame, "Error: " + e.getMessage(), "Communication Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleRegistrationResponse(RegistrationResponse response) {
        JOptionPane.showMessageDialog(frame, response.getMessage(), "Registration", JOptionPane.INFORMATION_MESSAGE);
    }

    private void handleLoginResponse(LoginResponse response) {
        JOptionPane.showMessageDialog(frame, response.getMessage(), "Login", JOptionPane.INFORMATION_MESSAGE);
    
        if (response.isSuccess()) {
            this.userId = response.getUserId(); // Set user ID upon successful login
    
            // Update UI to post-login view
            showPostLoginUI();
    
            // Fetch and display user messages and messages of interest
            SwingUtilities.invokeLater(() -> {
                try {
                    // Assuming the server sends these lists right after the LoginResponse
                    List<String> userMessages = (List<String>) inputStream.readObject();
                    List<String> messagesOfInterest = (List<String>) inputStream.readObject();
                    updateMessagesDisplay(userMessages, messagesOfInterest);
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(frame, "Error retrieving messages: " + e.getMessage(), "Communication Error", JOptionPane.ERROR_MESSAGE);
                }
            });
        }
    }

    private void updateMessagesDisplay(List<String> userMessages, List<String> messagesOfInterest) {
        // Example implementation to display messages
        JTextArea textArea = new JTextArea();
        userMessages.forEach(msg -> textArea.append("Your Message: " + msg + "\n"));
        messagesOfInterest.forEach(msg -> textArea.append("Message of Interest: " + msg + "\n"));
    
        // Update the frame
        frame.getContentPane().add(new JScrollPane(textArea), BorderLayout.CENTER);
        frame.revalidate();
        frame.repaint();
    }
    
    
    
    

    private void handlePostMessageResponse(PostMessageResponse response) {
        JOptionPane.showMessageDialog(frame, response.getMessage(), "Post Message", JOptionPane.INFORMATION_MESSAGE);
    }

    private void displayMessages() {
        try {
            List<String> userMessages = (List<String>) inputStream.readObject();
            List<String> messagesOfInterest = (List<String>) inputStream.readObject();
    
            updateMessagesPanel(userMessages, messagesOfInterest);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame, "Error retrieving messages: " + e.getMessage(), "Communication Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateMessagesPanel(List<String> userMessages, List<String> messagesOfInterest) {
        JPanel messagesPanel = new JPanel();
        messagesPanel.setLayout(new BoxLayout(messagesPanel, BoxLayout.Y_AXIS));
    
        messagesPanel.add(new JLabel("Your Messages:"));
        for (String message : userMessages) {
            messagesPanel.add(new JLabel(message));
        }
    
        messagesPanel.add(new JLabel("Messages of Interest:"));
        for (String message : messagesOfInterest) {
            messagesPanel.add(new JLabel(message));
        }
    
        frame.getContentPane().add(messagesPanel, BorderLayout.CENTER);
        frame.revalidate();
        frame.repaint();
    }    

    private void showPostLoginUI() {
        // Clear the frame and set up a new layout for post-login
        frame.getContentPane().removeAll();
        frame.setLayout(new BorderLayout());
    
        JPanel postLoginPanel = new JPanel(new BorderLayout());
        postLoginPanel.add(createPostPanel(), BorderLayout.NORTH); // Add post panel at the top
    
        // Optionally add more components to postLoginPanel if needed
    
        frame.getContentPane().add(postLoginPanel);
        frame.revalidate();
        frame.repaint();
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(YClient::new);
    }
}
