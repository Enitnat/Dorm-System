package com.dtdt.DormManager;

import com.dtdt.DormManager.controller.config.FirebaseInit;
import com.google.cloud.firestore.Firestore;
import javafx.application.Application;
import javafx.application.Platform; // <-- Import Platform
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {

    private static Stage stg;

    @Override
    public void start(Stage primaryStage) throws IOException {

        // Initialize Firebase
        try {
            FirebaseInit.initialize();
            System.out.println("Firebase initialized successfully");
            // This line is fine, it confirms the DB is ready
            Firestore db = FirebaseInit.getDatabase();
        } catch (IOException e) {
            System.err.println("Failed to initialize Firebase: " + e.getMessage());
            e.printStackTrace();
            // If Firebase fails, the app can't run.
            Platform.exit(); // <-- Exit the application
        }

        stg = primaryStage;
        //primaryStage.setResizable(false);
        primaryStage.setTitle("Dorm Management System");

        // Use the full, absolute path
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/com/dtdt/DormManager/view/login-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 900, 600);

        primaryStage.setScene(scene);
        primaryStage.show();
    }


    public void changeScene(String fxml) throws IOException {
        // Use the full, absolute path here too
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/com/dtdt/DormManager/view/" + fxml));
        stg.getScene().setRoot(fxmlLoader.load());
    }

    public static void main(String[] args) {
        launch();
    }
}