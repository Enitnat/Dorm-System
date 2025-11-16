package com.dtdt.DormManager.controller.admin;

import com.dtdt.DormManager.controller.config.FirebaseInit;
import com.dtdt.DormManager.model.MaintenanceRequest;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;

public class MaintenanceViewController {

    @FXML private VBox requestsContainer;

    private final Firestore db = FirebaseInit.db;
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("MMMM dd, yyyy");

    @FXML
    public void initialize() {
        loadMaintenanceRequests();
    }

    private void loadMaintenanceRequests() {
        requestsContainer.getChildren().clear();

        ApiFuture<QuerySnapshot> future = db.collection("maintenanceRequests")
                .whereEqualTo("status", "Pending") // Only show pending requests
                .orderBy("dateSubmitted", Query.Direction.ASCENDING) // Show oldest first
                .get();

        future.addListener(() -> {
            try {
                List<QueryDocumentSnapshot> documents = future.get().getDocuments();
                Platform.runLater(() -> {
                    if (documents.isEmpty()) {
                        requestsContainer.getChildren().add(new Label("No pending maintenance requests."));
                    } else {
                        for (QueryDocumentSnapshot document : documents) {
                            MaintenanceRequest request = document.toObject(MaintenanceRequest.class);
                            requestsContainer.getChildren().add(createRequestCard(request));
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, Runnable::run);
    }

    /**
     * Creates a UI card for a single maintenance request.
     */
    private VBox createRequestCard(MaintenanceRequest request) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 20; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 2);");

        // --- Card Header ---
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(request.getType());
        title.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button completeButton = new Button("Mark as Completed");
        completeButton.setStyle("-fx-background-color: #E8F5E9; -fx-text-fill: #2E7D32;");
        completeButton.setOnAction(e -> handleMarkAsCompleted(request, card));

        header.getChildren().addAll(title, spacer, completeButton);

        // --- Card Info ---
        String dateString = (request.getDateSubmitted() != null) ?
                dateFormatter.format(request.getDateSubmitted()) : "N/A";

        Label dateLabel = new Label("Submitted: " + dateString);
        dateLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #666;");

        // TODO: You'll need to fetch tenant/room name from these IDs
        Label tenantLabel = new Label("Tenant ID: " + request.getTenantId());
        Label roomLabel = new Label("Room ID: " + request.getRoomId());


        Label descriptionLabel = new Label(request.getIssueDescription());
        descriptionLabel.setWrapText(true);
        descriptionLabel.setMaxWidth(600);
        descriptionLabel.setStyle("-fx-font-size: 13; -fx-text-fill: #333;");

        card.getChildren().addAll(header, dateLabel, new Separator(), tenantLabel, roomLabel, descriptionLabel);
        return card;
    }

    /**
     * Handles the "Mark as Completed" button click.
     */
    private void handleMarkAsCompleted(MaintenanceRequest request, VBox cardToRemove) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Action");
        alert.setHeaderText("Complete Maintenance Request");
        alert.setContentText("Are you sure you want to mark this request as completed?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Update the status in Firebase
            db.collection("maintenanceRequests").document(request.getId())
                    .update("status", "Completed")
                    .addListener(() -> {
                        // Remove the card from the UI
                        Platform.runLater(() -> {
                            requestsContainer.getChildren().remove(cardToRemove);
                        });
                    }, Runnable::run);
        }
    }
}