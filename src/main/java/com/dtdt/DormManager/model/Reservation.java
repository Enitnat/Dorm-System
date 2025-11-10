package com.dtdt.DormManager.model;

import com.google.cloud.firestore.annotation.ServerTimestamp;
import java.util.Date;

// This object will be saved to the 'reservations' collection
public class Reservation {

    // Basic Info
    private String firstName;
    private String lastName;
    private String studentId;
    private String email;
    private String gender;
    private String currentYear;

    // Contract Info
    private String contractType; // e.g., "Full Semester", "Monthly"
    private Date preferredMoveInDate;

    // Admin Info
    private String status; // "Pending", "Approved", "Rejected"
    @ServerTimestamp // Automatically sets the time on the server
    private Date dateSubmitted;

    // No-arg constructor required for Firestore
    public Reservation() {}

    // Getters and Setters (You will need to generate these)
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getCurrentYear() { return currentYear; }
    public void setCurrentYear(String currentYear) { this.currentYear = currentYear; }
    public String getContractType() { return contractType; }
    public void setContractType(String contractType) { this.contractType = contractType; }
    public Date getPreferredMoveInDate() { return preferredMoveInDate; }
    public void setPreferredMoveInDate(Date preferredMoveInDate) { this.preferredMoveInDate = preferredMoveInDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getDateSubmitted() { return dateSubmitted; }
    public void setDateSubmitted(Date dateSubmitted) { this.dateSubmitted = dateSubmitted; }
}