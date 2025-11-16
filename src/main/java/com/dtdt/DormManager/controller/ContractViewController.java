package com.dtdt.DormManager.controller;

import com.dtdt.DormManager.Main;
import com.dtdt.DormManager.model.Contract;
import com.dtdt.DormManager.model.Tenant;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import com.dtdt.DormManager.controller.config.FirebaseInit;
import com.dtdt.DormManager.controller.TenantProfileController;

import java.io.IOException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class ContractViewController {

    private Tenant currentTenant;
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("MMMM dd, yyyy");
    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));

    // === FXML Header Components ===
    @FXML private Label tenantNameLabel;
    @FXML private Label tenantIdLabel;
    @FXML private Label tenantEmailLabel;
    @FXML private Label buildingLabel;
    @FXML private Label roomLabel;

    // === FXML Page-Specific Components ===
    @FXML private Label contractTypeLabel;
    @FXML private Label startDateLabel;
    @FXML private Label endDateLabel;
    @FXML private Label rentAmountLabel;
    @FXML private Label dateSignedLabel;

    public void initData(Tenant tenant) {
        this.currentTenant = tenant;

        // 1. Populate Header
        tenantNameLabel.setText(currentTenant.getFullName());
        tenantIdLabel.setText(currentTenant.getUserId());
        tenantEmailLabel.setText(currentTenant.getEmail());

        // 2. Load and populate async data
        loadRoomInfo();
        loadContractInfo();
    }

    private void loadRoomInfo() {
        // TODO: Load this from the tenant's roomID
        buildingLabel.setText("Building A");
        roomLabel.setText("Room 5210");
    }

    private void loadContractInfo() {
        // TODO: Get this from currentTenant.getContractID()
        String contractId = currentTenant.getContractID();

        if (contractId == null) {
            setAllLabels("No contract found.");
            return;
        }

        Firestore db = FirebaseInit.db;
        DocumentReference docRef = db.collection("contracts").document(contractId);
        ApiFuture<DocumentSnapshot> future = docRef.get();

        future.addListener(() -> {
            try {
                DocumentSnapshot document = future.get();
                if (document.exists()) {
                    Contract contract = document.toObject(Contract.class);
                    Platform.runLater(() -> populateContractFields(contract));
                } else {
                    Platform.runLater(() -> setAllLabels("Contract not found."));
                }
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> setAllLabels("Error loading contract."));
            }
        }, Runnable::run);
    }

    private void populateContractFields(Contract contract) {
        contractTypeLabel.setText(contract.getContractType());
        rentAmountLabel.setText(currencyFormatter.format(contract.getRentAmount()));

        // Format dates
        if(contract.getStartDate() != null)
            startDateLabel.setText(dateFormatter.format(contract.getStartDate()));
        if(contract.getEndDate() != null)
            endDateLabel.setText(dateFormatter.format(contract.getEndDate()));
        if(contract.getDateSigned() != null)
            dateSignedLabel.setText(dateFormatter.format(contract.getDateSigned()));
    }

    private void setAllLabels(String text) {
        contractTypeLabel.setText(text);
        startDateLabel.setText(text);
        endDateLabel.setText(text);
        rentAmountLabel.setText(text);
        dateSignedLabel.setText(text);
    }

    // --- Navigation Methods ---

    @FXML
    private void goToDashboard(ActionEvent event) throws IOException {
        loadScene(event, "tenant-dashboard.fxml", "Tenant Dashboard");
    }

    // --- THIS IS THE NEW METHOD ---
    @FXML
    private void goToProfile(ActionEvent event) throws IOException {
        loadScene(event, "tenant-profile-view.fxml", "Profile");
    }
    // --- END NEW METHOD ---

    @FXML
    private void goToPayment(ActionEvent event) throws IOException {
        loadScene(event, "payment-view.fxml", "Payment Registration");
    }

    @FXML
    private void onLogoutClick() throws IOException {
        Main main = new Main();
        main.changeScene("login-view.fxml");
    }

    // --- THIS METHOD IS UPDATED ---
    private void loadScene(ActionEvent event, String fxmlFile, String title) throws IOException {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/com/dtdt/DormManager/view/" + fxmlFile));
        Parent root = loader.load();

        // Pass the tenant data to the next controller
        if (title.equals("Tenant Dashboard")) {
            TenantDashboardController controller = loader.getController();
            controller.initData(this.currentTenant);
        } else if (title.equals("Payment Registration")) {
            PaymentController controller = loader.getController();
            controller.initData(this.currentTenant);
        } else if (title.equals("Profile")) { // <-- ADD THIS CASE
            TenantProfileController controller = loader.getController();
            controller.initData(this.currentTenant);
        }

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.getScene().setRoot(root);
        stage.setTitle(title);
    }
}