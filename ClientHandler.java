import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.io.Serializable;


public class ClientHandler extends Thread {
    private Socket clientSocket;
    private UserManager userManager;

    public ClientHandler(Socket socket, UserManager userManager) {
        this.clientSocket = socket;
        this.userManager = userManager;
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
            }

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("ClientHandler exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleRegistration(RegistrationRequest request, ObjectOutputStream output) throws IOException {
        boolean success = userManager.registerUser(request.getUsername(), request.getPassword(), request.getName(), request.getEmail());
        String message = success ? "Registration successful" : "Registration failed";
        output.writeObject(new RegistrationResponse(success, message));
    }
    
    private void handleLogin(LoginRequest request, ObjectOutputStream output) throws IOException {
        boolean authenticated = userManager.authenticateUser(request.getUsername(), request.getPassword());
        String message = authenticated ? "Login successful" : "Login failed";
        output.writeObject(new LoginResponse(authenticated, message));
    }
    

    // Additional methods for other functionalities
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
