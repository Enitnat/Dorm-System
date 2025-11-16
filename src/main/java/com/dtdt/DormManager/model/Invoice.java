package com.dtdt.DormManager.model;

import com.google.cloud.firestore.annotation.DocumentId;
import java.util.Date;

public class Invoice {
    @DocumentId private String id;
    private String tenantId;
    private String contractId;
    private String monthYear; // e.g., "November 2025"
    private double rentAmount;
    private double lateFee;
    private double totalAmount;
    private String status; // "Pending", "Paid", "Overdue"
    private Date dueDate;
    private Date datePaid;

    public Invoice() {}
    // --- All Getters and Setters go here ---
    // (Make sure you have getters/setters for all fields)
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getContractId() { return contractId; }
    public void setContractId(String contractId) { this.contractId = contractId; }
    public String getMonthYear() { return monthYear; }
    public void setMonthYear(String monthYear) { this.monthYear = monthYear; }
    public double getRentAmount() { return rentAmount; }
    public void setRentAmount(double rentAmount) { this.rentAmount = rentAmount; }
    public double getLateFee() { return lateFee; }
    public void setLateFee(double lateFee) { this.lateFee = lateFee; }
    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getDueDate() { return dueDate; }
    public void setDueDate(Date dueDate) { this.dueDate = dueDate; }
    public Date getDatePaid() { return datePaid; }
    public void setDatePaid(Date datePaid) { this.datePaid = datePaid; }
}