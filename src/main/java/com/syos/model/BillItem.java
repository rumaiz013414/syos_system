package com.syos.model;

import com.syos.strategy.PricingStrategy;
import com.syos.util.CommonVariables;

public class BillItem {
    private final int id;
    private final int billId;
    private final Product product;
    private final int quantity;
    private final double totalPrice;
    private final double discountAmount;

    private BillItem(BillItemBuilder billItemBuilder) {
        this.id             = 0;
        this.billId         = 0;
        this.product        = billItemBuilder.product;
        this.quantity       = billItemBuilder.quantity;
        this.totalPrice     = billItemBuilder.totalPrice;
        this.discountAmount = billItemBuilder.discountAmount;
    }

    public BillItem(int id, int billId, Product product, int quantity, double totalPrice, double discountAmount) {
        this.id             = id;
        this.billId         = billId;
        this.product        = product;
        this.quantity       = quantity;
        this.totalPrice     = totalPrice;
        this.discountAmount = discountAmount;
    }

    public int getId()               { return id; }
    public int getBillId()           { return billId; }
    public Product getProduct()      { return product; }
    public int getQuantity()         { return quantity; }
    public double getTotalPrice()    { return totalPrice; }
    public double getDiscountAmount(){ return discountAmount; }

    public static class BillItemBuilder {
        private final Product product;
        private final int quantity;
        private final double totalPrice;
        private final double discountAmount;

        public BillItemBuilder(Product product, int quantity, PricingStrategy strategy) {
            if (product == null) {
                throw new IllegalArgumentException("Product cannot be null");
            }
            if (quantity <= CommonVariables.MINIMUMQUANTITY) {
                throw new IllegalArgumentException("Quantity must be greater than 0");
            }
            this.product = product;
            this.quantity = quantity;

            double originalPrice = product.getPrice() * quantity;
            this.totalPrice = strategy.calculate(product, quantity);
            this.discountAmount = originalPrice - this.totalPrice;
        }

        public BillItem build() {
            return new BillItem(this);
        }
    }
}
