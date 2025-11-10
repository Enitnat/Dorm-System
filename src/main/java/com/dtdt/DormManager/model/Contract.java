package com.dtdt.DormManager.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.ServerTimestamp;
import java.util.Date;

public class Contract {

    @DocumentId
    private String id;
    
    private String tenantId; // Links to 'users' collection
    private String roomId;   // Links to 'rooms' collection
    
    private String contractType; // e.g., "Full Semester (6 months)"
    private double rentAmount;   // e.g., 5000
    
    private Date startDate;
    private Date endDate;
    
    @ServerTimestamp // Set by Firebase when created
    private Date dateSigned;

    // No-arg constructor for Firestore
    public Contract() {}

    // --- Getters and Setters (Generate these) ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public String getContractType() { return contractType; }
    public void setContractType(String contractType) { this.contractType = contractType; }
    public double getRentAmount() { return rentAmount; }
    public void setRentAmount(double rentAmount) { this.rentAmount = rentAmount; }
    public Date getStartDate() { return startDate; }
    public void setStartDate(Date startDate) { this.startDate = startDate; }
    public Date getEndDate() { return endDate; }
    public void setEndDate(Date endDate) { this.endDate = endDate; }
    public Date getDateSigned() { return dateSigned; }
    public void setDateSigned(Date dateSigned) { this.dateSigned = dateSigned; }
}