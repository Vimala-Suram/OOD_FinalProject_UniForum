package edu.northeastern.uniforum.forum.controller;

import edu.northeastern.uniforum.forum.dao.PostDAO;
import edu.northeastern.uniforum.forum.model.User;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CreatePostController {

    @FXML
    private ComboBox<PostDAO.CommunityDTO> communityComboBox;

    @FXML
    private TextField titleField;

    @FXML
    private Label titleCharCount;

    @FXML
    private Label validationMessage;

    @FXML
    private TextArea bodyTextArea;

    @FXML
    private Button closeButton;

    @FXML
    private ComboBox<String> tagComboBox;

    @FXML
    private HBox tagsContainer;

    private ForumController parentController;
    private User currentUser;

    private final PostDAO postDAO = new PostDAO();

    private List<String> tags = new ArrayList<>();

    public void setParentController(ForumController parentController) {
        this.parentController = parentController;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

	// Initialize form components and load data
	@FXML
	private void initialize() {
        showValidationMessage(null);

        titleField.textProperty().addListener((observable, oldValue, newValue) -> {
            int length = newValue != null ? newValue.length() : 0;
            titleCharCount.setText(length + "/300");

            if (length > 280) {
                titleCharCount.setStyle("-fx-text-fill: #ff585b; -fx-font-size: 12;");
            } else {
                titleCharCount.setStyle("-fx-text-fill: #555555; -fx-font-size: 12;");
            }
        });

        titleField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && newValue.length() > 300) {
                titleField.setText(oldValue);
            }
        });

        loadCommunities();

        loadTags();

        applyDarkThemeToComboBoxes();
    }

    private void applyDarkThemeToComboBoxes() {
        if (communityComboBox != null) {
            communityComboBox.setCellFactory(listView -> {
                return new javafx.scene.control.ListCell<PostDAO.CommunityDTO>() {
                    @Override
                    protected void updateItem(PostDAO.CommunityDTO item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                            setStyle("-fx-background-color: #2A2A2A;");
                        } else {
                            setText(item.name);
                            setStyle("-fx-background-color: #2A2A2A; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 8;");
                        }

                        setOnMouseEntered(e -> {
                            if (item != null && !isEmpty()) {
                                setStyle("-fx-background-color: #FF4500; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 8;");
                            }
                        });
                        setOnMouseExited(e -> {
                            if (item != null && !isEmpty()) {
                                setStyle("-fx-background-color: #2A2A2A; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 8;");
                            }
                        });
                    }
                };
            });

            communityComboBox.setButtonCell(new javafx.scene.control.ListCell<PostDAO.CommunityDTO>() {
                @Override
                protected void updateItem(PostDAO.CommunityDTO item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText("Choose a community");
                        setStyle("-fx-text-fill: #818384;");
                    } else {
                        setText(item.name);
                        setStyle("-fx-text-fill: white;");
                    }
                }
            });
        }

        if (tagComboBox != null) {
            tagComboBox.setCellFactory(listView -> {
                return new javafx.scene.control.ListCell<String>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                            setStyle("-fx-background-color: #2A2A2A;");
                        } else {
                            setText(item);
                            setStyle("-fx-background-color: #2A2A2A; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 8;");
                        }

                        setOnMouseEntered(e -> {
                            if (item != null && !isEmpty()) {
                                setStyle("-fx-background-color: #FF4500; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 8;");
                            }
                        });
                        setOnMouseExited(e -> {
                            if (item != null && !isEmpty()) {
                                setStyle("-fx-background-color: #2A2A2A; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 8;");
                            }
                        });
                    }
                };
            });

            tagComboBox.setButtonCell(new javafx.scene.control.ListCell<String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText("Select a tag");
                        setStyle("-fx-text-fill: #818384;");
                    } else {
                        setText(item);
                        setStyle("-fx-text-fill: white;");
                    }
                }
            });
        }
    }

	// Load available communities from database
	private void loadCommunities() {
        try {
            List<PostDAO.CommunityDTO> communities = postDAO.getAllCommunities();
            communityComboBox.getItems().setAll(communities);
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Failed to load communities: " + e.getMessage());
        }
    }

    @FXML
    private void onDraftsClicked() {
        System.out.println("Drafts clicked");
    }

	// Load available tags for post categorization
	private void loadTags() {
        tagComboBox.getItems().addAll("FAQs", "Installation Issues", "Project ideas");
    }

    @FXML
	// Handle tag selection and add to post
	private void onTagSelected() {
        String selectedTag = tagComboBox.getSelectionModel().getSelectedItem();
        if (selectedTag != null && !selectedTag.isEmpty() && !tags.contains(selectedTag)) {
            tags.add(selectedTag);
            tagComboBox.getSelectionModel().clearSelection();
            updateTagsDisplay();
        }
    }

	// Update visual display of selected tags
	private void updateTagsDisplay() {
        tagsContainer.getChildren().clear();

        for (String tag : tags) {
            HBox tagBox = new HBox(6);
            tagBox.setAlignment(Pos.CENTER_LEFT);
            tagBox.setStyle("-fx-background-color: #7678ED; -fx-background-radius: 12; -fx-padding: 4 8;");

            Label tagLabel = new Label(tag);
            tagLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 12;");

            Button removeBtn = new Button("×");
            removeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #ffffff; -fx-font-size: 14; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 0 0 0 4;");
            removeBtn.setOnAction(e -> removeTag(tag));

            tagBox.getChildren().addAll(tagLabel, removeBtn);
            tagsContainer.getChildren().add(tagBox);
        }
    }

    private void removeTag(String tag) {
        tags.remove(tag);
        updateTagsDisplay();
    }

    @FXML
    private void onBoldClicked() {
        wrapSelectedText("**", "**");
    }

    @FXML
    private void onItalicClicked() {
        wrapSelectedText("*", "*");
    }

    @FXML
    private void onLinkClicked() {
        String selectedText = bodyTextArea.getSelectedText();
        if (!selectedText.isEmpty()) {
            int start = bodyTextArea.getSelection().getStart();
            int end = bodyTextArea.getSelection().getEnd();
            String currentText = bodyTextArea.getText();
            String newText = currentText.substring(0, start) + "[" + selectedText + "](url)" + currentText.substring(end);
            bodyTextArea.setText(newText);
            bodyTextArea.selectRange(start + selectedText.length() + 3, start + selectedText.length() + 6);
        } else {
            insertText("[link text](url)");
            int pos = bodyTextArea.getCaretPosition();
            bodyTextArea.selectRange(pos - 4, pos - 1);
        }
    }

    @FXML
    private void onSaveDraftClicked() {
        System.out.println("Save Draft clicked");
    }

    @FXML
    private void onPostClicked() {
        PostDAO.CommunityDTO selectedCommunity =
                communityComboBox.getSelectionModel().getSelectedItem();
        String title = titleField.getText() != null ? titleField.getText().trim() : "";
        String body  = bodyTextArea.getText() != null ? bodyTextArea.getText().trim() : "";

        if (selectedCommunity == null) {
            showValidationMessage("Please select a community.");
            return;
        }
        if (title.isEmpty()) {
            showValidationMessage("Title is required.");
            return;
        }
        if (tags.isEmpty()) {
            showValidationMessage("Please select at least one tag.");
            return;
        }
        String tag = tags.get(0);
        showValidationMessage(null);

        try {
            if (currentUser == null) {
                showValidationMessage("User not logged in. Please log in again.");
                return;
            }

            int userId = currentUser.getUserId();
            postDAO.createPost(selectedCommunity.id, userId, title, body, tag);
            System.out.println("Post created successfully.");
            showValidationMessage(null);

            if (parentController != null) {
                parentController.loadPostsFromDB();
            }

            onCloseClicked();

        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Error creating post: " + e.getMessage());
        }
    }

    private void showValidationMessage(String message) {
        if (validationMessage == null) {
            return;
        }
        boolean hasMessage = message != null && !message.isBlank();
        validationMessage.setText(hasMessage ? message : "");
        validationMessage.setVisible(hasMessage);
        validationMessage.setManaged(hasMessage);
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

    private void insertText(String text) {
        int caretPosition = bodyTextArea.getCaretPosition();
        String currentText = bodyTextArea.getText();
        String newText = currentText.substring(0, caretPosition) + text + currentText.substring(caretPosition);
        bodyTextArea.setText(newText);
        bodyTextArea.positionCaret(caretPosition + text.length());
    }

    private void wrapSelectedText(String beforeMarker, String afterMarker) {
        String selectedText = bodyTextArea.getSelectedText();
        int start = bodyTextArea.getSelection().getStart();
        int end = bodyTextArea.getSelection().getEnd();

        String currentText = bodyTextArea.getText();

        if (!selectedText.isEmpty()) {
            String newText = currentText.substring(0, start) + beforeMarker + selectedText + afterMarker + currentText.substring(end);
            bodyTextArea.setText(newText);
            bodyTextArea.selectRange(start, start + beforeMarker.length() + selectedText.length() + afterMarker.length());
        } else {
            String newText = currentText.substring(0, start) + beforeMarker + afterMarker + currentText.substring(start);
            bodyTextArea.setText(newText);
            bodyTextArea.positionCaret(start + beforeMarker.length());
        }
    }
}
