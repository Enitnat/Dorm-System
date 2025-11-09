module com.dtdt.DormManager {
    requires javafx.controls;
    requires javafx.fxml;

    // Firebase dependencies
    requires firebase.admin;
    requires com.google.auth.oauth2;
    requires com.google.auth;
    requires google.cloud.firestore;
    requires com.google.api.apicommon;
    requires com.google.common;
    requires com.google.gson;

    // This is the important part!
    // It allows JavaFX to access your controllers and models
    opens com.dtdt.DormManager to javafx.fxml;
    opens com.dtdt.DormManager.controller to javafx.fxml;
    opens com.dtdt.DormManager.controller.admin to javafx.fxml;
    opens com.dtdt.DormManager.model to javafx.fxml;

    exports com.dtdt.DormManager;
}