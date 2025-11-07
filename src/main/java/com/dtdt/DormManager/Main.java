package com.dtdt.DormManager;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {

    private static Stage stg; // To store the main window stage

    @Override
    public void start(Stage primaryStage) throws IOException {
        stg = primaryStage; // Save the stage
        //primaryStage.setResizable(false);
        primaryStage.setTitle("Dorm Management System");

   
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("view/login-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 900, 600); 

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    
    public void changeScene(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("view/" + fxml));
        stg.getScene().setRoot(fxmlLoader.load());
    }

    public static void main(String[] args) {
        launch();
    }
}