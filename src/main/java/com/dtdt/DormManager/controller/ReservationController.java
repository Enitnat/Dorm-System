package com.dtdt.DormManager.controller;

import com.dtdt.DormManager.Main;
import com.dtdt.DormManager.controller.config.FirebaseInit;
import com.dtdt.DormManager.model.Reservation;
import com.google.cloud.firestore.Firestore;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import java.io.IOException;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

public class ReservationController {

    // === Basic Info ===
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField studentIdField;
    @FXML private TextField emailField;
    @FXML private ComboBox<String> currentYearBox;
    @FXML private ComboBox<String> genderBox;

    // === Contract Info ===
    @FXML private ComboBox<String> contractTypeBox;
    @FXML private DatePicker moveInDatePicker;
    
    // === Other Components ===
    @FXML private Button submitReservationButton;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;

    @FXML
    public void initialize() {
        // Populate ComboBoxes
        currentYearBox.setItems(FXCollections.observableArrayList(
                "1st Year", "2nd Year", "3rd Year", "4th Year", "5th Year"
        ));
        genderBox.setItems(FXCollections.observableArrayList("Male", "Female"));
        contractTypeBox.setItems(FXCollections.observableArrayList(
                "Full Semester (6 months)", "Monthly"
        ));
        
        // Disable submit button initially
        submitReservationButton.setDisable(true);
        
        // Hide labels
        errorLabel.setVisible(false);
        successLabel.setVisible(false);

        // Add listeners to all fields to validate form
        firstNameField.textProperty().addListener((obs, o, n) -> validateForm());
        lastNameField.textProperty().addListener((obs, o, n) -> validateForm());
        studentIdField.textProperty().addListener((obs, o, n) -> validateForm());
        emailField.textProperty().addListener((obs, o, n) -> validateForm());
        currentYearBox.valueProperty().addListener((obs, o, n) -> validateForm());
        genderBox.valueProperty().addListener((obs, o, n) -> validateForm());
        contractTypeBox.valueProperty().addListener((obs, o, n) -> validateForm());
        moveInDatePicker.valueProperty().addListener((obs, o, n) -> validateForm());
    }

    private void validateForm() {
        // Basic validation: checks if any field is empty
        boolean isValid = !firstNameField.getText().trim().isEmpty()
                && !lastNameField.getText().trim().isEmpty()
                && !studentIdField.getText().trim().isEmpty()
                && !emailField.getText().trim().isEmpty()
                && currentYearBox.getValue() != null
                && genderBox.getValue() != null
                && contractTypeBox.getValue() != null
                && moveInDatePicker.getValue() != null;
        
        submitReservationButton.setDisable(!isValid);
    }

    @FXML
    protected void onSubmitReservationClick() {
        try {
            // 1. Create new Reservation object
            Reservation newReservation = new Reservation();
            newReservation.setFirstName(firstNameField.getText().trim());
            newReservation.setLastName(lastNameField.getText().trim());
            newReservation.setStudentId(studentIdField.getText().trim());
            newReservation.setEmail(emailField.getText().trim());
            newReservation.setGender(genderBox.getValue());
            newReservation.setCurrentYear(currentYearBox.getValue());
            newReservation.setContractType(contractTypeBox.getValue());
            // Convert LocalDate from DatePicker to java.util.Date
            newReservation.setPreferredMoveInDate(
                Date.from(moveInDatePicker.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant())
            );
            newReservation.setStatus("Pending"); // Default status

            // 2. Save to 'reservations' collection in Firebase
            Firestore db = FirebaseInit.db;
            String reservationId = UUID.randomUUID().toString();
            db.collection("reservations").document(reservationId).set(newReservation).get();

            // 3. Show success and clear form
            System.out.println("Reservation submitted successfully: " + reservationId);
            successLabel.setText("Reservation submitted! An admin will review it.");
            successLabel.setVisible(true);
            errorLabel.setVisible(false);
            clearForm();

        } catch (Exception e) {
            System.err.println("Error submitting reservation: " + e.getMessage());
            e.printStackTrace();
            errorLabel.setText("An error occurred. Please try again.");
            errorLabel.setVisible(true);
            successLabel.setVisible(false);
        }
    }
    
    private void clearForm() {
        firstNameField.clear();
        lastNameField.clear();
        studentIdField.clear();
        emailField.clear();
        currentYearBox.setValue(null);
        genderBox.setValue(null);
        contractTypeBox.setValue(null);
        moveInDatePicker.setValue(null);
    }

    @FXML
    protected void goToSignIn(MouseEvent event) throws IOException {
        Main main = new Main();
        main.changeScene("login-view.fxml");
    }
}