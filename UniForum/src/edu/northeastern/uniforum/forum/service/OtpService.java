package edu.northeastern.uniforum.forum.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class OtpService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int OTP_LENGTH = 6;
    private static final Duration OTP_EXPIRY = Duration.ofMinutes(10);
    private static final Map<String, OtpRecord> OTP_STORE = new ConcurrentHashMap<>();

    private OtpService() {
    }

	// Generate 6-digit OTP with 10-minute expiry
	public static String generateOtp(String email) {
        String normalizedEmail = normalize(email);
        String otp = String.format("%06d", RANDOM.nextInt(1_000_000));
        OTP_STORE.put(normalizedEmail, new OtpRecord(otp, Instant.now().plus(OTP_EXPIRY)));
        return otp;
    }

	// Verify OTP and remove from store if valid or expired
	public static boolean verifyOtp(String email, String otp) {
        String normalizedEmail = normalize(email);
        OtpRecord record = OTP_STORE.get(normalizedEmail);

        if (record == null || record.isExpired()) {
            OTP_STORE.remove(normalizedEmail);
            return false;
        }
        boolean valid = record.code.equals(otp);
        if (valid) {
            OTP_STORE.remove(normalizedEmail);
        }
        return valid;
    }

	// Manually invalidate OTP for security purposes
	public static void invalidateOtp(String email) {
        OTP_STORE.remove(normalize(email));
    }

    private static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private static final class OtpRecord {
        private final String code;
        private final Instant expiresAt;

        private OtpRecord(String code, Instant expiresAt) {
            this.code = code;
            this.expiresAt = expiresAt;
        }

        private boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
