package com.dtdt.DormManager.controller.admin;

import com.dtdt.DormManager.controller.config.FirebaseInit;
import com.dtdt.DormManager.model.Invoice;
import com.dtdt.DormManager.model.Room;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import java.text.NumberFormat;
import java.util.Locale;

public class StatsViewController {
    @FXML private Label totalResidentsLabel;
    @FXML private Label occupancyRateLabel;
    @FXML private Label pendingMaintenanceLabel;
    @FXML private Label revenueLabel;
    @FXML private LineChart<String, Number> occupancyTrendChart;
    @FXML private BarChart<String, Number> revenueChart;

    private final Firestore db = FirebaseInit.db;
    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));

    @FXML
    public void initialize() {
        // Load real stats from Firebase
        updateStats();

        // --- Dummy Chart Data (as in your file) ---
        // (Real chart data requires complex aggregation, which is
        // usually done with Firebase Functions, not on the client)

        // Setup Occupancy Trend Chart
        XYChart.Series<String, Number> occupancySeries = new XYChart.Series<>();
        occupancySeries.setName("Occupancy %");
        occupancySeries.getData().add(new XYChart.Data<>("Jan", 75));
        occupancySeries.getData().add(new XYChart.Data<>("Feb", 78));
        occupancySeries.getData().add(new XYChart.Data<>("Mar", 82));
        occupancySeries.getData().add(new XYChart.Data<>("Apr", 85));
        occupancySeries.getData().add(new XYChart.Data<>("May", 85));
        occupancySeries.getData().add(new XYChart.Data<>("Jun", 87));
        occupancyTrendChart.getData().add(occupancySeries);

        // Setup Revenue Chart
        XYChart.Series<String, Number> revenueSeries = new XYChart.Series<>();
        revenueSeries.setName("Revenue");
        revenueSeries.getData().add(new XYChart.Data<>("Jan", 950000));
        revenueSeries.getData().add(new XYChart.Data<>("Feb", 980000));
        revenueSeries.getData().add(new XYChart.Data<>("Mar", 1100000));
        revenueSeries.getData().add(new XYChart.Data<>("Apr", 1150000));
        revenueSeries.getData().add(new XYChart.Data<>("May", 1180000));
        revenueSeries.getData().add(new XYChart.Data<>("Jun", 1200000));
        revenueChart.getData().add(revenueSeries);
    }

    /**
     * Fetches and calculates all statistics from Firebase
     */
    private void updateStats() {
        // 1. Get Total Residents (Count users where userType is "Tenant")
        ApiFuture<QuerySnapshot> tenantsFuture = db.collection("users")
                .whereEqualTo("userType", "Tenant")
                .get();
        tenantsFuture.addListener(() -> {
            try {
                int totalTenants = tenantsFuture.get().size();
                Platform.runLater(() -> totalResidentsLabel.setText(String.valueOf(totalTenants)));
            } catch (Exception e) { e.printStackTrace(); }
        }, Runnable::run);

        // 2. Get Occupancy Rate (Total Residents / Total Capacity)
        ApiFuture<QuerySnapshot> roomsFuture = db.collection("rooms").get();
        roomsFuture.addListener(() -> {
            try {
                int totalCapacity = 0;
                for (QueryDocumentSnapshot doc : roomsFuture.get().getDocuments()) {
                    Room room = doc.toObject(Room.class);
                    totalCapacity += room.getCapacity();
                }

                // Get total residents (we have to fetch it again here for this calculation)
                int totalTenants = db.collection("users").whereEqualTo("userType", "Tenant").get().get().size();

                double rate = (totalCapacity == 0) ? 0 : ((double) totalTenants / totalCapacity) * 100;

                Platform.runLater(() -> occupancyRateLabel.setText(String.format("%.0f%%", rate)));

            } catch (Exception e) { e.printStackTrace(); }
        }, Runnable::run);

        // 3. Get Pending Maintenance (Count requests where status is "Pending")
        ApiFuture<QuerySnapshot> maintenanceFuture = db.collection("maintenanceRequests")
                .whereEqualTo("status", "Pending")
                .get();
        maintenanceFuture.addListener(() -> {
            try {
                int pendingCount = maintenanceFuture.get().size();
                Platform.runLater(() -> pendingMaintenanceLabel.setText(String.valueOf(pendingCount)));
            } catch (Exception e) { e.printStackTrace(); }
        }, Runnable::run);

        // 4. Get Total Revenue (Sum of all "Paid" invoices)
        ApiFuture<QuerySnapshot> revenueFuture = db.collection("invoices")
                .whereEqualTo("status", "Paid")
                .get();
        revenueFuture.addListener(() -> {
            try {
                double totalRevenue = 0.0;
                for (QueryDocumentSnapshot doc : revenueFuture.get().getDocuments()) {
                    Invoice invoice = doc.toObject(Invoice.class);
                    totalRevenue += invoice.getTotalAmount();
                }

                double finalTotalRevenue = totalRevenue; // for lambda
                Platform.runLater(() -> revenueLabel.setText(currencyFormatter.format(finalTotalRevenue)));

            } catch (Exception e) { e.printStackTrace(); }
        }, Runnable::run);
    }
}