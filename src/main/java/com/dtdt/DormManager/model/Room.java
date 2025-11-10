package com.dtdt.DormManager.model;

import com.google.cloud.firestore.annotation.DocumentId;

public class Room {

    @DocumentId
    private String id;

    private String buildingId; // To link to a Building
    private String buildingName; // Denormalized for easy display
    private String roomNumber;
    private int floor;
    private String roomType; // "Single", "Double", etc.
    private double rate; // "5000"
    private String status; // "Available", "Occupied", "Maintenance"
    private int capacity;

    // Required no-arg constructor for Firestore
    public Room() {}

    // Getters and Setters...
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getBuildingId() { return buildingId; }
    public void setBuildingId(String buildingId) { this.buildingId = buildingId; }

    public String getBuildingName() { return buildingName; }
    public void setBuildingName(String buildingName) { this.buildingName = buildingName; }

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }

    public int getFloor() { return floor; }
    public void setFloor(int floor) { this.floor = floor; }

    public String getRoomType() { return roomType; }
    public void setRoomType(String roomType) { this.roomType = roomType; }

    public double getRate() { return rate; }
    public void setRate(double rate) { this.rate = rate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
}