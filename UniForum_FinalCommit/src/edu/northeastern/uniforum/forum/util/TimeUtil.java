package edu.northeastern.uniforum.forum.util;

import java.time.Duration;
import java.time.LocalDateTime;

public class TimeUtil {

	// Convert timestamp to human-readable relative time format
	public static String timeAgo(LocalDateTime created) {
        Duration diff = Duration.between(created, LocalDateTime.now());

        long seconds = diff.getSeconds();
        if (seconds < 60) return "just now";

        long minutes = seconds / 60;
        if (minutes < 60) return minutes + " min ago";

        long hours = minutes / 60;
        if (hours < 24) return hours + "h ago";

        long days = hours / 24;
        return days + " days ago";
    }
}
