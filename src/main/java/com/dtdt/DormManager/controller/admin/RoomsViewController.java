package com.dtdt.DormManager.controller.admin;

import com.dtdt.DormManager.controller.config.FirebaseInit;
import com.dtdt.DormManager.model.Building; // We need this to get a list of buildings
import com.dtdt.DormManager.model.Room;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.geometry.Pos;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class RoomsViewController {
    @FXML private FlowPane roomsContainer;

    @FXML
    public void initialize() {
        loadRooms();
    }

    private void loadRooms() {
        roomsContainer.getChildren().clear();

        ApiFuture<QuerySnapshot> future = FirebaseInit.db.collection("rooms").get();
        future.addListener(() -> {
            try {
                List<QueryDocumentSnapshot> documents = future.get().getDocuments();
                Platform.runLater(() -> {
                    for (QueryDocumentSnapshot document : documents) {
                        Room room = document.toObject(Room.class);
                        VBox card = createRoomCard(room);
                        roomsContainer.getChildren().add(card);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, Runnable::run);
    }

    @FXML
    private void onAddRoomClick() {
        // --- 1. First, get a list of available buildings for the ComboBox ---
        ObservableList<Building> buildings = FXCollections.observableArrayList();
        ApiFuture<QuerySnapshot> buildingsFuture = FirebaseInit.db.collection("buildings").get();
        buildingsFuture.addListener(() -> {
            try {
                for (QueryDocumentSnapshot doc : buildingsFuture.get().getDocuments()) {
                    buildings.add(doc.toObject(Building.class));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, Runnable::run);

        // --- 2. Create the Dialog ---
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add New Room");
        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // --- 3. Create Form Fields ---
        ComboBox<Building> buildingComboBox = new ComboBox<>(buildings);
        buildingComboBox.setPromptText("Select Building");
        // Use a cell factory to show building names in the dropdown
        buildingComboBox.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Building item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item.getName());
            }
        });
        buildingComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Building item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item.getName());
            }
        });

        TextField roomNumberField = new TextField();
        roomNumberField.setPromptText("Room Number (e.g., 101)");
        TextField floorField = new TextField();
        floorField.setPromptText("Floor (e.g., 1)");
        TextField typeField = new TextField();
        typeField.setPromptText("Room Type (e.g., Double)");
        TextField rateField = new TextField();
        rateField.setPromptText("Rate per bed (e.g., 5000)");
        TextField capacityField = new TextField();
        capacityField.setPromptText("Capacity (e.g., 2)");

        VBox content = new VBox(10,
                new Label("Building:"), buildingComboBox,
                new Label("Room Info:"), roomNumberField, floorField, typeField, rateField, capacityField
        );
        content.setStyle("-fx-padding: 10;");
        pane.setContent(content);

        // --- 4. Handle Dialog Submission ---
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                Building selectedBuilding = buildingComboBox.getValue();

                Room newRoom = new Room();
                newRoom.setBuildingId(selectedBuilding.getId());
                newRoom.setBuildingName(selectedBuilding.getName()); // Denormalize name
                newRoom.setRoomNumber(roomNumberField.getText());
                newRoom.setFloor(Integer.parseInt(floorField.getText()));
                newRoom.setRoomType(typeField.getText());
                newRoom.setRate(Double.parseDouble(rateField.getText()));
                newRoom.setCapacity(Integer.parseInt(capacityField.getText()));
                newRoom.setStatus("Available"); // Default status

                // --- 5. Save to Firebase ---
                String newRoomId = UUID.randomUUID().toString();
                DocumentReference docRef = FirebaseInit.db.collection("rooms").document(newRoomId);
                docRef.set(newRoom);

                // Add new card to UI immediately
                VBox card = createRoomCard(newRoom);
                roomsContainer.getChildren().add(card);

            } catch (Exception e) {
                e.printStackTrace();
                // TODO: Show an error alert
            }
        }
    }

    private VBox createRoomCard(Room room) {
        VBox card = new VBox();
        card.setStyle("-fx-background-color: white; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 0); " +
                "-fx-background-radius: 10; -fx-padding: 15; -fx-pref-width: 280;");

        // Header with room number and status
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(10);

        Label roomName = new Label("Room " + room.getRoomNumber());
        roomName.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");

        boolean isOccupied = "Occupied".equalsIgnoreCase(room.getStatus());
        Label status = new Label(room.getStatus());
        status.setStyle(isOccupied ?
                "-fx-background-color: #FFEBEE; -fx-text-fill: #C62828; -fx-padding: 4 8; -fx-background-radius: 4;" :
                "-fx-background-color: #E8F5E9; -fx-text-fill: #2E7D32; -fx-padding: 4 8; -fx-background-radius: 4;");
        // TODO: Add style for "Maintenance"

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(roomName, spacer, status);

        Separator separator = new Separator();
        separator.setStyle("-fx-padding: 10 0;");

        GridPane details = new GridPane();
        details.setVgap(8);
        details.setHgap(10);

        addDetailRow(details, 0, "Building:", room.getBuildingName());
        addDetailRow(details, 1, "Floor:", room.getFloor() + "");
        addDetailRow(details, 2, "Type:", room.getRoomType());
        addDetailRow(details, 3, "Rate:", "₱" + room.getRate() + "/mo");
        addDetailRow(details, 4, "Capacity:", "0/" + room.getCapacity()); // TODO: Track current occupancy

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setStyle("-fx-padding: 10 0 0 0;");

        Button viewDetails = new Button("View Details");
        viewDetails.setStyle("-fx-background-color: transparent; -fx-border-color: #1A1A1A; -fx-border-radius: 4;");

        MenuButton more = new MenuButton("More");
        more.setStyle("-fx-background-color: transparent;");
        MenuItem deleteItem = createDeleteMenuItem(room.getId(), card);
        more.getItems().addAll(
                new MenuItem("Edit"),
                deleteItem,
                new MenuItem(isOccupied ? "Mark as Available" : "Mark as Occupied")
        );

        actions.getChildren().addAll(viewDetails, more);
        card.getChildren().addAll(header, separator, details, actions);
        return card;
    }

    // Helper to create delete menu item
    private MenuItem createDeleteMenuItem(String documentId, Node cardToRemove) {
        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setOnAction(e -> {
            // TODO: Add "Are you sure?" confirmation

            ApiFuture<WriteResult> deleteFuture = FirebaseInit.db.collection("rooms").document(documentId).delete();
            deleteFuture.addListener(() -> {
                Platform.runLater(() -> {
                    roomsContainer.getChildren().remove(cardToRemove);
                });
            }, Runnable::run);
        });
        return deleteItem;
    }

    private void addDetailRow(GridPane grid, int row, String label, String value) {
        Label labelNode = new Label(label);
        labelNode.setStyle("-fx-text-fill: #666;");
        Label valueNode = new Label(value);
        grid.add(labelNode, 0, row);
        grid.add(valueNode, 1, row);
    }
}