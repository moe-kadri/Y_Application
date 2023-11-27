// import javafx.application.Application;
// import javafx.geometry.Insets;
// import javafx.scene.Scene;
// import javafx.scene.control.*;
// import javafx.scene.layout.*;
// import javafx.stage.Stage;

// public class SocialMediaApp extends Application {

// @Override
// public void start(Stage primaryStage) {
// BorderPane root = new BorderPane();

// // Top: Search Bar
// HBox searchBox = new HBox();
// TextField searchField = new TextField();
// Button searchButton = new Button("Search");
// searchBox.getChildren().addAll(searchField, searchButton);
// root.setTop(searchBox);

// // Center: Posts
// VBox postsContainer = new VBox(10);
// ScrollPane scrollPane = new ScrollPane(postsContainer);
// scrollPane.setFitToWidth(true);
// root.setCenter(scrollPane);

// // Set up the scene
// Scene scene = new Scene(root, 600, 400);
// primaryStage.setTitle("Social Media App");
// primaryStage.setScene(scene);
// primaryStage.show();

// // Bottom: Post Form
// HBox postBox = new HBox();
// TextField postField = new TextField();
// Button postButton = new Button("Post");
// postBox.getChildren().addAll(postField, postButton);
// root.setBottom(postBox);

// // Event handlers
// searchButton.setOnAction(e -> searchPosts(searchField.getText(),
// postsContainer));
// postButton.setOnAction(e -> addPost(postField.getText(), postsContainer));
// }

// private void searchPosts(String searchTerm, VBox postsContainer) {
// // TODO: Implement search functionality
// // Retrieve and display posts matching the search term
// }

// private void addPost(String postText, VBox postsContainer) {
// // Create a new post node and add it to the posts container
// if (!postText.isEmpty()) {
// Label postLabel = new Label(postText);
// Button likeButton = new Button("Like");
// Button commentButton = new Button("Comment");

// HBox postNode = new HBox(10, postLabel, likeButton, commentButton);
// postNode.setStyle("-fx-border-color: black;");
// postNode.setPadding(new Insets(5));

// postsContainer.getChildren().add(postNode); // Add post

// // Event handlers for like and comment buttons
// likeButton.setOnAction(e -> handleLike(postText));
// commentButton.setOnAction(e -> showCommentDialog(postText));
// }
// }

// private void showCommentDialog(String postText) {
// // Create a dialog for comments
// Dialog<String> dialog = new Dialog<>();
// dialog.setTitle("Comments for: " + postText);
// dialog.setHeaderText(null);

// // Create a ScrollPane to hold comments
// ScrollPane scrollPane = new ScrollPane();
// VBox commentContainer = new VBox(10);

// // Set the maximum height for the commentContainer
// commentContainer.setMaxHeight(300); // Adjust the value based on your
// preference

// // Create a TextField and a button to add a comment
// TextField commentField = new TextField();
// Button addCommentButton = new Button("Add Comment");

// addCommentButton.setOnAction(e -> {
// if (!commentField.getText().isEmpty()) {
// // Create a separate container for each comment
// VBox singleCommentContainer = createCommentContainer("Adam", "2020",
// commentField.getText());
// commentContainer.getChildren().add(singleCommentContainer);
// commentField.clear();
// }
// });

// commentContainer.getChildren().addAll(commentField, addCommentButton);
// scrollPane.setContent(commentContainer);

// // Set the preferred size for the ScrollPane
// scrollPane.setPrefSize(400, 200); // Adjust the values based on your
// preference

// // Set the button types
// dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK);

// // Set the content of the dialog
// VBox dialogContent = new VBox();
// dialogContent.getChildren().addAll(scrollPane);
// dialog.getDialogPane().setContent(dialogContent);

// // Show the dialog
// dialog.showAndWait();
// }

// // Create a container for each comment with author and date
// private VBox createCommentContainer(String author, String date, String
// commentText) {
// VBox commentBox = new VBox(5);

// // Author label
// Label authorLabel = new Label("Author: " + author);
// authorLabel.setStyle("-fx-font-size: 10;"); // Small font

// // Comment text
// Label commentLabel = new Label(commentText);

// // Date label
// Label dateLabel = new Label("Date: " + date);
// dateLabel.setStyle("-fx-font-size: 10;"); // Small font

// commentBox.getChildren().addAll(authorLabel, commentLabel, dateLabel);
// commentBox.setStyle("-fx-border-color: black;");
// commentBox.setPadding(new Insets(5));

// return commentBox;
// }

// private void handleLike(String postText) {
// // TODO: Implement like functionality
// System.out.println("Liked: " + postText);
// }

// public static void main(String[] args) {
// launch(args);
// }
// }
