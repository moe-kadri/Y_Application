import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class YServer {
    public static void main(String[] args) {
        int port = Integer.parseInt(args[0]); // Get port number from command line argument

        // Database connection details
        String jdbcURL = "jdbc:mysql://localhost/your_database_name";
        String jdbcUsername = "your_username";
        String jdbcPassword = "your_password";

        UserManager userManager = new UserManager(jdbcURL, jdbcUsername, jdbcPassword);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server is listening on port " + port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected");
                new ClientHandler(clientSocket, userManager).start();
            }
        } catch (IOException e) {
            System.err.println("Server exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
