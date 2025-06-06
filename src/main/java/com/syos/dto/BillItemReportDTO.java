package com.syos.dto;

public class BillItemReportDTO {
    private String productName;
    private String productCode; // Added for completeness if needed
    private int quantity;
    private double unitPrice;
    private double calculatedSubtotal; // quantity * unitPrice
    private double discountAmount;
    private double netPrice; // total_price for the item after discount

    // Constructor
    public BillItemReportDTO(String productName, String productCode, int quantity,
                             double unitPrice, double calculatedSubtotal,
                             double discountAmount, double netPrice) {
        this.productName = productName;
        this.productCode = productCode;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.calculatedSubtotal = calculatedSubtotal;
        this.discountAmount = discountAmount;
        this.netPrice = netPrice;
    }

    // Getters
    public String getProductName() {
        return productName;
    }

    public String getProductCode() {
        return productCode;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public double getCalculatedSubtotal() {
        return calculatedSubtotal;
    }

    public double getDiscountAmount() {
        return discountAmount;
    }

    public double getNetPrice() {
        return netPrice;
    }
}