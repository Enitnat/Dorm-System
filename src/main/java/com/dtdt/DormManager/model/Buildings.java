package com.dtdt.DormManager.model;

import com.google.cloud.firestore.annotation.DocumentId;

// This is a POJO (Plain Old Java Object)
public class Building {

    @DocumentId // This tells Firestore to map the document ID to this field
    private String id;

    private String name;
    private int floors;
    private int totalRooms;

    // IMPORTANT: A no-argument constructor is required by Firestore
    public Building() {}

    public Building(String name, int floors, int totalRooms) {
        this.name = name;
        this.floors = floors;
        this.totalRooms = totalRooms;
    }

    // --- Getters and Setters ---
    // (These are also required by Firestore)

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getFloors() { return floors; }
    public void setFloors(int floors) { this.floors = floors; }

    public int getTotalRooms() { return totalRooms; }
    public void setTotalRooms(int totalRooms) { this.totalRooms = totalRooms; }
}