package edu.northeastern.uniforum.forum.controller;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import edu.northeastern.uniforum.forum.dao.PostDAO;
import edu.northeastern.uniforum.forum.dao.UserDAO;
import edu.northeastern.uniforum.forum.model.Reply;
import edu.northeastern.uniforum.forum.model.User;
import edu.northeastern.uniforum.forum.util.SceneManager;
import edu.northeastern.uniforum.forum.util.TimeUtil;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import javafx.scene.layout.Priority;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.Group;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.scene.paint.Color;
import javafx.scene.input.MouseEvent;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.util.Duration;

public class ForumController {

	@FXML
	private TextField searchField;

	@FXML
	private Button askButton;

	@FXML
	private Button createPostButton;

	@FXML
	private Label usernameLabel;

	@FXML
	private Button homeNavButton;

	@FXML
	private VBox postContainer;

	@FXML
	private VBox recentPostsContainer;

	@FXML
	private StackPane modalOverlay;

	@FXML
	private VBox dialogContainer;

	@FXML
	private Region overlayBackground;

	@FXML
	private HBox filterStrip;

	@FXML
	private ComboBox<String> communityFilter;

	@FXML
	private ComboBox<String> tagFilter;

	@FXML
	private ComboBox<String> sortByFilter;

	private List<PostDAO.PostDTO> cachedPosts = new ArrayList<>();
	private PauseTransition searchDebounce;
	private User currentUser;
	private final PostDAO postDAO = new PostDAO();
	private boolean isExploreView = false;

	// Initialize forum UI components and load data
	@FXML
	private void initialize() {
		setupNavButtonHovers();

		setupSearchInteractions();

		setupFilterControls();

		loadPostsFromDB();
	}

	// Configure dropdown filters for communities, tags, and sorting
	private void setupFilterControls() {
		try {
			List<PostDAO.CommunityDTO> communities = postDAO.getAllCommunities();
			if (communityFilter != null) {
				communityFilter.getItems().clear();
				communityFilter.getItems().add("All Communities");
				for (PostDAO.CommunityDTO comm : communities) {
					communityFilter.getItems().add(comm.name);
				}
				communityFilter.setOnAction(e -> applyFilters());
			}
		} catch (SQLException e) {
			System.err.println("Error loading communities for filter: " + e.getMessage());
		}

		try {
			List<String> tags = postDAO.getAllTags();
			if (tagFilter != null) {
				tagFilter.getItems().clear();
				tagFilter.getItems().add("All Tags");
				tagFilter.getItems().addAll(tags);
				tagFilter.setOnAction(e -> applyFilters());
			}
		} catch (SQLException e) {
			System.err.println("Error loading tags for filter: " + e.getMessage());
		}

		if (sortByFilter != null) {
			sortByFilter.getItems().addAll("Most Liked", "Least Liked", "Latest", "Oldest");
			sortByFilter.setValue("Most Liked");
			sortByFilter.setOnAction(e -> applyFilters());
		}

		applyDarkThemeToComboBox(communityFilter);
		applyDarkThemeToComboBox(tagFilter);
		applyDarkThemeToComboBox(sortByFilter);
	}

	// Apply dark theme styling to dropdown components
	private void applyDarkThemeToComboBox(ComboBox<String> comboBox) {
		if (comboBox == null)
			return;

		comboBox.setCellFactory(listView -> {
			return new javafx.scene.control.ListCell<String>() {
				@Override
				protected void updateItem(String item, boolean empty) {
					super.updateItem(item, empty);
					if (empty || item == null) {
						setText(null);
						setStyle("-fx-background-color: #2A2A2A;");
					} else {
						setText(item);
						setStyle(
								"-fx-background-color: #2A2A2A; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 8;");
					}

					setOnMouseEntered(e -> {
						if (item != null && !isEmpty()) {
							setStyle(
									"-fx-background-color: #FF4500; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 8;");
						}
					});
					setOnMouseExited(e -> {
						if (item != null && !isEmpty()) {
							setStyle(
									"-fx-background-color: #2A2A2A; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 8;");
						}
					});
				}
			};
		});

		comboBox.setButtonCell(new javafx.scene.control.ListCell<String>() {
			@Override
			protected void updateItem(String item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					setText(comboBox.getPromptText());
					setStyle("-fx-text-fill: #818384;");
				} else {
					setText(item);
					setStyle("-fx-text-fill: white;");
				}
			}
		});
	}

	// Initialize controller with user data and load posts
	public void initData(User user) {
		this.currentUser = user;
		System.out.println("✓ Forum initialized for: " + user.getUsername());

		if (usernameLabel != null && user != null) {
			usernameLabel.setText(user.getUsername());
		}

		loadPostsFromDB();
	}

	// Configure search field with debounce and enter key handling
	private void setupSearchInteractions() {
		if (searchField == null) {
			return;
		}

		searchDebounce = new PauseTransition(Duration.millis(400));
		searchDebounce.setOnFinished(e -> performSearch());

		searchField.textProperty().addListener((obs, oldVal, newVal) -> {
			searchDebounce.stop();
			searchDebounce.playFromStart();
		});

		searchField.setOnAction(e -> performSearch());
	}

	// Load posts from database based on user's joined communities
	public void loadPostsFromDB() {
		try {
			if (currentUser != null) {
				List<PostDAO.PostDTO> posts = postDAO.getPostsFromJoinedCommunities(currentUser.getUserId());
				cachedPosts = posts != null ? posts : new ArrayList<>();
				System.out.println("Controller: Home posts.size = " + cachedPosts.size());
			} else {
				List<PostDAO.PostDTO> posts = postDAO.getAllPosts();
				cachedPosts = posts != null ? posts : new ArrayList<>();
				System.out.println("Controller: posts.size = " + cachedPosts.size());
			}

			performSearch();

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Error loading DB posts: " + e.getMessage());
		}
	}

	// Display posts in the main container with Reddit-style cards
	private void renderPosts(List<PostDAO.PostDTO> postsToRender) {
		postContainer.getChildren().clear();

		if (postsToRender == null || postsToRender.isEmpty()) {
			String message = "No posts available.";
			String keyword = searchField != null ? searchField.getText() : "";
			if (keyword != null && !keyword.trim().isEmpty()) {
				message = "No posts match \"" + keyword.trim() + "\".";
			}

			Label emptyLabel = new Label(message);
			emptyLabel.setStyle("-fx-text-fill: #555555; -fx-font-size: 14; -fx-padding: 24;");
			postContainer.getChildren().add(emptyLabel);
			return;
		}

		for (PostDAO.PostDTO post : postsToRender) {
			createRedditStylePost(post.postId, post.community, post.author, post.timeAgo, post.title, post.content,
					post.upvotes, post.comments, post.tag, false);
		}
	}

	private Node createReplyNode(Reply reply, int level) {

		HBox row = new HBox(6);

		if (level > 0) {
			VBox lineBox = new VBox();
			lineBox.setPrefWidth(16);
			lineBox.setAlignment(Pos.TOP_CENTER);

			Region vertical = new Region();
			VBox.setVgrow(vertical, Priority.ALWAYS);
			vertical.setStyle("-fx-border-color: #cccccc; -fx-border-width: 0 0 0 2;");

			lineBox.getChildren().add(vertical);
			row.getChildren().add(lineBox);
		}

		VBox card = new VBox(4);
		card.setPadding(new Insets(4, 8, 4, 8));

		String style = "-fx-background-color: transparent;";
		if (level > 0) {
			style += "-fx-border-color: #cccccc; -fx-border-width: 1 0 0 0;";
		}
		card.setStyle(style);

		Label text = new Label(reply.getText());
		text.setWrapText(true);
		text.setStyle("-fx-font-size: 12; -fx-text-fill: #555555;");

		card.getChildren().add(text);

		if (reply.getChildren() != null && !reply.getChildren().isEmpty()) {
			VBox childrenBox = new VBox(2);
			for (Reply child : reply.getChildren()) {
				Node childNode = createReplyNode(child, level + 1);
				childrenBox.getChildren().add(childNode);
			}
			card.getChildren().add(childrenBox);
		}

		row.getChildren().add(card);
		return row;
	}

	// Create a Reddit-style post card with voting and interaction buttons
	private void createRedditStylePost(int postId, String community, String author, String timeAgo, String title,
			String content, int upvotes, int comments, String tag, boolean hasJoinButton) {
		HBox postCard = new HBox(8);
		postCard.setPadding(new Insets(12));
		postCard.setStyle(
				"-fx-background-color: #1A1A1B; -fx-border-color: #343536; -fx-border-width: 1; -fx-cursor: hand; -fx-background-radius: 4;");

		postCard.setOnMouseClicked(e -> openPostDetail(postId));

		VBox voteBox = new VBox(4);
		voteBox.setAlignment(Pos.TOP_CENTER);
		voteBox.setPrefWidth(40);
		voteBox.setStyle("-fx-padding: 4 0;");

		Label voteCount = new Label(String.valueOf(upvotes));
		voteCount.setStyle("-fx-text-fill: white; -fx-font-size: 12; -fx-font-weight: bold;");

		int userVote = 0;
		if (currentUser != null) {
			try {
				userVote = postDAO.getUserVote(postId, currentUser.getUserId());
			} catch (SQLException e) {
				System.err.println("Error checking user vote: " + e.getMessage());
			}
		}

		String defaultUpvoteStyle = "-fx-background-color: transparent; -fx-text-fill: #818384; -fx-font-size: 16; -fx-cursor: hand; -fx-padding: 2 8;";
		String defaultDownvoteStyle = "-fx-background-color: transparent; -fx-text-fill: #818384; -fx-font-size: 16; -fx-cursor: hand; -fx-padding: 2 8;";
		String votedUpvoteStyle = "-fx-background-color: transparent; -fx-text-fill: #FF4500; -fx-font-size: 16; -fx-cursor: hand; -fx-padding: 2 8; -fx-font-weight: bold;";
		String votedDownvoteStyle = "-fx-background-color: transparent; -fx-text-fill: #7193FF; -fx-font-size: 16; -fx-cursor: hand; -fx-padding: 2 8; -fx-font-weight: bold;";

		Button upvoteBtn = new Button("▲");
		Button downvoteBtn = new Button("▼");

		upvoteBtn.setStyle(userVote == 1 ? votedUpvoteStyle : defaultUpvoteStyle);
		downvoteBtn.setStyle(userVote == -1 ? votedDownvoteStyle : defaultDownvoteStyle);

		upvoteBtn.setOnMouseClicked(e -> {
			e.consume();
			handleUpvote(postId, voteCount, upvoteBtn, downvoteBtn);
		});

		downvoteBtn.setOnMouseClicked(e -> {
			e.consume();
			handleDownvote(postId, voteCount, upvoteBtn, downvoteBtn);
		});

		voteBox.getChildren().addAll(upvoteBtn, voteCount, downvoteBtn);

		VBox contentBox = new VBox(6);
		contentBox.setPadding(new Insets(4, 0, 4, 0));
		VBox.setVgrow(contentBox, Priority.ALWAYS);

		HBox metaRow = new HBox(8);
		metaRow.setAlignment(Pos.CENTER_LEFT);

		Label communityLabel = new Label(community);
		communityLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12; -fx-font-weight: bold; -fx-cursor: hand;");

		HBox authorTimeBox = new HBox(4);
		authorTimeBox.setAlignment(Pos.CENTER_LEFT);

		Label postedByLabel = new Label("Posted by ");
		postedByLabel.setStyle("-fx-text-fill: #818384; -fx-font-size: 12;");

		Label authorNameLabel = new Label(author);
		authorNameLabel.setStyle("-fx-text-fill: #0079D3; -fx-font-size: 12; -fx-cursor: hand;");
		authorNameLabel.setOnMouseClicked(e -> navigateToUserSettings(author));
		authorNameLabel.setOnMouseEntered(
				e -> authorNameLabel.setStyle("-fx-text-fill: #7193FF; -fx-font-size: 12; -fx-cursor: hand;"));
		authorNameLabel.setOnMouseExited(
				e -> authorNameLabel.setStyle("-fx-text-fill: #0079D3; -fx-font-size: 12; -fx-cursor: hand;"));

		Label timeLabel = new Label(" • " + timeAgo);
		timeLabel.setStyle("-fx-text-fill: #818384; -fx-font-size: 12;");

		authorTimeBox.getChildren().addAll(postedByLabel, authorNameLabel, timeLabel);

		if (tag != null && !tag.trim().isEmpty()) {
			HBox tagBox = new HBox(6);
			tagBox.setAlignment(Pos.CENTER_LEFT);
			tagBox.setStyle("-fx-background-color: #FF4500; -fx-background-radius: 12; -fx-padding: 4 8;");

			Label tagLabel = new Label(tag);
			tagLabel.setStyle("-fx-text-fill: white; -fx-font-size: 11; -fx-font-weight: 600;");

			tagBox.getChildren().add(tagLabel);

			if (hasJoinButton) {
				Button joinBtn = new Button("Join");
				joinBtn.setStyle(
						"-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 11; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 4 16; -fx-cursor: hand; -fx-border-color: #FF4500; -fx-border-width: 1; -fx-border-radius: 20;");
				metaRow.getChildren().addAll(communityLabel, authorTimeBox, tagBox, new Region(), joinBtn);
				HBox.setHgrow(metaRow.getChildren().get(3), Priority.ALWAYS);
			} else {
				metaRow.getChildren().addAll(communityLabel, authorTimeBox, tagBox);
			}
		} else {
			if (hasJoinButton) {
				Button joinBtn = new Button("Join");
				joinBtn.setStyle(
						"-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 11; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 4 16; -fx-cursor: hand; -fx-border-color: #FF4500; -fx-border-width: 1; -fx-border-radius: 20;");
				metaRow.getChildren().addAll(communityLabel, authorTimeBox, new Region(), joinBtn);
				HBox.setHgrow(metaRow.getChildren().get(2), Priority.ALWAYS);
			} else {
				metaRow.getChildren().addAll(communityLabel, authorTimeBox);
			}
		}

		Label titleLabel = new Label(title);
		titleLabel.setStyle(
				"-fx-text-fill: white; -fx-font-size: 16; -fx-font-weight: 600; -fx-cursor: hand; -fx-wrap-text: true;");
		titleLabel.setWrapText(true);

		Label contentLabel = new Label(content);
		contentLabel.setStyle("-fx-text-fill: #D7DADC; -fx-font-size: 14;");
		contentLabel.setWrapText(false);
		contentLabel.setMaxWidth(Double.MAX_VALUE);
		contentLabel.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);

		HBox actionRow = new HBox(16);
		actionRow.setAlignment(Pos.CENTER_LEFT);
		actionRow.setStyle("-fx-padding: 8 0 0 0;");

		Button commentBtn = createActionButton("💬 Comment", String.valueOf(comments));
		commentBtn.setOnMouseClicked(e -> {
			e.consume();
			openPostDetail(postId);
		});

		actionRow.getChildren().addAll(commentBtn);

		contentBox.getChildren().addAll(metaRow, titleLabel, contentLabel, actionRow);

		postCard.getChildren().addAll(voteBox, contentBox);
		postContainer.getChildren().add(postCard);
	}

	// Open post detail modal with comments and replies
	private void openPostDetail(int postId) {
		try {
			PostDAO.PostDTO postData = null;

			var allPosts = postDAO.getAllPosts();
			for (PostDAO.PostDTO post : allPosts) {
				if (post.postId == postId) {
					postData = post;
					break;
				}
			}

			if (postData == null) {
				System.out.println("Post not found with id: " + postId);
				return;
			}

			FXMLLoader loader = new FXMLLoader(
					getClass().getResource("/edu/northeastern/uniforum/forum/view/post_detail.fxml"));
			Parent dialogContent = loader.load();

			ReplyController replyController = loader.getController();
			if (replyController != null) {
				replyController.setParentController(this);
				replyController.setCurrentUser(currentUser);
				replyController.setPostData(postData);
			}

			dialogContainer.getChildren().clear();
			dialogContainer.getChildren().add(dialogContent);

			double dialogWidth = 750;
			double dialogHeight = 600;

			if (modalOverlay != null && modalOverlay.getScene() != null) {
				javafx.stage.Window window = modalOverlay.getScene().getWindow();
				if (window instanceof javafx.stage.Stage) {
					javafx.stage.Stage mainStage = (javafx.stage.Stage) window;
					if (mainStage != null) {
						double windowWidth = mainStage.getWidth();
						double windowHeight = mainStage.getHeight();
						if (windowWidth > 0 && windowHeight > 0) {
							dialogWidth = Math.min(windowWidth * 0.70, 750);
							dialogHeight = Math.min(windowHeight * 0.80, 600);
						}
					}
				}
			}

			dialogContainer.setPrefWidth(dialogWidth);
			dialogContainer.setPrefHeight(dialogHeight);
			dialogContainer.setMaxWidth(dialogWidth);
			dialogContainer.setMaxHeight(dialogHeight);

			dialogContainer.setStyle(
					"-fx-background-color: #1A1A1B; -fx-background-radius: 8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 20, 0, 0, 0);");

			modalOverlay.setVisible(true);
			modalOverlay.setManaged(true);

			StackPane root = (StackPane) modalOverlay.getParent();
			if (root != null) {
				modalOverlay.prefWidthProperty().bind(root.widthProperty());
				modalOverlay.prefHeightProperty().bind(root.heightProperty());
			}

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Error opening post detail: " + e.getMessage());
		}
	}

	private Button createActionButton(String text, String count) {
		HBox btnContent = new HBox(4);
		btnContent.setAlignment(Pos.CENTER);

		Label textLabel = new Label(text);
		textLabel.setStyle("-fx-text-fill: #818384; -fx-font-size: 12; -fx-font-weight: 600;");

		if (!count.isEmpty()) {
			Label countLabel = new Label(count);
			countLabel.setStyle("-fx-text-fill: #818384; -fx-font-size: 12; -fx-font-weight: 600;");
			btnContent.getChildren().addAll(textLabel, countLabel);
		} else {
			btnContent.getChildren().add(textLabel);
		}

		Button btn = new Button();
		btn.setGraphic(btnContent);
		btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 4 8;");

		btn.setOnMouseEntered(e -> btn.setStyle(
				"-fx-background-color: #272729; -fx-cursor: hand; -fx-padding: 4 8; -fx-background-radius: 4;"));
		btn.setOnMouseExited(
				e -> btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 4 8;"));

		return btn;
	}

	private void addRecentPost(String title, String community, String timeAgo, int upvotes, int comments) {
		VBox recentPost = new VBox(4);
		recentPost.setPadding(new Insets(8, 0, 8, 0));
		recentPost.setStyle("-fx-border-color: #E0E0E0; -fx-border-width: 0 0 1 0;");

		Label titleLabel = new Label(title);
		titleLabel.setStyle(
				"-fx-text-fill: #3D348B; -fx-font-size: 13; -fx-font-weight: bold; -fx-cursor: hand; -fx-wrap-text: true;");
		titleLabel.setWrapText(true);

		HBox metaRow = new HBox(8);
		Label communityLabel = new Label(community);
		communityLabel.setStyle("-fx-text-fill: #555555; -fx-font-size: 11;");

		Label timeLabel = new Label(timeAgo);
		timeLabel.setStyle("-fx-text-fill: #555555; -fx-font-size: 11;");

		Label statsLabel = new Label("↑ " + upvotes + " • 💬 " + comments);
		statsLabel.setStyle("-fx-text-fill: #555555; -fx-font-size: 11;");

		metaRow.getChildren().addAll(communityLabel, timeLabel, new Region(), statsLabel);
		HBox.setHgrow(metaRow.getChildren().get(2), Priority.ALWAYS);

		recentPost.getChildren().addAll(titleLabel, metaRow);
		recentPostsContainer.getChildren().add(recentPost);
	}

	@FXML
	private void onSearch() {
		performSearch();
	}

	@FXML
	private void onAskClicked() {
		performSearch();
	}

	// Filter and sort posts based on search criteria and view mode
	private void performSearch() {
		if (cachedPosts == null) {
			cachedPosts = new ArrayList<>();
		}

		String keyword = searchField != null ? searchField.getText() : "";
		List<PostDAO.PostDTO> filtered = new ArrayList<>(cachedPosts);

		if (keyword != null && !keyword.trim().isEmpty()) {
			String lowered = keyword.trim().toLowerCase();
			filtered.removeIf(post -> {
				String title = post.title != null ? post.title.toLowerCase() : "";
				String content = post.content != null ? post.content.toLowerCase() : "";
				return !title.contains(lowered) && !content.contains(lowered);
			});
		}

		if (isExploreView) {
			if (communityFilter != null && communityFilter.getValue() != null
					&& !communityFilter.getValue().equals("All Communities")) {
				String selectedCommunity = communityFilter.getValue();
				filtered.removeIf(post -> !post.community.equals(selectedCommunity));
			}

			if (tagFilter != null && tagFilter.getValue() != null && !tagFilter.getValue().equals("All Tags")) {
				String selectedTag = tagFilter.getValue();
				filtered.removeIf(post -> post.tag == null || !post.tag.equals(selectedTag));
			}

			if (sortByFilter != null && sortByFilter.getValue() != null) {
				String sortOption = sortByFilter.getValue();
				switch (sortOption) {
				case "Most Liked":
					filtered.sort((a, b) -> Integer.compare(b.upvotes, a.upvotes));
					break;
				case "Least Liked":
					filtered.sort((a, b) -> Integer.compare(a.upvotes, b.upvotes));
					break;
				case "Latest":
					try {
						List<PostDAO.PostDTO> timeSorted = postDAO.getAllPosts();
						if (keyword != null && !keyword.trim().isEmpty()) {
							String lowered = keyword.trim().toLowerCase();
							timeSorted.removeIf(post -> {
								String title = post.title != null ? post.title.toLowerCase() : "";
								String content = post.content != null ? post.content.toLowerCase() : "";
								return !title.contains(lowered) && !content.contains(lowered);
							});
						}
						if (communityFilter != null && communityFilter.getValue() != null
								&& !communityFilter.getValue().equals("All Communities")) {
							String selectedCommunity = communityFilter.getValue();
							timeSorted.removeIf(post -> !post.community.equals(selectedCommunity));
						}
						if (tagFilter != null && tagFilter.getValue() != null
								&& !tagFilter.getValue().equals("All Tags")) {
							String selectedTag = tagFilter.getValue();
							timeSorted.removeIf(post -> post.tag == null || !post.tag.equals(selectedTag));
						}
						filtered = timeSorted;
					} catch (SQLException e) {
						System.err.println("Error reloading posts for latest sort: " + e.getMessage());
					}
					break;
				case "Oldest":
					try {
						List<PostDAO.PostDTO> timeSorted = postDAO.getAllPosts();
						if (keyword != null && !keyword.trim().isEmpty()) {
							String lowered = keyword.trim().toLowerCase();
							timeSorted.removeIf(post -> {
								String title = post.title != null ? post.title.toLowerCase() : "";
								String content = post.content != null ? post.content.toLowerCase() : "";
								return !title.contains(lowered) && !content.contains(lowered);
							});
						}
						if (communityFilter != null && communityFilter.getValue() != null
								&& !communityFilter.getValue().equals("All Communities")) {
							String selectedCommunity = communityFilter.getValue();
							timeSorted.removeIf(post -> !post.community.equals(selectedCommunity));
						}
						if (tagFilter != null && tagFilter.getValue() != null
								&& !tagFilter.getValue().equals("All Tags")) {
							String selectedTag = tagFilter.getValue();
							timeSorted.removeIf(post -> post.tag == null || !post.tag.equals(selectedTag));
						}
						java.util.Collections.reverse(timeSorted);
						filtered = timeSorted;
					} catch (SQLException e) {
						System.err.println("Error reloading posts for oldest sort: " + e.getMessage());
					}
					break;
				}
			} else {
				filtered.sort((a, b) -> Integer.compare(b.upvotes, a.upvotes));
			}
		} else {
			if (keyword != null && !keyword.trim().isEmpty()) {
				filtered.sort((a, b) -> Integer.compare(b.upvotes, a.upvotes));
			}
		}

		renderPosts(filtered);
	}

	// Open create post modal dialog
	@FXML
	private void onCreatePostClicked() {
		try {
			FXMLLoader loader = new FXMLLoader(
					getClass().getResource("/edu/northeastern/uniforum/forum/view/create_post.fxml"));
			Parent dialogContent = loader.load();

			CreatePostController dialogController = loader.getController();
			if (dialogController != null) {
				dialogController.setParentController(this);
				dialogController.setCurrentUser(currentUser);
			}

			dialogContainer.getChildren().clear();
			dialogContainer.getChildren().add(dialogContent);

			double dialogWidth = 650;
			double dialogHeight = 550;

			if (modalOverlay != null && modalOverlay.getScene() != null) {
				javafx.stage.Window window = modalOverlay.getScene().getWindow();
				if (window instanceof javafx.stage.Stage) {
					javafx.stage.Stage mainStage = (javafx.stage.Stage) window;
					if (mainStage != null) {
						double windowWidth = mainStage.getWidth();
						double windowHeight = mainStage.getHeight();
						if (windowWidth > 0 && windowHeight > 0) {
							dialogWidth = Math.min(windowWidth * 0.7, 650);
							dialogHeight = Math.min(windowHeight * 0.75, 550);
						}
					}
				}
			}

			dialogContainer.setPrefWidth(dialogWidth);
			dialogContainer.setPrefHeight(dialogHeight);
			dialogContainer.setMaxWidth(dialogWidth);
			dialogContainer.setMaxHeight(dialogHeight);

			dialogContainer.setStyle(
					"-fx-background-color: white; -fx-background-radius: 8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 20, 0, 0, 0);");

			modalOverlay.setVisible(true);
			modalOverlay.setManaged(true);

			StackPane root = (StackPane) modalOverlay.getParent();
			if (root != null) {
				modalOverlay.prefWidthProperty().bind(root.widthProperty());
				modalOverlay.prefHeightProperty().bind(root.heightProperty());
			}

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Error opening create post dialog: " + e.getMessage());
		}
	}

	public void closeModal() {
		if (modalOverlay != null) {
			modalOverlay.setVisible(false);
			modalOverlay.setManaged(false);
			dialogContainer.getChildren().clear();
		}
	}

	@FXML
	private void onOverlayClicked(javafx.scene.input.MouseEvent event) {
		if (event.getTarget() == overlayBackground) {
			closeModal();
		}
	}

	@FXML
	private void onProfileClicked(MouseEvent event) {
		if (currentUser == null) {
			return;
		}

		ContextMenu contextMenu = new ContextMenu();

		String contextMenuStyle = "-fx-background-color: #2A2A2A; " + "-fx-border-color: #343536; "
				+ "-fx-border-radius: 4; " + "-fx-background-radius: 4;";
		contextMenu.setStyle(contextMenuStyle);

		MenuItem settingsItem = new MenuItem("Settings");
		settingsItem.setOnAction(e -> {
			SceneManager.switchToSettings(currentUser);
		});
		settingsItem.setStyle("-fx-text-fill: white; -fx-background-color: #2A2A2A;");

		MenuItem logoutItem = new MenuItem("Logout");
		logoutItem.setOnAction(e -> {
			handleLogout();
		});
		logoutItem.setStyle("-fx-text-fill: white; -fx-background-color: #2A2A2A;");

		contextMenu.getItems().addAll(settingsItem, logoutItem);

		contextMenu.show(usernameLabel, event.getScreenX(), event.getScreenY());
	}

	private void handleLogout() {
		currentUser = null;

		SceneManager.switchToLogin();
	}

	@FXML
	private void onHomeClicked() {
		if (currentUser == null) {
			System.out.println("User must be logged in to view home feed.");
			return;
		}

		isExploreView = false;
		if (filterStrip != null) {
			filterStrip.setVisible(false);
			filterStrip.setManaged(false);
		}

		try {
			List<PostDAO.PostDTO> posts = postDAO.getPostsFromJoinedCommunities(currentUser.getUserId());
			cachedPosts = posts != null ? posts : new ArrayList<>();

			System.out.println("Controller: Home posts.size = " + cachedPosts.size());

			if (searchField != null) {
				searchField.clear();
			}

			renderPosts(cachedPosts);
		} catch (SQLException e) {
			System.err.println("Error loading home posts: " + e.getMessage());
			e.printStackTrace();
		}
	}

	@FXML
	private void onPopularClicked() {
		System.out.println("Popular navigation clicked");
	}

	@FXML
	private void onExploreClicked() {
		isExploreView = true;
		if (filterStrip != null) {
			filterStrip.setVisible(true);
			filterStrip.setManaged(true);
		}

		try {
			List<PostDAO.PostDTO> posts = postDAO.getAllPostsSortedByLikes();
			cachedPosts = posts != null ? posts : new ArrayList<>();

			System.out.println("Controller: Explore posts.size = " + cachedPosts.size());

			if (searchField != null) {
				searchField.clear();
			}

			applyFilters();
		} catch (SQLException e) {
			System.err.println("Error loading explore posts: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private void applyFilters() {
		if (!isExploreView) {
			return;
		}

		if (sortByFilter != null && sortByFilter.getValue() != null) {
			String sortOption = sortByFilter.getValue();
			if (sortOption.equals("Latest") || sortOption.equals("Oldest")) {
				try {
					cachedPosts = postDAO.getAllPosts();
				} catch (SQLException e) {
					System.err.println("Error reloading posts for time-based sort: " + e.getMessage());
				}
			}
		}

		performSearch();
	}

	@FXML
	private void onAllClicked() {
		System.out.println("All navigation clicked");
	}

	@FXML
	private void onCoursesClicked() {
		if (currentUser == null) {
			System.out.println("User must be logged in to view courses.");
			return;
		}

		SceneManager.switchToCourseSelection(currentUser);
	}

	@FXML
	private void onStartCommunityClicked() {
		System.out.println("Start a community clicked");
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

	private void handleUpvote(int postId, Label voteCountLabel, Button upvoteBtn, Button downvoteBtn) {
		if (currentUser == null) {
			System.out.println("User must be logged in to vote.");
			return;
		}

		try {
			boolean voteAdded = postDAO.handleUpvote(postId, currentUser.getUserId());

			int newCount = postDAO.getVoteCount(postId);
			voteCountLabel.setText(String.valueOf(newCount));

			for (PostDAO.PostDTO post : cachedPosts) {
				if (post.postId == postId) {
					post.upvotes = newCount;
					break;
				}
			}

			String defaultUpvoteStyle = "-fx-background-color: transparent; -fx-text-fill: #555555; -fx-font-size: 16; -fx-cursor: hand; -fx-padding: 2 8;";
			String defaultDownvoteStyle = "-fx-background-color: transparent; -fx-text-fill: #555555; -fx-font-size: 16; -fx-cursor: hand; -fx-padding: 2 8;";
			String votedUpvoteStyle = "-fx-background-color: transparent; -fx-text-fill: #7678ED; -fx-font-size: 16; -fx-cursor: hand; -fx-padding: 2 8; -fx-font-weight: bold;";
			String votedDownvoteStyle = "-fx-background-color: transparent; -fx-text-fill: #7678ED; -fx-font-size: 16; -fx-cursor: hand; -fx-padding: 2 8; -fx-font-weight: bold;";

			if (voteAdded) {
				int currentVote = postDAO.getUserVote(postId, currentUser.getUserId());
				if (currentVote == 1) {
					upvoteBtn.setStyle(votedUpvoteStyle);
					downvoteBtn.setStyle(defaultDownvoteStyle);
				} else {
					upvoteBtn.setStyle(defaultUpvoteStyle);
					downvoteBtn.setStyle(defaultDownvoteStyle);
				}
			} else {
				upvoteBtn.setStyle(defaultUpvoteStyle);
				downvoteBtn.setStyle(defaultDownvoteStyle);
			}

			if (!voteAdded) {
				System.out.println("Upvote removed (user had already upvoted).");
			}
		} catch (SQLException e) {
			System.err.println("Error handling upvote: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private void handleDownvote(int postId, Label voteCountLabel, Button upvoteBtn, Button downvoteBtn) {
		if (currentUser == null) {
			System.out.println("User must be logged in to vote.");
			return;
		}

		try {
			boolean voteAdded = postDAO.handleDownvote(postId, currentUser.getUserId());

			int newCount = postDAO.getVoteCount(postId);
			voteCountLabel.setText(String.valueOf(newCount));

			for (PostDAO.PostDTO post : cachedPosts) {
				if (post.postId == postId) {
					post.upvotes = newCount;
					break;
				}
			}

			String defaultUpvoteStyle = "-fx-background-color: transparent; -fx-text-fill: #555555; -fx-font-size: 16; -fx-cursor: hand; -fx-padding: 2 8;";
			String defaultDownvoteStyle = "-fx-background-color: transparent; -fx-text-fill: #555555; -fx-font-size: 16; -fx-cursor: hand; -fx-padding: 2 8;";
			String votedUpvoteStyle = "-fx-background-color: transparent; -fx-text-fill: #7678ED; -fx-font-size: 16; -fx-cursor: hand; -fx-padding: 2 8; -fx-font-weight: bold;";
			String votedDownvoteStyle = "-fx-background-color: transparent; -fx-text-fill: #7678ED; -fx-font-size: 16; -fx-cursor: hand; -fx-padding: 2 8; -fx-font-weight: bold;";

			if (voteAdded) {
				int currentVote = postDAO.getUserVote(postId, currentUser.getUserId());
				if (currentVote == -1) {
					upvoteBtn.setStyle(defaultUpvoteStyle);
					downvoteBtn.setStyle(votedDownvoteStyle);
				} else {
					upvoteBtn.setStyle(defaultUpvoteStyle);
					downvoteBtn.setStyle(defaultDownvoteStyle);
				}
			} else {
				upvoteBtn.setStyle(defaultUpvoteStyle);
				downvoteBtn.setStyle(defaultDownvoteStyle);
			}

			if (!voteAdded) {
				System.out.println("Downvote removed (user had already downvoted).");
			}
		} catch (SQLException e) {
			System.err.println("Error handling downvote: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private void setupNavButtonHovers() {
		if (homeNavButton != null && homeNavButton.getParent() != null) {
			VBox navContainer = (VBox) homeNavButton.getParent();
			String normalStyle = "-fx-alignment: CENTER_LEFT; -fx-background-color: transparent; -fx-text-fill: #FFFFFF; -fx-background-radius: 20; -fx-padding: 10 12; -fx-font-size: 14px; -fx-font-weight: 600; -fx-cursor: hand;";
			String hoverStyle = "-fx-alignment: CENTER_LEFT; -fx-background-color: rgba(255, 69, 0, 0.1); -fx-text-fill: #FFFFFF; -fx-background-radius: 20; -fx-padding: 10 12; -fx-font-size: 14px; -fx-font-weight: 600; -fx-cursor: hand;";

			for (javafx.scene.Node node : navContainer.getChildren()) {
				if (node instanceof Button) {
					Button btn = (Button) node;
					btn.setStyle(normalStyle);
					btn.setOnMouseEntered(e -> btn.setStyle(hoverStyle));
					btn.setOnMouseExited(e -> btn.setStyle(normalStyle));
				}
			}
		}
	}

}
