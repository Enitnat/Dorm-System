package com.dtdt.DormManager.controller;

// === JavaFX Imports ===
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

// === Java IO Import ===
import java.io.IOException;

// === Model Imports ===
import com.dtdt.DormManager.model.Admin;
import com.dtdt.DormManager.controller.admin.AdminDashboardController;
import com.dtdt.DormManager.model.Owner;
import com.dtdt.DormManager.model.Tenant;
import com.dtdt.DormManager.model.User;

// === Main App Import ===
import com.dtdt.DormManager.Main;

public class LoginController {

    // === Login View Components ===
    @FXML private TextField studentIdField;
    @FXML private TextField emailFieldLogin;
    @FXML private PasswordField passwordFieldLogin;
    @FXML private Button signInButton;

    // === Sign Up View Components ===
    @FXML private TextField fullNameField;
    @FXML private TextField studentIdFieldSignUp;
    @FXML private TextField emailFieldSignUp;
    @FXML private PasswordField passwordFieldSignUp;
    @FXML private PasswordField confirmPasswordField;
    @FXML private ComboBox<String> currentYearBox;
    @FXML private Button createAccountButton;

    /**
     * This method is automatically called after the FXML is loaded.
     * We use it to populate the ComboBox.
     */
    @FXML
    public void initialize() {
        // This check is important because the ComboBox only exists in signup-view.fxml
        // It prevents errors when loading login-view.fxml
        if (currentYearBox != null) {
            currentYearBox.setItems(FXCollections.observableArrayList(
                    "1st Year", "2nd Year", "3rd Year", "4th Year", "5th Year"
            ));
        }
    }

    /**
     * Handles the "Sign In" button click.
     * @param event The ActionEvent from the button click.
     */
    @FXML
    protected void onSignInClick(ActionEvent event) throws IOException {
        System.out.println("Sign In button clicked.");

        // === 1. DUMMY AUTHENTICATION ===
        // TODO: Replace this with real database authentication
        // Simple credential check to demonstrate role-based navigation.
        // If the credentials match the built-in admin account, create an Admin.
        // Otherwise we create a Tenant for demonstration (existing behavior).
        User user;
        String idInput = studentIdField.getText() == null ? "" : studentIdField.getText().trim();
        String emailInput = emailFieldLogin.getText() == null ? "" : emailFieldLogin.getText().trim();
        String pwInput = passwordFieldLogin.getText() == null ? "" : passwordFieldLogin.getText();

        // Hard-coded admin credential (change as needed)
        if ((idInput.equalsIgnoreCase("admin") || emailInput.equalsIgnoreCase("admin@dorm.local"))
                && pwInput.equals("adminpass")) {
            user = new Admin("admin", "admin@dorm.local", "adminpass", "System Admin", "Manager");
        } else {
            // Default to tenant (existing demo behavior)
            user = new Tenant(
                    idInput,                    // student id
                    emailInput,                 // email
                    pwInput,                    // password
                    "Tenant Name",             // Full Name (demo)
                    2                           // Current Year (demo)
            );
        }
        // --- End of Dummy Auth ---


        // === 2. CHECK USER ROLE AND LOAD DASHBOARD ===
        if (user instanceof Tenant) {
            // Load the tenant dashboard
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("/com/dtdt/DormManager/view/tenant-dashboard.fxml"));
            Parent root = loader.load();

            // Get the controller of the new dashboard
            TenantDashboardController controller = loader.getController();

            // Pass the logged-in tenant to the controller
            controller.initData((Tenant) user);

            // Get the current stage (window) and change the scene
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("Tenant Dashboard");



        } else if (user instanceof Admin) {
                // Load the admin dashboard
                FXMLLoader loader = new FXMLLoader(Main.class.getResource("/com/dtdt/DormManager/view/admin/admin-dashboard.fxml"));
                Parent root = loader.load();

                // Get the controller and pass the admin data
                AdminDashboardController controller = loader.getController();
                controller.initData((Admin) user);

                // Get the current stage and set the scene
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.getScene().setRoot(root);
                stage.setTitle("Admin Dashboard");
        } else if (user instanceof Owner) {
            // TODO: Build this
            // main.changeScene("owner-dashboard.fxml");
        }
    }

    /**
     * Handles the "Create Account" button click.
     */
    @FXML
    protected void onCreateAccountClick() throws IOException {
        System.out.println("Create Account button clicked.");

        // 1. Check if passwords match
        if (!passwordFieldSignUp.getText().equals(confirmPasswordField.getText())) {
            System.err.println("Passwords do not match!");
            // TODO: Show a visible error message to the user
            return;
        }

        // 2. Get the selected year (as an integer)
        int year = 0;
        String selectedYear = currentYearBox.getValue();
        if (selectedYear != null && !selectedYear.isEmpty()) {
            year = Integer.parseInt(selectedYear.substring(0, 1));
        }

        // 3. Create a new Tenant object from the model
        Tenant newTenant = new Tenant(
                studentIdFieldSignUp.getText(),
                emailFieldSignUp.getText(),
                passwordFieldSignUp.getText(), // TODO: Hash this password!
                fullNameField.getText(),
                year
        );

        // 4. TODO: Save this 'newTenant' object to your database
        System.out.println("New Tenant Created: " + newTenant.getFullName());

        // 5. After successful creation, automatically switch to the login screen
        goToSignIn(null); // Pass 'null' because it's not a mouse click event
    }



    @FXML
    protected void goToReservation(MouseEvent event) throws IOException {
        Main main = new Main();
        main.changeScene("reservation-view.fxml"); // Make sure this is updated too
    }

    @FXML
    protected void goToSignIn(MouseEvent event) throws IOException {
        Main main = new Main();
        main.changeScene("login-view.fxml");
    }
}