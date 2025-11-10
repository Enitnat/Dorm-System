package com.dtdt.DormManager.controller.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import java.io.IOException; // <-- You need this import
import java.io.InputStream;

public class FirebaseInit {

    // This is the static database instance
    public static Firestore db;

    // The initialize method wraps all the logic
    public static void initialize() throws IOException { // <-- Add "throws IOException"
        try {
            // Make sure "serviceAccountKey.json" is in 'src/main/resources'
            InputStream serviceAccount = FirebaseInit.class.getResourceAsStream("/serviceAccountKey.json");

            if (serviceAccount == null) {
                // This provides a much clearer error message
                throw new IOException("Cannot find Key, Misplace?");
            }

            GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials) // <-- This line is critical
                    .build();

            // Check if app is already initialized to avoid errors
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                System.out.println("Firebase App Initialized.");
            }

            db = FirestoreClient.getFirestore();

        } catch (IOException e) {
            System.err.println("Error initializing Firebase: " + e.getMessage());
            e.printStackTrace();
            // Re-throw the exception so Main.java can catch it
            throw e;
        }
    }

    // Add this helper method so Main.java can access the db
    public static Firestore getDatabase() {
        return db;
    }
}