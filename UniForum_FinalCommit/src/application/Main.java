package application;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
	private static Stage primaryStage;

    @Override
    public void start(Stage stage) {
        try {
        	primaryStage = stage;

            Parent root = FXMLLoader.load(
                getClass().getResource("/edu/northeastern/uniforum/forum/view/LoginView.fxml")
            );

            if (root == null) {
                throw new IllegalStateException("LoginView.fxml not found on classpath");
            }

            Scene scene = new Scene(root, 1400, 800);
            scene.getStylesheets().add(
                getClass().getResource("reddit-dark-theme.css").toExternalForm()
            );
            primaryStage.setScene(scene);
            primaryStage.setTitle("UniForum");
            primaryStage.setResizable(true);
            primaryStage.centerOnScreen();
            primaryStage.show();

        } catch(Exception e) {
            e.printStackTrace();
        }
    }

	public static Stage getPrimaryStage() {
		return primaryStage;
	}

    public static void main(String[] args) {
        launch(args);
    }
}
