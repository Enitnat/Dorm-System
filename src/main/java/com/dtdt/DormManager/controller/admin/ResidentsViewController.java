package com.dtdt.DormManager.controller.admin;

import com.dtdt.DormManager.controller.config.FirebaseInit;
import com.dtdt.DormManager.model.Tenant;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

import java.util.List;

public class ResidentsViewController {

    // --- FXML Components ---
    @FXML private TextField searchField;
    @FXML private Button addResidentButton;
    @FXML private ComboBox<String> buildingFilterBox;
    @FXML private ComboBox<String> floorFilterBox;
    @FXML private ComboBox<String> roomTypeFilterBox;
    @FXML private ComboBox<String> sortBox;

    @FXML private TableView<Tenant> residentsTable;
    @FXML private TableColumn<Tenant, String> nameColumn;
    @FXML private TableColumn<Tenant, String> studentIdColumn;
    @FXML private TableColumn<Tenant, String> roomColumn;
    @FXML private TableColumn<Tenant, String> contactColumn;
    @FXML private TableColumn<Tenant, String> statusColumn;
    @FXML private TableColumn<Tenant, Void> actionsColumn;

    // --- Data ---
    private final ObservableList<Tenant> tenantList = FXCollections.observableArrayList();
    private final Firestore db = FirebaseInit.db;

    @FXML
    public void initialize() {
        // 1. Set up the table columns
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        studentIdColumn.setCellValueFactory(new PropertyValueFactory<>("userId"));

        // This links to 'assignedRoomID' in your Tenant model (which is null for now)
        roomColumn.setCellValueFactory(new PropertyValueFactory<>("assignedRoomID"));

        // We'll use email for "Contact"
        contactColumn.setCellValueFactory(new PropertyValueFactory<>("email"));

        // Placeholder for status
        statusColumn.setCellValueFactory(cellData -> new SimpleStringProperty("Active"));

        // 2. Set up the custom "Actions" column
        setupActionsColumn();

        // 3. Set the table's data source
        residentsTable.setItems(tenantList);

        // 4. Load the data from Firebase
        loadTenants();
    }

    @FXML
    private void onAddResidentClick() {
        System.out.println("Add Resident button clicked.");
        // This is a good place to open a new dialog to manually
        // create a tenant, like we do for reservations.
        // For now, we recommend using the "Reservations" tab.
    }

    private void loadTenants() {
        tenantList.clear();

        ApiFuture<QuerySnapshot> future = db.collection("users")
                .whereEqualTo("userType", "Tenant")
                .get();

        future.addListener(() -> {
            try {
                List<QueryDocumentSnapshot> documents = future.get().getDocuments();
                for (QueryDocumentSnapshot document : documents) {
                    Tenant tenant = document.toObject(Tenant.class);
                    tenantList.add(tenant);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, Platform::runLater);
    }

    private void setupActionsColumn() {
        actionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button viewButton = new Button("View");
            private final HBox pane = new HBox(viewButton);

            {
                pane.setSpacing(10);
                viewButton.setOnAction(event -> {
                    Tenant tenant = getTableView().getItems().get(getIndex());
                    handleViewDetails(tenant);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    private void handleViewDetails(Tenant tenant) {
        System.out.println("Viewing details for: " + tenant.getFullName());
        // TODO: Open a new dialog or view to show this tenant's
        // full contract, payment history, room, etc.
    }
}