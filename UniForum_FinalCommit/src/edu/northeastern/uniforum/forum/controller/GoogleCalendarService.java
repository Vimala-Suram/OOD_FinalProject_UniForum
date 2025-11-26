package edu.northeastern.uniforum.forum.controller;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Collections;

public class GoogleCalendarService {

    private static final String APPLICATION_NAME = "Google Meet Scheduler";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final java.io.File TOKENS_DIRECTORY = new java.io.File("tokens");

    private static Calendar serviceInstance;

    private static final String CREDENTIALS_FILE_PATH = "client_secret.json";

    private static String getCredentialsFilePath() {
        java.io.File file = new java.io.File(CREDENTIALS_FILE_PATH);
        if (file.exists() && file.isFile()) {
            return file.getAbsolutePath();
        }

        file = new java.io.File("src/" + CREDENTIALS_FILE_PATH);
        if (file.exists() && file.isFile()) {
            return file.getAbsolutePath();
        }

        file = new java.io.File("bin/" + CREDENTIALS_FILE_PATH);
        if (file.exists() && file.isFile()) {
            return file.getAbsolutePath();
        }

        java.io.InputStream resourceStream = GoogleCalendarService.class.getResourceAsStream("/" + CREDENTIALS_FILE_PATH);
        if (resourceStream != null) {
            try {
                java.io.File tempFile = java.io.File.createTempFile("client_secret", ".json");
                tempFile.deleteOnExit();
                try (java.io.FileOutputStream out = new java.io.FileOutputStream(tempFile)) {
                    resourceStream.transferTo(out);
                }
                return tempFile.getAbsolutePath();
            } catch (Exception e) {
            }
        }

        return CREDENTIALS_FILE_PATH;
    }

    public static Calendar getInstance() throws Exception {
        if (serviceInstance != null) return serviceInstance;

        var httpTransport = GoogleNetHttpTransport.newTrustedTransport();

        String credentialsPath = getCredentialsFilePath();
        var in = new FileInputStream(credentialsPath);
        var clientSecrets = com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        var flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, clientSecrets, Collections.singleton(CalendarScopes.CALENDAR))
                .setDataStoreFactory(new FileDataStoreFactory(TOKENS_DIRECTORY))
                .setAccessType("offline")
                .build();

        Credential credential = new com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp(
                flow, new com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver())
                .authorize("user");

        serviceInstance = new Calendar.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();

        return serviceInstance;
    }
}
