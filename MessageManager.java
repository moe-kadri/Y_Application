import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.sql.Statement;

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

    public Message postMessage(int userId, String username, String content) {
        String sql = "INSERT INTO posts (user_id, content) VALUES (?, ?)";
        try (Connection connection = connect();
                PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, userId);
            statement.setString(2, content);
            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Creating message failed, no rows affected.");
            }
            int postedMessageID = -1;
            String postedAt = "-1";

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    postedMessageID = generatedKeys.getInt(1);
                    // Retrieve posted_at separately
                    String sqlGetPostedAt = "SELECT posted_at FROM posts WHERE id = ?";
                    try (PreparedStatement getPostedAtStatement = connection.prepareStatement(sqlGetPostedAt)) {
                        getPostedAtStatement.setInt(1, postedMessageID);
                        try (ResultSet resultSet = getPostedAtStatement.executeQuery()) {
                            if (resultSet.next()) {
                                postedAt = resultSet.getString("posted_at");
                            }
                        }
                    }
                } else {
                    throw new SQLException("Creating message failed, no ID obtained.");
                }
            }
            return new Message(postedMessageID, userId, username, content, postedAt, -1, null);
        } catch (SQLException e) {
            e.printStackTrace();
            return new Message(-1, userId, username, content, "", -1, null);
        }
    }

    public List<Message> getMessagesByUser(int userId, String username) {
        String sql = "SELECT p.id, p.content, p.posted_at, r.reaction FROM posts p LEFT JOIN reactions r ON r.post_id = p.id AND r.author_id = p.user_id WHERE p.user_id = ?;";
        List<Message> messages = new ArrayList<>();
        try (Connection connection = connect();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                String content = resultSet.getString("content");
                String date = resultSet.getString("posted_at");
                int ID = resultSet.getInt("id");
                int reaction = resultSet.getInt("reaction");
                Map<Integer, Integer> reactionCountMap = getPostReactionsCount(ID);
                messages.add(new Message(ID, userId, username, content, date, reaction, reactionCountMap));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return messages;
    }

    public List<String> getFeedForUser(int userId) {
        String sql = "SELECT content FROM posts "
                + "JOIN followers ON posts.user_id = followers.followed_id "
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
        String sql = "SELECT p.content, p.id as post_id, u.id as user_id, u.username, p.posted_at, r.reaction FROM posts p JOIN users u ON p.user_id = u.id JOIN followers f ON f.follower_id = ? AND f.followed_id = p.user_id LEFT JOIN reactions r ON r.post_id = p.id AND r.author_id = f.follower_id;";

        try (Connection conn = DriverManager.getConnection(jdbcURL, jdbcUsername, jdbcPassword);
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId); // Set the user_id if filtering by user

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String content = rs.getString("content");
                    String username = rs.getString("username");
                    String date = rs.getString("posted_at");
                    int user_id = rs.getInt("user_id");
                    int id = rs.getInt("post_id");
                    int reaction = rs.getInt("reaction");
                    Map<Integer, Integer> reactionCountMap = getPostReactionsCount(id);
                    // System.out.println(reactionCountMap.size());
                    messages.add(new Message(id, user_id, username, content, date, reaction, reactionCountMap));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            // Handle exceptions
        }
        return messages;
    }

    public List<Message> getExplorePage(int userId) {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT p.content, p.id as post_id, u.id as user_id, u.username, p.posted_at, r.reaction FROM posts p JOIN users u ON p.user_id = u.id LEFT JOIN reactions r ON r.post_id = p.id AND r.author_id = ?;";

        try (Connection conn = DriverManager.getConnection(jdbcURL, jdbcUsername, jdbcPassword);
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId); // Set the user_id if filtering by user

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String content = rs.getString("content");
                    String username = rs.getString("username");
                    String date = rs.getString("posted_at");
                    int user_id = rs.getInt("user_id");
                    int id = rs.getInt("post_id");
                    int reaction = rs.getInt("reaction");
                    Map<Integer, Integer> reactionCountMap = getPostReactionsCount(id);
                    messages.add(new Message(id, user_id, username, content, date, reaction, reactionCountMap));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            // Handle exceptions
        }
        return messages;
    }

    public List<Comment> retrieveComments(int post_id) {
        List<Comment> comments = new ArrayList<>();
        String sql = "SELECT c.comment_id, c.author_id, u.username, c.content, c.posted_at FROM comments c, users u where u.id = c.author_id and c.post_id = ? ;";

        try (Connection conn = DriverManager.getConnection(jdbcURL, jdbcUsername, jdbcPassword);
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, post_id); // Set the user_id if filtering by user

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String content = rs.getString("content");
                    String username = rs.getString("username");
                    String date = rs.getString("posted_at");
                    int user_id = rs.getInt("author_id");
                    int comment_id = rs.getInt("comment_id");
                    comments.add(new Comment(comment_id, post_id, user_id, username, content, date));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            // Handle exceptions
        }
        return comments;
    }

    public RefreshFeedResponse getRefreshFeed(int userId, String LastRefreshed) {

        List<Message> messages = new ArrayList<>();
        String sql = "SELECT p.content, p.id, u.id as user_id, u.username, p.posted_at, r.reaction FROM ( SELECT * FROM posts WHERE posted_at > ?) p JOIN users u ON p.user_id = u.id JOIN followers f ON f.follower_id = ? AND f.followed_id = p.user_id LEFT JOIN reactions r ON r.post_id = p.id AND r.author_id = f.follower_id;";
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
                    int user_id = rs.getInt("user_id");
                    int id = rs.getInt("id");
                    int reaction = rs.getInt("reaction");
                    Map<Integer, Integer> reactionCountMap = getPostReactionsCount(id);
                    messages.add(new Message(id, user_id, username, content, date, reaction, reactionCountMap));
                }
            }
            success = true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new RefreshFeedResponse(success, messages);
    }

    public List<Integer> getFollowersList(int userID) {
        String sql = "SELECT follower_id FROM followers where followed_id = ?;";
        List<Integer> followers = new ArrayList<>();
        try (Connection connection = connect();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userID);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                followers.add(resultSet.getInt("follower_id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return followers;
    }

    public Comment postComment(Comment comment) {
        String sql = "INSERT INTO comments (post_id, author_id, content) VALUES (?, ?, ?)";
        try (Connection connection = connect();
                PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, comment.getPost_id());
            statement.setInt(2, comment.getAuthor_id());
            statement.setString(3, comment.getContent());

            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Creating message failed, no rows affected.");
            }
            int postedCommentID = -1;
            String postedAt = "-1";

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    postedCommentID = generatedKeys.getInt(1);
                    // Retrieve posted_at separately
                    String sqlGetPostedAt = "SELECT posted_at FROM comments WHERE comment_id = ?";
                    try (PreparedStatement getPostedAtStatement = connection.prepareStatement(sqlGetPostedAt)) {
                        getPostedAtStatement.setInt(1, postedCommentID);
                        try (ResultSet resultSet = getPostedAtStatement.executeQuery()) {
                            if (resultSet.next()) {
                                postedAt = resultSet.getString("posted_at");
                            }
                        }
                    }
                } else {
                    throw new SQLException("Creating message failed, no ID obtained.");
                }
            }
            comment.setComment_id(postedCommentID);
            comment.setDate(postedAt);
            return comment;
        } catch (SQLException e) {
            e.printStackTrace();
            return comment;
        }
    }

    public PostReactionResponse postReaction(Reaction reaction) {
        boolean oldReactionDeleted = false;
        boolean newReactionAdded = false;
        String deleteSQL = "DELETE FROM reactions WHERE post_id = ? AND author_id = ?";

        try (Connection connection = connect();
                PreparedStatement statement = connection.prepareStatement(deleteSQL)) {

            statement.setInt(1, reaction.getPost_id());
            statement.setInt(2, reaction.getAuthor_id());
            statement.executeUpdate();

            oldReactionDeleted = true;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        String sql = "INSERT INTO reactions (post_id, author_id, reaction) VALUES (?, ?, ?);";
        try (Connection connection = connect();
                PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, reaction.getPost_id());
            statement.setInt(2, reaction.getAuthor_id());
            statement.setInt(3, reaction.getEmojiNumber());

            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Creating message failed, no rows affected.");
            }
            int postedReactionID = -1;
            newReactionAdded = true;
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    postedReactionID = generatedKeys.getInt(1);

                } else {
                    throw new SQLException("Creating message failed, no ID obtained.");
                }
            }
            reaction.setReaction_id(postedReactionID);

            return new PostReactionResponse(oldReactionDeleted, newReactionAdded, reaction);
        } catch (SQLException e) {
            e.printStackTrace();
            return new PostReactionResponse(oldReactionDeleted, newReactionAdded, reaction);
        }
    }

    public RemoveReactionResponse RemoveReaction(int post_id, int user_id) {
        boolean oldReactionDeleted = false;
        String deleteSQL = "DELETE FROM reactions WHERE post_id = ? AND author_id = ?";

        try (Connection connection = connect();
                PreparedStatement statement = connection.prepareStatement(deleteSQL)) {

            statement.setInt(1, post_id);
            statement.setInt(2, user_id);

            int rowsAffected = statement.executeUpdate();
            if (rowsAffected > 0) {
                oldReactionDeleted = true;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new RemoveReactionResponse(oldReactionDeleted, post_id, user_id);
    }

    public Map<Integer, Integer> getPostReactionsCount(int postId) {
        String sql = "SELECT reaction, COUNT(*) as reaction_count FROM reactions WHERE post_id = ? GROUP BY reaction";

        Map<Integer, Integer> reactionCountMap = new HashMap<>();

        try (Connection connection = connect();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, postId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    int reaction = resultSet.getInt("reaction");
                    int reactionCount = resultSet.getInt("reaction_count");
                    reactionCountMap.put(reaction, reactionCount);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace(); // Handle the exception according to your application's needs
        }

        return reactionCountMap;
    }
}
