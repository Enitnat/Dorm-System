package com.dtdt.DormManager.controller;

import com.dtdt.DormManager.Main;
import com.dtdt.DormManager.controller.config.FirebaseInit;
import com.dtdt.DormManager.controller.TenantProfileController;
import com.dtdt.DormManager.model.Announcement;
import com.dtdt.DormManager.model.Contract;
import com.dtdt.DormManager.model.Room;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import javafx.application.Platform;
import java.util.Date;
import java.util.UUID;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import com.dtdt.DormManager.model.Tenant;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;

import java.io.IOException;
import java.text.SimpleDateFormat;

public class TenantDashboardController {

    private Tenant currentTenant;
    private final Firestore db = FirebaseInit.db;
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("MMMM dd, yyyy");

    // === FXML Components ===
    @FXML private ImageView profileImageView;
    @FXML private Label tenantNameLabel;
    @FXML private Label tenantIdLabel;
    @FXML private Label tenantEmailLabel;
    @FXML private Label buildingLabel;
    @FXML private Label roomLabel;
    @FXML private Label contractTypeLabel;
    @FXML private Label contractDatesLabel;
    @FXML private Button requestMaintenanceButton;
    @FXML private VBox announcementsVBox;
    @FXML private VBox maintenanceVBox;

    public void initData(Tenant tenant) {
        currentTenant = tenant;

        // 1. Populate Header
        tenantNameLabel.setText(currentTenant.getFullName());
        tenantIdLabel.setText(currentTenant.getUserId());
        tenantEmailLabel.setText(currentTenant.getEmail());

        // 2. Load dynamic/linked data
        loadLinkedData();
        loadAnnouncements();
        loadMaintenanceRequestsFromFirebase();
    }

    private void loadLinkedData() {
        // --- 1. Load Room Info ---
        if (currentTenant.getRoomID() != null) {
            DocumentReference roomRef = db.collection("rooms").document(currentTenant.getRoomID());

            // --- FIX 1: Changed 'future ->' to '() ->' ---
            roomRef.get().addListener(() -> {
                try {
                    DocumentSnapshot doc = roomRef.get().get(); // Get result
                    if (doc.exists()) {
                        Room room = doc.toObject(Room.class);
                        Platform.runLater(() -> {
                            buildingLabel.setText(room.getBuildingName());
                            roomLabel.setText("Room " + room.getRoomNumber());
                        });
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }, Runnable::run);
        } else {
            buildingLabel.setText("N/A");
            roomLabel.setText("Not Assigned");
        }

        // --- 2. Load Contract Info ---
        if (currentTenant.getContractID() != null) {
            DocumentReference contractRef = db.collection("contracts").document(currentTenant.getContractID());

            // --- FIX 2: Changed 'future ->' to '() ->' ---
            contractRef.get().addListener(() -> {
                try {
                    DocumentSnapshot doc = contractRef.get().get(); // Get result
                    if (doc.exists()) {
                        Contract contract = doc.toObject(Contract.class);
                        Platform.runLater(() -> {
                            contractTypeLabel.setText(contract.getContractType());
                            String dates = dateFormatter.format(contract.getStartDate()) + " - " +
                                    dateFormatter.format(contract.getEndDate());
                            contractDatesLabel.setText(dates);
                        });
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }, Runnable::run);
        } else {
            contractTypeLabel.setText("No Contract");
            contractDatesLabel.setText("N/A");
        }
    }

    private void loadAnnouncements() {
        announcementsVBox.getChildren().clear();

        ApiFuture<QuerySnapshot> future = db.collection("announcements")
                .orderBy("datePosted", Query.Direction.DESCENDING)
                .limit(5)
                .get();

        future.addListener(() -> {
            try {
                List<QueryDocumentSnapshot> documents = future.get().getDocuments();
                Platform.runLater(() -> {
                    if (documents.isEmpty()) {
                        announcementsVBox.getChildren().add(new Label("No announcements right now."));
                        return;
                    }
                    for (QueryDocumentSnapshot document : documents) {
                        Announcement ann = document.toObject(Announcement.class);
                        announcementsVBox.getChildren().add(createAnnouncementCard(ann));
                    }
                });
            } catch (Exception e) { e.printStackTrace(); }
        }, Runnable::run);
    }

    private VBox createAnnouncementCard(Announcement ann) {
        VBox card = new VBox(5);
        card.setStyle("-fx-border-color: #E0E0E0; -fx-border-width: 0 0 1 0; -fx-padding: 0 0 15 0;");

        String dateString = (ann.getDatePosted() != null) ?
                dateFormatter.format(ann.getDatePosted()) : "Recently";

        Label dateLabel = new Label(dateString);
        dateLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #666;");

        Label titleLabel = new Label(ann.getTitle());
        titleLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");

        Label contentLabel = new Label(ann.getContent());
        contentLabel.setWrapText(true);

        card.getChildren().addAll(dateLabel, titleLabel, contentLabel);
        return card;
    }

    @FXML
    private void onRequestMaintenanceClick() {
        System.out.println("Request Maintenance button clicked.");
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("/com/dtdt/DormManager/view/maintenance-dialog.fxml"));
            Parent root = loader.load();

            MaintenanceDialogController controller = loader.getController();

            Stage owner = (Stage) requestMaintenanceButton.getScene().getWindow();
            Stage dialog = new Stage();
            dialog.initOwner(owner);
            dialog.setTitle("Request Maintenance");
            dialog.setScene(new Scene(root));
            controller.setDialogStage(dialog);

            dialog.showAndWait();

            MaintenanceDialogController.MaintenanceResult result = controller.getResult();
            if (result != null) {
                java.time.LocalDate now = java.time.LocalDate.now();
                java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("d/M/yyyy");
                String dateText = now.format(fmt);

                javafx.scene.layout.VBox card = new javafx.scene.layout.VBox();
                card.setStyle("-fx-background-color: #EAEAEA; -fx-background-radius: 8; -fx-padding: 15;");
                card.setSpacing(5);

                javafx.scene.control.Label dateLabel = new javafx.scene.control.Label("Pending: " + dateText);
                dateLabel.setStyle("-fx-text-fill: #1a1a1a; -fx-font-weight: bold;");

                javafx.scene.control.Label descLabel = new javafx.scene.control.Label(result.description);
                descLabel.setWrapText(true);
                descLabel.setStyle("-fx-text-fill: #333333;");

                javafx.scene.control.Label typeLabel = new javafx.scene.control.Label(result.type);
                typeLabel.setStyle("-fx-text-fill: #333333; -fx-font-weight: bold;");

                card.getChildren().addAll(dateLabel, typeLabel, descLabel);
                maintenanceVBox.getChildren().add(0, card);
                saveMaintenanceRequestToFirebase(result);
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void onLogoutClick() throws IOException {
        System.out.println("Logout clicked.");
        Main main = new Main();
        main.changeScene("login-view.fxml");
    }

    @FXML
    private void onPaymentClick(ActionEvent event) throws IOException {
        System.out.println("Payment link clicked.");
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/com/dtdt/DormManager/view/payment-view.fxml"));
        Parent root = loader.load();
        PaymentController controller = loader.getController();
        controller.initData(this.currentTenant);
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.getScene().setRoot(root);
        stage.setTitle("Payment Registration");
    }

    @FXML
    private void onProfileClick(ActionEvent event) throws IOException {
        System.out.println("Profile link clicked.");
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/com/dtdt/DormManager/view/tenant-profile-view.fxml"));
        Parent root = loader.load();
        TenantProfileController controller = loader.getController();
        controller.initData(this.currentTenant);
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.getScene().setRoot(root);
        stage.setTitle("Tenant Profile");
    }

    private void saveMaintenanceRequestToFirebase(com.dtdt.DormManager.controller.MaintenanceDialogController.MaintenanceResult result) {
        try {
            Firestore db = FirebaseInit.db;
            if (db == null) {
                System.err.println("Firebase database not initialized");
                return;
            }

            Map<String, Object> data = new HashMap<>();
            data.put("type", result.type);
            data.put("description", result.description);
            data.put("dateSubmittedString", result.dateSubmitted);
            data.put("dateSubmitted", new java.util.Date()); // Use java.util.Date
            data.put("status", "Pending");
            data.put("tenantId", currentTenant.getUserId());
            data.put("roomId", currentTenant.getRoomID());

            String docId = UUID.randomUUID().toString(); // This line needs java.util.UUID

            ApiFuture<WriteResult> future = db.collection("maintenanceRequests")
                    .document(docId)
                    .set(data);

            future.addListener(() -> {
                try {
                    future.get();
                    System.out.println("Maintenance request saved to Firebase: " + result);
                } catch (Exception e) {
                    System.err.println("Error saving maintenance request: " + e.getMessage());
                    e.printStackTrace();
                }
            }, Executors.newSingleThreadExecutor());
        } catch (Exception e) {
            System.err.println("Error in saveMaintenanceRequestToFirebase: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadMaintenanceRequestsFromFirebase() {
        try {
            Firestore db = FirebaseInit.db;
            if (db == null) {
                System.err.println("Firebase database not initialized");
                return;
            }

            ApiFuture<QuerySnapshot> future = db.collection("maintenanceRequests")
                    .whereEqualTo("tenantId", currentTenant.getUserId())
                    .orderBy("dateSubmitted", Query.Direction.DESCENDING)
                    .get();

            future.addListener(() -> {
                try {
                    QuerySnapshot snap = future.get();
                    if (snap != null) {
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            try {
                                String type = doc.getString("type");
                                String description = doc.getString("description");
                                String status = doc.getString("status");

                                // This line needs java.util.Date
                                Date date = doc.getDate("dateSubmitted");
                                String dateText = (date != null) ?
                                        dateFormatter.format(date) :
                                        doc.getString("dateSubmittedString");

                                Platform.runLater(() -> {
                                    VBox card = new VBox();
                                    card.setStyle("-fx-background-color: #EAEAEA; -fx-background-radius: 8; -fx-padding: 15;");
                                    card.setSpacing(5);

                                    Label dateLabel = new Label((status != null ? status : "Pending") + ": " + (dateText != null ? dateText : ""));
                                    dateLabel.setStyle("-fx-text-fill: #1a1a1a; -fx-font-weight: bold;");

                                    Label typeLabel = new Label(type != null ? type : "General Maintenance / Others");
                                    typeLabel.setStyle("-fx-text-fill: #333333; -fx-font-weight: bold;");

                                    Label descLabel = new Label(description != null ? description : "");
                                    descLabel.setWrapText(true);
                                    descLabel.setStyle("-fx-text-fill: #333333;");

                                    card.getChildren().addAll(dateLabel, typeLabel, descLabel);
                                    maintenanceVBox.getChildren().add(card);
                                });

                            } catch (Exception e) {
                                System.err.println("Error parsing maintenance doc: " + e.getMessage());
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error loading maintenance requests: " + e.getMessage());
                    e.printStackTrace();
                }
            }, Executors.newSingleThreadExecutor());

        } catch (Exception e) {
            System.err.println("Error in loadMaintenanceRequestsFromFirebase: " + e.getMessage());
            e.printStackTrace();
        }
    }
}