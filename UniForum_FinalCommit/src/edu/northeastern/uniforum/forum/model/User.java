package edu.northeastern.uniforum.forum.model;

public class User {
    private int userId;
    private String username;
    private String passwordHash; 
    private String email;
    private String linkedinUrl;
    private String githubUrl;
    private String department;

    public User(String username, String passwordHash, String email) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
        this.linkedinUrl = null;
        this.githubUrl = null;
        this.department = null;
    }

    public User(int userId, String username, String passwordHash, String email) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
        this.linkedinUrl = null;
        this.githubUrl = null;
        this.department = null;
    }

    public User(int userId, String username, String passwordHash, String email, 
                String linkedinUrl, String githubUrl, String department) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
        this.linkedinUrl = linkedinUrl;
        this.githubUrl = githubUrl;
        this.department = department;
    }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getLinkedinUrl() { return linkedinUrl; }
    public void setLinkedinUrl(String linkedinUrl) { this.linkedinUrl = linkedinUrl; }
    public String getGithubUrl() { return githubUrl; }
    public void setGithubUrl(String githubUrl) { this.githubUrl = githubUrl; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
}