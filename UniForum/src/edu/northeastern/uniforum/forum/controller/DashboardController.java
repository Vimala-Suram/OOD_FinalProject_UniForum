package edu.northeastern.uniforum.forum.controller;

import edu.northeastern.uniforum.forum.model.User;
import edu.northeastern.uniforum.forum.util.SceneManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

public class DashboardController {

    @FXML private Label usernameLabel;
    @FXML private Label userHandleLabel;
    @FXML private Circle profileCircle;
    @FXML private VBox mainContentArea;

    private User currentUser;

    public void initData(User user) {
        this.currentUser = user;

        if (usernameLabel != null) {
            usernameLabel.setText(user.getUsername());
        }
        if (userHandleLabel != null) {
            userHandleLabel.setText("@" + user.getUsername().toLowerCase());
        }

        loadForumView();
    }

    private void loadForumView() {
        try {
            mainContentArea.getChildren().clear();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/edu/northeastern/uniforum/forum/view/forum.fxml"));
            BorderPane forum = (BorderPane) loader.load();

            ForumController controller = loader.getController();
            if (controller != null && currentUser != null) {
                controller.initData(currentUser);
            }

            forum.prefWidthProperty().bind(mainContentArea.widthProperty());
            forum.prefHeightProperty().bind(mainContentArea.heightProperty());
            VBox.setVgrow(forum, Priority.ALWAYS);

            mainContentArea.getChildren().add(forum);

        } catch (Exception e) {
            System.err.println("Error loading forum: " + e.getMessage());
            e.printStackTrace();

            Label error = new Label("Error: " + e.getMessage());
            error.setStyle("-fx-text-fill: red; -fx-font-size: 16px; -fx-padding: 20;");
            mainContentArea.getChildren().add(error);
        }
    }

    @FXML private void handleHomeAction() { loadForumView(); }
    @FXML private void handleProfileAction() { System.out.println("Profile"); }
    @FXML private void handleGroupsAction() { System.out.println("Groups"); }
    @FXML private void handleTrendingAction() { System.out.println("Trending"); }
    @FXML private void handleSavedAction() { System.out.println("Saved"); }
    @FXML private void handleSettingsAction() { SceneManager.switchToSettings(currentUser); }
    @FXML private void handleLogoutAction() { SceneManager.switchToLogin(); }
    @FXML private void handleNotificationsAction() { System.out.println("Notifications"); }
}
