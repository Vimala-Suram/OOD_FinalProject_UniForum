package edu.northeastern.uniforum.forum.controller;

import edu.northeastern.uniforum.forum.dao.UserDAO;
import edu.northeastern.uniforum.forum.model.User;
import edu.northeastern.uniforum.forum.service.EmailService;
import edu.northeastern.uniforum.forum.service.EmailServiceFactory;
import edu.northeastern.uniforum.forum.service.OtpService;
import edu.northeastern.uniforum.forum.util.PasswordUtil;
import edu.northeastern.uniforum.forum.util.SceneManager;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class ForgotPasswordController {

    @FXML private TextField emailField;
    @FXML private TextField otpField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private VBox otpStepContainer;
    @FXML private VBox resetStepContainer;
    @FXML private Label statusLabel;

    private final UserDAO userDAO = new UserDAO();
    private final EmailService emailService = EmailServiceFactory.getInstance();
    private String currentEmail;
    private boolean otpVerified = false;

    @FXML
    public void initialize() {
        setStepVisibility(false, false);
        statusLabel.setText("");
    }

    @FXML
    private void handleBackAction() {
        SceneManager.switchToLogin();
    }

    @FXML
    private void handleSendOtp() {
        String email = emailField.getText() != null ? emailField.getText().trim() : "";
        if (!isValidEmail(email)) {
            showStatus("Please enter a valid email address.", false);
            return;
        }

        User user = userDAO.getUserByEmail(email);
        if (user == null) {
            showStatus("No account is registered with this email address.", false);
            return;
       }
        try {
            String otp = OtpService.generateOtp(email);
            emailService.sendOtpEmail(email, otp);
            currentEmail = email;
            otpVerified = false;
            setStepVisibility(true, false);
            emailField.setDisable(true);
            showStatus("OTP sent! Please check your email. (Valid for 10 minutes)", true);
        } catch (Exception ex) {
            showStatus("Unable to send OTP. Please try again later.", false);
            ex.printStackTrace();
        }
    }

    @FXML
    private void handleVerifyOtp() {
        if (currentEmail == null) {
            showStatus("Please request an OTP first.", false);
            return;
        }

        String otp = otpField.getText() != null ? otpField.getText().trim() : "";
        if (!otp.matches("\\d{6}")) {
            showStatus("Enter the 6-digit OTP that was emailed to you.", false);
            return;
        }

        if (OtpService.verifyOtp(currentEmail, otp)) {
            otpVerified = true;
            setStepVisibility(true, true);
            showStatus("OTP verified! You can now set a new password.", true);
        } else {
            showStatus("Invalid or expired OTP. Please try again.", false);
        }
    }

    @FXML
    private void handleResendOtp() {
        emailField.setDisable(false);
        handleSendOtp();
    }

    @FXML
    private void handleResetPassword() {
        if (currentEmail == null || !otpVerified) {
            showStatus("Please verify the OTP before resetting your password.", false);
            return;
        }

        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (newPassword == null || newPassword.length() < 6) {
            showStatus("Password must be at least 6 characters long.", false);
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            showStatus("Passwords do not match.", false);
            return;
        }

        String hashed = PasswordUtil.hashPassword(newPassword);
        boolean updated = userDAO.updatePasswordByEmail(currentEmail, hashed);
        if (updated) {
            showStatus("Password updated successfully! Redirecting to login...", true);
            OtpService.invalidateOtp(currentEmail);
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Password Reset");
            alert.setHeaderText(null);
            alert.setContentText("Your password has been updated. You can now log in with the new password.");
            alert.show();

            PauseTransition delay = new PauseTransition(Duration.seconds(2.5));
            delay.setOnFinished(e -> SceneManager.switchToLogin());
            delay.play();
        } else {
            showStatus("Something went wrong while updating the password. Please try again.", false);
        }
    }

    private void setStepVisibility(boolean showOtpStep, boolean showResetStep) {
        if (otpStepContainer != null) {
            otpStepContainer.setVisible(showOtpStep);
            otpStepContainer.setManaged(showOtpStep);
        }
        if (resetStepContainer != null) {
            resetStepContainer.setVisible(showResetStep);
            resetStepContainer.setManaged(showResetStep);
        }
    }

    private void showStatus(String message, boolean success) {
        if (statusLabel != null) {
            statusLabel.setText(message);
            statusLabel.setStyle(success
                    ? "-fx-text-fill: #2E8B57; -fx-font-weight: bold;"
                    : "-fx-text-fill: #D7263D; -fx-font-weight: bold;");
        }
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }
}
