package com.dtdt.DormManager.controller;

import com.dtdt.DormManager.Main;
import com.dtdt.DormManager.controller.config.FirebaseInit;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.WriteResult;
import javafx.application.Platform;
import java.util.HashMap;
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

public class TenantDashboardController {

    private Tenant currentTenant;

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

    /**
     * This method is called by the LoginController to pass in the
     * currently logged-in tenant.
     */
    public void initData(Tenant tenant) {
        currentTenant = tenant;

        // 1. Populate Header
        tenantNameLabel.setText(currentTenant.getFullName());
        tenantIdLabel.setText(currentTenant.getUserId());
        tenantEmailLabel.setText(currentTenant.getEmail());

        // Example: Load a profile image (you would store this path in the model)
        // Image profilePic = new Image(getClass().getResourceAsStream("/com/dtdt/DormManager/img/default-profile.png"));
        // profileImageView.setImage(profilePic);

        // 2. Populate Building/Room Info (Dummy Data)
        // TODO: Get this info from the tenant's record (e.g., from a Room object)
        buildingLabel.setText("Building A");
        roomLabel.setText("Room 5210");
        contractTypeLabel.setText("6-Month Term");
        contractDatesLabel.setText("July 2025 - December 2025");

        // 3. Load dynamic content
        loadAnnouncements();
        loadMaintenanceRequests();
        // Load persisted maintenance requests from Firebase
        loadMaintenanceRequestsFromFirebase();
    }

    /**
     * FXML Action: Called when "Request Maintenance" button is clicked.
     */
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
                // create a new maintenance card in the maintenanceVBox
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

                // add to the top of the maintenance VBox
                maintenanceVBox.getChildren().add(0, card);

                // Persist to Firebase
                saveMaintenanceRequestToFirebase(result);
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * FXML Action: Called when "Logout" hyperlink is clicked.
     */
    @FXML
    private void onLogoutClick() throws IOException {
        System.out.println("Logout clicked.");
        // Reload the login screen
        Main main = new Main();
        main.changeScene("login-view.fxml");
    }

    @FXML
    private void onPaymentClick(ActionEvent event) throws IOException {
        System.out.println("Payment link clicked.");

        // 1. Load the payment-view.fxml file
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/com/dtdt/DormManager/view/payment-view.fxml"));
        Parent root = loader.load();

        // 2. Get the controller of the new scene
        PaymentController controller = loader.getController();

        // 3. Pass the *current* tenant data to the new controller
        controller.initData(this.currentTenant);

        // 4. Get the current stage (window) and change the scene
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.getScene().setRoot(root);
        stage.setTitle("Payment Registration");


    }

    // === Private Helper Methods ===

    private void loadAnnouncements() {
        // TODO: Fetch announcements from the database
        // For now, we are just using the dummy data in the FXML.
        // You would clear the dummy data first:
        // announcementsVBox.getChildren().clear();

        // Then add new ones:
        // VBox announcementCard = createAnnouncementCard("Oct 20, 2025", "Water shutoff...");
        // announcementsVBox.getChildren().add(announcementCard);
    }

    private void loadMaintenanceRequests() {
        // TODO: Fetch this tenant's requests from the database
        // For now, we are just using the dummy data in the FXML.
        // maintenanceVBox.getChildren().clear();
        // ...
    }

    /**
     * Save maintenance request to Firebase Firestore under: maintenance/{userId}/requests/{doc}
     */
    private void saveMaintenanceRequestToFirebase(com.dtdt.DormManager.controller.MaintenanceDialogController.MaintenanceResult result) {
        try {
            Firestore db = FirebaseInit.getDatabase();
            if (db == null) {
                System.err.println("Firebase database not initialized");
                return;
            }

            Map<String, Object> data = new HashMap<>();
            data.put("type", result.type);
            data.put("description", result.description);
            data.put("dateSubmitted", result.dateSubmitted);
            data.put("status", "Pending");

            // Use a generated document id to avoid collisions
            String docId = result.dateSubmitted.replace("/", "_") + "_" + System.currentTimeMillis();

            ApiFuture<WriteResult> future = db.collection("maintenance")
                    .document(currentTenant.getUserId())
                    .collection("requests")
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

    /**
     * Load maintenance requests from Firebase for the current tenant and populate the UI.
     */
    private void loadMaintenanceRequestsFromFirebase() {
        try {
            Firestore db = FirebaseInit.getDatabase();
            if (db == null) {
                System.err.println("Firebase database not initialized");
                return;
            }

            ApiFuture<QuerySnapshot> future = db.collection("maintenance")
                    .document(currentTenant.getUserId())
                    .collection("requests")
                    .get();

            future.addListener(() -> {
                try {
                    QuerySnapshot snap = future.get();
                    if (snap != null) {
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            try {
                                String type = doc.getString("type");
                                String description = doc.getString("description");
                                String dateSubmitted = doc.getString("dateSubmitted");
                                String status = doc.getString("status");

                                // Create UI card on JavaFX thread
                                Platform.runLater(() -> {
                                    javafx.scene.layout.VBox card = new javafx.scene.layout.VBox();
                                    card.setStyle("-fx-background-color: #EAEAEA; -fx-background-radius: 8; -fx-padding: 15;");
                                    card.setSpacing(5);

                                    javafx.scene.control.Label dateLabel = new javafx.scene.control.Label((status != null ? status : "Pending") + ": " + (dateSubmitted != null ? dateSubmitted : ""));
                                    dateLabel.setStyle("-fx-text-fill: #1a1a1a; -fx-font-weight: bold;");

                                    javafx.scene.control.Label typeLabel = new javafx.scene.control.Label(type != null ? type : "General Maintenance / Others");
                                    typeLabel.setStyle("-fx-text-fill: #333333; -fx-font-weight: bold;");

                                    javafx.scene.control.Label descLabel = new javafx.scene.control.Label(description != null ? description : "");
                                    descLabel.setWrapText(true);
                                    descLabel.setStyle("-fx-text-fill: #333333;");

                                    card.getChildren().addAll(dateLabel, typeLabel, descLabel);
                                    // Add to bottom so older requests are at the end; latest remain on top if desired
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