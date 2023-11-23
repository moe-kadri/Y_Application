import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MessageManager {
    private String jdbcURL;
    private String jdbcUsername;
    private String jdbcPassword;

    public MessageManager(String jdbcURL, String jdbcUsername, String jdbcPassword) {
        this.jdbcURL = jdbcURL;
        this.jdbcUsername = jdbcUsername;
        this.jdbcPassword = jdbcPassword;
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(jdbcURL, jdbcUsername, jdbcPassword);
    }

    public boolean postMessage(int userId, String content) {
        String sql = "INSERT INTO messages (user_id, content) VALUES (?, ?)";
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setString(2, content);
            statement.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<String> getMessagesByUser(int userId) {
        String sql = "SELECT content FROM messages WHERE user_id = ?";
        List<String> messages = new ArrayList<>();
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                String content = resultSet.getString("content");
                messages.add(content);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return messages;
    }

    public List<String> getFeedForUser(int userId) {
        String sql = "SELECT content FROM messages "
                   + "JOIN followers ON messages.user_id = followers.followed_id "
                   + "WHERE followers.follower_id = ?";
        List<String> feed = new ArrayList<>();
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                String content = resultSet.getString("content");
                feed.add(content);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return feed;
    }

    public List<Message> getMessagesOfInterest(int userId) {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT m.content, u.username FROM messages m JOIN users u ON m.user_id <> u.id WHERE m.user_id = ?"; // Adjust WHERE clause as needed
    
        try (Connection conn = DriverManager.getConnection(jdbcURL, jdbcUsername, jdbcPassword);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId); // Set the user_id if filtering by user
    
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String content = rs.getString("content");
                    String username = rs.getString("username");
                    messages.add(new Message(username, content));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            // Handle exceptions
        }
        return messages;
    }
    
}

