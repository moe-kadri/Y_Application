import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.io.Serializable;

public class ClientHandler extends Thread {
    private Socket clientSocket;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    private UserManager userManager;
    private MessageManager messageManager;
    private FollowManager followManager;

    // Constructor
    public ClientHandler(Socket socket, UserManager userManager, MessageManager messageManager, FollowManager followManager) {
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
                }
                // Add handling for other request types
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
            closeConnections();
        }
    }

    private void handleRegistration(RegistrationRequest request) throws IOException {
        boolean success = userManager.registerUser(
            request.getUsername(), 
            request.getPassword(), 
            request.getName(), 
            request.getEmail()
        );
        String message = success ? "Registration successful" : "Registration failed";
        output.writeObject(new RegistrationResponse(success, message));
    }
    

    private void handleLogin(LoginRequest request) throws IOException {
        boolean authenticated = userManager.authenticateUser(request.getUsername(), request.getPassword());
        int userId = authenticated ? userManager.getUserId(request.getUsername()) : -1;
        LoginResponse response = new LoginResponse(authenticated, "Login " + (authenticated ? "successful" : "failed"), userId);
        output.writeObject(response);
    
        if (authenticated) {
            sendUserMessages(userId);
        }
    }
    
    

    private void handlePostMessage(PostMessageRequest request) throws IOException {
        boolean success = messageManager.postMessage(request.getUserId(), request.getContent());
        String message = success ? "Message posted successfully" : "Failed to post message";
        PostMessageResponse postMessageResponse = new PostMessageResponse(success, message);
        output.writeObject(postMessageResponse);
    }
    
    

    private void handleFollowRequest(FollowRequest request) throws IOException {
        boolean success;
        String message;
    
        int followedUserId = userManager.getUserId(request.getFollowedUsername());
        if (followedUserId == -1) {
            success = false;
            message = "User not found: " + request.getFollowedUsername();
        } else if (request.isFollow()) {
            success = followManager.followUser(request.getFollowerId(), followedUserId);
            message = success ? "You are now following " + request.getFollowedUsername() : "Failed to follow " + request.getFollowedUsername();
        } else {
            success = followManager.unfollowUser(request.getFollowerId(), followedUserId);
            message = success ? "You unfollowed " + request.getFollowedUsername() : "Failed to unfollow " + request.getFollowedUsername();
        }
    
        output.writeObject(new FollowResponse(success, message));
    }
    
    
    
    
    

    private void sendUserMessages(int userId) throws IOException {
        List<String> userMessages = messageManager.getMessagesByUser(userId);
        List<Message> messagesOfInterest = messageManager.getMessagesOfInterest(userId);
        output.writeObject(userMessages);
        output.writeObject(messagesOfInterest);
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

    public LoginResponse(boolean success, String message, int userId) {
        this.success = success;
        this.message = message;
        this.userId = userId;
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
    
    // Setters if needed
}



class PostMessageRequest implements Serializable {
    private int userId;
    private String content;

    // Constructor
    public PostMessageRequest(int userId, String content) {
        this.userId = userId;
        this.content = content;
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

class PostMessageResponse implements Serializable {
    private boolean success;
    private String message;

    // Constructor
    public PostMessageResponse(boolean success, String message) {
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

    // Setters, if you need to modify the fields after object creation
    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setMessage(String message) {
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
    private String message;

    // Constructor
    public FollowResponse(boolean success, String message) {
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

    // Setters, if you need to modify the fields after object creation
    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

class Message implements Serializable {
    private final String username;
    private final String content;

    public Message(String username, String content) {
        this.username = username;
        this.content = content;
    }

    public String getUsername() {
        return username;
    }

    public String getContent() {
        return content;
    }
}

