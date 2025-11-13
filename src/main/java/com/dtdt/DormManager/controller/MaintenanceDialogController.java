package com.dtdt.DormManager.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class MaintenanceDialogController {

    @FXML private ComboBox<String> typesComboBox;
    @FXML private TextArea descriptionArea;
    @FXML private Button submitButton;
    @FXML private Button cancelButton;

    private Stage dialogStage;
    private MaintenanceResult result;

    @FXML
    public void initialize() {
        // populate types
        typesComboBox.getItems().addAll(
                "Plumbing",
                "Electrical",
                "Air Conditioning / Ventilation",
                "Furniture / Fixtures",
                "Cleaning / Sanitation",
                "Pest Control",
                "Internet / Connectivity",
                "Appliance Repair",
                "General Maintenance / Others"
        );
        typesComboBox.setValue("General Maintenance / Others");
    }

    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    public MaintenanceResult getResult() {
        return result;
    }

    @FXML
    private void onCancel() {
        this.result = null;
        if (dialogStage != null) dialogStage.close();
    }

    @FXML
    private void onSubmit() {
        String type = (typesComboBox.getValue() != null) ? typesComboBox.getValue() : "General Maintenance / Others";
        String description = (descriptionArea.getText() != null) ? descriptionArea.getText().trim() : "";

        // Basic validation: ensure description not empty
        if (description.isEmpty()) {
            descriptionArea.requestFocus();
            return;
        }

        String dateSubmitted = LocalDate.now().format(DateTimeFormatter.ofPattern("d/M/yyyy"));

        this.result = new MaintenanceResult(type, description, dateSubmitted);
        if (dialogStage != null) dialogStage.close();
    }

    public static class MaintenanceResult {
        public final String type;
        public final String description;
        public final String dateSubmitted;

        public MaintenanceResult(String type, String description, String dateSubmitted) {
            this.type = type;
            this.description = description;
            this.dateSubmitted = dateSubmitted;
        }

        @Override
        public String toString() {
            return "MaintenanceResult{" + "type='" + type + '\'' + ", description='" + description + '\'' + ", dateSubmitted='" + dateSubmitted + '\'' + '}';
        }
    }
}
