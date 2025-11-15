package com.dtdt.DormManager.controller.admin;

import com.dtdt.DormManager.controller.config.FirebaseInit;
import com.dtdt.DormManager.model.Reservation;
import com.dtdt.DormManager.model.Tenant; // <-- IMPORT TENANT MODEL
import com.dtdt.DormManager.model.User;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class ReservationsViewController {

    @FXML private TableView<Reservation> reservationsTable;
    @FXML private TableColumn<Reservation, String> nameColumn; // This will show First + Last
    @FXML private TableColumn<Reservation, String> emailColumn;
    @FXML private TableColumn<Reservation, Date> dateSubmittedColumn;
    @FXML private TableColumn<Reservation, String> contractTypeColumn;
    @FXML private TableColumn<Reservation, String> statusColumn;
    @FXML private TableColumn<Reservation, Void> actionsColumn;

    private final ObservableList<Reservation> reservationList = FXCollections.observableArrayList();
    private final Firestore db = FirebaseInit.db;

    @FXML
    public void initialize() {
        // 1. Set up the table columns

        // Combine firstName and lastName into one column
        nameColumn.setCellValueFactory(cellData -> {
            String name = cellData.getValue().getFirstName() + " " + cellData.getValue().getLastName();
            return new SimpleStringProperty(name);
        });

        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        dateSubmittedColumn.setCellValueFactory(new PropertyValueFactory<>("dateSubmitted"));
        contractTypeColumn.setCellValueFactory(new PropertyValueFactory<>("contractType"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        // 2. Set up the custom "Actions" column
        setupActionsColumn();

        // 3. Set the table's data source
        reservationsTable.setItems(reservationList);

        // 4. Load the data from Firebase
        loadPendingReservations();
    }

    private void loadPendingReservations() {
        reservationList.clear(); // Clear existing data

        ApiFuture<QuerySnapshot> future = db.collection("reservations")
                .whereEqualTo("status", "Pending")
                .get();

        future.addListener(() -> {
            try {
                List<QueryDocumentSnapshot> documents = future.get().getDocuments();
                for (QueryDocumentSnapshot document : documents) {
                    Reservation reservation = document.toObject(Reservation.class);
                    // The 'id' field is now automatically set by Firestore
                    reservationList.add(reservation);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, Platform::runLater); // Run this on the JavaFX thread
    }

    private void setupActionsColumn() {
        actionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button acceptButton = new Button("Accept");
            private final Button denyButton = new Button("Deny");
            private final HBox pane = new HBox(acceptButton, denyButton);

            {
                pane.setSpacing(10);
                acceptButton.setStyle("-fx-background-color: #2E7D32; -fx-text-fill: white;");
                denyButton.setStyle("-fx-background-color: #C62828; -fx-text-fill: white;");

                acceptButton.setOnAction(event -> {
                    Reservation reservation = getTableView().getItems().get(getIndex());
                    handleAccept(reservation);
                });

                denyButton.setOnAction(event -> {
                    Reservation reservation = getTableView().getItems().get(getIndex());
                    handleDeny(reservation);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(pane);
                }
            }
        });
    }

    // --- NEW FEATURE ---
    // Inside ReservationsViewController.java

    // Inside ReservationsViewController.java

    private void handleAccept(Reservation reservation) {
        System.out.println("Accepting: " + reservation.getFirstName());

        String plainTextPassword = generateRandomPassword(8);
        String hashedPassword = User.hashPassword(plainTextPassword);
        String fullName = reservation.getFirstName() + " " + reservation.getLastName();

        // --- THIS IS THE FIX ---
        // We are now saving fields that match your Java model
        java.util.Map<String, Object> newTenant = new java.util.HashMap<>();

        // Use the field names from your User.java and Tenant.java
        newTenant.put("userId", reservation.getStudentId()); // Uses "userId"
        newTenant.put("assignedRoomID", null);
        newTenant.put("currentYear", reservation.getCurrentYear()); // Saves "1st Year" as a String

        newTenant.put("email", reservation.getEmail());
        newTenant.put("passwordHash", hashedPassword);
        newTenant.put("firstName", reservation.getFirstName());
        newTenant.put("lastName", reservation.getLastName());
        newTenant.put("fullName", fullName);
        newTenant.put("genderType", reservation.getGender());
        newTenant.put("userType", "Tenant");
        newTenant.put("contractID", null);
        // --- END FIX ---

        // Save using the studentID as the document key
        ApiFuture<WriteResult> userFuture = db.collection("users")
                .document(reservation.getStudentId())
                .set(newTenant);

        userFuture.addListener(() -> {
            try {
                userFuture.get();
                System.out.println("Tenant created: " + fullName);
                System.out.println("Temp Password: " + plainTextPassword);

                updateReservationStatus(reservation, "Approved");

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, Platform::runLater);
    }

    // --- FIXED ---
    private void handleDeny(Reservation reservation) {
        System.out.println("Denying: " + reservation.getFirstName());
        updateReservationStatus(reservation, "Rejected");
    }

    // --- FIXED (This now updates the database) ---
    private void updateReservationStatus(Reservation reservation, String newStatus) {
        String docId = reservation.getId(); // <-- Get the document ID
        if (docId == null) {
            System.err.println("Error: Reservation ID is null. Cannot update status.");
            return;
        }

        // Update the 'status' field in Firebase
        ApiFuture<WriteResult> future = db.collection("reservations")
                .document(docId)
                .update("status", newStatus);

        future.addListener(() -> {
            try {
                future.get(); // Wait for update to complete
                // Remove from the list ONLY after successful update
                reservationList.remove(reservation);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, Platform::runLater);
    }

    // --- NEW HELPER ---
    private String generateRandomPassword(int length) {
        String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}