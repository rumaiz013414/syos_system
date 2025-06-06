package com.syos.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Date; // Assuming Bill model still uses java.util.Date

public class BillReportDTO {
    private int serialNumber;
    private Date billDate; // Reflects Bill model's Date
    private double totalAmount;
    private double cashTendered;
    private double changeReturned;
    private String transactionType;
    private List<BillItemReportDTO> items; // Nested DTO for bill items

    // Constructor
    public BillReportDTO(int serialNumber, Date billDate, double totalAmount,
                         double cashTendered, double changeReturned,
                         String transactionType, List<BillItemReportDTO> items) {
        this.serialNumber = serialNumber;
        this.billDate = billDate;
        this.totalAmount = totalAmount;
        this.cashTendered = cashTendered;
        this.changeReturned = changeReturned;
        this.transactionType = transactionType;
        this.items = items;
    }

    // Getters
    public int getSerialNumber() {
        return serialNumber;
    }

    public Date getBillDate() {
        return billDate;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public double getCashTendered() {
        return cashTendered;
    }

    public double getChangeReturned() {
        return changeReturned;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public List<BillItemReportDTO> getItems() {
        return items;
    }
}