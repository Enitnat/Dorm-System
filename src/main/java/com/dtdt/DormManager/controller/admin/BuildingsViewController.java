package com.dtdt.DormManager.controller.admin;

import com.dtdt.DormManager.Main; // Import Main to access db
import com.dtdt.DormManager.model.Building; // Import your new model
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import javafx.application.Platform; // <-- VERY IMPORTANT for UI updates
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Pos;
import javafx.scene.image.ImageView;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class BuildingsViewController {
    @FXML private VBox buildingsContainer;

    @FXML
    public void initialize() {
        loadBuildings(); // This will now load from Firebase
    }

    @FXML
    private void onAddBuildingClick() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add New Building");
        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField nameField = new TextField();
        nameField.setPromptText("Building Name");
        TextField floorsField = new TextField();
        floorsField.setPromptText("Number of Floors");
        TextField roomsField = new TextField();
        roomsField.setPromptText("Total Rooms");

        VBox content = new VBox(10, nameField, floorsField, roomsField);
        content.setStyle("-fx-padding: 10;");
        pane.setContent(content);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // --- 1. Get data and create Model ---
                String name = nameField.getText();
                int floors = Integer.parseInt(floorsField.getText());
                int rooms = Integer.parseInt(roomsField.getText());

                Building newBuilding = new Building(name, floors, rooms);

                // --- 2. Save to Firebase ---
                // Create a new document with a random ID
                DocumentReference docRef = Main.db.collection("buildings").document(UUID.randomUUID().toString());
                ApiFuture<WriteResult> future = docRef.set(newBuilding);

                // (Optional) You can add a listener to confirm it saved
                future.addListener(() -> {
                    try {
                        System.out.println("Building saved at: " + future.get().getUpdateTime());
                        // Add to UI *after* saving, must use Platform.runLater
                        Platform.runLater(() -> {
                            addBuildingCard(newBuilding);
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }, Runnable::run); // Use a simple executor

            } catch (NumberFormatException e) {
                System.err.println("Invalid number format for floors or rooms.");
                // TODO: Show an error alert to the user
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void loadBuildings() {
        buildingsContainer.getChildren().clear(); // Clear old data

        // --- 1. Asynchronously get all buildings ---
        ApiFuture<QuerySnapshot> future = Main.db.collection("buildings").get();

        // --- 2. Add a listener to run when data is retrieved ---
        future.addListener(() -> {
            try {
                List<QueryDocumentSnapshot> documents = future.get().getDocuments();

                // --- 3. IMPORTANT: Update UI on the JavaFX Application Thread ---
                Platform.runLater(() -> {
                    for (QueryDocumentSnapshot document : documents) {
                        // Convert the Firebase document back into our Building object
                        Building building = document.toObject(Building.class);
                        addBuildingCard(building);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, Runnable::run); // Use a simple executor
    }

    /**
     * Updated to take a Building object directly
     */
    private void addBuildingCard(Building building) {
        VBox card = new VBox();
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 0); -fx-padding: 20;");

        HBox content = new HBox(20);
        content.setAlignment(Pos.CENTER_LEFT);

        ImageView buildingImage = new ImageView(); // Placeholder
        buildingImage.setFitWidth(120);
        buildingImage.setFitHeight(120);
        buildingImage.setPreserveRatio(true);

        VBox details = new VBox(10);

        Label buildingName = new Label(building.getName());
        buildingName.setStyle("-fx-font-size: 20; -fx-font-weight: bold;");
        Label buildingDesc = new Label(building.getFloors() + "-Story Building"); // Use getter
        buildingDesc.setStyle("-fx-text-fill: #666;");

        HBox stats = new HBox(40);
        stats.getChildren().addAll(
                createInfoBox("Occupancy Rate", "85%"), // TODO: Add to model
                createInfoBox("Total Rooms", String.valueOf(building.getTotalRooms())), // Use getter
                createInfoBox("Available Rooms", "6") // TODO: Add to model
        );
        details.getChildren().addAll(buildingName, buildingDesc, stats);

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox actions = new VBox(10);
        actions.setAlignment(Pos.TOP_RIGHT);

        Button viewButton = new Button("View Details");
        viewButton.setStyle("-fx-background-color: transparent; -fx-border-color: #1A1A1A; -fx-border-radius: 4;");
        viewButton.setOnAction(e -> onViewDetailsClick(building.getName()));

        MenuButton more = new MenuButton("More");
        more.setStyle("-fx-background-color: transparent;");
        more.getItems().addAll(
                new MenuItem("Edit"),
                // Pass the building ID to the delete function
                createDeleteMenuItem(building.getId(), card)
        );
        actions.getChildren().addAll(viewButton, more);

        content.getChildren().addAll(buildingImage, details, spacer, actions);
        card.getChildren().add(content);

        buildingsContainer.getChildren().add(card);
    }

    // Helper method to create a "Delete" menu item
    private MenuItem createDeleteMenuItem(String documentId, Node cardToRemove) {
        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setOnAction(e -> {
            // TODO: Add an "Are you sure?" confirmation dialog

            // Delete from Firebase
            ApiFuture<WriteResult> deleteFuture = Main.db.collection("buildings").document(documentId).delete();

            // Add listener to remove from UI *after* successful delete
            deleteFuture.addListener(() -> {
                Platform.runLater(() -> {
                    buildingsContainer.getChildren().remove(cardToRemove);
                });
            }, Runnable::run);
        });
        return deleteItem;
    }

    private VBox createInfoBox(String title, String value) {
        VBox box = new VBox();
        box.setAlignment(Pos.CENTER_RIGHT);
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: #666;");
        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");
        box.getChildren().addAll(titleLabel, valueLabel);
        return box;
    }

    private void onViewDetailsClick(String buildingName) {
        System.out.println("View details for " + buildingName);
    }
}