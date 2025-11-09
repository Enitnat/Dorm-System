package com.dtdt.DormManager.controller.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;

import java.io.IOException;
import java.io.InputStream;

public class FirebaseInit {

    private static Firestore db;

    public static void initialize() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) return;

        InputStream serviceAccount = FirebaseInit.class.getResourceAsStream("/serviceAccountKey.json");
        if (serviceAccount == null) {
            throw new IllegalStateException("Key not Found. (rawr)");
        }

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .setProjectId("dorm-management-sys") // Replace with Firebase Project ID
                .build();

        FirebaseApp.initializeApp(options);
        db = FirestoreClient.getFirestore();
    }

    public static Firestore getDatabase() {
        return db;
    }
}
