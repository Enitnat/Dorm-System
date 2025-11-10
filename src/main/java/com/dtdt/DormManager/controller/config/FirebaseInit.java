package com.dtdt.DormManager.controller.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import java.io.InputStream;

public class FirebaseInit {

    // This is the static database instance your friend mentioned
    public static Firestore db;

    public static void initialize() {
        try {
            // Your friend was right, name the file "serviceAccountKey.json"
            // Make sure this file is in your 'src/main/resources' folder
            InputStream serviceAccount = FirebaseInit.class.getResourceAsStream("/serviceAccountKey.json");

            if (serviceAccount == null) {
                throw new RuntimeException("Cannot find serviceAccountKey.json. Make sure it's in src/main/resources");
            }

            GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .build();

            // Check if app is already initialized to avoid errors
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                System.out.println("Firebase initialized successfully!");
            }

            db = FirestoreClient.getFirestore();

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error initializing Firebase: " + e.getMessage());
        }
    }
}