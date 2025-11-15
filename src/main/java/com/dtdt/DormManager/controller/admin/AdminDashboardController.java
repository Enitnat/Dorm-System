package com.dtdt.DormManager.controller.admin;

import com.dtdt.DormManager.Main;
import com.dtdt.DormManager.model.Admin;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import java.io.IOException;

public class AdminDashboardController {
    @FXML private Label adminNameLabel;
    @FXML private StackPane contentArea;
    @FXML private Button statsButton;
    @FXML private Button buildingsButton;
    @FXML private Button roomsButton;
    @FXML private Button residentsButton;
    @FXML private Button maintenanceButton;
    @FXML private Button reservationsButton;

    private Admin currentAdmin;
    private Node currentContent;

    public void initData(Admin admin) {
        this.currentAdmin = admin;
        adminNameLabel.setText(admin.getFullName());
        
        // Load statistics view by default
        onStatsClick();
    }

    @FXML
    private void onStatsClick() {
        loadView("stats-view.fxml");
        setActiveButton(statsButton);
    }

    @FXML
    private void onBuildingsClick() {
        loadView("buildings-view.fxml");
        setActiveButton(buildingsButton);
    }

    @FXML
    private void onRoomsClick() {
        loadView("rooms-view.fxml");
        setActiveButton(roomsButton);
    }

    @FXML
    private void onResidentsClick() {
        loadView("residents-view.fxml");
        setActiveButton(residentsButton);
    }

    @FXML
    private void onMaintenanceClick() {
        loadView("maintenance-view.fxml");
        setActiveButton(maintenanceButton);
    }

    @FXML
    private void onReservationsClick() {
        loadView("reservations-view.fxml");
        setActiveButton(reservationsButton);
    }

    @FXML
    private void onLogoutClick() throws IOException {
        Main main = new Main();
        main.changeScene("login-view.fxml");
    }

    private void loadView(String fxmlFile) {
        try {
            // Use absolute resource path to avoid classloader issues
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("/com/dtdt/DormManager/view/admin/" + fxmlFile));
            Node view = loader.load();

            // Clear previous content and set new content
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
            currentContent = view;

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error loading view: " + e.getMessage());
        } catch (RuntimeException re) {
            // Catch other runtime exceptions (e.g., missing resource)
            re.printStackTrace();
            System.err.println("Runtime error loading view: " + re.getMessage());
        }
    }

    private void setActiveButton(Button activeButton) {
        // Reset all buttons
    statsButton.setStyle("-fx-background-color: transparent; -fx-alignment: CENTER_LEFT;");
    buildingsButton.setStyle("-fx-background-color: transparent; -fx-alignment: CENTER_LEFT;");
    roomsButton.setStyle("-fx-background-color: transparent; -fx-alignment: CENTER_LEFT;");
    residentsButton.setStyle("-fx-background-color: transparent; -fx-alignment: CENTER_LEFT;");
    maintenanceButton.setStyle("-fx-background-color: transparent; -fx-alignment: CENTER_LEFT;");
    reservationsButton.setStyle("-fx-background-color: transparent; -fx-alignment: CENTER_LEFT;");

        // Set active button style
    activeButton.setStyle("-fx-background-color: #F4F7FC; -fx-alignment: CENTER_LEFT; -fx-font-weight: bold;");
    }
}