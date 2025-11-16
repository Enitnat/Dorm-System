package com.dtdt.DormManager.controller.admin;

import com.dtdt.DormManager.model.Building;
import com.dtdt.DormManager.model.Room;
import com.dtdt.DormManager.model.Tenant; // Import Tenant for occupancy tracking
import com.dtdt.DormManager.service.RoomStore;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;
import java.util.Map;
import java.util.HashMap;
import com.dtdt.DormManager.controller.config.FirebaseInit;

public class RoomsViewController {
    @FXML private FlowPane roomsContainer;
    @FXML private ComboBox<Building> buildingCombo;
    @FXML private ComboBox<String> floorCombo;
    @FXML private ComboBox<String> typeCombo;
    @FXML private ComboBox<String> statusCombo;
    @FXML private Button addRoomBtn;

    // Local cache of rooms and tenants
    private final ObservableList<Room> roomList = FXCollections.observableArrayList();
    private final ObservableList<Tenant> tenantList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        loadTenantsForOccupancy(); // Load tenants first
        loadRooms(); // Then load rooms (which triggers render)
        setupFilters();
    }

    /**
     * Fetches all tenants to calculate occupancy for each room.
     */
    private void loadTenantsForOccupancy() {
        ApiFuture<QuerySnapshot> future = FirebaseInit.db.collection("users")
                .whereEqualTo("userType", "Tenant")
                .get();
        future.addListener(() -> {
            try {
                List<Tenant> fetchedTenants = new ArrayList<>();
                for (QueryDocumentSnapshot document : future.get().getDocuments()) {
                    fetchedTenants.add(document.toObject(Tenant.class));
                }
                Platform.runLater(() -> {
                    tenantList.setAll(fetchedTenants);
                    renderRooms(); // Re-render rooms to update occupancy counts
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, Runnable::run);
    }

    private void setupFilters() {
        // When building changes, repopulate floor/type/status
        if (buildingCombo != null) {
            buildingCombo.setCellFactory(lv -> new ListCell<>() {
                @Override protected void updateItem(Building item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getName());
                }
            });
            buildingCombo.setButtonCell(new ListCell<>() {
                @Override protected void updateItem(Building item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getName());
                }
            });

            buildingCombo.setOnAction(e -> populateDependentFiltersAndRender());
        }

        if (floorCombo != null) floorCombo.setOnAction(e -> renderRooms());
        if (typeCombo != null) typeCombo.setOnAction(e -> renderRooms());
        if (statusCombo != null) statusCombo.setOnAction(e -> renderRooms());
    }

    private void populateDependentFiltersAndRender() {
        Building selected = buildingCombo == null ? null : buildingCombo.getValue();

        // If "All Buildings" is selected (or nothing), clear dependent filters
        if (selected == null || selected.getId() == null) {
            if (floorCombo != null) floorCombo.getItems().clear();
            if (typeCombo != null) typeCombo.getItems().clear();
            if (statusCombo != null) statusCombo.getItems().clear();

            // Add "All" options back
            if (floorCombo != null) floorCombo.getItems().add("All Floors");
            if (typeCombo != null) typeCombo.getItems().add("All Types");
            if (statusCombo != null) statusCombo.getItems().add("All Statuses");

            renderRooms();
            return;
        }

        // Gather unique floors/types/status for the selected building
        var floors = FXCollections.observableArrayList("All Floors");
        var types = FXCollections.observableArrayList("All Types");
        var statuses = FXCollections.observableArrayList("All Statuses");

        for (Room r : roomList) {
            if (selected.getId().equals(r.getBuildingId())) {
                String f = String.valueOf(r.getFloor());
                if (!floors.contains(f)) floors.add(f);
                if (r.getRoomType() != null && !types.contains(r.getRoomType())) types.add(r.getRoomType());
                if (r.getStatus() != null && !statuses.contains(r.getStatus())) statuses.add(r.getStatus());
            }
        }

        if (floorCombo != null) {
            floorCombo.setItems(floors);
            floorCombo.getSelectionModel().select("All Floors");
        }
        if (typeCombo != null) {
            typeCombo.setItems(types);
            typeCombo.getSelectionModel().select("All Types");
        }
        if (statusCombo != null) {
            statusCombo.setItems(statuses);
            statusCombo.getSelectionModel().select("All Statuses");
        }

        renderRooms();
    }

    private void loadRooms() {
        ApiFuture<QuerySnapshot> future = FirebaseInit.db.collection("rooms").get();
        future.addListener(() -> {
            try {
                List<QueryDocumentSnapshot> documents = future.get().getDocuments();
                List<Room> fetched = new ArrayList<>();
                for (QueryDocumentSnapshot document : documents) {
                    Room room = document.toObject(Room.class);
                    fetched.add(room);
                }
                Platform.runLater(() -> {
                    roomList.setAll(fetched);
                    RoomStore.getInstance().setRooms(fetched);
                    populateBuildings();
                    renderRooms(); // Initial render
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, Runnable::run);
    }

    private void populateBuildings() {
        if (buildingCombo == null) return;

        // Add "All Buildings" option
        Building allBuildings = new Building();
        allBuildings.setId(null); // Use null ID as identifier for "All"
        allBuildings.setName("All Buildings");

        var buildings = FXCollections.observableArrayList(allBuildings);

        // Gather unique buildings from roomList
        for (Room r : roomList) {
            Building b = new Building();
            b.setId(r.getBuildingId());
            b.setName(r.getBuildingName());
            boolean exists = false;
            for (Building ex : buildings) if (Objects.equals(ex.getId(), b.getId())) { exists = true; break; }
            if (!exists) buildings.add(b);
        }
        buildingCombo.setItems(buildings);
        buildingCombo.getSelectionModel().select(allBuildings); // Select "All Buildings" by default
    }

    /**
     * Renders room cards from roomList, applying filters.
     */
    private void renderRooms() {
        // Filter rooms by selections
        Building selectedBuilding = buildingCombo == null ? null : buildingCombo.getValue();
        String selectedFloor = floorCombo == null ? null : floorCombo.getValue();
        String selectedType = typeCombo == null ? null : typeCombo.getValue();
        String selectedStatus = statusCombo == null ? null : statusCombo.getValue();

        List<Room> filtered = new ArrayList<>();
        for (Room r : roomList) {
            // Apply filters
            if (selectedBuilding != null && selectedBuilding.getId() != null && !Objects.equals(r.getBuildingId(), selectedBuilding.getId())) continue;
            if (selectedFloor != null && !selectedFloor.equals("All Floors") && !String.valueOf(r.getFloor()).equals(selectedFloor)) continue;
            if (selectedType != null && !selectedType.equals("All Types") && !Objects.equals(r.getRoomType(), selectedType)) continue;
            if (selectedStatus != null && !selectedStatus.equals("All Statuses") && !Objects.equals(r.getStatus(), selectedStatus)) continue;
            filtered.add(r);
        }

        // Sort by room number
        filtered.sort(Comparator.comparing(r -> safeString(r.getRoomNumber())));

        Platform.runLater(() -> {
            roomsContainer.getChildren().clear();
            for (Room room : filtered) {
                VBox card = createRoomCard(room);
                roomsContainer.getChildren().add(card);
            }
        });
    }

    private String safeString(String s) {
        return s == null ? "" : s.toLowerCase();
    }

    @FXML
    private void onAddRoomClick() {
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

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add New Room");
        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<Building> buildingComboBox = new ComboBox<>(buildings);
        buildingComboBox.setPromptText("Select Building");
        buildingComboBox.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Building item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item.getName());
            }
        });
        buildingComboBox.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Building item, boolean empty) {
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

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                Building selectedBuilding = buildingComboBox.getValue();
                if (selectedBuilding == null) throw new Exception("Building must be selected.");

                Room newRoom = new Room();
                newRoom.setBuildingId(selectedBuilding.getId());
                newRoom.setBuildingName(selectedBuilding.getName());
                newRoom.setRoomNumber(roomNumberField.getText());
                newRoom.setFloor(Integer.parseInt(floorField.getText()));
                newRoom.setRoomType(typeField.getText());
                newRoom.setRate(Double.parseDouble(rateField.getText()));
                newRoom.setCapacity(Integer.parseInt(capacityField.getText()));
                newRoom.setStatus("Available");

                String newRoomId = UUID.randomUUID().toString();
                newRoom.setId(newRoomId);
                DocumentReference docRef = FirebaseInit.db.collection("rooms").document(newRoomId);
                docRef.set(newRoom);

                roomList.add(newRoom);
                RoomStore.getInstance().addRoom(newRoom);
                populateBuildings();
                renderRooms();

            } catch (Exception e) {
                e.printStackTrace();
                // --- TODO FIX: Show an error alert ---
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Failed to Add Room");
                alert.setContentText("Please check your inputs and try again. " + e.getMessage());
                alert.showAndWait();
                // --- END FIX ---
            }
        }
    }

    /**
     * Gets the current occupancy count for a specific room.
     * @param roomId The ID of the room to check.
     * @return The number of tenants assigned to that room.
     */
    private int getOccupancyForRoom(String roomId) {
        int count = 0;
        if (roomId == null) return 0;
        for (Tenant tenant : tenantList) {
            if (roomId.equals(tenant.getRoomID())) {
                count++;
            }
        }
        return count;
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

        String statusText = room.getStatus() == null ? "Available" : room.getStatus();
        Label status = new Label(statusText);
        if ("Occupied".equalsIgnoreCase(statusText)) {
            status.setStyle("-fx-background-color: #FFEBEE; -fx-text-fill: #C62828; -fx-padding: 4 8; -fx-background-radius: 4;");
        } else if ("Maintenance".equalsIgnoreCase(statusText)) {
            status.setStyle("-fx-background-color: #FFF8E1; -fx-text-fill: #F9A825; -fx-padding: 4 8; -fx-background-radius: 4;");
        } else { // Available
            status.setStyle("-fx-background-color: #E8F5E9; -fx-text-fill: #2E7D32; -fx-padding: 4 8; -fx-background-radius: 4;");
        }

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

        int currentOccupancy = getOccupancyForRoom(room.getId());
        addDetailRow(details, 4, "Capacity:", currentOccupancy + "/" + room.getCapacity());

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setStyle("-fx-padding: 10 0 0 0;");

        Button viewDetails = new Button("View Details");
        viewDetails.setStyle("-fx-background-color: transparent; -fx-border-color: #1A1A1A; -fx-border-radius: 4;");

        MenuButton more = new MenuButton("More");
        more.setStyle("-fx-background-color: transparent;");
        MenuItem editItem = new MenuItem("Edit");
        editItem.setOnAction(e -> openEditRoomDialog(room));
        MenuItem deleteItem = createDeleteMenuItem(room.getId(), card);

        // --- START FIX ---
        // We check the status when the menu item is created to set the text
        boolean isCurrentlyOccupied = "Occupied".equalsIgnoreCase(room.getStatus());
        MenuItem statusToggle = new MenuItem(isCurrentlyOccupied ? "Mark as Available" : "Mark as Occupied");

        statusToggle.setOnAction(e -> {
            // But here, we check the status *again* to be 100% sure
            boolean stillOccupied = "Occupied".equalsIgnoreCase(room.getStatus());
            String newStatus = stillOccupied ? "Available" : "Occupied";

            try {
                Map<String, Object> updates = new HashMap<>();
                updates.put("status", newStatus);
                FirebaseInit.db.collection("rooms").document(room.getId()).update(updates);

                // Update local model
                room.setStatus(newStatus);

                // Update RoomStore using your pattern
                RoomStore.getInstance().removeById(room.getId());
                RoomStore.getInstance().addRoom(room);

                // Re-render all cards to reflect the change
                renderRooms();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        // --- END FIX ---

        more.getItems().addAll(editItem, deleteItem, statusToggle);

        actions.getChildren().addAll(viewDetails, more);
        card.getChildren().addAll(header, separator, details, actions);
        return card;
    }

    /**
     * Opens a dialog to edit Room Type and Rate.
     * Capacity is still auto-calculated from Room Type.
     */
    private void openEditRoomDialog(Room room) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Room");
        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<String> typeSelect = new ComboBox<>();
        typeSelect.getItems().addAll("Single", "Shared(D)", "Shared");
        typeSelect.setValue(room.getRoomType() == null ? "Single" : room.getRoomType());

        TextField rateField = new TextField(String.valueOf(room.getRate()));
        rateField.setPromptText("Rate per bed");

        VBox content = new VBox(10,
                new Label("Room Type:"), typeSelect,
                new Label("Rate (per bed):"), rateField
        );
        content.setStyle("-fx-padding: 10;");
        pane.setContent(content);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                String newType = typeSelect.getValue();
                int newCapacity = capacityForType(newType);
                double newRate = Double.parseDouble(rateField.getText());

                Map<String, Object> updates = new HashMap<>();
                updates.put("roomType", newType);
                updates.put("capacity", newCapacity);
                updates.put("rate", newRate);

                FirebaseInit.db.collection("rooms").document(room.getId()).update(updates).addListener(() -> {
                    Platform.runLater(() -> {
                        // Update local model
                        room.setRoomType(newType);
                        room.setCapacity(newCapacity);
                        room.setRate(newRate);

                        // --- THIS IS THE FIX ---
                        // Use the correct remove/add pattern
                        RoomStore.getInstance().removeById(room.getId());
                        RoomStore.getInstance().addRoom(room);
                        // --- END FIX ---

                        renderRooms();
                    });
                }, Runnable::run);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Map the room type to capacity according to rules: Single=1, Shared(D)=2, Shared=4
    private int capacityForType(String type) {
        if (type == null) return 1;
        switch (type) {
            case "Shared(D)": return 2;
            case "Shared": return 4;
            case "Single":
            default:
                return 1;
        }
    }

    private MenuItem createDeleteMenuItem(String documentId, Node cardToRemove) {
        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setOnAction(e -> {
            // --- TODO FIX: Add "Are you sure?" confirmation ---
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirm Deletion");
            alert.setHeaderText("Delete Room");
            alert.setContentText("Are you sure you want to delete this room? This action cannot be undone.");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                // User clicked OK, proceed with delete
                ApiFuture<WriteResult> deleteFuture = FirebaseInit.db.collection("rooms").document(documentId).delete();
                deleteFuture.addListener(() -> {
                    Platform.runLater(() -> {
                        roomsContainer.getChildren().remove(cardToRemove);
                        roomList.removeIf(r -> Objects.equals(r.getId(), documentId));
                        RoomStore.getInstance().removeById(documentId);
                    });
                }, Runnable::run);
            }
            // --- END FIX ---
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