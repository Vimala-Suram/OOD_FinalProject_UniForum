package edu.northeastern.uniforum.forum.util;
import java.io.IOException;

import application.Main;
import edu.northeastern.uniforum.forum.controller.DashboardController;
import edu.northeastern.uniforum.forum.controller.ForumController;
import edu.northeastern.uniforum.forum.controller.SettingsController;
import edu.northeastern.uniforum.forum.model.User;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class SceneManager {

	// Load FXML scene with specified dimensions and center on screen
	public static void loadScene(String fxmlPath, Stage stage, double width, double height) {
		try {
			FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(fxmlPath));
			Parent root = loader.load();
			Scene scene = new Scene(root, width, height);
			stage.setScene(scene);
			stage.setResizable(true);
			stage.centerOnScreen();
			stage.show();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Navigate to login screen
	public static void switchToLogin() {
		loadScene("/edu/northeastern/uniforum/forum/view/LoginView.fxml", Main.getPrimaryStage(), 1400, 800);
	}

	// Navigate to user registration screen
	public static void switchToRegistration() {
		loadScene("/edu/northeastern/uniforum/forum/view/RegistrationView.fxml", Main.getPrimaryStage(), 1400, 800);
	}

	public static void switchToForgotPassword() {
		loadScene("/edu/northeastern/uniforum/forum/view/ForgotPasswordView.fxml", Main.getPrimaryStage(), 1400, 800);
	}

	public static void switchToForum(User user) {
		try {
			FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource("/edu/northeastern/uniforum/forum/view/forum.fxml"));
			Parent root = loader.load();
			Scene scene = new Scene(root, 1400, 800);
			Main.getPrimaryStage().setScene(scene);
			Main.getPrimaryStage().setResizable(true);
			Main.getPrimaryStage().centerOnScreen();
			Main.getPrimaryStage().show();

			ForumController controller = loader.getController();
			if (controller != null) {
				controller.initData(user);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void switchToSettings(User user) {
		switchToSettings(user, user);
	}

	public static void switchToSettings(User loggedInUser, User viewedUser) {
		try {
			FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource("/edu/northeastern/uniforum/forum/view/SettingsView.fxml"));
			Parent root = loader.load();
			Scene scene = new Scene(root, 1400, 800);
			Main.getPrimaryStage().setScene(scene);
			Main.getPrimaryStage().setResizable(true);
			Main.getPrimaryStage().centerOnScreen();
			Main.getPrimaryStage().show();

			SettingsController controller = loader.getController();
			if (controller != null) {
				controller.setLoggedInUser(loggedInUser);
				controller.initData(viewedUser);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void switchToCourseSelection(User user) {
		try {
			FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource("/edu/northeastern/uniforum/forum/view/CourseSelection.fxml"));
			Parent root = loader.load();
			Scene scene = new Scene(root, 1400, 800);
			Main.getPrimaryStage().setScene(scene);
			Main.getPrimaryStage().setResizable(true);
			Main.getPrimaryStage().centerOnScreen();
			Main.getPrimaryStage().show();

			edu.northeastern.uniforum.forum.controller.CourseSelectionController controller = loader.getController();
			if (controller != null) {
				controller.setCurrentUser(user);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
