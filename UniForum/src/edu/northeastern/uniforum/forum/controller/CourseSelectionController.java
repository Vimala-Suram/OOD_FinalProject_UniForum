package edu.northeastern.uniforum.forum.controller;

import edu.northeastern.uniforum.forum.dao.PostDAO;
import edu.northeastern.uniforum.forum.dao.UserDAO;
import edu.northeastern.uniforum.forum.model.User;
import edu.northeastern.uniforum.forum.util.SceneManager;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.ListCell;
import javafx.scene.control.CheckBox;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.util.Callback;
import javafx.application.Platform;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CourseSelectionController {

    @FXML private ComboBox<String> deptCombo;
    @FXML private ListView<String> courseListView;
    @FXML private Button nextBtn;
    @FXML private Label selectedCoursesLabel;

    private User currentUser;
    private final PostDAO postDAO = new PostDAO();
    private final UserDAO userDAO = new UserDAO();

    private Map<String, Integer> courseToCommunityMap = new HashMap<>();
    private List<Integer> selectedCommunityIds = new ArrayList<>();

    private List<String> allSelectedCourses = new ArrayList<>();

    private boolean isUpdatingSelection = false;

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (user != null) {
            initializeCommunities();
            Platform.runLater(() -> {
                loadUserExistingCourses();
            });
        }
    }

    private void loadUserExistingCourses() {
        if (currentUser == null) {
            return;
        }

        try {
            List<Integer> userCommunityIds = userDAO.getUserCommunities(currentUser.getUserId());

            allSelectedCourses.clear();
            for (Map.Entry<String, Integer> entry : courseToCommunityMap.entrySet()) {
                if (userCommunityIds.contains(entry.getValue())) {
                    allSelectedCourses.add(entry.getKey());
                }
            }

            System.out.println("Loaded " + allSelectedCourses.size() + " existing courses for user: " + currentUser.getUsername());

            updateSelectedCoursesLabel();

            if (deptCombo != null && deptCombo.getValue() != null) {
                Platform.runLater(() -> {
                    if (courseListView != null) {
                        courseListView.refresh();
                    }
                });
            }
        } catch (Exception e) {
            System.err.println("Error loading user's existing courses: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void initialize() {
        if (deptCombo != null) {
            deptCombo.getItems().addAll("IS", "CSYE", "DAMG", "TELE");
            deptCombo.setOnAction(e -> updateCourseList());

            deptCombo.setCellFactory(listView -> {
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

            deptCombo.setButtonCell(new javafx.scene.control.ListCell<String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText("Choose Department");
                        setStyle("-fx-text-fill: #818384;");
                    } else {
                        setText(item);
                        setStyle("-fx-text-fill: white;");
                    }
                }
            });
        }

        if (courseListView != null) {
            courseListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

            courseListView.setCellFactory(listView -> {
                CheckBoxListCell<String> cell = new CheckBoxListCell<>(item -> {
                    javafx.beans.property.SimpleBooleanProperty property = 
                        new javafx.beans.property.SimpleBooleanProperty(allSelectedCourses.contains(item));

                    property.addListener((obs, oldVal, newVal) -> {
                        if (!isUpdatingSelection) {
                            if (newVal) {
                                if (!allSelectedCourses.contains(item)) {
                                    allSelectedCourses.add(item);
                                }
                            } else {
                                allSelectedCourses.remove(item);
                            }
                            updateSelectedCoursesLabel();
                        }
                    });

                    return property;
                });

                cell.setStyle("-fx-background-color: #0B1416; -fx-text-fill: white; -fx-font-size: 14px;");

                cell.itemProperty().addListener((obs, oldItem, newItem) -> {
                    if (newItem != null) {
                        cell.setStyle("-fx-background-color: #0B1416; -fx-text-fill: white; -fx-font-size: 14px;");
                    }
                });

                return cell;
            });
        }

        if (courseListView != null) {
            courseListView.getSelectionModel().getSelectedItems().addListener(
                (ListChangeListener.Change<? extends String> change) -> {
                    if (!isUpdatingSelection) {
                        updateSelectedCoursesFromCurrentDepartment();
                    }
                }
            );
        }

        if (nextBtn != null) {
            nextBtn.setOnAction(e -> saveSelectionsAndContinue());
        }

        if (currentUser != null) {
            initializeCommunities();
        }
    }

    @FXML
    private void handleBackToForum() {
        if (currentUser != null) {
            SceneManager.switchToForum(currentUser);
        }
    }

    private void updateCourseList() {
        if (courseListView == null || deptCombo == null) {
            return;
        }

        String selectedDept = deptCombo.getValue();
        if (selectedDept == null) {
            courseListView.getItems().clear();
            return;
        }

        List<String> courses = new ArrayList<>();
        switch (selectedDept) {
            case "IS" -> courses.addAll(List.of(
                    "INFO 5002-01 Intro to Python for Info Sys",
                    "INFO 5100-04 Application Engineer & Dev",
                    "INFO 6105-01 Data Sci Eng Methods",
                    "INFO 6106-01 Neural Modeling Methods & Tool"
            ));
            case "CSYE" -> courses.addAll(List.of(
                    "CSYE 6225-03 Netwrk Strctrs & Cloud Cmpting",
                    "CSYE 7105-01 Parallel Machine Learning & AI",
                    "CSYE 7280-01 User Experience Design/Testing",
                    "CSYE 7380-02 Theory & Prac App AI Gen Model"
            ));
            case "DAMG" -> courses.addAll(List.of(
                    "DAMG 6210-01 Data Mgt and Database Design",
                    "DAMG 7250-01 Big Data Architec & Governance",
                    "DAMG 7374-01 ST: Gen AI w/ LLM in Data Eng",
                    "DAMG 7245-02 Big Data Sys & Intel Analytics"
            ));
            case "TELE" -> courses.addAll(List.of(
                    "TELE 5330-01 Data Networking",
                    "TELE 6530-01 Connected Devices",
                    "TELE 7374-02 Special Topics: Building Digital Twins",
                    "TELE 5600-01 Linux for Network Engineers"
            ));
        }

        courseListView.setItems(FXCollections.observableArrayList(courses));

        Platform.runLater(() -> {
            if (courseListView != null) {
                courseListView.refresh();
            }
        });
    }

    private void updateSelectedCoursesFromCurrentDepartment() {
        if (courseListView == null || selectedCoursesLabel == null) {
            return;
        }

        ObservableList<String> selected = courseListView.getSelectionModel().getSelectedItems();
        String currentDept = deptCombo != null ? deptCombo.getValue() : null;

        if (currentDept == null) {
            return;
        }

        List<String> coursesInCurrentDept = new ArrayList<>();
        for (String course : courseListView.getItems()) {
            if (isCourseFromDepartment(course, currentDept)) {
                coursesInCurrentDept.add(course);
            }
        }

        List<String> coursesToRemove = new ArrayList<>();
        for (String existingCourse : allSelectedCourses) {
            if (isCourseFromDepartment(existingCourse, currentDept)) {
                coursesToRemove.add(existingCourse);
            }
        }
        allSelectedCourses.removeAll(coursesToRemove);

        for (String course : selected) {
            if (isCourseFromDepartment(course, currentDept) && !allSelectedCourses.contains(course)) {
                allSelectedCourses.add(course);
            }
        }

        updateSelectedCoursesLabel();
    }

    private boolean isCourseFromDepartment(String course, String department) {
        switch (department) {
            case "IS" -> {
                return course.startsWith("INFO");
            }
            case "SES" -> {
                return course.startsWith("CSYE");
            }
            case "DAMG" -> {
                return course.startsWith("DAMG");
            }
            case "TELE" -> {
                return course.startsWith("TELE");
            }
        }
        return false;
    }

    private void initializeCommunities() {
        try {
            List<PostDAO.CommunityDTO> existingCommunities = postDAO.getAllCommunities();
            Map<String, Integer> existingMap = new HashMap<>();
            for (PostDAO.CommunityDTO comm : existingCommunities) {
                existingMap.put(comm.name, comm.id);
            }

            createCourseCommunityMapping(existingMap);
        } catch (SQLException e) {
            System.err.println("Error initializing communities: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createCourseCommunityMapping(Map<String, Integer> existingCommunities) {
        courseToCommunityMap.clear();

        List<String> allCourses = new ArrayList<>();
        allCourses.add("INFO 5002-01 Intro to Python for Info Sys");
        allCourses.add("INFO 5100-04 Application Engineer & Dev");
        allCourses.add("INFO 6105-01 Data Sci Eng Methods");
        allCourses.add("INFO 6106-01 Neural Modeling Methods & Tool");
        allCourses.add("CSYE 6225-03 Netwrk Strctrs & Cloud Cmpting");
        allCourses.add("CSYE 7105-01 Parallel Machine Learning & AI");
        allCourses.add("CSYE 7280-01 User Experience Design/Testing");
        allCourses.add("CSYE 7380-02 Theory & Prac App AI Gen Model");
        allCourses.add("DAMG 6210-01 Data Mgt and Database Design");
        allCourses.add("DAMG 7250-01 Big Data Architec & Governance");
        allCourses.add("DAMG 7374-01 ST: Gen AI w/ LLM in Data Eng");
        allCourses.add("DAMG 7245-02 Big Data Sys & Intel Analytics");
        allCourses.add("TELE 5330-01 Data Networking");
        allCourses.add("TELE 6530-01 Connected Devices");
        allCourses.add("TELE 7374-02 Special Topics: Building Digital Twins");
        allCourses.add("TELE 5600-01 Linux for Network Engineers");

        for (String course : allCourses) {
            if (!existingCommunities.containsKey(course)) {
                createCommunityIfNotExists(course);
            }
        }

        try {
            List<PostDAO.CommunityDTO> allCommunities = postDAO.getAllCommunities();
            for (PostDAO.CommunityDTO comm : allCommunities) {
                courseToCommunityMap.put(comm.name, comm.id);
            }
            System.out.println("Loaded " + courseToCommunityMap.size() + " communities");
        } catch (SQLException e) {
            System.err.println("Error loading communities: " + e.getMessage());
        }
    }

    private void createCommunityIfNotExists(String communityName) {
        try {
            String sql = "INSERT OR IGNORE INTO Communities (community_name, moderator_id) VALUES (?, 1)";
            java.sql.Connection conn = edu.northeastern.uniforum.db.Database.getConnection();
            try (java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, communityName);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Error creating community: " + e.getMessage());
        }
    }

    private void updateSelectedCoursesLabel() {
        if (selectedCoursesLabel == null) {
            return;
        }

        if (allSelectedCourses.isEmpty()) {
            selectedCoursesLabel.setText("No courses selected");
            selectedCoursesLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12px; -fx-background-color: #0B1416; -fx-padding: 8; -fx-background-radius: 4; -fx-border-color: #343536; -fx-border-width: 1; -fx-border-radius: 4;");
        } else {
            String coursesText = allSelectedCourses.size() > 3 
                ? String.join(", ", allSelectedCourses.subList(0, 3)) + " ... (" + allSelectedCourses.size() + " total)"
                : String.join(", ", allSelectedCourses) + " (" + allSelectedCourses.size() + " total)";
            selectedCoursesLabel.setText("Selected: " + coursesText);
            selectedCoursesLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12px; -fx-background-color: #0B1416; -fx-padding: 8; -fx-background-radius: 4; -fx-border-color: #343536; -fx-border-width: 1; -fx-border-radius: 4;");
        }
    }

    private void saveSelectionsAndContinue() {
        if (currentUser == null) {
            System.out.println("User not logged in");
            if (selectedCoursesLabel != null) {
                selectedCoursesLabel.setText("Error: User not logged in");
                selectedCoursesLabel.setStyle("-fx-text-fill: #FF4500; -fx-font-size: 12px; -fx-background-color: #0B1416; -fx-padding: 8; -fx-background-radius: 4; -fx-border-color: #343536; -fx-border-width: 1; -fx-border-radius: 4;");
            }
            return;
        }

        if (courseListView == null || selectedCoursesLabel == null) {
            return;
        }

        ObservableList<String> selectedItems = courseListView.getSelectionModel().getSelectedItems();

        if (allSelectedCourses.isEmpty()) {
            selectedCoursesLabel.setText("Please select at least one course to continue");
            selectedCoursesLabel.setStyle("-fx-text-fill: #FF4500; -fx-font-size: 12px; -fx-background-color: #0B1416; -fx-padding: 8; -fx-background-radius: 4; -fx-border-color: #343536; -fx-border-width: 1; -fx-border-radius: 4;");
            return;
        }

        selectedCommunityIds.clear();
        for (String courseName : allSelectedCourses) {
            Integer communityId = courseToCommunityMap.get(courseName);
            if (communityId != null) {
                selectedCommunityIds.add(communityId);
            } else {
                System.err.println("Community not found for course: " + courseName + ". Creating it now...");
                createCommunityIfNotExists(courseName);
                try {
                    List<PostDAO.CommunityDTO> allCommunities = postDAO.getAllCommunities();
                    for (PostDAO.CommunityDTO comm : allCommunities) {
                        courseToCommunityMap.put(comm.name, comm.id);
                    }
                    communityId = courseToCommunityMap.get(courseName);
                    if (communityId != null) {
                        selectedCommunityIds.add(communityId);
                    }
                } catch (SQLException e) {
                    System.err.println("Error reloading communities: " + e.getMessage());
                }
            }
        }

        if (selectedCommunityIds.isEmpty()) {
            selectedCoursesLabel.setText("Error: Could not find or create communities for selected courses");
            selectedCoursesLabel.setStyle("-fx-text-fill: #FF4500; -fx-font-size: 12px; -fx-background-color: #0B1416; -fx-padding: 8; -fx-background-radius: 4; -fx-border-color: #343536; -fx-border-width: 1; -fx-border-radius: 4;");
            return;
        }

        if (userDAO.updateUserCommunities(currentUser.getUserId(), selectedCommunityIds)) {
            System.out.println("Successfully updated user to " + selectedCommunityIds.size() + " communities");
            SceneManager.switchToForum(currentUser);
        } else {
            selectedCoursesLabel.setText("Error saving selections. Please try again.");
            selectedCoursesLabel.setStyle("-fx-text-fill: #FF4500; -fx-font-size: 12px; -fx-background-color: #0B1416; -fx-padding: 8; -fx-background-radius: 4; -fx-border-color: #343536; -fx-border-width: 1; -fx-border-radius: 4;");
        }
    }
}
