package com.dtdt.DormManager.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.ServerTimestamp;
import com.google.cloud.firestore.annotation.PropertyName; // <-- NEW IMPORT
import java.util.Date;

public class MaintenanceRequest {

    @DocumentId
    private String id;

    private String type; // e.g., "Plumbing", "Electrical"

    // FIX 2: Add this field to match a field in your database documents
    private String dateSubmittedString;

    private String tenantId;
    private String roomId;

    // FIX 1: Use @PropertyName to map the database field "description" to this Java field
    @PropertyName("description")
    private String issueDescription;

    private String status; // "Pending", "In Progress", "Completed"

    @ServerTimestamp
    private Date dateSubmitted;

    public MaintenanceRequest() {}

    // --- Getters and Setters ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    // FIX 2: Getter/Setter for the added field
    public String getDateSubmittedString() { return dateSubmittedString; }
    public void setDateSubmittedString(String dateSubmittedString) { this.dateSubmittedString = dateSubmittedString; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    // FIX 1: Use @PropertyName on getter/setter as well
    @PropertyName("description")
    public String getIssueDescription() { return issueDescription; }

    @PropertyName("description")
    public void setIssueDescription(String issueDescription) { this.issueDescription = issueDescription; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getDateSubmitted() { return dateSubmitted; }
    public void setDateSubmitted(Date dateSubmitted) { this.dateSubmitted = dateSubmitted; }
}