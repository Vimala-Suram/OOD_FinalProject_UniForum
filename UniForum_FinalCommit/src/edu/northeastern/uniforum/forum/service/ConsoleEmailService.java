package edu.northeastern.uniforum.forum.service;

public class ConsoleEmailService implements EmailService {

	// Debug implementation that prints OTP to console
	@Override
	public void sendOtpEmail(String recipientEmail, String otp) {
        System.out.printf("DEBUG: OTP %s sent to %s%n", otp, recipientEmail);
    }
}
