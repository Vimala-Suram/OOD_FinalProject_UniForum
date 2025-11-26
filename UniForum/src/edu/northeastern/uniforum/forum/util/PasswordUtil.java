package edu.northeastern.uniforum.forum.util;

import org.mindrot.jbcrypt.BCrypt; 

public class PasswordUtil {

	// Hash password using BCrypt with salt generation
	public static String hashPassword(String plaintextPassword) {
        return BCrypt.hashpw(plaintextPassword, BCrypt.gensalt());
    }

	// Verify plaintext password against stored BCrypt hash
	public static boolean checkPassword(String plaintextPassword, String storedHash) {
        return BCrypt.checkpw(plaintextPassword, storedHash);
    }
}