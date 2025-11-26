package edu.northeastern.uniforum.forum.controller;

import edu.northeastern.uniforum.forum.dao.UserDAO;
import edu.northeastern.uniforum.forum.model.User;
import edu.northeastern.uniforum.forum.util.PasswordUtil;
import edu.northeastern.uniforum.forum.util.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;

    private final UserDAO userDAO = new UserDAO();

	// Authenticate user and redirect to appropriate screen
	@FXML
	public void handleLoginButtonAction() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Please enter username and password.");
            return;
        }

        User user = userDAO.getUserByUsername(username);

        if (user != null && PasswordUtil.checkPassword(password, user.getPasswordHash())) {
            messageLabel.setText("Login Successful! Welcome, " + user.getUsername());

            if (!userDAO.hasUserJoinedCommunities(user.getUserId())) {
                SceneManager.switchToCourseSelection(user);
            } else {
                SceneManager.switchToForum(user);
            }
        } else {
            messageLabel.setText("Invalid username or password.");
        }
    }

	// Navigate to registration screen
	@FXML
	public void handleRegisterLinkAction() {
        SceneManager.switchToRegistration();
    }

	// Navigate to forgot password screen
	@FXML
	public void handleForgotPasswordAction() {
        SceneManager.switchToForgotPassword();
    }
}
