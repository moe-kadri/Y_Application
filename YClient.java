import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
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
    private int userId;
    private String username; // User ID of the logged-in user
    private JTextField followUsernameField;
    private JButton followButton, unfollowButton;
    private String LastRefreshed;
    private Timer timer;
    JPanel messagePanel;
    private List<CommentPanel> commentPanels;
    private List<CommentPanel> postCommentPanels;
    private List<EmojiButtons> emojiButtons;
    private List<RemoveReactionEmojiButton> RemoveReactionEmojiButtons;
    JPanel PostsPanel;
    JPanel explorePagePanel;
    JPanel explorePanel;

    public YClient() {
        connectToServer("localhost", 56300); // Replace with your server's address and port
        initializeUI();
        commentPanels = new ArrayList<>();
        postCommentPanels = new ArrayList<>();
        emojiButtons = new ArrayList<>();
        RemoveReactionEmojiButtons = new ArrayList<>();
    }

    public void checkForResponses() {
        try {

            while (true) {
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
                } else if (response instanceof NewMessagePosted) {
                    handleNewMessagePosted((NewMessagePosted) response);
                } else if (response instanceof PostCommentResponse) {
                    handlePostedCommentResponse((PostCommentResponse) response);
                } else if (response instanceof RetrieveCommentsResponse) {
                    handleRetrieveCommentsResponse((RetrieveCommentsResponse) response);
                } else if (response instanceof PostReactionResponse) {
                    handlePostReactionResponse((PostReactionResponse) response);
                } else if (response instanceof RemoveReactionResponse) {
                    handleRemoveReactionResponse((RemoveReactionResponse) response);
                } else if (response instanceof getExplorePageResponse) {
                    handleGetExplorePageResponse((getExplorePageResponse) response);
                }

            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame, "Error retrieving messages: " + e.getMessage(),
                    "Communication Error", JOptionPane.ERROR_MESSAGE);
        }
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
        frame.setSize(700, 800);
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

    private void handleViewComments(Post post) {

        JFrame commentFrame = new JFrame("Comments");
        commentFrame.setSize(400, 300);
        commentFrame.setLocation(frame.getLocation());
        JPanel commentsPanel = new JPanel();
        commentsPanel.setLayout(new BoxLayout(commentsPanel, BoxLayout.Y_AXIS));
        retrieveComments(post.getID(), commentsPanel);

        JButton addCommentButton = new JButton("Add Comment");
        JTextField commentField = new JTextField(20);

        addCommentButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handlePostComment(new Comment(-1, post.getID(), userId, username, commentField.getText(), ""));

                // // Display the new comment
                // displayComment(commentsPanel,
                // new Comment(-1, post.getID(), userId, username, commentField.getText(), "Just
                // Now"));
                postCommentPanels.add(new CommentPanel(post.getID(), commentsPanel));
                // Clear the comment text field
                commentField.setText("");
            }
        });

        JScrollPane scrollPane = new JScrollPane(commentsPanel);

        JPanel commentPanel = new JPanel();
        commentPanel.setLayout(new BorderLayout());
        commentPanel.add(scrollPane, BorderLayout.CENTER);

        // Add the text field and button to a sub-panel for better layout
        JPanel addCommentPanel = new JPanel(new BorderLayout());
        addCommentPanel.add(commentField, BorderLayout.CENTER);
        addCommentPanel.add(addCommentButton, BorderLayout.EAST);

        commentPanel.add(addCommentPanel, BorderLayout.SOUTH);

        commentFrame.add(commentPanel);
        commentFrame.setVisible(true);
    }

    private void displayComment(JPanel commentsPanel, Comment comment) {
        JPanel commentContainer = new JPanel(new BorderLayout());
        commentContainer.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        String htmlComment = "<html><b>" + comment.getAuthor_username() + "</b><br>" + comment.getContent()
                + "<br><font size=\"2\">" + comment.getDate() + "</font></html>";

        JEditorPane editorPane = new JEditorPane();
        editorPane.setContentType("text/html");
        editorPane.setText(htmlComment);
        editorPane.setEditable(false);
        editorPane.setBackground(commentContainer.getBackground());

        commentContainer.add(editorPane, BorderLayout.CENTER);

        // Add the comment container to the commentsPanel
        commentsPanel.add(commentContainer);

        // Repaint the frame
        commentsPanel.revalidate();
        commentsPanel.repaint();
    }

    private void handleRetrieveCommentsResponse(RetrieveCommentsResponse res) {
        int post_id = res.getPost_id();
        for (CommentPanel p : commentPanels) {
            if (post_id == p.getPost_id()) {
                for (Comment c : res.getComments()) {
                    displayComment(p.commentsPanel, c);
                }
                commentPanels.remove(p);
                break;
            }
        }
    }

    private void handlePostComment(Comment comment) {
        PostCommentRequest req = new PostCommentRequest(comment);
        sendRequest(req);
    }

    private void handlePostReaction(Reaction reaction) {
        PostReactionRequest req = new PostReactionRequest(reaction);
        sendRequest(req);
    }

    private void handleRemoveReactionRequest(int post_id, int user_id) {
        RemoveReactionRequest req = new RemoveReactionRequest(post_id, user_id);
        sendRequest(req);
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

    private void handGetExplorePageRequest() {
        sendRequest(new getExplorePageRequest(userId));
    }

    private void handleRefreshFeed() {
        RefreshFeedRequest request = new RefreshFeedRequest(userId, LastRefreshed);
        sendRequest(request);
    }

    // private void handlePostMessage() {
    // String messageContent = messageField.getText();
    // PostMessageRequest request = new PostMessageRequest(userId, username,
    // messageContent);
    // sendRequest(request);
    // messageField.setText(""); // Clear the message field after sending
    // }
    private void handlePostMessage() {
        String messageContent = messageField.getText();

        PostMessageRequest request = new PostMessageRequest(userId, username, messageContent);
        sendRequest(request);
        messageField.setText(""); // Clear the message field after sending
    }

    private void retrieveComments(int post_id, JPanel commentsPanel) {
        RetrieveCommentsRequest req = new RetrieveCommentsRequest(post_id);
        commentPanels.add(new CommentPanel(post_id, commentsPanel));
        sendRequest(req);
    }

    private void sendRequest(Object request) {
        try {
            outputStream.writeObject(request);

        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame, "Error: " + e.getMessage(), "Communication Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleRegistrationResponse(RegistrationResponse response) {
        JOptionPane.showMessageDialog(frame, response.getMessage(), "Registration", JOptionPane.INFORMATION_MESSAGE);
    }

    private void handleLoginResponse(LoginResponse response) {

        if (response.isSuccess()) {
            this.userId = response.getUserId();
            this.username = response.getUsername();
            List<Message> userMessages = response.getUserMessages();
            List<Message> messagesOfInterest = response.getMessagesOfInterest();
            showPostLoginUI(userMessages, messagesOfInterest);
            handGetExplorePageRequest();

            if (messagesOfInterest.size() > 0)
                LastRefreshed = new String(messagesOfInterest.get(messagesOfInterest.size() - 1).getDate());
            else {
                LastRefreshed = "1900-01-01 00:00:00";
            }
            // timer = new Timer(3000, new ActionListener() {
            // @Override
            // public void actionPerformed(ActionEvent e) {
            // handleRefreshFeed();
            // }
            // });
            // timer.start();

        } else {
            JOptionPane.showMessageDialog(frame, response.getMessage(), "Login", JOptionPane.INFORMATION_MESSAGE);

        }

    }

    private void updateMessagesDisplay(List<Message> userMessages, List<Message> messagesOfInterest) {
        messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS));

        for (Message msg : userMessages) {
            JPanel userMessagePanel = createMessagePanel(
                    new Post(msg.getID(), msg.getUserID(), msg.getUsername(), msg.getContent(), msg.getDate(),
                            msg.getReaction(), msg.getReactionCountMap()));
            messagePanel.add(userMessagePanel);
        }

        for (Message msg : messagesOfInterest) {
            JPanel messageItemPanel = createMessagePanel(
                    new Post(msg.getID(), msg.getUserID(), msg.getUsername(), msg.getContent(), msg.getDate(),
                            msg.getReaction(), msg.getReactionCountMap()));
            messagePanel.add(messageItemPanel);
        }

        if (userMessages.isEmpty() && messagesOfInterest.isEmpty()) {
            JPanel noMessagePanel = createMessagePanel(new Post(0, 0, "No messages to display.", "", "", -1, null));
            messagePanel.add(noMessagePanel);
        }

        JScrollPane scrollPane = new JScrollPane(messagePanel);
        JPanel containerPanel = new JPanel(new BorderLayout());
        containerPanel.add(scrollPane, BorderLayout.CENTER);

        PostsPanel.removeAll();
        PostsPanel.add(containerPanel, BorderLayout.CENTER);
        PostsPanel.revalidate();
        PostsPanel.repaint();
    }

    private void handleGetExplorePageResponse(getExplorePageResponse res) {
        updateExploreMessages(res.getMessages());
    }

    private void updateExploreMessages(List<Message> messages) {

        for (Message msg : messages) {
            JPanel messageItemPanel = createMessagePanel(
                    new Post(msg.getID(), msg.getUserID(), msg.getUsername(), msg.getContent(), msg.getDate(),
                            msg.getReaction(), msg.getReactionCountMap()));
            explorePanel.add(messageItemPanel);
        }

        if (messages.isEmpty()) {
            JPanel noMessagePanel = createMessagePanel(new Post(0, 0, "No messages to display.", "", "", -1, null));
            explorePanel.add(noMessagePanel);
        }

    }

    // private JPanel createMessagePanel(String username, String content, String
    // date) {
    // JPanel messagePanel = new JPanel(new BorderLayout());
    // messagePanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));

    // String htmlMessage = "<html><b>" + username + "</b><br>" + content +
    // "<br><font size=\"2\">" + date
    // + "</font></html>";

    // JEditorPane editorPane = new JEditorPane();
    // editorPane.setContentType("text/html");
    // editorPane.setText(htmlMessage);
    // editorPane.setEditable(false);
    // editorPane.setBackground(messagePanel.getBackground());

    // messagePanel.add(editorPane, BorderLayout.CENTER);

    // return messagePanel;
    // }

    private JPanel createMessagePanel(Post message) {
        JPanel messagePanel = new JPanel();
        messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS));
        messagePanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        // Message content
        String htmlMessage = "<html><b>" + message.getUsername() + "</b><br>" + message.getContent()
                + "<br><font size=\"2\">"
                + message.getDate() + "</font></html>";

        JEditorPane editorPane = new JEditorPane();
        editorPane.setContentType("text/html");
        editorPane.setText(htmlMessage);
        editorPane.setEditable(false);
        editorPane.setBackground(messagePanel.getBackground());
        int loveCount = 0;
        int thumbsUpCount = 0;
        int cryingCount = 0;
        int angryCount = 0;
        int laughingCount = 0;
        if (message.getReactionCountMap() != null) {
            Map<Integer, Integer> map = message.getReactionCountMap();
            if (map.containsKey(1)) {
                loveCount = map.get(1);
            }
            if (map.containsKey(2)) {
                thumbsUpCount = map.get(2);
            }
            if (map.containsKey(3)) {
                cryingCount = map.get(3);
            }
            if (map.containsKey(4)) {
                angryCount = map.get(4);
            }
            if (map.containsKey(5)) {
                laughingCount = map.get(5);
            }
        }
        // Emoji buttons
        JPanel emojiPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        // Love Emoji
        JButton loveButton = createEmojiButton("\u2764\uFE0F");
        JLabel loveLabel = createEmojiCountLabel(loveCount); // Replace 2 with the actual count
        JPanel lovePanel = new JPanel(new BorderLayout());
        lovePanel.add(loveButton, BorderLayout.NORTH);
        lovePanel.add(loveLabel, BorderLayout.SOUTH);
        emojiPanel.add(lovePanel);

        // Thumbs Up Emoji
        JButton thumbsUpButton = createEmojiButton("\uD83D\uDC4D");
        JLabel thumbsUpLabel = createEmojiCountLabel(thumbsUpCount);
        JPanel thumbsUpPanel = new JPanel(new BorderLayout());
        thumbsUpPanel.add(thumbsUpButton, BorderLayout.NORTH);
        thumbsUpPanel.add(thumbsUpLabel, BorderLayout.SOUTH);
        emojiPanel.add(thumbsUpPanel);

        // Crying Emoji
        JButton cryingButton = createEmojiButton("\uD83D\uDE22");
        JLabel cryingLabel = createEmojiCountLabel(cryingCount);
        JPanel cryingPanel = new JPanel(new BorderLayout());
        cryingPanel.add(cryingButton, BorderLayout.NORTH);
        cryingPanel.add(cryingLabel, BorderLayout.SOUTH);
        emojiPanel.add(cryingPanel);

        // Angry Emoji
        JButton angryButton = createEmojiButton("\uD83D\uDE20");
        JLabel angryLabel = createEmojiCountLabel(angryCount);
        JPanel angryPanel = new JPanel(new BorderLayout());
        angryPanel.add(angryButton, BorderLayout.NORTH);
        angryPanel.add(angryLabel, BorderLayout.SOUTH);
        emojiPanel.add(angryPanel);

        // Laughing Emoji
        JButton laughingButton = createEmojiButton("\uD83D\uDE02");
        JLabel laughingLabel = createEmojiCountLabel(laughingCount);
        JPanel laughingPanel = new JPanel(new BorderLayout());
        laughingPanel.add(laughingButton, BorderLayout.NORTH);
        laughingPanel.add(laughingLabel, BorderLayout.SOUTH);
        emojiPanel.add(laughingPanel);

        int reaction = message.getReaction();
        loveButton.setBackground((reaction == 1) ? Color.RED : null);
        thumbsUpButton.setBackground((reaction == 2) ? Color.RED : null);
        cryingButton.setBackground((reaction == 3) ? Color.RED : null);
        angryButton.setBackground((reaction == 4) ? Color.RED : null);
        laughingButton.setBackground((reaction == 5) ? Color.RED : null);
        loveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (loveButton.getBackground() == Color.RED) {
                    RemoveReactionEmojiButtons
                            .add(new RemoveReactionEmojiButton(message.getID(), loveButton, loveLabel));
                    handleRemoveReactionRequest(message.getID(), userId);
                } else {
                    JButton previousButton = null;
                    JButton newButton = loveButton;
                    JLabel previousLabel = null;
                    JLabel newLabel = loveLabel;
                    if (loveButton.getBackground() == Color.RED) {
                        previousButton = loveButton;
                        previousLabel = loveLabel;
                    } else if (thumbsUpButton.getBackground() == Color.RED) {
                        previousButton = thumbsUpButton;
                        previousLabel = thumbsUpLabel;
                    } else if (cryingButton.getBackground() == Color.RED) {
                        previousButton = cryingButton;
                        previousLabel = cryingLabel;
                    } else if (angryButton.getBackground() == Color.RED) {
                        previousButton = angryButton;
                        previousLabel = angryLabel;
                    } else if (laughingButton.getBackground() == Color.RED) {
                        previousButton = laughingButton;
                        previousLabel = laughingLabel;
                    }

                    emojiButtons
                            .add(new EmojiButtons(message.getID(), previousButton, newButton, previousLabel, newLabel));
                    handlePostReaction(new Reaction(-1, message.getID(), userId, 1));

                }

            }
        });
        thumbsUpButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (thumbsUpButton.getBackground() == Color.RED) {
                    RemoveReactionEmojiButtons
                            .add(new RemoveReactionEmojiButton(message.getID(), thumbsUpButton, thumbsUpLabel));
                    handleRemoveReactionRequest(message.getID(), userId);
                } else {
                    JButton previousButton = null;
                    JButton newButton = thumbsUpButton;
                    JLabel previousLabel = null;
                    JLabel newLabel = thumbsUpLabel;
                    if (loveButton.getBackground() == Color.RED) {
                        previousButton = loveButton;
                        previousLabel = loveLabel;
                    } else if (thumbsUpButton.getBackground() == Color.RED) {
                        previousButton = thumbsUpButton;
                        previousLabel = thumbsUpLabel;
                    } else if (cryingButton.getBackground() == Color.RED) {
                        previousButton = cryingButton;
                        previousLabel = cryingLabel;
                    } else if (angryButton.getBackground() == Color.RED) {
                        previousButton = angryButton;
                        previousLabel = angryLabel;
                    } else if (laughingButton.getBackground() == Color.RED) {
                        previousButton = laughingButton;
                        previousLabel = laughingLabel;
                    }
                    emojiButtons
                            .add(new EmojiButtons(message.getID(), previousButton, newButton, previousLabel, newLabel));

                    handlePostReaction(new Reaction(-1, message.getID(), userId, 2));

                }
            }
        });
        cryingButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (cryingButton.getBackground() == Color.RED) {
                    RemoveReactionEmojiButtons
                            .add(new RemoveReactionEmojiButton(message.getID(), cryingButton, cryingLabel));
                    handleRemoveReactionRequest(message.getID(), userId);
                } else {
                    JButton previousButton = null;
                    JButton newButton = cryingButton;
                    JLabel previousLabel = null;
                    JLabel newLabel = cryingLabel;
                    if (loveButton.getBackground() == Color.RED) {
                        previousButton = loveButton;
                        previousLabel = loveLabel;
                    } else if (thumbsUpButton.getBackground() == Color.RED) {
                        previousButton = thumbsUpButton;
                        previousLabel = thumbsUpLabel;
                    } else if (cryingButton.getBackground() == Color.RED) {
                        previousButton = cryingButton;
                        previousLabel = cryingLabel;
                    } else if (angryButton.getBackground() == Color.RED) {
                        previousButton = angryButton;
                        previousLabel = angryLabel;
                    } else if (laughingButton.getBackground() == Color.RED) {
                        previousButton = laughingButton;
                        previousLabel = laughingLabel;
                    }
                    emojiButtons
                            .add(new EmojiButtons(message.getID(), previousButton, newButton, previousLabel, newLabel));

                    handlePostReaction(new Reaction(-1, message.getID(), userId, 3));

                }
            }
        });
        angryButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (angryButton.getBackground() == Color.RED) {
                    RemoveReactionEmojiButtons
                            .add(new RemoveReactionEmojiButton(message.getID(), angryButton, angryLabel));
                    handleRemoveReactionRequest(message.getID(), userId);
                } else {
                    JButton previousButton = null;
                    JButton newButton = angryButton;
                    JLabel previousLabel = null;
                    JLabel newLabel = angryLabel;
                    if (loveButton.getBackground() == Color.RED) {
                        previousButton = loveButton;
                        previousLabel = loveLabel;
                    } else if (thumbsUpButton.getBackground() == Color.RED) {
                        previousButton = thumbsUpButton;
                        previousLabel = thumbsUpLabel;
                    } else if (cryingButton.getBackground() == Color.RED) {
                        previousButton = cryingButton;
                        previousLabel = cryingLabel;
                    } else if (angryButton.getBackground() == Color.RED) {
                        previousButton = angryButton;
                        previousLabel = angryLabel;
                    } else if (laughingButton.getBackground() == Color.RED) {
                        previousButton = laughingButton;
                        previousLabel = laughingLabel;
                    }
                    emojiButtons
                            .add(new EmojiButtons(message.getID(), previousButton, newButton, previousLabel, newLabel));

                    handlePostReaction(new Reaction(-1, message.getID(), userId, 4));

                }
            }
        });
        laughingButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (laughingButton.getBackground() == Color.RED) {
                    RemoveReactionEmojiButtons
                            .add(new RemoveReactionEmojiButton(message.getID(), laughingButton, laughingLabel));
                    handleRemoveReactionRequest(message.getID(), userId);
                } else {
                    JButton previousButton = null;
                    JButton newButton = laughingButton;
                    JLabel previousLabel = null;
                    JLabel newLabel = laughingLabel;
                    if (loveButton.getBackground() == Color.RED) {
                        previousButton = loveButton;
                        previousLabel = loveLabel;
                    } else if (thumbsUpButton.getBackground() == Color.RED) {
                        previousButton = thumbsUpButton;
                        previousLabel = thumbsUpLabel;
                    } else if (cryingButton.getBackground() == Color.RED) {
                        previousButton = cryingButton;
                        previousLabel = cryingLabel;
                    } else if (angryButton.getBackground() == Color.RED) {
                        previousButton = angryButton;
                        previousLabel = angryLabel;
                    } else if (laughingButton.getBackground() == Color.RED) {
                        previousButton = laughingButton;
                        previousLabel = laughingLabel;
                    }
                    emojiButtons
                            .add(new EmojiButtons(message.getID(), previousButton, newButton, previousLabel, newLabel));

                    handlePostReaction(new Reaction(-1, message.getID(), userId, 5));

                }
            }
        });

        JPanel viewCommentsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        // View Comments button
        JButton viewCommentsButton = new JButton("View Comments");
        viewCommentsButton.addActionListener(e -> handleViewComments(message));
        viewCommentsPanel.add(viewCommentsButton);
        // Add components to the message panel
        messagePanel.add(editorPane);
        messagePanel.add(emojiPanel);
        messagePanel.add(viewCommentsPanel);

        return messagePanel;
    }

    private JLabel createEmojiCountLabel(int count) {
        JLabel label = new JLabel(String.valueOf(count));
        label.setHorizontalAlignment(JLabel.CENTER);
        label.setFont(new Font("Arial", Font.PLAIN, 10)); // Set a smaller font size

        return label;
    }

    private JButton createEmojiButton(String emoji) {
        JButton button = new JButton(emoji);
        button.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 12)); // Set the font to display emojis properly

        return button;
    }

    private void handlePostMessageResponse(PostMessageResponse response) {
        String message = response.isSuccess() ? "Message posted successfully" : "Failed to post message";

        JOptionPane.showMessageDialog(frame, message, "Post Message", JOptionPane.INFORMATION_MESSAGE);
        Message msg = response.getMessage();
        JPanel messageItemPanel = createMessagePanel(
                new Post(msg.getID(), msg.getUserID(), msg.getUsername(), msg.getContent(), msg.getDate(),
                        msg.getReaction(), msg.getReactionCountMap()));
        messagePanel.add(messageItemPanel);
    }

    private void showPostLoginUI(List<Message> userMessages, List<Message> messagesOfInterest) {
        frame.getContentPane().removeAll();
        frame = new JFrame("Y Platform Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(700, 800);
        frame.setLayout(new BorderLayout());

        JTabbedPane tabbedPane = new JTabbedPane();
        PostsPanel = new JPanel();
        PostsPanel.setLayout(new BorderLayout());
        explorePagePanel = new JPanel();
        explorePagePanel.setLayout(new BorderLayout());

        JPanel postLoginPanel = new JPanel(new BorderLayout());
        postLoginPanel.add(createPostPanel(), BorderLayout.NORTH);
        postLoginPanel.add(createFollowPanel(), BorderLayout.SOUTH); // Add follow/unfollow panel

        // Update message display within this method
        updateMessagesDisplay(userMessages, messagesOfInterest);

        PostsPanel.add(postLoginPanel, BorderLayout.PAGE_START);
        PostsPanel.revalidate();
        PostsPanel.repaint();
        tabbedPane.addTab("Posts", PostsPanel);
        explorePanel = new JPanel();
        explorePanel.setLayout(new BoxLayout(explorePanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(explorePanel);
        JPanel containerPanel = new JPanel(new BorderLayout());
        containerPanel.add(scrollPane, BorderLayout.CENTER);
        explorePagePanel.add(containerPanel, BorderLayout.CENTER);

        explorePagePanel.revalidate();
        explorePagePanel.repaint();
        tabbedPane.addTab("Explore", explorePagePanel);
        frame.getContentPane().add(tabbedPane, BorderLayout.CENTER);
        frame.setVisible(true);
    }

    private void handleFollow() {
        String usernameToFollow = followUsernameField.getText();
        FollowRequest request = new FollowRequest(userId, usernameToFollow, true);
        sendRequest(request);
    }

    private void handleUnfollow() {
        String usernameToUnfollow = followUsernameField.getText();
        FollowRequest request = new FollowRequest(userId, usernameToUnfollow, false);
        sendRequest(request);
    }

    private void handleFollowResponse(FollowResponse response) {
        JOptionPane.showMessageDialog(frame, response.getMessage(), "Follow Status", JOptionPane.INFORMATION_MESSAGE);
        if (response.isFollowRequest()) {
            for (Message msg : response.getMessages()) {
                JPanel messageItemPanel = createMessagePanel(
                        new Post(msg.getID(), msg.getUserID(), msg.getUsername(), msg.getContent(), msg.getDate(),
                                msg.getReaction(), msg.getReactionCountMap()));
                messagePanel.add(messageItemPanel);
            }
        }
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
                JPanel messageItemPanel = createMessagePanel(
                        new Post(msg.getID(), msg.getUserID(), msg.getUsername(), msg.getContent(), msg.getDate(),
                                msg.getReaction(), msg.getReactionCountMap()));
                messagePanel.add(messageItemPanel);
            }
        }
    }

    private void handleNewMessagePosted(NewMessagePosted message) {

        Message msg = message.getMessage();

        JPanel messageItemPanel = createMessagePanel(
                new Post(msg.getID(), msg.getUserID(), msg.getUsername(), msg.getContent(), msg.getDate(),
                        msg.getReaction(), msg.getReactionCountMap()));
        messagePanel.add(messageItemPanel);

    }

    private void handlePostedCommentResponse(PostCommentResponse res) {
        int post_id = res.getComment().getPost_id();
        for (CommentPanel p : postCommentPanels) {
            if (p.post_id == post_id) {
                displayComment(p.getCommentsPanel(), res.getComment());
                postCommentPanels.remove(p);
                break;
            }
        }
    }

    private void handlePostReactionResponse(PostReactionResponse res) {
        for (EmojiButtons e : emojiButtons) {
            if (e.post_id == res.getReaction().getPost_id()) {
                if (res.isOldReactionDeleted() && res.isNewReactionAdded()) {
                    if (e.getPreviousButton() != null)
                        e.getPreviousButton().setBackground(null);
                    e.getNewButton().setBackground(Color.RED);
                    if (e.getPreviousLabel() != null)
                        e.getPreviousLabel().setText(Integer.parseInt(e.getPreviousLabel().getText()) - 1 + "");
                    e.getNewLabel().setText(Integer.parseInt(e.getNewLabel().getText()) + 1 + "");
                } else if (res.isOldReactionDeleted()) {
                    e.getPreviousButton().setBackground(null);
                    e.getPreviousLabel().setText(Integer.parseInt(e.getPreviousLabel().getText()) - 1 + "");

                    JOptionPane.showMessageDialog(frame, "Failed to Post Reaction.", "Post Reaction",
                            JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(frame, "Failed to Post Reaction.", "Post Reaction",
                            JOptionPane.INFORMATION_MESSAGE);
                }
                emojiButtons.remove(e);
                break;
            }
        }

    }

    private void handleRemoveReactionResponse(RemoveReactionResponse res) {
        if (res.isSuccess()) {
            for (RemoveReactionEmojiButton b : RemoveReactionEmojiButtons) {
                if (b.getPost_id() == res.getPost_id()) {
                    b.getButton().setBackground(null);
                    b.getLabel().setText(Integer.parseInt(b.getLabel().getText()) - 1 + "");
                    RemoveReactionEmojiButtons.remove(b);
                    break;
                }
            }
        }
    }

    // private static JPanel createChatPanel() {
    // JPanel chatPanel = new JPanel(new BorderLayout());

    // List<Chat> chats = createDummyChats();
    // DefaultListModel<Chat> chatListModel = new DefaultListModel<>();

    // for (Chat chat : chats) {
    // chatListModel.addElement(chat);
    // }

    // JList<Chat> chatList = new JList<>(chatListModel);
    // chatList.setCellRenderer(new ChatListCellRenderer());

    // JScrollPane scrollPane = new JScrollPane(chatList);
    // chatPanel.add(scrollPane, BorderLayout.CENTER);

    // chatList.addMouseListener(new java.awt.event.MouseAdapter() {
    // public void mouseClicked(java.awt.event.MouseEvent evt) {
    // JList<Chat> list = (JList<Chat>) evt.getSource();
    // if (evt.getClickCount() == 2) {
    // int index = list.locationToIndex(evt.getPoint());
    // if (index >= 0) {
    // Chat selectedChat = list.getModel().getElementAt(index);
    // openChatWindow(selectedChat);
    // }
    // }
    // }
    // });

    // return chatPanel;
    // }

    // private static void openChatWindow(Chat chat) {
    // JFrame chatFrame = new JFrame(chat.getName() + " Chat");
    // chatFrame.setSize(400, 500);
    // chatFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    // JPanel chatContainer = new JPanel();
    // chatContainer.setLayout(new GridBagLayout());

    // JTextPane chatArea = new JTextPane();
    // chatArea.setEditable(false);

    // JScrollPane scrollPane = new JScrollPane(chatArea);
    // GridBagConstraints chatConstraints = new GridBagConstraints();
    // chatConstraints.gridx = 0;
    // chatConstraints.gridy = 0;
    // chatConstraints.weightx = 1.0;
    // chatConstraints.weighty = 1.0;
    // chatConstraints.fill = GridBagConstraints.BOTH;
    // chatContainer.add(scrollPane, chatConstraints);

    // JTextField messageField = new JTextField();
    // JButton sendButton = new JButton("Send");

    // sendButton.addActionListener(new ActionListener() {
    // @Override
    // public void actionPerformed(ActionEvent e) {
    // String message = messageField.getText();
    // if (!message.isEmpty()) {
    // addChatToPane(chatArea, chat.getUserName(), message);
    // messageField.setText("");
    // }
    // }
    // });

    // JPanel inputPanel = new JPanel(new BorderLayout());
    // inputPanel.add(messageField, BorderLayout.CENTER);
    // inputPanel.add(sendButton, BorderLayout.EAST);

    // GridBagConstraints inputConstraints = new GridBagConstraints();
    // inputConstraints.gridx = 0;
    // inputConstraints.gridy = 1;
    // inputConstraints.weightx = 1.0;
    // inputConstraints.fill = GridBagConstraints.HORIZONTAL;
    // chatContainer.add(inputPanel, inputConstraints);

    // chatFrame.add(chatContainer, BorderLayout.CENTER);
    // chatFrame.setVisible(true);
    // }

    // private static void addChatToPane(JTextPane chatArea, String username, String
    // message) {
    // StyledDocument doc = chatArea.getStyledDocument();
    // SimpleAttributeSet keyWord = new SimpleAttributeSet();
    // StyleConstants.setForeground(keyWord, Color.BLACK);

    // try {
    // // Create a border for the chat
    // Border border = BorderFactory.createLineBorder(Color.GRAY, 1);

    // // Create a panel to contain the chat and set the border
    // JPanel chatPanel = new JPanel(new BorderLayout());
    // chatPanel.setBorder(border);

    // // Add username to the panel
    // JLabel usernameLabel = new JLabel(username);
    // chatPanel.add(usernameLabel, BorderLayout.NORTH);

    // // Add message to the panel
    // JTextPane chatMessage = new JTextPane();
    // chatMessage.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    // chatMessage.setEditable(false);
    // chatMessage.setText(message);
    // chatPanel.add(chatMessage, BorderLayout.CENTER);

    // // Add date in small font to the panel
    // SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    // String dateStr = dateFormat.format(new Date());
    // JLabel dateLabel = new JLabel(dateStr);
    // dateLabel.setFont(new Font(dateLabel.getFont().getName(), Font.PLAIN, 10));
    // chatPanel.add(dateLabel, BorderLayout.SOUTH);

    // // Insert the panel into the JTextPane
    // chatArea.insertComponent(chatPanel);

    // // Insert a new line after each chat
    // doc.insertString(doc.getLength(), "\n", null);

    // // Scroll to the bottom of the chatArea
    // chatArea.setCaretPosition(doc.getLength());

    // } catch (Exception e) {
    // e.printStackTrace();
    // }
    // }

    // private static List<Chat> createDummyChats() {
    // List<Chat> chats = new ArrayList<>();
    // chats.add(new Chat("John Doe", "john.doe@example.com"));
    // chats.add(new Chat("Alice Smith", "alice.smith@example.com"));
    // chats.add(new Chat("Bob Johnson", "bob.johnson@example.com"));
    // return chats;
    // }

    static class Chat {

        private String name;
        private String userName;

        public Chat(String name, String userName) {
            this.name = name;
            this.userName = userName;
        }

        public String getName() {
            return name;
        }

        public String getUserName() {
            return userName;
        }

        @Override
        public String toString() {
            return name + " (" + userName + ")";
        }
    }

    static class ChatListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
                JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof Chat) {
                Chat chat = (Chat) value;
                setText("<html><b>" + chat.getName() + "</b><br><font color='#888888'>"
                        + chat.getUserName() + "</font></html>");
            }

            return this;
        }
    }

    public static void main(String[] args) {
        // SwingUtilities.invokeLater(YClient::new);
        YClient y = new YClient();
        y.checkForResponses();
    }
}

class Post {
    private String username;
    private String content;
    private String date;
    private int ID;
    private int userID;
    private int reaction;
    private Map<Integer, Integer> reactionCountMap;

    public Post(int iD, int userID, String username, String content, String date, int reaction,
            Map<Integer, Integer> reactionCountMap) {
        this.username = username;
        this.content = content;
        this.date = date;
        ID = iD;
        this.userID = userID;
        this.reaction = reaction;
        this.reactionCountMap = reactionCountMap;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public int getID() {
        return ID;
    }

    public void setID(int iD) {
        ID = iD;
    }

    public int getUserID() {
        return userID;
    }

    public void setUserID(int userID) {
        this.userID = userID;
    }

    public int getReaction() {
        return reaction;
    }

    public void setReaction(int reaction) {
        this.reaction = reaction;
    }

    public Map<Integer, Integer> getReactionCountMap() {
        return reactionCountMap;
    }

    public void setReactionCountMap(Map<Integer, Integer> reactionCountMap) {
        this.reactionCountMap = reactionCountMap;
    }

}

class CommentPanel {
    int post_id;
    JPanel commentsPanel;

    public CommentPanel(int post_id, JPanel commentsPanel) {
        this.post_id = post_id;
        this.commentsPanel = commentsPanel;
    }

    public int getPost_id() {
        return post_id;
    }

    public void setPost_id(int post_id) {
        this.post_id = post_id;
    }

    public JPanel getCommentsPanel() {
        return commentsPanel;
    }

    public void setCommentsPanel(JPanel commentsPanel) {
        this.commentsPanel = commentsPanel;
    }

}

class EmojiButtons {
    int post_id;
    private JButton previousButton;
    private JButton newButton;
    private JLabel previousLabel;
    private JLabel newLabel;

    public EmojiButtons(int post_id, JButton previousButton, JButton newButton, JLabel previousLabel, JLabel newLabel) {
        this.post_id = post_id;
        this.previousButton = previousButton;
        this.newButton = newButton;
        this.previousLabel = previousLabel;
        this.newLabel = newLabel;
    }

    public int getPost_id() {
        return post_id;
    }

    public void setPost_id(int post_id) {
        this.post_id = post_id;
    }

    public JButton getPreviousButton() {
        return previousButton;
    }

    public void setPreviousButton(JButton previousButton) {
        this.previousButton = previousButton;
    }

    public JButton getNewButton() {
        return newButton;
    }

    public void setNewButton(JButton newButton) {
        this.newButton = newButton;
    }

    public JLabel getPreviousLabel() {
        return previousLabel;
    }

    public void setPreviousLabel(JLabel previousLabel) {
        this.previousLabel = previousLabel;
    }

    public JLabel getNewLabel() {
        return newLabel;
    }

    public void setNewLabel(JLabel newLabel) {
        this.newLabel = newLabel;
    }

}

class RemoveReactionEmojiButton {
    private int post_id;
    private JButton button;
    private JLabel label;

    public RemoveReactionEmojiButton(int post_id, JButton button, JLabel label) {
        this.post_id = post_id;
        this.button = button;
        this.label = label;
    }

    public int getPost_id() {
        return post_id;
    }

    public void setPost_id(int post_id) {
        this.post_id = post_id;
    }

    public JButton getButton() {
        return button;
    }

    public void setButton(JButton button) {
        this.button = button;
    }

    public JLabel getLabel() {
        return label;
    }

    public void setLabel(JLabel label) {
        this.label = label;
    }

}