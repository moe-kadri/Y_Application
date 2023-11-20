import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class YServer {
    public static void main(String[] args) {
        // Check if the port number is provided
        if (args.length < 1) {
            System.out.println("Usage: java YServer <port number>");
            return;
        }

        // Parse the port number from the command line argument
        int port = Integer.parseInt(args[0]);

        // Database connection details
        String jdbcURL = "jdbc:mysql://localhost/Y:3306";
        String jdbcUsername = "root";
        String jdbcPassword = "kali";

        // Create an instance of UserManager
        UserManager userManager = new UserManager(jdbcURL, jdbcUsername, jdbcPassword);

        // Start the server and listen for client connections
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server is listening on port " + port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected");
                // Create a new thread for each client
                new ClientHandler(clientSocket, userManager).start();
            }
        } catch (IOException e) {
            System.err.println("Server exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
