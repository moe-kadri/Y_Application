import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.io.Serializable;

public class ClientHandler extends Thread {
    private Socket clientSocket;
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

    public void run() {
        try (ObjectInputStream input = new ObjectInputStream(clientSocket.getInputStream());
             ObjectOutputStream output = new ObjectOutputStream(clientSocket.getOutputStream())) {

            Object request = input.readObject();

            if (request instanceof RegistrationRequest) {
                RegistrationRequest regRequest = (RegistrationRequest) request;
                handleRegistration(regRequest, output);
            } else if (request instanceof LoginRequest) {
                LoginRequest loginRequest = (LoginRequest) request;
                handleLogin(loginRequest, output);
            } else if (request instanceof PostMessageRequest) {
                PostMessageRequest messageRequest = (PostMessageRequest) request;
                handlePostMessage(messageRequest, output);
            } else if (request instanceof FollowRequest) {
                FollowRequest followRequest = (FollowRequest) request;
                handleFollowRequest(followRequest, output);
            }

            // Handle other types of requests if necessary

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("ClientHandler exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleRegistration(RegistrationRequest request, ObjectOutputStream output) throws IOException {
        boolean success = userManager.registerUser(
            request.getUsername(), 
            request.getPassword(), 
            request.getName(), 
            request.getEmail()
        );
        String message = success ? "Registration successful" : "Registration failed";
        output.writeObject(new RegistrationResponse(success, message));
    }

    private void handleLogin(LoginRequest request, ObjectOutputStream output) throws IOException {
        boolean authenticated = userManager.authenticateUser(
            request.getUsername(), 
            request.getPassword()
        );
        
        LoginResponse response;
        if (authenticated) {
            response = new LoginResponse(true, "Login successful");
            output.writeObject(response);
            sendUserMessages(request.getUsername(), output);
        } else {
            response = new LoginResponse(false, "Login failed");
            output.writeObject(response);
        }
    }

    private void handlePostMessage(PostMessageRequest request, ObjectOutputStream output) throws IOException {
        boolean success = messageManager.postMessage(request.getUserId(), request.getContent());
        String message = success ? "Message posted successfully" : "Failed to post message";
        output.writeObject(new PostMessageResponse(success, message));
    }

    private void handleFollowRequest(FollowRequest request, ObjectOutputStream output) throws IOException {
        boolean success;
        if (request.isFollow()) {
            success = followManager.followUser(request.getFollowerId(), request.getFollowedId());
        } else {
            success = followManager.unfollowUser(request.getFollowerId(), request.getFollowedId());
        }
        String message = success ? "Operation successful" : "Operation failed";
        output.writeObject(new FollowResponse(success, message));
    }

    private void sendUserMessages(String username, ObjectOutputStream output) throws IOException {
        int userId = userManager.getUserId(username);
        
        List<String> userMessages = messageManager.getMessagesByUser(userId);
        output.writeObject(userMessages); // Send the user's messages

        List<String> feedMessages = messageManager.getFeedForUser(userId);
        output.writeObject(feedMessages); // Send the messages of interest
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
    // Include additional fields as needed, e.g., user details

    // Constructor
    public LoginResponse(boolean success, String message) {
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

    // Additional getters for any other user-specific fields
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
    private int followedId;
    private boolean isFollow; // true for follow, false for unfollow

    // Constructor
    public FollowRequest(int followerId, int followedId, boolean isFollow) {
        this.followerId = followerId;
        this.followedId = followedId;
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
