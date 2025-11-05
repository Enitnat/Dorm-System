module com.dtdt.DormManager {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.dtdt.DormManager to javafx.fxml;
    exports com.dtdt.DormManager;
}