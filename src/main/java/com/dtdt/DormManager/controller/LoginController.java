package com.dtdt.DormManager.controller;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import com.dtdt.DormManager.model.Admin;
import com.dtdt.DormManager.controller.admin.AdminDashboardController;
import com.dtdt.DormManager.model.Tenant;
import com.dtdt.DormManager.model.User;
import com.dtdt.DormManager.Main;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;

public class LoginController {

    // === Login View Components ===
    @FXML private TextField studentIdField;
    @FXML private TextField emailFieldLogin;
    @FXML private PasswordField passwordFieldLogin;
    @FXML private TextField passwordTextFieldLogin;
    @FXML private Button signInButton;
    @FXML private Button togglePasswordLoginBtn;
    @FXML private Label loginErrorLabel;

    // === Sign Up View Components ===
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField studentIdFieldSignUp;
    @FXML private TextField emailFieldSignUp;
    @FXML private PasswordField passwordFieldSignUp;
    @FXML private TextField passwordTextFieldSignUp;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField confirmPasswordTextField;
    @FXML private ComboBox<String> currentYearBox;
    @FXML private ComboBox<String> genderBox;
    @FXML private Button createAccountButton;
    @FXML private Button togglePasswordSignUpBtn;
    @FXML private Button toggleConfirmPasswordBtn;
    
    // === Error Labels ===
    @FXML private Label passwordErrorLabel;
    @FXML private Label yearLevelErrorLabel;
    @FXML private Label studentIdErrorLabel;

    // Password visibility flags
    private boolean isPasswordVisibleLogin = false;
    private boolean isPasswordVisibleSignUp = false;
    private boolean isConfirmPasswordVisible = false;

    /**
     * This method is automatically called after the FXML is loaded.
     * We use it to populate the ComboBox and set up listeners for validation.
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

        if (genderBox != null) {
            genderBox.setItems(FXCollections.observableArrayList("Male", "Female"));
        }
        
        // Limit student ID to 7 characters
        if (studentIdFieldSignUp != null) {
            studentIdFieldSignUp.textProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null && newValue.length() > 7) {
                    studentIdFieldSignUp.setText(oldValue);
                }
            });
        }

        // Add listeners for form validation
        if (createAccountButton != null) {
            createAccountButton.setDisable(true); // Initially disabled
            
            // Add listeners to all fields to validate on change
            if (firstNameField != null) firstNameField.textProperty().addListener((obs, old, newVal) -> validateForm());
            if (lastNameField != null) lastNameField.textProperty().addListener((obs, old, newVal) -> validateForm());
            if (studentIdFieldSignUp != null) studentIdFieldSignUp.textProperty().addListener((obs, old, newVal) -> validateForm());
            if (emailFieldSignUp != null) emailFieldSignUp.textProperty().addListener((obs, old, newVal) -> validateForm());
            if (passwordFieldSignUp != null) passwordFieldSignUp.textProperty().addListener((obs, old, newVal) -> validateForm());
            if (confirmPasswordField != null) confirmPasswordField.textProperty().addListener((obs, old, newVal) -> validateForm());
            if (currentYearBox != null) currentYearBox.valueProperty().addListener((obs, old, newVal) -> validateForm());
            if (genderBox != null) genderBox.valueProperty().addListener((obs, old, newVal) -> validateForm());
        }
        
        // Bind TextFields to PasswordFields for password visibility toggle
        if (passwordTextFieldLogin != null && passwordFieldLogin != null) {
            passwordTextFieldLogin.textProperty().bindBidirectional(passwordFieldLogin.textProperty());
        }
        if (passwordTextFieldSignUp != null && passwordFieldSignUp != null) {
            passwordTextFieldSignUp.textProperty().bindBidirectional(passwordFieldSignUp.textProperty());
        }
        if (confirmPasswordTextField != null && confirmPasswordField != null) {
            confirmPasswordTextField.textProperty().bindBidirectional(confirmPasswordField.textProperty());
        }
        
        // Bind managed property to visible property for error labels
        if (loginErrorLabel != null) {
            loginErrorLabel.managedProperty().bind(loginErrorLabel.visibleProperty());
        }
        if (passwordErrorLabel != null) {
            passwordErrorLabel.managedProperty().bind(passwordErrorLabel.visibleProperty());
        }
        if (yearLevelErrorLabel != null) {
            yearLevelErrorLabel.managedProperty().bind(yearLevelErrorLabel.visibleProperty());
        }
        if (studentIdErrorLabel != null) {
            studentIdErrorLabel.managedProperty().bind(studentIdErrorLabel.visibleProperty());
        }
    }

    /**
     * Validates the signup form and enables/disables the Create Account button.
     */
    private void validateForm() {
        boolean isValid = firstNameField.getText() != null && !firstNameField.getText().trim().isEmpty()
                && lastNameField.getText() != null && !lastNameField.getText().trim().isEmpty()
                && studentIdFieldSignUp.getText() != null && !studentIdFieldSignUp.getText().trim().isEmpty()
                && emailFieldSignUp.getText() != null && !emailFieldSignUp.getText().trim().isEmpty()
                && passwordFieldSignUp.getText() != null && !passwordFieldSignUp.getText().isEmpty()
                && confirmPasswordField.getText() != null && !confirmPasswordField.getText().isEmpty()
                && currentYearBox.getValue() != null
                && genderBox.getValue() != null;
        
        createAccountButton.setDisable(!isValid);
    }
    
    /**
     * Validates password length (minimum 8 characters).
     * Returns true if valid, false otherwise.
     */
    private boolean validatePasswordLength(String password) {
        if (password.length() < 8) {
            passwordErrorLabel.setText("Your password must be at least 8 characters.");
            passwordErrorLabel.setVisible(true);
            return false;
        } else {
            passwordErrorLabel.setVisible(false);
            return true;
        }
    }
    
    /**
     * Validates that student ID matches the selected year level.
     * 21 - 5th Year, 22 - 4th Year, 23 - 3rd Year, 24 - 2nd Year, 25 - 1st Year
     * Returns true if valid, false otherwise.
     */
    private boolean validateYearLevelMatch(String studentId, String yearLevel) {
        if (studentId == null || studentId.length() < 2 || yearLevel == null) {
            return false;
        }
        
        String yearPrefix = studentId.substring(0, 2);
        String expectedPrefix = null;
        
        switch (yearLevel) {
            case "5th Year":
                expectedPrefix = "21";
                break;
            case "4th Year":
                expectedPrefix = "22";
                break;
            case "3rd Year":
                expectedPrefix = "23";
                break;
            case "2nd Year":
                expectedPrefix = "24";
                break;
            case "1st Year":
                expectedPrefix = "25";
                break;
        }
        
        if (!yearPrefix.equals(expectedPrefix)) {
            yearLevelErrorLabel.setText("Your Student ID does not match your Year Level. Please try again.");
            yearLevelErrorLabel.setVisible(true);
            return false;
        } else {
            yearLevelErrorLabel.setVisible(false);
            return true;
        }
    }
    
    /**
     * Checks if a student ID already exists in the Firebase database.
     * Returns true if it exists, false otherwise.
     */
    private boolean checkStudentIdExists(String studentId) {
        try {
            Firestore db = FirestoreClient.getFirestore();
            // Query the users collection for any document with this studentID
            var query = db.collection("users")
                    .whereEqualTo("studentID", studentId)
                    .get()
                    .get();
            
            if (!query.getDocuments().isEmpty()) {
                studentIdErrorLabel.setText("This Student ID already has an account.");
                studentIdErrorLabel.setVisible(true);
                return true;
            } else {
                studentIdErrorLabel.setVisible(false);
                return false;
            }
        } catch (Exception e) {
            System.err.println("Error checking student ID: " + e.getMessage());
            e.printStackTrace();
            return false;
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

    /**
     * Handles the "Sign In" button click.
     */
    @FXML
    protected void onSignInClick(ActionEvent event) throws IOException {
        System.out.println("Sign In button clicked.");

        String idInput = studentIdField.getText() == null ? "" : studentIdField.getText().trim();
        String emailInput = emailFieldLogin.getText() == null ? "" : emailFieldLogin.getText().trim();
        String pwInput = passwordFieldLogin.getText() == null ? "" : passwordFieldLogin.getText();

        // Check for admin login first
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
            
            loginErrorLabel.setVisible(false);
            return;
        }

        // Authenticate tenant with Firebase
        try {
            Firestore db = FirestoreClient.getFirestore();
            
            // Query for user by student ID
            var query = db.collection("users")
                    .whereEqualTo("studentID", idInput)
                    .get()
                    .get();
            
            if (query.getDocuments().isEmpty()) {
                // No user found with this student ID
                loginErrorLabel.setText("Invalid Credentials");
                loginErrorLabel.setVisible(true);
                System.err.println("No user found with student ID: " + idInput);
                return;
            }
            
            // Get the user document
            var userDoc = query.getDocuments().get(0);
            Map<String, Object> userData = userDoc.getData();
            
            // Verify password
            String storedPasswordHash = (String) userData.get("passwordHash");
            String inputPasswordHash = hashPassword(pwInput);
            
            if (!storedPasswordHash.equals(inputPasswordHash)) {
                // Password doesn't match
                loginErrorLabel.setText("Invalid Credentials");
                loginErrorLabel.setVisible(true);
                System.err.println("Password mismatch for student ID: " + idInput);
                return;
            }
            
            // Authentication successful - create Tenant object with actual data
            String firstName = (String) userData.get("firstName");
            String lastName = (String) userData.get("lastName");
            String email = (String) userData.get("email");
            String studentID = (String) userData.get("studentID");
            String currentYearStr = (String) userData.get("currentYear");
            
            // Parse current year (e.g., "1st Year" -> 1)
            int currentYear = 1;
            if (currentYearStr != null) {
                currentYear = Integer.parseInt(currentYearStr.substring(0, 1));
            }
            
            // Create Tenant object with full name as "Last Name, First Name"
            String fullName = lastName + ", " + firstName;
            User user = new Tenant(studentID, email, pwInput, fullName, currentYear);
            
            // Navigate to tenant dashboard
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("/com/dtdt/DormManager/view/tenant-dashboard.fxml"));
            Parent root = loader.load();

            TenantDashboardController controller = loader.getController();
            controller.initData((Tenant) user);

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
     * Handles the "Create Account" button click.
     */
    @FXML
    protected void onCreateAccountClick() throws IOException {
        System.out.println("Create Account button clicked.");

        // 1. Get form data
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String studentId = studentIdFieldSignUp.getText().trim();
        String email = emailFieldSignUp.getText().trim();
        String password = passwordFieldSignUp.getText();
        String confirmPassword = confirmPasswordField.getText();
        String selectedSex = genderBox.getValue();
        String selectedYear = currentYearBox.getValue();

        // 2. Check if student ID already exists
        if (checkStudentIdExists(studentId)) {
            System.err.println("Student ID already exists!");
            return;
        }

        // 3. Validate password length (minimum 8 characters)
        if (!validatePasswordLength(password)) {
            System.err.println("Password is too short!");
            return;
        }

        // 4. Validate year level matches student ID
        if (!validateYearLevelMatch(studentId, selectedYear)) {
            System.err.println("Student ID does not match year level!");
            return;
        }

        // 5. Check if passwords match
        if (!password.equals(confirmPassword)) {
            System.err.println("Passwords do not match!");
            return;
        }

        // 6. Hash the password
        String passwordHash = hashPassword(password);
        if (passwordHash == null) {
            System.err.println("Failed to hash password!");
            return;
        }

        // 7. Generate document ID: FirstLetterLastName + StudentID + GenderLetter
        String genderLetter = selectedSex.equalsIgnoreCase("Male") ? "M" : "F";
        String documentId = lastName.substring(0, 1).toUpperCase() + studentId + genderLetter;

        // 8. Create user data map
        Map<String, Object> userData = new HashMap<>();
        userData.put("firstName", firstName);
        userData.put("lastName", lastName);
        userData.put("genderType", selectedSex);
        userData.put("email", email);
        userData.put("studentID", studentId);
        userData.put("passwordHash", passwordHash);
        userData.put("userType", "Tenant");
        userData.put("currentYear", selectedYear);
        userData.put("roomID", null);
        userData.put("contractID", null);

        // 9. Store in Firebase
        try {
            Firestore db = FirestoreClient.getFirestore();
            db.collection("users").document(documentId).set(userData).get();
            System.out.println("User created successfully with ID: " + documentId);
            
            // 10. After successful creation, switch to login screen
            goToSignIn(null);
        } catch (Exception e) {
            System.err.println("Error creating user in Firebase: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    protected void goToReservation(MouseEvent event) throws IOException {
        Main main = new Main();
        main.changeScene("reservation-view.fxml");
    }

    @FXML
    protected void goToSignIn(MouseEvent event) throws IOException {
        Main main = new Main();
        main.changeScene("login-view.fxml");
    }

    // Hover effects for Create Account button
    @FXML
    protected void onMouseEntered(MouseEvent event) {
        if (event.getSource() instanceof Button) {
            Button button = (Button) event.getSource();
            // Darken the background slightly on hover
            button.setStyle("-fx-background-color: #0D0D0D; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand;");
        }
    }

    @FXML
    protected void onMouseExited(MouseEvent event) {
        if (event.getSource() instanceof Button) {
            Button button = (Button) event.getSource();
            // Return to original color
            button.setStyle("-fx-background-color: #1A1A1A; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand;");
        }
    }

    // Password visibility toggles
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

    @FXML
    protected void togglePasswordVisibilitySignUp() {
        isPasswordVisibleSignUp = !isPasswordVisibleSignUp;
        if (isPasswordVisibleSignUp) {
            passwordTextFieldSignUp.setVisible(true);
            passwordFieldSignUp.setVisible(false);
            togglePasswordSignUpBtn.setText("\uD83D\uDE48"); // 🙈
        } else {
            passwordFieldSignUp.setVisible(true);
            passwordTextFieldSignUp.setVisible(false);
            togglePasswordSignUpBtn.setText("\uD83D\uDC41"); // 👁
        }
    }

    @FXML
    protected void toggleConfirmPasswordVisibility() {
        isConfirmPasswordVisible = !isConfirmPasswordVisible;
        if (isConfirmPasswordVisible) {
            confirmPasswordTextField.setVisible(true);
            confirmPasswordField.setVisible(false);
            toggleConfirmPasswordBtn.setText("\uD83D\uDE48"); // 🙈
        } else {
            confirmPasswordField.setVisible(true);
            confirmPasswordTextField.setVisible(false);
            toggleConfirmPasswordBtn.setText("\uD83D\uDC41"); // 👁
        }
    }
}
