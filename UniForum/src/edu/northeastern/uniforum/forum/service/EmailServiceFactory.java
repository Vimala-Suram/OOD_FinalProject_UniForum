package edu.northeastern.uniforum.forum.service;

public final class EmailServiceFactory {

    private static EmailService INSTANCE;

    private EmailServiceFactory() {
    }

    public static EmailService getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ConsoleEmailService();
        }
        return INSTANCE;
    }

    public static void setCustomService(EmailService service) {
        INSTANCE = service;
    }
}
