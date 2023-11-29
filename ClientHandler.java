import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;

import java.io.Serializable;

public class ClientHandler extends Thread {
    private Socket clientSocket;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    private UserManager userManager;
    private MessageManager messageManager;
    private FollowManager followManager;
    Client client;
    static List<Client> activeUsers = new ArrayList<Client>();

    // Constructor
    public ClientHandler(Socket socket, UserManager userManager, MessageManager messageManager,
            FollowManager followManager) {
        this.clientSocket = socket;
        this.userManager = userManager;
        this.messageManager = messageManager;
        this.followManager = followManager;
    }

    @Override
    public void run() {
        try {
            output = new ObjectOutputStream(clientSocket.getOutputStream());
            input = new ObjectInputStream(clientSocket.getInputStream());

            while (true) {
                Object request = input.readObject();

                if (request instanceof RegistrationRequest) {
                    handleRegistration((RegistrationRequest) request);
                } else if (request instanceof LoginRequest) {
                    handleLogin((LoginRequest) request);
                } else if (request instanceof PostMessageRequest) {
                    handlePostMessage((PostMessageRequest) request);
                } else if (request instanceof FollowRequest) {
                    handleFollowRequest((FollowRequest) request);
                } else if (request instanceof RefreshFeedRequest) {
                    handleRefreshFeed((RefreshFeedRequest) request);
                } else if (request instanceof PostCommentRequest) {
                    handlePostCommentRequest((PostCommentRequest) request);
                } else if (request instanceof RetrieveCommentsRequest) {
                    handleRetrieveCommentsRequest((RetrieveCommentsRequest) request);
                } else if (request instanceof PostReactionRequest) {
                    handlePostReactionRequest((PostReactionRequest) request);
                } else if (request instanceof RemoveReactionRequest) {
                    handleRemoveReactionRequest((RemoveReactionRequest) request);
                }

            }
        } catch (EOFException e) {
            System.err.println("Connection terminated: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("ClientHandler IOException: " + e.getMessage());
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            System.err.println("Class not found: " + e.getMessage());
            e.printStackTrace();
        } finally {
            activeUsers.remove(client);
            closeConnections();
        }
    }

    private void handleRegistration(RegistrationRequest request) throws IOException {
        boolean success = userManager.registerUser(
                request.getUsername(),
                request.getPassword(),
                request.getName(),
                request.getEmail());
        String message = success ? "Registration successful" : "Registration failed";
        output.writeObject(new RegistrationResponse(success, message));
    }

    private void handleLogin(LoginRequest request) throws IOException {
        boolean authenticated = userManager.authenticateUser(request.getUsername(), request.getPassword());
        int userId = authenticated ? userManager.getUserId(request.getUsername()) : -1;
        LoginResponse response = new LoginResponse(authenticated, "Login " + (authenticated ? "successful" : "failed"),
                userId, request.getUsername());
        if (authenticated) {
            AddMessagesLists(userId, request.getUsername(), response);
        }
        output.writeObject(response);
        client = new Client(userId, output);
        activeUsers.add(client);
    }

    private void handleRefreshFeed(RefreshFeedRequest request) throws IOException {
        RefreshFeedResponse RefreshFeedMessages = messageManager.getRefreshFeed(request.getUserId(), request.getDate());
        output.writeObject(RefreshFeedMessages);
    }

    private void handlePostMessage(PostMessageRequest request) throws IOException {
        Message msg = messageManager.postMessage(request.getUserId(), request.getUsername(), request.getContent());
        if (msg.getID() >= 0) {
            informClientsOfPostedMessage(msg);
        }
        PostMessageResponse postMessageResponse = new PostMessageResponse(msg.getID() >= 0, msg);
        output.writeObject(postMessageResponse);
    }

    private void informClientsOfPostedMessage(Message msg) throws IOException {
        List<Integer> followers = messageManager.getFollowersList(msg.getUserID());
        for (Client client : activeUsers) {
            if (followers.contains(client.userId)) {
                client.output.writeObject(new NewMessagePosted(msg));
            }
        }
    }

    private void handleFollowRequest(FollowRequest request) throws IOException {
        boolean success;
        String message;
        List<Message> messages = null;
        int followedUserId = userManager.getUserId(request.getFollowedUsername());
        if (followedUserId == -1) {
            success = false;
            message = "User not found: " + request.getFollowedUsername();
        } else if (request.isFollow()) {
            success = followManager.followUser(request.getFollowerId(), followedUserId);
            message = success ? "You are now following " + request.getFollowedUsername()
                    : "Failed to follow " + request.getFollowedUsername();
            messages = messageManager.getMessagesByUser(followedUserId, request.getFollowedUsername());
            // Add Retrieve the posts for this user
        } else {
            success = followManager.unfollowUser(request.getFollowerId(), followedUserId);
            message = success ? "You unfollowed " + request.getFollowedUsername()
                    : "Failed to unfollow " + request.getFollowedUsername();
        }

        output.writeObject(new FollowResponse(success, message, messages, request.isFollow()));
    }

    private void handlePostCommentRequest(PostCommentRequest req) throws IOException {
        Comment postedComment = messageManager.postComment(req.getComment());
        PostCommentResponse postCommentResponse = new PostCommentResponse(postedComment.getComment_id() >= 0,
                postedComment);
        output.writeObject(postCommentResponse);
    }

    private void handlePostReactionRequest(PostReactionRequest req) throws IOException {
        PostReactionResponse res = messageManager.postReaction(req.getReaction());
        output.writeObject(res);
    }

    private void handleRemoveReactionRequest(RemoveReactionRequest req) throws IOException {
        RemoveReactionResponse res = messageManager.RemoveReaction(req.getPost_id(), req.getUser_id());
        output.writeObject(res);
    }

    private void handleRetrieveCommentsRequest(RetrieveCommentsRequest req) throws IOException {
        List<Comment> comments = messageManager.retrieveComments(req.getPost_id());
        RetrieveCommentsResponse retrieveCommentsResponse = new RetrieveCommentsResponse(req.getPost_id(), comments);
        output.writeObject(retrieveCommentsResponse);
    }

    private void AddMessagesLists(int userId, String username, LoginResponse res) throws IOException {
        List<Message> userMessages = messageManager.getMessagesByUser(userId, username);
        List<Message> messagesOfInterest = messageManager.getMessagesOfInterest(userId);
        res.setUserMessages(userMessages);
        res.setMessagesOfInterest(messagesOfInterest);
    }

    private void closeConnections() {
        try {
            if (input != null) {
                input.close();
            }
            if (output != null) {
                output.close();
            }
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing connections: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Additional methods as needed
}

class RegistrationRequest implements Serializable {
    private String username;
    private String password;
    private String name;
    private String email;

    // Constructor
    public RegistrationRequest(String username, String password, String name, String email) {
        this.username = username;
        this.password = password;
        this.name = name;
        this.email = email;
    }

    // Getters
    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }
}

class LoginRequest implements Serializable {
    private String username;
    private String password;

    // Constructor
    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // Getters
    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}

class RegistrationResponse implements Serializable {
    private boolean success;
    private String message;

    // Constructor
    public RegistrationResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    // Getters
    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}

class LoginResponse implements Serializable {
    private boolean success;
    private String message;
    private int userId; // User ID
    private String username;
    private List<Message> userMessages;
    private List<Message> messagesOfInterest;

    public String getUsername() {
        return username;
    }

    public void setUsername(String usename) {
        this.username = usename;
    }

    public List<Message> getUserMessages() {
        return userMessages;
    }

    public List<Message> getMessagesOfInterest() {
        return messagesOfInterest;
    }

    public LoginResponse(boolean success, String message, int userId, String username) {
        this.success = success;
        this.message = message;
        this.userId = userId;
        this.username = username;
    }

    // Getters
    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserMessages(List<Message> userMessages) {
        this.userMessages = userMessages;
    }

    public void setMessagesOfInterest(List<Message> messagesOfInterest) {
        this.messagesOfInterest = messagesOfInterest;
    }

}

class RefreshFeedResponse implements Serializable {
    private boolean success;
    private List<Message> messages;

    public RefreshFeedResponse(boolean success, List<Message> messages) {
        this.success = success;
        this.messages = messages;
    }

    // Getters
    public boolean isSuccess() {
        return success;
    }

    public List<Message> getMessages() {
        return messages;
    }

}

class PostMessageRequest implements Serializable {
    private int userId;
    private String username;
    private String content;

    // Constructor
    public PostMessageRequest(int userId, String username, String content) {
        this.userId = userId;
        this.content = content;
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    // Getters
    public int getUserId() {
        return userId;
    }

    public String getContent() {
        return content;
    }

    // Setters, if you need to modify the fields after object creation
    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setContent(String content) {
        this.content = content;
    }
}

class RemoveReactionRequest implements Serializable {
    private int post_id;
    private int user_id;

    public RemoveReactionRequest(int post_id, int user_id) {
        this.post_id = post_id;
        this.user_id = user_id;
    }

    public int getPost_id() {
        return post_id;
    }

    public void setPost_id(int post_id) {
        this.post_id = post_id;
    }

    public int getUser_id() {
        return user_id;
    }

    public void setUser_id(int user_id) {
        this.user_id = user_id;
    }

}

class RemoveReactionResponse implements Serializable {
    private boolean success;
    private int post_id;
    private int user_id;

    public RemoveReactionResponse(boolean success, int post_id, int user_id) {
        this.success = success;
        this.post_id = post_id;
        this.user_id = user_id;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getPost_id() {
        return post_id;
    }

    public void setPost_id(int post_id) {
        this.post_id = post_id;
    }

    public int getUser_id() {
        return user_id;
    }

    public void setUser_id(int user_id) {
        this.user_id = user_id;
    }

}

class PostReactionRequest implements Serializable {
    private Reaction reaction;

    public PostReactionRequest(Reaction reaction) {
        this.reaction = reaction;
    }

    public Reaction getReaction() {
        return reaction;
    }

    public void setReaction(Reaction reaction) {
        this.reaction = reaction;
    }

}

class PostReactionResponse implements Serializable {
    private boolean oldReactionDeleted;
    private boolean newReactionAdded;
    private Reaction reaction;

    public PostReactionResponse(boolean oldReactionDeleted, boolean newReactionAdded, Reaction reaction) {
        this.oldReactionDeleted = oldReactionDeleted;
        this.newReactionAdded = newReactionAdded;
        this.reaction = reaction;
    }

    public boolean isOldReactionDeleted() {
        return oldReactionDeleted;
    }

    public void setOldReactionDeleted(boolean oldReactionDeleted) {
        this.oldReactionDeleted = oldReactionDeleted;
    }

    public boolean isNewReactionAdded() {
        return newReactionAdded;
    }

    public void setNewReactionAdded(boolean newReactionAdded) {
        this.newReactionAdded = newReactionAdded;
    }

    public Reaction getReaction() {
        return reaction;
    }

    public void setReaction(Reaction reaction) {
        this.reaction = reaction;
    }

}

class PostCommentRequest implements Serializable {
    private Comment comment;

    public Comment getComment() {
        return comment;
    }

    public PostCommentRequest(Comment comment) {
        this.comment = comment;
    }

}

class PostCommentResponse implements Serializable {
    private boolean success;
    private Comment comment;

    public PostCommentResponse(boolean success, Comment comment) {
        this.success = success;
        this.comment = comment;
    }

    public Comment getComment() {
        return comment;
    }

    public void setComment(Comment comment) {
        this.comment = comment;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

}

class Comment implements Serializable {
    private int comment_id;
    private int post_id;
    private int author_id;
    private String author_username;
    private String content;
    private String date;

    public Comment(int comment_id, int post_id, int user_id, String username, String content, String date) {
        this.comment_id = comment_id;
        this.post_id = post_id;
        this.author_id = user_id;
        this.author_username = username;
        this.content = content;
        this.date = date;
    }

    public int getComment_id() {
        return comment_id;
    }

    public void setComment_id(int comment_id) {
        this.comment_id = comment_id;
    }

    public int getPost_id() {
        return post_id;
    }

    public void setPost_id(int post_id) {
        this.post_id = post_id;
    }

    public int getAuthor_id() {
        return author_id;
    }

    public void setAuthor_id(int user_id) {
        this.author_id = user_id;
    }

    public String getAuthor_username() {
        return author_username;
    }

    public void setAuthor_username(String username) {
        this.author_username = username;
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

}

class Reaction implements Serializable {
    private int reaction_id;
    private int post_id;
    private int author_id;
    private int emojiNumber;

    public int getReaction_id() {
        return reaction_id;
    }

    public void setReaction_id(int reaction_id) {
        this.reaction_id = reaction_id;
    }

    public int getPost_id() {
        return post_id;
    }

    public void setPost_id(int post_id) {
        this.post_id = post_id;
    }

    public int getAuthor_id() {
        return author_id;
    }

    public void setAuthor_id(int author_id) {
        this.author_id = author_id;
    }

    public int getEmojiNumber() {
        return emojiNumber;
    }

    public void setEmojiNumber(int emojiNumber) {
        this.emojiNumber = emojiNumber;
    }

    public Reaction(int reaction_id, int post_id, int author_id, int emojiNumber) {
        this.reaction_id = reaction_id;
        this.post_id = post_id;
        this.author_id = author_id;
        this.emojiNumber = emojiNumber;
    }

}

class PostMessageResponse implements Serializable {
    private boolean success;
    private Message message;

    // Constructor
    public PostMessageResponse(boolean success, Message message) {
        this.success = success;
        this.message = message;
    }

    // Getters
    public boolean isSuccess() {
        return success;
    }

    public Message getMessage() {
        return message;
    }

    // Setters, if you need to modify the fields after object creation
    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setMessage(Message message) {
        this.message = message;
    }
}

class FollowRequest implements Serializable {
    private int followerId;
    private int followedId; // Changed from followedUsername to followedId
    private String followedUsername; // Changed from followedId to followedUsername
    private boolean isFollow; // true for follow, false for unfollow

    // Constructor
    public FollowRequest(int followerId, String followedUsername, boolean isFollow) {
        this.followerId = followerId;
        this.followedUsername = followedUsername;
        this.isFollow = isFollow;
    }

    // Getters
    public int getFollowerId() {
        return followerId;
    }

    public int getFollowedId() {
        return followedId;
    }

    public boolean isFollow() {
        return isFollow;
    }

    public String getFollowedUsername() {
        return followedUsername;
    }

    // Setters, if you need to modify the fields after object creation
    public void setFollowerId(int followerId) {
        this.followerId = followerId;
    }

    public void setFollowedId(int followedId) {
        this.followedId = followedId;
    }

    public void setFollow(boolean isFollow) {
        this.isFollow = isFollow;
    }
}

class FollowResponse implements Serializable {
    private boolean success;
    private boolean FollowRequest;
    private String message;
    private List<Message> messages;

    public FollowResponse(boolean success, String message, List<Message> messages, boolean FollowRequest) {
        this.success = success;
        this.message = message;
        this.messages = messages;
        this.FollowRequest = FollowRequest;
    }

    public boolean isFollowRequest() {
        return FollowRequest;
    }

    public void setFollowRequest(boolean followRequest) {
        FollowRequest = followRequest;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    // Getters
    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    // Setters, if you need to modify the fields after object creation
    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

class Message implements Serializable {
    private final int ID;
    private final int userID;
    private final String username;
    private final String content;
    private final String date;
    private int reaction;
    private Map<Integer, Integer> reactionCountMap;

    public Message(int iD, int userID, String username, String content, String date, int reaction,
            Map<Integer, Integer> reactionCountMap) {
        ID = iD;
        this.userID = userID;
        this.username = username;
        this.content = content;
        this.date = date;
        this.reaction = reaction;
        this.reactionCountMap = reactionCountMap;
    }

    public int getID() {
        return ID;
    }

    public int getUserID() {
        return userID;
    }

    public String getUsername() {
        return username;
    }

    public String getContent() {
        return content;
    }

    public String getDate() {
        return date;
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

class RefreshFeedRequest implements Serializable {
    private int userId;
    private String date;

    // Constructor
    public RefreshFeedRequest(int userId, String date) {
        this.userId = userId;
        this.date = date;
    }

    // Getters
    public int getUserId() {
        return userId;
    }

    public String getDate() {
        return date;
    }

    // Setters, if you need to modify the fields after object creation
    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setDate(String date) {
        this.date = date;
    }
}

class RetrieveCommentsRequest implements Serializable {
    private int post_id;

    public int getPost_id() {
        return post_id;
    }

    public void setPost_id(int post_id) {
        this.post_id = post_id;
    }

    // Constructor
    public RetrieveCommentsRequest(int post_id) {
        this.post_id = post_id;
    }

}

class RetrieveCommentsResponse implements Serializable {
    private int post_id;
    private List<Comment> comments;

    public RetrieveCommentsResponse(int post_id, List<Comment> comments) {
        this.post_id = post_id;
        this.comments = comments;
    }

    public int getPost_id() {
        return post_id;
    }

    public void setPost_id(int post_id) {
        this.post_id = post_id;
    }

    public List<Comment> getComments() {
        return comments;
    }
}

class NewMessagePosted implements Serializable {
    private Message message;

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    // Constructor
    public NewMessagePosted(Message message) {
        this.message = message;
    }

}

class Client {
    int userId;
    ObjectOutputStream output;

    public Client(int userId, ObjectOutputStream output) {
        this.userId = userId;
        this.output = output;
    }

}