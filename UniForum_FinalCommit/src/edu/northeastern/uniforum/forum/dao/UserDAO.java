package edu.northeastern.uniforum.forum.dao;

import edu.northeastern.uniforum.db.Database;
import edu.northeastern.uniforum.forum.model.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.List;
import java.util.ArrayList;

public class UserDAO {

    private static final String INSERT_USER_SQL = 
            "INSERT INTO Users (user_name, PasswordHash, Email) VALUES (?, ?, ?)";

    private static final String SELECT_USER_BY_USERNAME_SQL =
            "SELECT user_id, user_name, PasswordHash, Email, LinkedInURL, GitHubURL, Department FROM Users WHERE user_name = ?";

    private static final String SELECT_USER_BY_EMAIL_SQL =
            "SELECT user_id, user_name, PasswordHash, Email, LinkedInURL, GitHubURL, Department FROM Users WHERE Email = ?";

	// Register new user with validation and duplicate checking
	public boolean registerUser(User user) {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_USER_SQL)) {

            statement.setString(1, user.getUsername());
            statement.setString(2, user.getPasswordHash());
            statement.setString(3, user.getEmail());

            int rowsAffected = statement.executeUpdate();

            return rowsAffected == 1; 

        } catch (SQLException e) {
            String errorMessage = e.getMessage();
            System.err.println("Database error during user registration: " + errorMessage);
            System.err.println("SQL State: " + e.getSQLState());
            System.err.println("Error Code: " + e.getErrorCode());

            if (e.getErrorCode() == 19 || (errorMessage != null && errorMessage.contains("UNIQUE"))) {
                System.err.println("Username or email already exists.");
            }
            e.printStackTrace();
            return false;
        }
    }

	// Retrieve user by username for authentication
	public User getUserByUsername(String username) {

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_USER_BY_USERNAME_SQL)) {

            statement.setString(1, username);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return new User(
                        rs.getInt("user_id"),
                        rs.getString("user_name"),
                        rs.getString("PasswordHash"),
                        rs.getString("Email"),
                        rs.getString("LinkedInURL"),
                        rs.getString("GitHubURL"),
                        rs.getString("Department")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching user: " + e.getMessage());
        }
        return null;
    }

    public User getUserByEmail(String email) {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_USER_BY_EMAIL_SQL)) {

            statement.setString(1, email == null ? null : email.trim());

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return new User(
                            rs.getInt("user_id"),
                            rs.getString("user_name"),
                            rs.getString("PasswordHash"),
                            rs.getString("Email"),
                            rs.getString("LinkedInURL"),
                            rs.getString("GitHubURL"),
                            rs.getString("Department")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching user by email: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

	// Check if user has joined any communities
	public boolean hasUserJoinedCommunities(int userId) {
        String sql = "SELECT COUNT(*) FROM Community_User WHERE user_id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking user communities: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

	// Add user to multiple communities using batch operations
	public boolean joinUserToCommunities(int userId, List<Integer> communityIds) {
        if (communityIds == null || communityIds.isEmpty()) {
            return false;
        }

        String sql = "INSERT OR IGNORE INTO Community_User (community_id, user_id) VALUES (?, ?)";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            for (Integer communityId : communityIds) {
                statement.setInt(1, communityId);
                statement.setInt(2, userId);
                statement.addBatch();
            }

            statement.executeBatch();
            return true;
        } catch (SQLException e) {
            System.err.println("Error joining user to communities: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public List<Integer> getUserCommunities(int userId) {
        List<Integer> communityIds = new ArrayList<>();
        String sql = "SELECT community_id FROM Community_User WHERE user_id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    communityIds.add(rs.getInt("community_id"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting user communities: " + e.getMessage());
            e.printStackTrace();
        }
        return communityIds;
    }

    public boolean removeUserFromCommunities(int userId, List<Integer> communityIds) {
        if (communityIds == null || communityIds.isEmpty()) {
            return true;
        }

        String sql = "DELETE FROM Community_User WHERE community_id = ? AND user_id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            for (Integer communityId : communityIds) {
                statement.setInt(1, communityId);
                statement.setInt(2, userId);
                statement.addBatch();
            }

            statement.executeBatch();
            return true;
        } catch (SQLException e) {
            System.err.println("Error removing user from communities: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateUserCommunities(int userId, List<Integer> newCommunityIds) {
        try {
            List<Integer> currentCommunities = getUserCommunities(userId);

            List<Integer> toRemove = new ArrayList<>();
            for (Integer currentId : currentCommunities) {
                if (!newCommunityIds.contains(currentId)) {
                    toRemove.add(currentId);
                }
            }

            List<Integer> toAdd = new ArrayList<>();
            for (Integer newId : newCommunityIds) {
                if (!currentCommunities.contains(newId)) {
                    toAdd.add(newId);
                }
            }

            if (!toRemove.isEmpty()) {
                removeUserFromCommunities(userId, toRemove);
            }

            if (!toAdd.isEmpty()) {
                joinUserToCommunities(userId, toAdd);
            }

            return true;
        } catch (Exception e) {
            System.err.println("Error updating user communities: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean updatePassword(int userId, String newPasswordHash) {
        String sql = "UPDATE Users SET PasswordHash = ? WHERE user_id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, newPasswordHash);
            statement.setInt(2, userId);

            int rowsAffected = statement.executeUpdate();
            return rowsAffected == 1;
        } catch (SQLException e) {
            System.err.println("Error updating password: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean updatePasswordByEmail(String email, String newPasswordHash) {
        String sql = "UPDATE Users SET PasswordHash = ? WHERE Email = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, newPasswordHash);
            statement.setString(2, email);

            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            System.err.println("Error updating password by email: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateUsername(int userId, String newUsername) {
        String sql = "UPDATE Users SET user_name = ? WHERE user_id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, newUsername);
            statement.setInt(2, userId);

            int rowsAffected = statement.executeUpdate();
            return rowsAffected == 1;
        } catch (SQLException e) {
            System.err.println("Error updating username: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateEmail(int userId, String newEmail) {
        String sql = "UPDATE Users SET Email = ? WHERE user_id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, newEmail);
            statement.setInt(2, userId);

            int rowsAffected = statement.executeUpdate();
            return rowsAffected == 1;
        } catch (SQLException e) {
            System.err.println("Error updating email: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateLinkedinUrl(int userId, String newLinkedinUrl) {
        String sql = "UPDATE Users SET LinkedInURL = ? WHERE user_id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, newLinkedinUrl.isEmpty() ? null : newLinkedinUrl);
            statement.setInt(2, userId);

            int rowsAffected = statement.executeUpdate();
            return rowsAffected == 1;
        } catch (SQLException e) {
            System.err.println("Error updating LinkedIn URL: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateGithubUrl(int userId, String newGithubUrl) {
        String sql = "UPDATE Users SET GitHubURL = ? WHERE user_id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, newGithubUrl.isEmpty() ? null : newGithubUrl);
            statement.setInt(2, userId);

            int rowsAffected = statement.executeUpdate();
            return rowsAffected == 1;
        } catch (SQLException e) {
            System.err.println("Error updating GitHub URL: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateDepartment(int userId, String newDepartment) {
        String sql = "UPDATE Users SET Department = ? WHERE user_id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, newDepartment.isEmpty() ? null : newDepartment);
            statement.setInt(2, userId);

            int rowsAffected = statement.executeUpdate();
            return rowsAffected == 1;
        } catch (SQLException e) {
            System.err.println("Error updating Department: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

	// Update multiple user fields in a single operation
	public boolean updateUser(int userId, String newUsername, String newEmail, String newPasswordHash,
                             String newLinkedinUrl, String newGithubUrl, String newDepartment) {
        try {
            boolean success = true;

            if (newUsername != null && !newUsername.trim().isEmpty()) {
                success = updateUsername(userId, newUsername.trim()) && success;
            }

            if (newEmail != null && !newEmail.trim().isEmpty()) {
                success = updateEmail(userId, newEmail.trim()) && success;
            }

            if (newPasswordHash != null && !newPasswordHash.isEmpty()) {
                success = updatePassword(userId, newPasswordHash) && success;
            }

            if (newLinkedinUrl != null) {
                success = updateLinkedinUrl(userId, newLinkedinUrl) && success;
            }

            if (newGithubUrl != null) {
                success = updateGithubUrl(userId, newGithubUrl) && success;
            }

            if (newDepartment != null) {
                success = updateDepartment(userId, newDepartment) && success;
            }

            return success;
        } catch (Exception e) {
            System.err.println("Error updating user: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}