package com.dtdt.DormManager.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import com.dtdt.DormManager.model.Admin;
import com.dtdt.DormManager.controller.admin.AdminDashboardController;
import com.dtdt.DormManager.model.Tenant;
import com.dtdt.DormManager.model.User;
import com.dtdt.DormManager.Main;
import com.dtdt.DormManager.controller.config.FirebaseInit; // Import Firebase
import com.google.cloud.firestore.Firestore;
// Unused imports from sign-up have been removed

public class LoginController {

    // === Login View Components ===
    @FXML private TextField studentIdField;
    @FXML private TextField emailFieldLogin;
    @FXML private PasswordField passwordFieldLogin;
    @FXML private TextField passwordTextFieldLogin;
    @FXML private Button signInButton;
    @FXML private Button togglePasswordLoginBtn;
    @FXML private Label loginErrorLabel;

    // === All Sign-Up @FXML variables have been removed ===

    // Password visibility flag
    private boolean isPasswordVisibleLogin = false;

    /**
     * This method is automatically called after the FXML is loaded.
     */
    @FXML
    public void initialize() {
        // Bind TextFields to PasswordFields for password visibility toggle
        if (passwordTextFieldLogin != null && passwordFieldLogin != null) {
            passwordTextFieldLogin.textProperty().bindBidirectional(passwordFieldLogin.textProperty());
        }

        // Bind managed property to visible property for error labels
        if (loginErrorLabel != null) {
            loginErrorLabel.managedProperty().bind(loginErrorLabel.visibleProperty());
            loginErrorLabel.setVisible(false); // Start hidden
        }
    }

    /**
     * Hashes a password using SHA-256.
     */
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Error hashing password: " + e.getMessage());
            return null;
        }
    }

    @FXML
    protected void onSignInClick(ActionEvent event) throws IOException {
        System.out.println("Sign In button clicked.");

        String idInput = studentIdField.getText() == null ? "" : studentIdField.getText().trim();
        String emailInput = emailFieldLogin.getText() == null ? "" : emailFieldLogin.getText().trim();
        String pwInput = passwordFieldLogin.getText() == null ? "" : passwordFieldLogin.getText();

        Firestore db = FirebaseInit.db;
        if (db == null) {
            loginErrorLabel.setText("Database connection not established.");
            loginErrorLabel.setVisible(true);
            return;
        }

        // Check for admin login first
        // TODO: Move this to Firebase as well
        if ((idInput.equalsIgnoreCase("admin") || emailInput.equalsIgnoreCase("admin@dorm.local"))
                && pwInput.equals("adminpass")) {
            User user = new Admin("admin", "admin@dorm.local", "adminpass", "System Admin", "Manager");

            FXMLLoader loader = new FXMLLoader(Main.class.getResource("/com/dtdt/DormManager/view/admin/admin-dashboard.fxml"));
            Parent root = loader.load();

            AdminDashboardController controller = loader.getController();
            controller.initData((Admin) user);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("Admin Dashboard");

            // Resize window for admin dashboard
            stage.sizeToScene();
            stage.centerOnScreen();

            loginErrorLabel.setVisible(false);
            return;
        }

        // Authenticate tenant with Firebase
        try {
            // Query for user by student ID
            var query = db.collection("users")
                    .whereEqualTo("userId", idInput) // Query for "userId"
                    .get()
                    .get();

            if (query.getDocuments().isEmpty()) {
                // No user found
                loginErrorLabel.setText("Invalid Credentials");
                loginErrorLabel.setVisible(true);
                return;
            }

            var userDoc = query.getDocuments().get(0);
            Tenant tenant = userDoc.toObject(Tenant.class); // Convert directly to Tenant

            // Verify password
            String storedPasswordHash = tenant.getPasswordHash();
            String inputPasswordHash = hashPassword(pwInput);

            if (storedPasswordHash == null || !storedPasswordHash.equals(inputPasswordHash)) {
                // Password doesn't match
                loginErrorLabel.setText("Invalid Credentials");
                loginErrorLabel.setVisible(true);
                return;
            }

            // Authentication successful - Navigate to tenant dashboard
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("/com/dtdt/DormManager/view/tenant-dashboard.fxml"));
            Parent root = loader.load();

            TenantDashboardController controller = loader.getController();
            controller.initData(tenant); // Pass the Tenant object

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("Tenant Dashboard");

            loginErrorLabel.setVisible(false);

        } catch (Exception e) {
            System.err.println("Error during authentication: " + e.getMessage());
            e.printStackTrace();
            loginErrorLabel.setText("Invalid Credentials");
            loginErrorLabel.setVisible(true);
        }
    }

    /**
     * Handles the "Make a Reservation" text link click.
     */
    @FXML
    protected void goToReservation(MouseEvent event) throws IOException {
        Main main = new Main();
        main.changeScene("reservation-view.fxml");
    }

    /**
     * Handles the "Sign in" text link click (from reservation-view).
     */
    @FXML
    protected void goToSignIn(MouseEvent event) throws IOException {
        Main main = new Main();
        main.changeScene("login-view.fxml");
    }

    /**
     * Toggles password visibility for the login form.
     */
    @FXML
    protected void togglePasswordVisibilityLogin() {
        isPasswordVisibleLogin = !isPasswordVisibleLogin;
        if (isPasswordVisibleLogin) {
            passwordTextFieldLogin.setVisible(true);
            passwordFieldLogin.setVisible(false);
            togglePasswordLoginBtn.setText("\uD83D\uDE48"); // 🙈
        } else {
            passwordFieldLogin.setVisible(true);
            passwordTextFieldLogin.setVisible(false);
            togglePasswordLoginBtn.setText("\uD83D\uDC41"); // 👁
        }
    }

    // All old sign-up methods have been removed
}