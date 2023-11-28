import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
                            msg.getReaction()));
            messagePanel.add(userMessagePanel);
        }

        for (Message msg : messagesOfInterest) {
            JPanel messageItemPanel = createMessagePanel(
                    new Post(msg.getID(), msg.getUserID(), msg.getUsername(), msg.getContent(), msg.getDate(),
                            msg.getReaction()));
            messagePanel.add(messageItemPanel);
        }

        if (userMessages.isEmpty() && messagesOfInterest.isEmpty()) {
            JPanel noMessagePanel = createMessagePanel(new Post(0, 0, "No messages to display.", "", "", -1));
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

        // Emoji buttons
        JPanel emojiPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton loveButton = createEmojiButton("\u2764\uFE0F");
        JButton thumbsUpButton = createEmojiButton("\uD83D\uDC4D");
        JButton cryingButton = createEmojiButton("\uD83D\uDE22");
        JButton angryButton = createEmojiButton("\uD83D\uDE20");
        JButton laughingButton = createEmojiButton("\uD83D\uDE02");

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
                    RemoveReactionEmojiButtons.add(new RemoveReactionEmojiButton(message.getID(), loveButton));
                    handleRemoveReactionRequest(message.getID(), userId);
                } else {
                    handlePostReaction(new Reaction(-1, message.getID(), userId, 1));
                    emojiButtons
                            .add(new EmojiButtons(message.getID(), loveButton, thumbsUpButton, cryingButton,
                                    angryButton,
                                    laughingButton));

                }

            }
        });
        thumbsUpButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (thumbsUpButton.getBackground() == Color.RED) {
                    RemoveReactionEmojiButtons.add(new RemoveReactionEmojiButton(message.getID(), thumbsUpButton));
                    handleRemoveReactionRequest(message.getID(), userId);
                } else {
                    handlePostReaction(new Reaction(-1, message.getID(), userId, 2));
                    emojiButtons
                            .add(new EmojiButtons(message.getID(), loveButton, thumbsUpButton, cryingButton,
                                    angryButton,
                                    laughingButton));

                }
            }
        });
        cryingButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (cryingButton.getBackground() == Color.RED) {
                    RemoveReactionEmojiButtons.add(new RemoveReactionEmojiButton(message.getID(), cryingButton));
                    handleRemoveReactionRequest(message.getID(), userId);
                } else {
                    handlePostReaction(new Reaction(-1, message.getID(), userId, 3));
                    emojiButtons
                            .add(new EmojiButtons(message.getID(), loveButton, thumbsUpButton, cryingButton,
                                    angryButton,
                                    laughingButton));

                }
            }
        });
        angryButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (angryButton.getBackground() == Color.RED) {
                    RemoveReactionEmojiButtons.add(new RemoveReactionEmojiButton(message.getID(), angryButton));
                    handleRemoveReactionRequest(message.getID(), userId);
                } else {
                    handlePostReaction(new Reaction(-1, message.getID(), userId, 4));
                    emojiButtons
                            .add(new EmojiButtons(message.getID(), loveButton, thumbsUpButton, cryingButton,
                                    angryButton,
                                    laughingButton));

                }
            }
        });
        laughingButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (laughingButton.getBackground() == Color.RED) {
                    RemoveReactionEmojiButtons.add(new RemoveReactionEmojiButton(message.getID(), laughingButton));
                    handleRemoveReactionRequest(message.getID(), userId);
                } else {
                    handlePostReaction(new Reaction(-1, message.getID(), userId, 5));
                    emojiButtons
                            .add(new EmojiButtons(message.getID(), loveButton, thumbsUpButton, cryingButton,
                                    angryButton,
                                    laughingButton));

                }
            }
        });
        emojiPanel.add(loveButton);
        emojiPanel.add(thumbsUpButton);
        emojiPanel.add(cryingButton);
        emojiPanel.add(angryButton);
        emojiPanel.add(laughingButton);

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
                        msg.getReaction()));
        messagePanel.add(messageItemPanel);
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
        if (response.isFollowRequest()) {
            for (Message msg : response.getMessages()) {
                JPanel messageItemPanel = createMessagePanel(
                        new Post(msg.getID(), msg.getUserID(), msg.getUsername(), msg.getContent(), msg.getDate(),
                                msg.getReaction()));
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
                                msg.getReaction()));
                messagePanel.add(messageItemPanel);
            }
        }
    }

    private void handleNewMessagePosted(NewMessagePosted message) {

        Message msg = message.getMessage();

        JPanel messageItemPanel = createMessagePanel(
                new Post(msg.getID(), msg.getUserID(), msg.getUsername(), msg.getContent(), msg.getDate(),
                        msg.getReaction()));
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
                    int reaction = res.getReaction().getEmojiNumber();
                    e.getLoveButton().setBackground((reaction == 1) ? Color.RED : null);
                    e.getThumbsUpButton().setBackground((reaction == 2) ? Color.RED : null);
                    e.getCryingButton().setBackground((reaction == 3) ? Color.RED : null);
                    e.getAngryButton().setBackground((reaction == 4) ? Color.RED : null);
                    e.getLaughingButton().setBackground((reaction == 5) ? Color.RED : null);
                } else if (res.isOldReactionDeleted()) {
                    e.getLoveButton().setBackground(null);
                    e.getThumbsUpButton().setBackground(null);
                    e.getCryingButton().setBackground(null);
                    e.getAngryButton().setBackground(null);
                    e.getLaughingButton().setBackground(null);
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
                    RemoveReactionEmojiButtons.remove(b);
                    break;
                }
            }
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

    public Post(int iD, int userID, String username, String content, String date, int reaction) {
        this.username = username;
        this.content = content;
        this.date = date;
        ID = iD;
        this.userID = userID;
        this.reaction = reaction;
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
    private JButton loveButton;
    private JButton thumbsUpButton;
    private JButton cryingButton;
    private JButton angryButton;
    private JButton laughingButton;

    public EmojiButtons(int post_id, JButton loveButton, JButton thumbsUpButton, JButton cryingButton,
            JButton angryButton, JButton laughingButton) {
        this.post_id = post_id;
        this.loveButton = loveButton;
        this.thumbsUpButton = thumbsUpButton;
        this.cryingButton = cryingButton;
        this.angryButton = angryButton;
        this.laughingButton = laughingButton;
    }

    public int getPost_id() {
        return post_id;
    }

    public void setPost_id(int post_id) {
        this.post_id = post_id;
    }

    public JButton getLoveButton() {
        return loveButton;
    }

    public void setLoveButton(JButton loveButton) {
        this.loveButton = loveButton;
    }

    public JButton getThumbsUpButton() {
        return thumbsUpButton;
    }

    public void setThumbsUpButton(JButton thumbsUpButton) {
        this.thumbsUpButton = thumbsUpButton;
    }

    public JButton getCryingButton() {
        return cryingButton;
    }

    public void setCryingButton(JButton cryingButton) {
        this.cryingButton = cryingButton;
    }

    public JButton getAngryButton() {
        return angryButton;
    }

    public void setAngryButton(JButton angryButton) {
        this.angryButton = angryButton;
    }

    public JButton getLaughingButton() {
        return laughingButton;
    }

    public void setLaughingButton(JButton laughingButton) {
        this.laughingButton = laughingButton;
    }

}

class RemoveReactionEmojiButton {
    private int post_id;
    private JButton button;

    public RemoveReactionEmojiButton(int post_id, JButton button) {
        this.post_id = post_id;
        this.button = button;
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

}