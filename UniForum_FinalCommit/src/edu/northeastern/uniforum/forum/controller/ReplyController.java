package edu.northeastern.uniforum.forum.controller;

import edu.northeastern.uniforum.forum.dao.PostDAO;
import edu.northeastern.uniforum.forum.dao.ReplyDAO;
import edu.northeastern.uniforum.forum.dao.UserDAO;
import edu.northeastern.uniforum.forum.model.User;
import edu.northeastern.uniforum.forum.util.SceneManager;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.net.URI;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReplyController {

    @FXML
    private Button closeButton;

    @FXML
    private VBox postContentContainer;

    @FXML
    private VBox repliesContainer;

    @FXML
    private TextArea replyTextArea;

    @FXML
    private Button addCommentButton;

    @FXML
    private VBox replyEditorSection;

    private ForumController parentController;
    private PostDAO.PostDTO postData;
    private int postId;
    private User currentUser;
    private final ReplyDAO replyDAO = new ReplyDAO();

    public void setParentController(ForumController parentController) {
        this.parentController = parentController;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public void setPostData(PostDAO.PostDTO postData) {
        this.postData = postData;
        this.postId = postData.postId;
        displayPost();
        loadReplies();
    }

    private void displayPost() {
        postContentContainer.getChildren().clear();

        HBox metaRow = new HBox(8);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        Label communityLabel = new Label(postData.community);
        communityLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12; -fx-font-weight: bold;");

        HBox authorTimeBox = new HBox(4);
        authorTimeBox.setAlignment(Pos.CENTER_LEFT);

        Label postedByLabel = new Label("Posted by ");
        postedByLabel.setStyle("-fx-text-fill: #818384; -fx-font-size: 12;");

        Label authorNameLabel = new Label(postData.author);
        authorNameLabel.setStyle("-fx-text-fill: #0079D3; -fx-font-size: 12; -fx-cursor: hand;");
        authorNameLabel.setOnMouseClicked(e -> navigateToUserSettings(postData.author));
        authorNameLabel.setOnMouseEntered(e -> authorNameLabel.setStyle("-fx-text-fill: #7193FF; -fx-font-size: 12; -fx-cursor: hand;"));
        authorNameLabel.setOnMouseExited(e -> authorNameLabel.setStyle("-fx-text-fill: #0079D3; -fx-font-size: 12; -fx-cursor: hand;"));

        Label timeLabel = new Label(" • " + postData.timeAgo);
        timeLabel.setStyle("-fx-text-fill: #818384; -fx-font-size: 12;");

        authorTimeBox.getChildren().addAll(postedByLabel, authorNameLabel, timeLabel);

        if (postData.tag != null && !postData.tag.trim().isEmpty()) {
            HBox tagBox = new HBox(6);
            tagBox.setAlignment(Pos.CENTER_LEFT);
            tagBox.setStyle("-fx-background-color: #FF4500; -fx-background-radius: 12; -fx-padding: 4 8;");

            Label tagLabel = new Label(postData.tag);
            tagLabel.setStyle("-fx-text-fill: white; -fx-font-size: 11; -fx-font-weight: 600;");

            tagBox.getChildren().add(tagLabel);
            metaRow.getChildren().addAll(communityLabel, authorTimeBox, tagBox);
        } else {
            metaRow.getChildren().addAll(communityLabel, authorTimeBox);
        }

        Label titleLabel = new Label(postData.title);
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18; -fx-font-weight: bold; -fx-wrap-text: true;");
        titleLabel.setWrapText(true);

        TextFlow contentFlow = createTextFlowWithLinks(postData.content);

        postContentContainer.getChildren().addAll(metaRow, titleLabel, contentFlow);
    }

    private void loadReplies() {
        repliesContainer.getChildren().clear();

        try {
            var replies = replyDAO.getRepliesByPostId(postId);

            if (replies.isEmpty()) {
                Label noRepliesLabel = new Label("No comments yet. Be the first to comment!");
                noRepliesLabel.setStyle("-fx-text-fill: #555555; -fx-font-size: 14; -fx-padding: 16;");
                repliesContainer.getChildren().add(noRepliesLabel);
            } else {
                for (ReplyDAO.ReplyDTO reply : replies) {
                    createReplyCard(reply);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Error loading replies: " + e.getMessage());
        }
    }

    private void createReplyCard(ReplyDAO.ReplyDTO reply) {
        HBox replyCard = new HBox(8);
        replyCard.setStyle("-fx-background-color: #272729; -fx-background-radius: 4; -fx-padding: 12; -fx-border-color: #343536; -fx-border-radius: 4;");

        VBox voteBox = new VBox(4);
        voteBox.setAlignment(Pos.TOP_CENTER);
        voteBox.setPrefWidth(40);

        VBox contentBox = new VBox(4);
        contentBox.setPadding(new Insets(0, 0, 0, 0));

        HBox authorTimeBox = new HBox(4);
        authorTimeBox.setAlignment(Pos.CENTER_LEFT);

        Label authorNameLabel = new Label(reply.author);
        authorNameLabel.setStyle("-fx-text-fill: #0079D3; -fx-font-size: 11; -fx-cursor: hand;");
        authorNameLabel.setOnMouseClicked(e -> navigateToUserSettings(reply.author));
        authorNameLabel.setOnMouseEntered(e -> authorNameLabel.setStyle("-fx-text-fill: #7193FF; -fx-font-size: 11; -fx-cursor: hand;"));
        authorNameLabel.setOnMouseExited(e -> authorNameLabel.setStyle("-fx-text-fill: #0079D3; -fx-font-size: 11; -fx-cursor: hand;"));

        Label timeLabel = new Label(" • " + reply.timeAgo);
        timeLabel.setStyle("-fx-text-fill: #818384; -fx-font-size: 11;");

        authorTimeBox.getChildren().addAll(authorNameLabel, timeLabel);

        TextFlow contentFlow = createTextFlowWithLinks(reply.content);

        contentBox.getChildren().addAll(authorTimeBox, contentFlow);
        replyCard.getChildren().addAll(voteBox, contentBox);
        repliesContainer.getChildren().add(replyCard);
    }

    private TextFlow createTextFlowWithLinks(String text) {
        TextFlow textFlow = new TextFlow();
        textFlow.setStyle("-fx-text-fill: #D7DADC; -fx-font-size: 14;");
        textFlow.setLineSpacing(2.0);

        if (text == null || text.isEmpty()) {
            return textFlow;
        }

        Pattern urlPattern = Pattern.compile(
            "(?i)\\b((?:https?://)" +
            "[\\w\\-]+\\.[a-z]{2,}(?:\\.[a-z]{2,})?(?:/[\\w\\-.,@?^=%&:/~+#]*)?)",
            Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = urlPattern.matcher(text);
        int lastEnd = 0;

        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                String beforeText = text.substring(lastEnd, matcher.start());
                Text beforeTextNode = new Text(beforeText);
                beforeTextNode.setStyle("-fx-fill: #D7DADC; -fx-font-size: 14;");
                textFlow.getChildren().add(beforeTextNode);
            }

            String url = matcher.group(1);
            final String fullUrl = (!url.toLowerCase().startsWith("http://") && !url.toLowerCase().startsWith("https://"))
                ? "https://" + url
                : url;

            Hyperlink link = new Hyperlink(url);
            link.setStyle("-fx-text-fill: #0079D3; -fx-font-size: 14; -fx-underline: true;");
            link.setOnAction(e -> {
                try {
                    Desktop desktop = Desktop.getDesktop();
                    if (desktop.isSupported(Desktop.Action.BROWSE)) {
                        desktop.browse(new URI(fullUrl));
                    }
                } catch (Exception ex) {
                    System.out.println("Error opening URL: " + ex.getMessage());
                }
            });
            textFlow.getChildren().add(link);

            lastEnd = matcher.end();
        }

        if (lastEnd < text.length()) {
            String afterText = text.substring(lastEnd);
            Text afterTextNode = new Text(afterText);
            afterTextNode.setStyle("-fx-fill: #D7DADC; -fx-font-size: 14;");
            textFlow.getChildren().add(afterTextNode);
        }

        if (textFlow.getChildren().isEmpty()) {
            Text textNode = new Text(text);
            textNode.setStyle("-fx-fill: #D7DADC; -fx-font-size: 14;");
            textFlow.getChildren().add(textNode);
        }

        return textFlow;
    }

    @FXML
    private void onPostReplyClicked() {
        String replyText = replyTextArea.getText() != null ? replyTextArea.getText().trim() : "";

        if (replyText.isEmpty()) {
            System.out.println("Reply cannot be empty.");
            return;
        }

        try {
            if (currentUser == null) {
                System.out.println("User not logged in. Please log in again.");
                return;
            }

            int userId = currentUser.getUserId();
            replyDAO.createReply(postId, userId, replyText);
            System.out.println("Reply posted successfully.");

            replyTextArea.clear();

            replyEditorSection.setVisible(false);
            replyEditorSection.setManaged(false);

            loadReplies();

            if (parentController != null) {
                parentController.loadPostsFromDB();
            }

        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Error posting reply: " + e.getMessage());
        }
    }

    @FXML
    private void onAddCommentClicked() {
        boolean isVisible = replyEditorSection.isVisible();
        replyEditorSection.setVisible(!isVisible);
        replyEditorSection.setManaged(!isVisible);

        if (!isVisible && replyTextArea != null) {
            replyTextArea.requestFocus();
        }
    }

    @FXML
    private void onCancelClicked() {
        replyTextArea.clear();
        replyEditorSection.setVisible(false);
        replyEditorSection.setManaged(false);
    }

    @FXML
    private void onCloseClicked() {
        if (parentController != null) {
            parentController.closeModal();
        } else {
            Stage stage = (Stage) closeButton.getScene().getWindow();
            if (stage != null) {
                stage.close();
            }
        }
    }

    @FXML
    private void onCloseButtonHover() {
        closeButton.setStyle("-fx-background-color: #F5F5F5; -fx-text-fill: #3D348B; -fx-font-size: 20; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 4 8; -fx-min-width: 32; -fx-min-height: 32;");
    }

    @FXML
    private void onCloseButtonExit() {
        closeButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #555555; -fx-font-size: 20; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 4 8; -fx-min-width: 32; -fx-min-height: 32;");
    }

    private void navigateToUserSettings(String username) {
        UserDAO userDAO = new UserDAO();
        User user = userDAO.getUserByUsername(username);
        if (user != null && currentUser != null) {
            SceneManager.switchToSettings(currentUser, user);
        } else {
            System.out.println("User not found: " + username);
        }
    }
}
