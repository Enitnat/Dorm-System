package com.dtdt.DormManager;

// 1. IMPORT your new Firebase config class
import com.dtdt.DormManager.controller.config.FirebaseInit;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {

    private static Stage stg;

    @Override
    public void start(Stage primaryStage) throws IOException {
        stg = primaryStage;
        //primaryStage.setResizable(false);
        primaryStage.setTitle("Dorm Management System");

        // 2. INITIALIZE Firebase right at the start
        FirebaseInit.initialize();

        // 3. (Recommended Fix) Use the full, absolute path for your FXML
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/com/dtdt/DormManager/view/login-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 900, 600);

        primaryStage.setScene(scene);
        primaryStage.show();
    }


    public void changeScene(String fxml) throws IOException {
        // 3. (Recommended Fix) Use the full, absolute path here as well
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/com/dtdt/DormManager/view/" + fxml));
        stg.getScene().setRoot(fxmlLoader.load());
    }

    public static void main(String[] args) {
        launch();
    }
}