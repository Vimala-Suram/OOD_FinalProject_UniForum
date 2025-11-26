package edu.northeastern.uniforum.forum.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.application.Platform;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;

import com.google.api.services.calendar.model.*;
import com.google.api.services.calendar.Calendar;
import com.google.api.client.util.DateTime;

import edu.northeastern.uniforum.forum.model.User;

public class MeetingController {

    @FXML private TextField subjectField;
    @FXML private DatePicker datePicker;
    @FXML private TextField timeField;
    @FXML private TextField emailField;
    @FXML private Label statusLabel;

    private User loggedInUser;
    private User targetUser;

    public void setLoggedInUser(User user) {
        this.loggedInUser = user;
    }

    public void setTargetUser(User user) {
        this.targetUser = user;
    }

    public void setAttendeeEmail(String email) {
        if (emailField != null && email != null) {
            emailField.setText(email);
        }
    }

	// Schedule Google Calendar meeting with validation and error handling
	@FXML
	public void handleScheduleMeeting() {
        if (statusLabel != null) {
            statusLabel.setText("");
            statusLabel.setStyle("-fx-text-fill: #4caf50; -fx-font-size: 14px;");
        }

        try {
            String subject = subjectField != null ? subjectField.getText().trim() : "";
            LocalDate date = datePicker != null ? datePicker.getValue() : null;
            String timeString = timeField != null ? timeField.getText().trim() : "";
            String email = emailField != null ? emailField.getText().trim() : "";

            if (subject.isEmpty()) {
                showError("Please enter a meeting subject!");
                return;
            }

            if (date == null) {
                showError("Please select a date!");
                return;
            }

            if (timeString.isEmpty()) {
                showError("Please enter a time (HH:MM format)!");
                return;
            }

            if (email.isEmpty() || !isValidEmail(email)) {
                showError("Please enter a valid email address!");
                return;
            }

            LocalTime time;
            try {
                if (timeString.matches("\\d{1,2}:\\d{2}")) {
                    time = LocalTime.parse(timeString, DateTimeFormatter.ofPattern("H:mm"));
                } else if (timeString.matches("\\d{1,2}:\\d{2}:\\d{2}")) {
                    time = LocalTime.parse(timeString);
                } else {
                    showError("Time format invalid. Use HH:MM (e.g., 14:30)");
                    return;
                }
            } catch (DateTimeParseException e) {
                showError("Time format invalid. Use HH:MM (e.g., 14:30)");
                return;
            }

            LocalDateTime startDT = LocalDateTime.of(date, time);
            if (startDT.isBefore(LocalDateTime.now())) {
                showError("Cannot schedule meetings in the past!");
                return;
            }

            LocalDateTime endDT = startDT.plusHours(1);

            if (statusLabel != null) {
                statusLabel.setText("Creating meeting...");
                statusLabel.setStyle("-fx-text-fill: #2196F3; -fx-font-size: 14px;");
            }

            new Thread(() -> {
                try {
                    System.out.println("DEBUG: Starting meeting creation...");
                    System.out.println("DEBUG: Subject: " + subject);
                    System.out.println("DEBUG: Start: " + startDT);
                    System.out.println("DEBUG: End: " + endDT);
                    System.out.println("DEBUG: Attendee: " + email);

                    String meetLink = createGoogleMeet(subject, startDT, endDT, email);

                    System.out.println("DEBUG: Meeting created successfully!");
                    System.out.println("DEBUG: Meet Link: " + meetLink);

                    Platform.runLater(() -> {
                        if (statusLabel != null) {
                            statusLabel.setText("✓ Meeting created successfully!");
                            statusLabel.setStyle("-fx-text-fill: #4caf50; -fx-font-size: 12px;");
                        }

                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Meeting Scheduled ✅");
                        alert.setHeaderText("Your meeting has been created!");
                        alert.setContentText("Google Meet Link:\n" + meetLink + 
                                           "\n\nEmail invitations sent to:\n• " + email +
                                           (loggedInUser != null && loggedInUser.getEmail() != null ? "\n• " + loggedInUser.getEmail() : "") +
                                           "\n\nCheck your Gmail for the calendar invite!");
                        alert.showAndWait();

                        try {
                            ((javafx.stage.Stage) subjectField.getScene().getWindow()).close();
                        } catch (Exception e) {
                            System.err.println("Could not close window: " + e.getMessage());
                        }
                    });
                } catch (Exception ex) {
                    System.err.println("ERROR: Failed to create meeting");
                    ex.printStackTrace();

                    Platform.runLater(() -> {
                        String errorMsg = ex.getMessage();
                        if (errorMsg == null || errorMsg.isEmpty()) {
                            errorMsg = "Unknown error occurred. Check console for details.";
                        }
                        showError("Failed to create meeting:\n" + errorMsg);
                    });
                }
            }).start();

        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Error: " + ex.getMessage());
        }
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

	// Display error message in status label with red styling
	private void showError(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
            statusLabel.setStyle("-fx-text-fill: #f44336; -fx-font-size: 14px;");
        }
    }

    private String createGoogleMeet(String summary,
                                    LocalDateTime start,
                                    LocalDateTime end,
                                    String attendeeEmail) throws Exception {

        System.out.println("DEBUG: Getting Google Calendar service...");
        Calendar service = GoogleCalendarService.getInstance();
        System.out.println("DEBUG: Calendar service obtained successfully");

        System.out.println("DEBUG: Creating event object...");
        Event event = new Event();
        event.setSummary(summary);
        event.setDescription("Meeting scheduled via UniForum");
        System.out.println("DEBUG: Event object created");

        ZoneId zoneId = ZoneId.systemDefault();
        long startMillis = start.atZone(zoneId).toInstant().toEpochMilli();
        long endMillis = end.atZone(zoneId).toInstant().toEpochMilli();

        DateTime startDateTime = new DateTime(startMillis);
        DateTime endDateTime = new DateTime(endMillis);

        EventDateTime startEventDateTime = new EventDateTime()
                .setDateTime(startDateTime)
                .setTimeZone(zoneId.getId());

        EventDateTime endEventDateTime = new EventDateTime()
                .setDateTime(endDateTime)
                .setTimeZone(zoneId.getId());

        event.setStart(startEventDateTime);
        event.setEnd(endEventDateTime);

        EventAttendee attendee = new EventAttendee().setEmail(attendeeEmail);

        List<EventAttendee> attendees = new java.util.ArrayList<>();
        attendees.add(attendee);

        if (loggedInUser != null && loggedInUser.getEmail() != null && 
            !loggedInUser.getEmail().equals(attendeeEmail)) {
            attendees.add(new EventAttendee().setEmail(loggedInUser.getEmail()));
        }

        event.setAttendees(attendees);
        System.out.println("DEBUG: Attendees added: " + attendees.size());

        System.out.println("DEBUG: Setting up Google Meet conference...");
        ConferenceData conferenceData = new ConferenceData();
        CreateConferenceRequest createRequest = new CreateConferenceRequest();
        createRequest.setRequestId("meet-" + System.currentTimeMillis());
        createRequest.setConferenceSolutionKey(new ConferenceSolutionKey().setType("hangoutsMeet"));

        conferenceData.setCreateRequest(createRequest);
        event.setConferenceData(conferenceData);
        System.out.println("DEBUG: Conference data configured");

        System.out.println("DEBUG: Inserting event into Google Calendar...");
        System.out.println("DEBUG: This may take a few seconds...");
        Event createdEvent = service.events()
                .insert("primary", event)
                .setConferenceDataVersion(1)
                .setSendUpdates("all")
                .execute();
        System.out.println("DEBUG: Event inserted successfully!");

        System.out.println("DEBUG: Extracting Google Meet link...");
        String meetLink = null;
        if (createdEvent.getConferenceData() != null && 
            createdEvent.getConferenceData().getEntryPoints() != null) {
            System.out.println("DEBUG: Conference data found, checking entry points...");
            for (EntryPoint entryPoint : createdEvent.getConferenceData().getEntryPoints()) {
                if ("video".equals(entryPoint.getEntryPointType())) {
                    meetLink = entryPoint.getUri();
                    System.out.println("DEBUG: Found video entry point: " + meetLink);
                    break;
                }
            }
        }

        if (meetLink == null || meetLink.isEmpty()) {
            System.out.println("DEBUG: No entry points found, trying hangoutLink...");
            meetLink = createdEvent.getHangoutLink();
            if (meetLink != null) {
                System.out.println("DEBUG: Found hangoutLink: " + meetLink);
            }
        }

        if (meetLink == null || meetLink.isEmpty()) {
            System.err.println("ERROR: No Google Meet link found in created event");
            System.err.println("DEBUG: Event ID: " + createdEvent.getId());
            System.err.println("DEBUG: Event HTML Link: " + createdEvent.getHtmlLink());
            throw new Exception("Failed to generate Google Meet link. Event created but no Meet link available.");
        }

        System.out.println("DEBUG: Successfully extracted Meet link: " + meetLink);
        return meetLink;
    }
}