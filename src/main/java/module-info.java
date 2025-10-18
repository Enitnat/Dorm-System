module com.example.dormmanagement {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.dormmanagement to javafx.fxml;
    exports com.example.dormmanagement;
}