package edu.northeastern.uniforum.forum.service;

public interface EmailService {

    void sendOtpEmail(String recipientEmail, String otp) throws Exception;
}
