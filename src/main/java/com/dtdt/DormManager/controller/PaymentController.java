package com.dtdt.DormManager.controller;

import com.dtdt.DormManager.Main;
import com.dtdt.DormManager.model.Tenant;
import com.dtdt.DormManager.controller.config.FirebaseInit;
import com.google.cloud.firestore.Firestore;
import javafx.fxml.FXML;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.FileChooser;

import java.io.IOException;
import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;
import com.google.api.core.ApiFuture;
import java.util.concurrent.Executors;

public class PaymentController {

    private Tenant currentTenant;

    // === FXML Header Components ===
    @FXML private Label tenantNameLabel;
    @FXML private Label tenantIdLabel;
    @FXML private Label tenantEmailLabel;
    @FXML private Label buildingLabel;
    @FXML private Label roomLabel;
    @FXML private Label contractTypeLabel;
    @FXML private Label contractDatesLabel;

    // === FXML Page-Specific Components ===
    @FXML private ComboBox<String> receiptComboBox;
    @FXML private VBox billingHistoryVBox;
    @FXML private Label validationMessageLabel;

    // payment method buttons
    @FXML private Button gcashButton;
    @FXML private Button bpiButton;
    @FXML private Button bdoButton;
    @FXML private Button metroButton;
    @FXML private Button makePaymentButton;

    // currently selected payment method (text)
    private String selectedMethod = null;
    
    // currently selected month
    private String selectedMonth = null;
    
    // Map to track month boxes and their status (UNPAID or PAID)
    private Map<String, HBox> monthButtons = new HashMap<>();
    private Map<String, String> monthStatus = new HashMap<>();
    
    // Map to store payment details for each month (for PDF generation and Firebase)
    private Map<String, PaymentDetails> paymentRecords = new HashMap<>();

    /**
     * Handler for the payment option buttons in the main view. Makes clicked button black with white text,
     * and resets the others to white with black text.
     */
    @FXML
    private void selectPaymentMethod(ActionEvent event) {
        Object src = event.getSource();
        // Reset all to default
        resetPaymentButtonStyle(gcashButton);
        resetPaymentButtonStyle(bpiButton);
        resetPaymentButtonStyle(bdoButton);
        resetPaymentButtonStyle(metroButton);

        // Style the clicked button as selected
        if (src == gcashButton) {
            setSelectedStyle(gcashButton);
            selectedMethod = "Gcash";
        } else if (src == bpiButton) {
            setSelectedStyle(bpiButton);
            selectedMethod = "BPI";
        } else if (src == bdoButton) {
            setSelectedStyle(bdoButton);
            selectedMethod = "BDO";
        } else if (src == metroButton) {
            setSelectedStyle(metroButton);
            selectedMethod = "Metrobank";
        }
        
        // Update button state and validation
        updateMakePaymentButtonState();
    }

    private void resetPaymentButtonStyle(Button btn) {
        if (btn == null) return;
        btn.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-border-color: #E0E0E0;");
    }

    private void setSelectedStyle(Button btn) {
        if (btn == null) return;
        btn.setStyle("-fx-background-color: black; -fx-text-fill: white;");
    }
    
    /**
     * Create and populate month tabs for billing
     */
    private void createMonthTabs() {
        // The month tabs are now in the scrollpane as HBox items
        // We need to make them clickable by adding event handlers
        
        String[] months = {"July 2025", "August 2025", "September 2025", "October 2025", "November 2025", "December 2025"};
        
        // Get all HBox children from the billingHistoryVBox
        for (int i = 0; i < billingHistoryVBox.getChildren().size(); i++) {
            if (billingHistoryVBox.getChildren().get(i) instanceof HBox) {
                HBox monthBox = (HBox) billingHistoryVBox.getChildren().get(i);
                String month = months[i];
                
                // Check if status was already loaded from Firebase, otherwise default to UNPAID
                String status = monthStatus.getOrDefault(month, "UNPAID");
                
                monthButtons.put(month, monthBox);
                
                // Update the UI based on status
                updateMonthBoxUI(month, monthBox, status);
            }
        }
    }
    
    /**
     * Update the UI of a month box based on its status
     */
    private void updateMonthBoxUI(String month, HBox monthBox, String status) {
        monthStatus.put(month, status);
        
        if (status.equals("PAID")) {
            // Disable and gray out paid months
            monthBox.setDisable(true);
            monthBox.setStyle("-fx-border-color: #E0E0E0; -fx-border-width: 0 0 1 0; -fx-padding: 0 0 15 0; -fx-cursor: default; -fx-opacity: 0.7;");
            
            // Update the status label in the HBox
            updateStatusLabel(monthBox, "PAID", "#008000");
        } else {
            // Enable UNPAID months
            monthBox.setDisable(false);
            monthBox.setStyle("-fx-border-color: #E0E0E0; -fx-border-width: 0 0 1 0; -fx-padding: 0 0 15 0; -fx-cursor: hand;");
            monthBox.setOnMouseClicked(event -> selectMonth(month, monthBox));
            
            // Update the status label in the HBox
            updateStatusLabel(monthBox, "UNPAID", "#D8000C");
        }
    }
    
    /**
     * Update the status label inside a month HBox
     */
    private void updateStatusLabel(HBox monthBox, String status, String color) {
        for (javafx.scene.Node node : monthBox.getChildren()) {
            if (node instanceof VBox) {
                VBox vbox = (VBox) node;
                boolean foundStatusHBox = false;
                
                // Iterate through VBox children to find the status HBox
                for (javafx.scene.Node child : vbox.getChildren()) {
                    if (child instanceof HBox) {
                        HBox statusHBox = (HBox) child;
                        
                        // Check if first child is the "Current Status:" label
                        if (statusHBox.getChildren().size() >= 2) {
                            javafx.scene.Node firstNode = statusHBox.getChildren().get(0);
                            if (firstNode instanceof Label) {
                                Label firstLabel = (Label) firstNode;
                                // Only process the status HBox (the one with "Current Status:" label)
                                if (firstLabel.getText().equals("Current Status:")) {
                                    javafx.scene.Node statusNode = statusHBox.getChildren().get(1);
                                    if (statusNode instanceof Label) {
                                        Label statusLabel = (Label) statusNode;
                                        statusLabel.setText(status);
                                        statusLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
                                    }
                                    foundStatusHBox = true;
                                    break; // Found and updated the status, exit
                                }
                            }
                        }
                    }
                    if (foundStatusHBox) break;
                }
            }
        }
    }
    
    /**
     * Handle month selection from the HBox items
     */
    private void selectMonth(String month, HBox monthBox) {
        // Reset all month boxes to default style
        for (HBox box : monthButtons.values()) {
            if (box != null && !box.isDisabled()) {
                box.setStyle("-fx-border-color: #E0E0E0; -fx-border-width: 0 0 1 0; -fx-padding: 0 0 15 0; -fx-cursor: hand;");
            }
        }
        
        // Highlight the selected month
        if (monthBox != null) {
            monthBox.setStyle("-fx-border-color: #1A1A1A; -fx-border-width: 0 0 3 0; -fx-padding: 0 0 15 0; -fx-background-color: #F0F0F0; -fx-cursor: hand;");
            selectedMonth = month;
        }
        
        // Update button state and validation
        updateMakePaymentButtonState();
    }
    
    /**
     * Update the state of the Make Payment button based on selections
     */
    private void updateMakePaymentButtonState() {
        if (selectedMethod != null && selectedMonth != null) {
            // Both selected - enable the button
            makePaymentButton.setDisable(false);
            makePaymentButton.setStyle("-fx-background-color: #1A1A1A; -fx-text-fill: WHITE; -fx-cursor: hand;");
            validationMessageLabel.setText("");
            validationMessageLabel.setStyle("");
        } else {
            // Missing selection - disable the button
            makePaymentButton.setDisable(true);
            makePaymentButton.setStyle("-fx-background-color: #D3D3D3; -fx-text-fill: #666666; -fx-cursor: default;");
            
            // Show appropriate message
            if (selectedMethod == null && selectedMonth == null) {
                validationMessageLabel.setText("⚠ Please choose a payment method and a month to pay for");
                validationMessageLabel.setStyle("-fx-text-fill: #D8000C; -fx-font-weight: bold;");
            } else if (selectedMethod == null) {
                validationMessageLabel.setText("⚠ Please choose a payment method");
                validationMessageLabel.setStyle("-fx-text-fill: #D8000C; -fx-font-weight: bold;");
            } else {
                validationMessageLabel.setText("⚠ Please choose a month to pay for");
                validationMessageLabel.setStyle("-fx-text-fill: #D8000C; -fx-font-weight: bold;");
            }
        }
    }
    
    /**
     * Mark a month as PAID by updating its status and disabling it
     */
    private void markMonthAsPaid(String month) {
        HBox monthBox = monthButtons.get(month);
        if (monthBox != null) {
            // Update UI and status using the centralized method
            updateMonthBoxUI(month, monthBox, "PAID");
            
            // Also save the status to Firebase
            saveMonthStatusToFirebase(month, "PAID");
        }
    }
    
    /**
     * Save month status to Firebase
     */
    private void saveMonthStatusToFirebase(String month, String status) {
        try {
            Firestore db = FirebaseInit.getDatabase();
            if (db == null) {
                System.err.println("Firebase database not initialized");
                return;
            }
            
            // Create a map with just the status
            Map<String, Object> statusMap = new HashMap<>();
            statusMap.put("status", status);
            
            // Save status to: payments/[userId]/status/[month]
            String documentId = month.replace(" ", "_").replace(",", "");
            ApiFuture<com.google.cloud.firestore.WriteResult> future = db.collection("payments")
                    .document(currentTenant.getUserId())
                    .collection("status")
                    .document(documentId)
                    .set(statusMap);
            
            // This will execute asynchronously
            future.addListener(() -> {
                try {
                    future.get();
                    System.out.println("Payment status saved to Firebase for: " + month + " -> " + status);
                } catch (Exception e) {
                    System.err.println("Error saving payment status to Firebase: " + e.getMessage());
                    e.printStackTrace();
                }
            }, Executors.newSingleThreadExecutor());
        } catch (Exception e) {
            System.err.println("Error in saveMonthStatusToFirebase: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Reset all payment method buttons to default style
     */
    private void resetPaymentMethodButtons() {
        resetPaymentButtonStyle(gcashButton);
        resetPaymentButtonStyle(bpiButton);
        resetPaymentButtonStyle(bdoButton);
        resetPaymentButtonStyle(metroButton);
    }

    // Handler to open payment dialog
    @FXML
    private void openPaymentDialog(ActionEvent event) {
        // Validate selections before opening dialog
        if (selectedMethod == null || selectedMonth == null) {
            if (selectedMethod == null && selectedMonth == null) {
                validationMessageLabel.setText("⚠ Please choose a payment method and a month to pay for");
            } else if (selectedMethod == null) {
                validationMessageLabel.setText("⚠ Please choose a payment method");
            } else {
                validationMessageLabel.setText("⚠ Please choose a month to pay for");
            }
            validationMessageLabel.setStyle("-fx-text-fill: #D8000C; -fx-font-weight: bold;");
            return;
        }
        
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("/com/dtdt/DormManager/view/payment-dialog.fxml"));
            Parent dialogRoot = loader.load();

            // Create a new stage for the popup
            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setTitle("Make Payment");
            dialogStage.setScene(new Scene(dialogRoot));

            // Pass the stage to the dialog controller so it can close itself
            PaymentDialogController dialogController = loader.getController();
            dialogController.setDialogStage(dialogStage);
            // Pass currently selected method from main view buttons
            if (selectedMethod != null) dialogController.setSelectedMethod(selectedMethod);
            // Pass selected month
            if (selectedMonth != null) dialogController.setSelectedMonth(selectedMonth);

            dialogStage.showAndWait();

            PaymentDialogController.PaymentResult result = dialogController.getResult();
            if (result != null) {
                // For now, just print the result. Integrate with backend/payment logic here.
                System.out.println("Payment submitted: " + result);
                
                // Store payment details for this month
                PaymentDetails details = new PaymentDetails(
                    selectedMonth,
                    result.amount,
                    result.method,
                    result.reference,
                    result.payerName,
                    new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date())
                );
                paymentRecords.put(selectedMonth, details);
                
                // Save to Firebase
                savePaymentToFirebase(details);
                
                // Mark the selected month as PAID
                markMonthAsPaid(selectedMonth);
                
                // Update receipt combo box to show only paid months
                updateReceiptComboBox();
                
                // Clear selections after successful submission
                validationMessageLabel.setText("✓ Payment submitted successfully!");
                validationMessageLabel.setStyle("-fx-text-fill: #008000; -fx-font-weight: bold;");
                
                // Reset selections
                selectedMethod = null;
                selectedMonth = null;
                resetPaymentMethodButtons();
                updateMakePaymentButtonState();
                
                // TODO: Update UI, show confirmation, etc.
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method will be called by the previous controller (or Login)
     * to pass in the currently logged-in tenant.
     */
    public void initData(Tenant tenant) {
        currentTenant = tenant;

        // 1. Populate Header
        tenantNameLabel.setText(currentTenant.getFullName());
        tenantIdLabel.setText(currentTenant.getUserId());
        tenantEmailLabel.setText(currentTenant.getEmail());

        // 2. Populate Building/Room Info (Dummy Data)
        // TODO: Get this info from the tenant's record
        buildingLabel.setText("Building A");
        roomLabel.setText("Room 5210");
        contractTypeLabel.setText("6-Month Term");
        contractDatesLabel.setText("July 2025 - December 2025");

        // 3. Load dynamic content
        loadBillingHistory();
        
        // 4. Create month tabs
        createMonthTabs();
        
        // 5. Load payment records from Firebase
        loadPaymentRecordsFromFirebase();
        
        // 6. Initialize validation state
        validationMessageLabel.setText("⚠ Please choose a payment method and a month to pay for");
        validationMessageLabel.setStyle("-fx-text-fill: #D8000C; -fx-font-weight: bold;");
        makePaymentButton.setDisable(true);
        makePaymentButton.setStyle("-fx-background-color: #D3D3D3; -fx-text-fill: #666666; -fx-cursor: default;");
    }

    private void loadBillingHistory() {
        // TODO: Fetch this tenant's billing history from the database
        // You would clear the dummy data first:
        // billingHistoryVBox.getChildren().clear();

        // Then add new ones:
        // HBox billCard = createBillingCard("October 2025", "PENDING", 5000.00);
        // billingHistoryVBox.getChildren().add(billCard);
    }

    /**
     * FXML Action: Called when "Dashboard" hyperlink is clicked.
     */
    @FXML
    private void goToDashboard(ActionEvent event) throws IOException {
        // 1. Load the dashboard FXML
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/com/dtdt/DormManager/view/tenant-dashboard.fxml"));
        Parent root = loader.load();

        // 2. Get the dashboard controller
        TenantDashboardController controller = loader.getController();

        // 3. Pass the tenant data BACK to the dashboard
        controller.initData(this.currentTenant);

        // 4. Get the current stage (window) from the event
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.getScene().setRoot(root);
        stage.setTitle("Tenant Dashboard");


    }

    @FXML
    private void onProfileClick(ActionEvent event) throws IOException {
        System.out.println("Profile link clicked.");

        // 1. Load the profile-view.fxml file
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/com/dtdt/DormManager/view/tenant-profile-view.fxml"));
        Parent root = loader.load();

        // 2. Get the controller of the new scene
        TenantProfileController controller = loader.getController();

        // 3. Pass the *current* tenant data to the new controller
        controller.initData(this.currentTenant);

        // 4. Get the current stage (window) and change the scene
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.getScene().setRoot(root);
        stage.setTitle("Tenant Profile");
    }

    /**
     * FXML Action: Called when "Logout" hyperlink is clicked.
     */
    @FXML
    private void onLogoutClick() throws IOException {
        System.out.println("Logout clicked.");
        Main main = new Main();
        main.changeScene("login-view.fxml");
    }
    
    /**
     * Update the receipt combo box to show only paid months
     */
    private void updateReceiptComboBox() {
        receiptComboBox.getItems().clear();
        for (String month : paymentRecords.keySet()) {
            if (monthStatus.get(month).equals("PAID")) {
                receiptComboBox.getItems().add(month);
            }
        }
    }
    
    /**
     * Save PDF receipt for the selected month
     */
    @FXML
    private void savePDF() {
        String selectedMonth = receiptComboBox.getValue();
        if (selectedMonth == null) {
            validationMessageLabel.setText("⚠ Please select a paid month to generate receipt");
            validationMessageLabel.setStyle("-fx-text-fill: #D8000C; -fx-font-weight: bold;");
            return;
        }
        
        PaymentDetails details = paymentRecords.get(selectedMonth);
        if (details == null) {
            validationMessageLabel.setText("⚠ No payment record found for this month");
            validationMessageLabel.setStyle("-fx-text-fill: #D8000C; -fx-font-weight: bold;");
            return;
        }
        
        try {
            // Create a file chooser to select save location
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Payment Receipt");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
            fileChooser.setInitialFileName("Receipt_" + currentTenant.getUserId() + "_" + selectedMonth.replace(" ", "_") + ".txt");
            
            File file = fileChooser.showSaveDialog(makePaymentButton.getScene().getWindow());
            if (file != null) {
                generatePDFReceipt(file, details);
                validationMessageLabel.setText("✓ Receipt saved successfully!");
                validationMessageLabel.setStyle("-fx-text-fill: #008000; -fx-font-weight: bold;");
            }
        } catch (Exception e) {
            validationMessageLabel.setText("⚠ Error saving receipt: " + e.getMessage());
            validationMessageLabel.setStyle("-fx-text-fill: #D8000C; -fx-font-weight: bold;");
            e.printStackTrace();
        }
    }
    
    /**
     * Generate a text-based PDF receipt
     */
    private void generatePDFReceipt(File file, PaymentDetails details) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("========================================\n");
            writer.write("         PAYMENT RECEIPT\n");
            writer.write("========================================\n\n");
            
            writer.write("Tenant Information:\n");
            writer.write("Name: " + currentTenant.getFullName() + "\n");
            writer.write("ID: " + currentTenant.getUserId() + "\n");
            writer.write("Email: " + currentTenant.getEmail() + "\n\n");
            
            writer.write("Building: " + buildingLabel.getText() + "\n");
            writer.write("Room: " + roomLabel.getText() + "\n\n");
            
            writer.write("Payment Details:\n");
            writer.write("Month: " + details.month + "\n");
            writer.write("Amount: ₱" + String.format("%.2f", details.amount) + "\n");
            writer.write("Payment Method: " + details.paymentMethod + "\n");
            writer.write("Reference Number: " + (details.referenceNumber != null && !details.referenceNumber.isEmpty() ? details.referenceNumber : "N/A") + "\n");
            writer.write("Payer Name: " + (details.payerName != null && !details.payerName.isEmpty() ? details.payerName : "N/A") + "\n");
            writer.write("Payment Date: " + details.paymentDate + "\n\n");
            
            writer.write("========================================\n");
            writer.write("This is a computer-generated receipt.\n");
            writer.write("========================================\n");
        }
    }
    
    /**
     * Inner class to store payment details
     */
    private static class PaymentDetails {
        String month;
        double amount;
        String paymentMethod;
        String referenceNumber;
        String payerName;
        String paymentDate;
        
        PaymentDetails(String month, double amount, String paymentMethod, String referenceNumber, String payerName, String paymentDate) {
            this.month = month;
            this.amount = amount;
            this.paymentMethod = paymentMethod;
            this.referenceNumber = referenceNumber;
            this.payerName = payerName;
            this.paymentDate = paymentDate;
        }
        
        // Convert to a Map for Firestore
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("month", this.month);
            map.put("amount", this.amount);
            map.put("paymentMethod", this.paymentMethod);
            map.put("referenceNumber", this.referenceNumber);
            map.put("payerName", this.payerName);
            map.put("paymentDate", this.paymentDate);
            return map;
        }
    }
    
    /**
     * Save payment details to Firebase Firestore
     */
    private void savePaymentToFirebase(PaymentDetails details) {
        try {
            Firestore db = FirebaseInit.getDatabase();
            if (db == null) {
                System.err.println("Firebase database not initialized");
                return;
            }
            
            // Create document in Firestore under: payments/[userId]/months/[month]
            String documentId = details.month.replace(" ", "_").replace(",", "");
            ApiFuture<com.google.cloud.firestore.WriteResult> future = db.collection("payments")
                    .document(currentTenant.getUserId())
                    .collection("months")
                    .document(documentId)
                    .set(details.toMap());
            
            // This will execute asynchronously
            future.addListener(() -> {
                try {
                    future.get();
                    System.out.println("Payment record saved to Firebase for: " + details.month);
                } catch (Exception e) {
                    System.err.println("Error saving payment to Firebase: " + e.getMessage());
                    e.printStackTrace();
                }
            }, Executors.newSingleThreadExecutor());
        } catch (Exception e) {
            System.err.println("Error in savePaymentToFirebase: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Load payment records from Firebase
     */
    private void loadPaymentRecordsFromFirebase() {
        try {
            Firestore db = FirebaseInit.getDatabase();
            if (db == null) {
                System.err.println("Firebase database not initialized");
                return;
            }
            
            // First, load payment details
            ApiFuture<com.google.cloud.firestore.QuerySnapshot> future = db.collection("payments")
                    .document(currentTenant.getUserId())
                    .collection("months")
                    .get();
            
            // This will execute asynchronously
            future.addListener(() -> {
                try {
                    com.google.cloud.firestore.QuerySnapshot querySnapshot = future.get();
                    if (querySnapshot != null) {
                        querySnapshot.forEach(doc -> {
                            try {
                                String month = (String) doc.get("month");
                                double amount = doc.getDouble("amount");
                                String method = (String) doc.get("paymentMethod");
                                String reference = (String) doc.get("referenceNumber");
                                String payer = (String) doc.get("payerName");
                                String date = (String) doc.get("paymentDate");
                                
                                PaymentDetails details = new PaymentDetails(month, amount, method, reference, payer, date);
                                paymentRecords.put(month, details);
                                
                                System.out.println("Loaded payment record from Firebase: " + month);
                            } catch (Exception e) {
                                System.err.println("Error parsing payment record: " + e.getMessage());
                            }
                        });
                    }
                    
                    // Then, load payment statuses
                    loadPaymentStatusesFromFirebase();
                } catch (Exception e) {
                    System.err.println("Error loading payments from Firebase: " + e.getMessage());
                    e.printStackTrace();
                }
            }, Executors.newSingleThreadExecutor());
        } catch (Exception e) {
            System.err.println("Error in loadPaymentRecordsFromFirebase: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Load payment statuses from Firebase
     */
    private void loadPaymentStatusesFromFirebase() {
        try {
            Firestore db = FirebaseInit.getDatabase();
            if (db == null) {
                System.err.println("Firebase database not initialized");
                return;
            }
            
            // Fetch all payment statuses for this tenant
            ApiFuture<com.google.cloud.firestore.QuerySnapshot> future = db.collection("payments")
                    .document(currentTenant.getUserId())
                    .collection("status")
                    .get();
            
            // This will execute asynchronously
            future.addListener(() -> {
                try {
                    com.google.cloud.firestore.QuerySnapshot querySnapshot = future.get();
                    if (querySnapshot != null) {
                        querySnapshot.forEach(doc -> {
                            try {
                                // Convert document ID back to month name
                                String month = doc.getId().replace("_", " ");
                                String status = (String) doc.get("status");
                                
                                monthStatus.put(month, status);
                                System.out.println("Loaded payment status from Firebase: " + month + " -> " + status);
                            } catch (Exception e) {
                                System.err.println("Error parsing payment status: " + e.getMessage());
                            }
                        });
                    }
                    
                    // Update receipt combo box and UI after loading both details and statuses on the FX thread
                    Platform.runLater(() -> {
                        updateReceiptComboBox();
                        updateAllMonthBoxesUI();
                    });
                } catch (Exception e) {
                    System.err.println("Error loading payment statuses from Firebase: " + e.getMessage());
                    e.printStackTrace();
                }
            }, Executors.newSingleThreadExecutor());
        } catch (Exception e) {
            System.err.println("Error in loadPaymentStatusesFromFirebase: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Update all month boxes UI based on loaded statuses
     */
    private void updateAllMonthBoxesUI() {
        String[] months = {"July 2025", "August 2025", "September 2025", "October 2025", "November 2025", "December 2025"};
        
        for (int i = 0; i < billingHistoryVBox.getChildren().size(); i++) {
            if (billingHistoryVBox.getChildren().get(i) instanceof HBox) {
                HBox monthBox = (HBox) billingHistoryVBox.getChildren().get(i);
                String month = months[i];
                
                String status = monthStatus.getOrDefault(month, "UNPAID");
                updateMonthBoxUI(month, monthBox, status);
            }
        }
    }
}