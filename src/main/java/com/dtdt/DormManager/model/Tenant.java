package com.dtdt.DormManager.model;

public class Tenant extends User {

    // --- THIS IS THE FIX ---
    private String currentYear; // Changed from int to String
    // --- END FIX ---

    private String assignedRoomID;

    public Tenant() {}

    // Constructor updated for String
    public Tenant(String userId, String email, String passwordHash, String fullName, String currentYear) {
        super(userId, email, passwordHash, fullName);
        this.currentYear = currentYear;
    }

    // --- Getters and Setters Updated ---

    public String getCurrentYear() { return currentYear; }
    public void setCurrentYear(String currentYear) { this.currentYear = currentYear; }

    public String getAssignedRoomID() { return assignedRoomID; }
    public void setAssignedRoomID(String assignedRoomID) { this.assignedRoomID = assignedRoomID; }
}