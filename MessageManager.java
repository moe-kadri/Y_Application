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

    public List<Message> getMessagesByUser(int userId, String username) {
        String sql = "SELECT content, posted_at FROM messages WHERE user_id = ?";
        List<Message> messages = new ArrayList<>();
        try (Connection connection = connect();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                String content = resultSet.getString("content");
                String date = resultSet.getString("posted_at");
                messages.add(new Message(username, content, date));
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
        String sql = "SELECT m.content, u.username, m.posted_at FROM messages m, users u, followers f WHERE f.follower_id = ? AND f.followed_id = m.user_id AND f.followed_id = u.id; ";

        try (Connection conn = DriverManager.getConnection(jdbcURL, jdbcUsername, jdbcPassword);
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId); // Set the user_id if filtering by user

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String content = rs.getString("content");
                    String username = rs.getString("username");
                    String date = rs.getString("posted_at");
                    messages.add(new Message(username, content, date));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            // Handle exceptions
        }
        return messages;
    }

    public RefreshFeedResponse getRefreshFeed(int userId, String LastRefreshed) {

        List<Message> messages = new ArrayList<>();
        String sql = "SELECT m.content, u.username, m.posted_at FROM (select * From messages where posted_at > ?) m, users u, followers f WHERE f.follower_id = ? AND f.followed_id = m.user_id AND f.followed_id = u.id; ";
        boolean success = false;
        try (Connection conn = DriverManager.getConnection(jdbcURL, jdbcUsername, jdbcPassword);
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, LastRefreshed);
            stmt.setInt(2, userId); // Set the user_id if filtering by user

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String content = rs.getString("content");
                    String username = rs.getString("username");
                    String date = rs.getString("posted_at");
                    messages.add(new Message(username, content, date));
                }
            }
            success = true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new RefreshFeedResponse(success, messages);
    }

}
