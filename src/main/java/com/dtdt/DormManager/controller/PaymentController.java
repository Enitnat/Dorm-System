package com.dtdt.DormManager.controller;

import com.dtdt.DormManager.Main;
import com.dtdt.DormManager.model.Contract;
import com.dtdt.DormManager.model.Invoice;
import com.dtdt.DormManager.model.Room;
import com.dtdt.DormManager.model.Tenant;
import com.dtdt.DormManager.controller.config.FirebaseInit;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;

// --- ADD THESE IMPORTS ---
import com.dtdt.DormManager.controller.TenantProfileController;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import javafx.scene.layout.Priority;
// --- END IMPORTS ---

public class PaymentController {

    private Tenant currentTenant;
    private Contract currentContract;
    private Room currentRoom;
    private final Firestore db = FirebaseInit.db;
    private final List<Invoice> invoiceList = new ArrayList<>();

    // Formatters
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("MMMM dd, yyyy");
    private final SimpleDateFormat monthYearFormatter = new SimpleDateFormat("MMMM yyyy");
    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));

    // === FXML Header Components ===
    @FXML private Label tenantNameLabel;
    @FXML private Label tenantIdLabel;
    @FXML private Label tenantEmailLabel;
    @FXML private Label buildingLabel;
    @FXML private Label roomLabel;
    @FXML private Label contractTypeLabel;
    @FXML private Label contractDatesLabel;

    // === FXML Page-Specific Components ===
    @FXML private ComboBox<Invoice> receiptComboBox;
    @FXML private VBox billingHistoryVBox;
    @FXML private Label validationMessageLabel;

    @FXML
    public void initialize() {
        receiptComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Invoice invoice) {
                return invoice == null ? "Select a paid invoice" : invoice.getMonthYear();
            }
            @Override
            public Invoice fromString(String string) { return null; }
        });
    }

    public void initData(Tenant tenant) {
        this.currentTenant = tenant;
        tenantNameLabel.setText(currentTenant.getFullName());
        tenantIdLabel.setText(currentTenant.getUserId());
        tenantEmailLabel.setText(currentTenant.getEmail());
        loadLinkedData();
    }

    private void loadLinkedData() {
        if (currentTenant.getContractID() == null) {
            billingHistoryVBox.getChildren().add(new Label("You do not have an active contract."));
            return;
        }

        DocumentReference contractRef = db.collection("contracts").document(currentTenant.getContractID());
        ApiFuture<DocumentSnapshot> contractFuture = contractRef.get();

        contractFuture.addListener(() -> {
            try {
                DocumentSnapshot contractDoc = contractFuture.get();
                if (contractDoc.exists()) {
                    this.currentContract = contractDoc.toObject(Contract.class);

                    DocumentReference roomRef = db.collection("rooms").document(currentTenant.getRoomID());
                    ApiFuture<DocumentSnapshot> roomFuture = roomRef.get();
                    roomFuture.addListener(() -> {
                        try {
                            DocumentSnapshot roomDoc = roomFuture.get();
                            if (roomDoc.exists()) {
                                this.currentRoom = roomDoc.toObject(Room.class);
                            }
                            Platform.runLater(() -> {
                                populateHeader();
                                loadBillingHistory();
                            });
                        } catch (Exception e) { e.printStackTrace(); }
                    }, Runnable::run);
                } else {
                    Platform.runLater(() -> billingHistoryVBox.getChildren().add(new Label("Error: Contract not found.")));
                }
            } catch (Exception e) { e.printStackTrace(); }
        }, Runnable::run);
    }

    private void populateHeader() {
        if (currentRoom != null) {
            buildingLabel.setText(currentRoom.getBuildingName());
            roomLabel.setText("Room " + currentRoom.getRoomNumber());
        } else {
            buildingLabel.setText("N/A");
            roomLabel.setText("N/A");
        }

        if (currentContract != null) {
            contractTypeLabel.setText(currentContract.getContractType());
            String dates = dateFormatter.format(currentContract.getStartDate()) + " - " +
                    dateFormatter.format(currentContract.getEndDate());
            contractDatesLabel.setText(dates);
        } else {
            contractTypeLabel.setText("N/A");
            contractDatesLabel.setText("N/A");
        }
    }

    private void loadBillingHistory() {
        billingHistoryVBox.getChildren().clear();
        invoiceList.clear();

        ApiFuture<QuerySnapshot> future = db.collection("invoices")
                .whereEqualTo("tenantId", currentTenant.getUserId())
                .get();

        future.addListener(() -> {
            try {
                List<QueryDocumentSnapshot> documents = future.get().getDocuments();
                if (documents.isEmpty()) {
                    System.out.println("No invoices found, generating...");
                    generateInvoices();
                } else {
                    System.out.println("Found " + documents.size() + " invoices.");
                    for (QueryDocumentSnapshot doc : documents) {
                        invoiceList.add(doc.toObject(Invoice.class));
                    }
                    Platform.runLater(this::displayInvoices);
                }
            } catch (Exception e) { e.printStackTrace(); }
        }, Runnable::run);
    }

    /**
     * [FIXED] Generates invoices based on contract type.
     */
    private void generateInvoices() {
        List<Invoice> newInvoices = new ArrayList<>();
        String contractType = currentContract.getContractType();

        // --- THIS IS THE NEW LOGIC ---
        if ("Monthly".equalsIgnoreCase(contractType)) {
            // --- 1. MONTHLY PAYMENT LOGIC ---
            Calendar cal = Calendar.getInstance();
            cal.setTime(currentContract.getStartDate());
            Date endDate = currentContract.getEndDate();

            while (cal.getTime().before(endDate) || cal.getTime().equals(endDate)) {
                String monthYear = monthYearFormatter.format(cal.getTime());
                cal.set(Calendar.DAY_OF_MONTH, 5);
                Date dueDate = cal.getTime();

                Invoice invoice = new Invoice();
                invoice.setId(UUID.randomUUID().toString());
                invoice.setTenantId(currentTenant.getUserId());
                invoice.setContractId(currentContract.getId());
                invoice.setMonthYear(monthYear);
                invoice.setRentAmount(currentContract.getRentAmount());
                invoice.setLateFee(0);
                invoice.setTotalAmount(currentContract.getRentAmount());
                invoice.setDueDate(dueDate);
                invoice.setStatus("Pending");

                newInvoices.add(invoice);
                db.collection("invoices").document(invoice.getId()).set(invoice);

                cal.set(Calendar.DAY_OF_MONTH, 1);
                cal.add(Calendar.MONTH, 1);
            }
        } else {
            // --- 2. FULL PAYMENT (SEMESTERLY) LOGIC ---
            double totalRent = currentContract.getRentAmount() * 6; // Rate x 6 months

            // Set due date to the 5th of the start month
            Calendar cal = Calendar.getInstance();
            cal.setTime(currentContract.getStartDate());
            cal.set(Calendar.DAY_OF_MONTH, 5);
            Date dueDate = cal.getTime();

            // Create a descriptive name
            String monthYear = "Full Semester (" +
                    monthYearFormatter.format(currentContract.getStartDate()) + " - " +
                    monthYearFormatter.format(currentContract.getEndDate()) + ")";

            Invoice invoice = new Invoice();
            invoice.setId(UUID.randomUUID().toString());
            invoice.setTenantId(currentTenant.getUserId());
            invoice.setContractId(currentContract.getId());
            invoice.setMonthYear(monthYear);
            invoice.setRentAmount(totalRent);
            invoice.setLateFee(0);
            invoice.setTotalAmount(totalRent);
            invoice.setDueDate(dueDate);
            invoice.setStatus("Pending");

            newInvoices.add(invoice);
            db.collection("invoices").document(invoice.getId()).set(invoice);
        }
        // --- END OF NEW LOGIC ---

        invoiceList.addAll(newInvoices);
        Platform.runLater(this::displayInvoices);
    }

    /**
     * Renders the list of invoices to the UI, checking for late fees.
     */
    private void displayInvoices() {
        billingHistoryVBox.getChildren().clear();
        receiptComboBox.getItems().clear();

        Date today = new Date();

        for (Invoice invoice : invoiceList) {
            // Check for late fees
            if (("Pending".equals(invoice.getStatus()) || "Overdue".equals(invoice.getStatus())) && today.after(invoice.getDueDate())) {
                if (invoice.getLateFee() == 0) { // Only apply late fee once
                    double rent = invoice.getRentAmount();
                    double lateFee = rent * 0.10; // 10% late fee
                    double newTotal = invoice.getTotalAmount() + lateFee;

                    invoice.setLateFee(lateFee);
                    invoice.setTotalAmount(newTotal);
                    invoice.setStatus("Overdue");

                    // Update this in Firebase
                    db.collection("invoices").document(invoice.getId()).update("status", "Overdue", "lateFee", lateFee, "totalAmount", newTotal);
                }
            }

            if ("Paid".equals(invoice.getStatus())) {
                receiptComboBox.getItems().add(invoice);
            }

            billingHistoryVBox.getChildren().add(createInvoiceCard(invoice));
        }
    }

    private HBox createInvoiceCard(Invoice invoice) {
        Label monthLabel = new Label(invoice.getMonthYear());
        monthLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");

        Label statusLabel = new Label(invoice.getStatus());
        if ("Paid".equals(invoice.getStatus())) {
            statusLabel.setStyle("-fx-text-fill: #008000; -fx-font-weight: bold;");
        } else if ("Overdue".equals(invoice.getStatus())) {
            statusLabel.setStyle("-fx-text-fill: #D8000C; -fx-font-weight: bold;");
        } else {
            statusLabel.setStyle("-fx-text-fill: #F9A825; -fx-font-weight: bold;");
        }

        Label amountLabel = new Label(currencyFormatter.format(invoice.getTotalAmount()));
        amountLabel.setStyle("-fx-font-size: 14;");

        VBox details = new VBox(5, monthLabel, new HBox(10, new Label("Status:"), statusLabel), amountLabel);

        if (invoice.getLateFee() > 0) {
            Label lateFeeLabel = new Label("Includes " + currencyFormatter.format(invoice.getLateFee()) + " late fee");
            lateFeeLabel.setStyle("-fx-text-fill: #D8000C; -fx-font-size: 11;");
            details.getChildren().add(lateFeeLabel);
        }

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        if ("Paid".equals(invoice.getStatus())) {
            Button receiptButton = new Button("View Receipt");
            receiptButton.setOnAction(e -> handleViewReceipt(invoice));
            return new HBox(10, details, spacer, receiptButton);
        } else {
            Button payButton = new Button("Pay Now");
            payButton.setStyle("-fx-background-color: #1A1A1A; -fx-text-fill: white; -fx-font-weight: bold;");
            payButton.setOnAction(e -> handlePayNow(invoice));
            return new HBox(10, details, spacer, payButton);
        }
    }

    private void handlePayNow(Invoice invoice) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Payment");
        alert.setHeaderText("Pay for " + invoice.getMonthYear());
        alert.setContentText("Are you sure you want to pay " + currencyFormatter.format(invoice.getTotalAmount()) + "?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {

            Map<String, Object> updates = new HashMap<>();
            updates.put("status", "Paid");
            updates.put("datePaid", new Date());

            db.collection("invoices").document(invoice.getId()).update(updates).addListener(() -> {
                Platform.runLater(this::loadBillingHistory);
            }, Runnable::run);
        }
    }

    private void handleViewReceipt(Invoice invoice) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Receipt");
        alert.setHeaderText("Payment for " + invoice.getMonthYear());
        alert.setContentText(
                "Amount Paid: " + currencyFormatter.format(invoice.getTotalAmount()) + "\n" +
                        "Date Paid: " + dateFormatter.format(invoice.getDatePaid())
        );
        alert.showAndWait();
    }

    // --- Navigation Methods ---

    @FXML
    private void goToDashboard(ActionEvent event) throws IOException {
        loadScene(event, "tenant-dashboard.fxml", "Tenant Dashboard");
    }

    @FXML
    private void onProfileClick(ActionEvent event) throws IOException {
        loadScene(event, "tenant-profile-view.fxml", "Profile");
    }

    @FXML
    private void onLogoutClick() throws IOException {
        Main main = new Main();
        main.changeScene("login-view.fxml");
    }

    @FXML
    private void savePDF() {
        Invoice selectedInvoice = receiptComboBox.getValue();
        if (selectedInvoice == null) {
            validationMessageLabel.setText("Please select a paid invoice from the dropdown.");
            validationMessageLabel.setVisible(true);
            return;
        }

        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Payment Receipt");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));

            String monthYear = selectedInvoice.getMonthYear().replace(" ", "_").replaceAll("[^a-zA-Z0-9_-]", "");
            String fileName = "Receipt_" + currentTenant.getUserId() + "_" + monthYear + ".txt";
            fileChooser.setInitialFileName(fileName);

            File file = fileChooser.showSaveDialog(receiptComboBox.getScene().getWindow());

            if (file != null) {
                generateReceiptFile(file, selectedInvoice);

                validationMessageLabel.setText("✓ Receipt saved successfully!");
                validationMessageLabel.setVisible(true);
                validationMessageLabel.setStyle("-fx-text-fill: #008000; -fx-font-weight: bold;");
            } else {
                validationMessageLabel.setText("Save operation cancelled.");
                validationMessageLabel.setVisible(true);
                validationMessageLabel.setStyle("-fx-text-fill: #555555;");
            }
        } catch (Exception e) {
            validationMessageLabel.setText("⚠ Error saving receipt: " + e.getMessage());
            validationMessageLabel.setVisible(true);
            validationMessageLabel.setStyle("-fx-text-fill: #D8000C; -fx-font-weight: bold;");
            e.printStackTrace();
        }
    }

    private void generateReceiptFile(File file, Invoice invoice) throws IOException {
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
            writer.write("For: " + invoice.getMonthYear() + "\n");
            writer.write("Amount Paid: " + currencyFormatter.format(invoice.getTotalAmount()) + "\n");
            writer.write(" (Rent: " + currencyFormatter.format(invoice.getRentAmount()) + ")\n");
            if (invoice.getLateFee() > 0) {
                writer.write(" (Late Fee: " + currencyFormatter.format(invoice.getLateFee()) + ")\n");
            }
            writer.write("Payment Date: " + dateFormatter.format(invoice.getDatePaid()) + "\n");
            writer.write("Status: " + invoice.getStatus() + "\n");
            writer.write("Invoice ID: " + invoice.getId() + "\n\n");

            writer.write("========================================\n");
            writer.write("This is a computer-generated receipt.\n");
            writer.write("========================================\n");
        }
    }

    private void loadScene(ActionEvent event, String fxmlFile, String title) throws IOException {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/com/dtdt/DormManager/view/" + fxmlFile));
        Parent root = loader.load();

        if (title.equals("Tenant Dashboard")) {
            TenantDashboardController controller = loader.getController();
            controller.initData(this.currentTenant);
        } else if (title.equals("Profile")) {
            com.dtdt.DormManager.controller.TenantProfileController controller = loader.getController();
            controller.initData(this.currentTenant);
        }

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.getScene().setRoot(root);
        stage.setTitle(title);
    }
}