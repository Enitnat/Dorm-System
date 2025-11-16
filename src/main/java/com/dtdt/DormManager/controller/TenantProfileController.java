package com.dtdt.DormManager.controller;

import com.dtdt.DormManager.Main;
import com.dtdt.DormManager.model.Tenant;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import com.dtdt.DormManager.controller.ChangePasswordDialogController;
import com.dtdt.DormManager.controller.ContractViewController;


import java.io.IOException;

public class TenantProfileController {

    @FXML private Label tenantNameLabel;
    @FXML private Label tenantIdLabel;
    @FXML private Label tenantEmailLabel;

    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private TextField studentIdField;
    @FXML private TextField buildingField;
    @FXML private TextField roomField;

    private Tenant currentTenant; // This is the class-level variable

    public void initData(Tenant tenant) { // 'tenant' is the parameter
        this.currentTenant = tenant; // Save the parameter to the class variable

        // Populate header
        tenantNameLabel.setText(currentTenant.getFullName()); // Use the class variable
        tenantIdLabel.setText(currentTenant.getUserId());
        tenantEmailLabel.setText(currentTenant.getEmail());

        // Populate fields
        nameField.setText(currentTenant.getFullName());
        emailField.setText(currentTenant.getEmail());
        studentIdField.setText(currentTenant.getUserId());

        buildingField.setText(currentTenant.getRoomID() != null ? currentTenant.getRoomID() : "Not Assigned");
        roomField.setText("N/A");
    }

    // --- Navigation ---
    @FXML
    private void goToDashboard(ActionEvent event) throws IOException {
        loadScene(event, "/com/dtdt/DormManager/view/tenant-dashboard.fxml", "Tenant Dashboard");
    }

    @FXML
    private void goToPayment(ActionEvent event) throws IOException {
        loadScene(event, "/com/dtdt/DormManager/view/payment-view.fxml", "Payment Registration");
    }

    @FXML
    private void onLogoutClick() throws IOException {
        Main main = new Main();
        main.changeScene("login-view.fxml");
    }

    // --- Main Feature ---

    @FXML
    private void onChangePasswordClick(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("/com/dtdt/DormManager/view/change-password-dialog.fxml"));
            Parent root = loader.load();

            // This line needs the import
            ChangePasswordDialogController dialogController = loader.getController();
            // This line needs the import to work
            dialogController.initData(currentTenant);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Change Password");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(((Node) event.getSource()).getScene().getWindow());
            dialogStage.setScene(new Scene(root));
            dialogStage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onViewContractClick(ActionEvent event) throws IOException {
        loadScene(event, "/com/dtdt/DormManager/view/contract-view.fxml", "My Contract");
    }

    // Helper method to load new scenes
    private void loadScene(ActionEvent event, String fxmlFile, String title) throws IOException {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource(fxmlFile));
        Parent root = loader.load();

        // --- SYNTAX FIXED HERE ---
        if (title.equals("Tenant Dashboard")) {
            com.dtdt.DormManager.controller.TenantDashboardController controller = loader.getController();
            controller.initData(currentTenant);
        } else if (title.equals("Payment Registration")) {
            com.dtdt.DormManager.controller.PaymentController controller = loader.getController();
            controller.initData(currentTenant);
        } else if (title.equals("My Contract")) {
            ContractViewController controller = loader.getController(); // This line needs the import
            controller.initData(currentTenant); // This line needs the import to work
        }
        // --- END FIX ---

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.getScene().setRoot(root);
        stage.setTitle(title);
    }
}