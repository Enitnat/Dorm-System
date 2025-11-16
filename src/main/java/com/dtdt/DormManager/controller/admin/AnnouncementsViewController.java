package com.dtdt.DormManager.controller.admin;

import com.dtdt.DormManager.controller.config.FirebaseInit;
import com.dtdt.DormManager.model.Announcement;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class AnnouncementsViewController {

    @FXML private TextField titleField;
    @FXML private TextArea contentArea;
    @FXML private Button postButton;
    @FXML private VBox announcementsListVBox;

    private final Firestore db = FirebaseInit.db;
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("MMMM dd, yyyy 'at' hh:mm a");

    @FXML
    public void initialize() {
        loadAnnouncements();
    }

    @FXML
    private void onPostAnnouncementClick() {
        String title = titleField.getText();
        String content = contentArea.getText();

        if (title.isEmpty() || content.isEmpty()) {
            showError("Missing Fields", "Please enter both a title and content.");
            return;
        }

        // Create new Announcement object
        Announcement announcement = new Announcement();
        announcement.setId(UUID.randomUUID().toString()); // Set a unique ID
        announcement.setTitle(title);
        announcement.setContent(content);
        // datePosted will be set by @ServerTimestamp on the model

        // Save to Firebase
        db.collection("announcements").document(announcement.getId()).set(announcement)
            .addListener(() -> {
                Platform.runLater(() -> {
                    // Clear fields and reload list
                    titleField.clear();
                    contentArea.clear();
                    loadAnnouncements(); // Refresh the list
                });
            }, Runnable::run);
    }

    private void loadAnnouncements() {
        announcementsListVBox.getChildren().clear();

        // Query to get announcements, ordered by date, newest first
        ApiFuture<QuerySnapshot> future = db.collection("announcements")
                                            .orderBy("datePosted", Query.Direction.DESCENDING)
                                            .get();
        
        future.addListener(() -> {
            try {
                List<QueryDocumentSnapshot> documents = future.get().getDocuments();
                Platform.runLater(() -> {
                    if (documents.isEmpty()) {
                        announcementsListVBox.getChildren().add(new Label("No announcements found."));
                    } else {
                        for (QueryDocumentSnapshot document : documents) {
                            Announcement ann = document.toObject(Announcement.class);
                            announcementsListVBox.getChildren().add(createAnnouncementCard(ann));
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, Runnable::run);
    }

    /**
     * Creates a UI card (VBox) for a single announcement.
     */
    private VBox createAnnouncementCard(Announcement announcement) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 20; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 2);");

        // --- Card Header ---
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label title = new Label(announcement.getTitle());
        title.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");
        
        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button deleteButton = new Button("Delete");
        deleteButton.setStyle("-fx-background-color: #FFEBEE; -fx-text-fill: #C62828;");
        deleteButton.setOnAction(e -> handleDelete(announcement, card));
        
        header.getChildren().addAll(title, spacer, deleteButton);

        // --- Card Content ---
        String dateString = (announcement.getDatePosted() != null) ? 
                              dateFormatter.format(announcement.getDatePosted()) : "Just now";
        Label dateLabel = new Label(dateString);
        dateLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #666;");

        Text contentText = new Text(announcement.getContent());
        contentText.setWrappingWidth(600); // Adjust as needed
        
        card.getChildren().addAll(header, dateLabel, new Separator(), contentText);
        return card;
    }

    /**
     * Handles the delete button click on an announcement card.
     */
    private void handleDelete(Announcement announcement, Node cardToRemove) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Deletion");
        alert.setHeaderText("Delete Announcement");
        alert.setContentText("Are you sure you want to delete this announcement: '" + announcement.getTitle() + "'?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Delete from Firebase
            db.collection("announcements").document(announcement.getId()).delete()
                .addListener(() -> {
                    // Remove from UI on success
                    Platform.runLater(() -> {
                        announcementsListVBox.getChildren().remove(cardToRemove);
                    });
                }, Runnable::run);
        }
    }

    private void showError(String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }
}