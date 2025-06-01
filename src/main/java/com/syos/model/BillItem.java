package com.syos.model;

import com.syos.strategy.PricingStrategy;

public class BillItem {
    private final int      id;        
    private final int      billId;    
    private final Product  product;
    private final int      quantity;
    private final double   totalPrice;

    private BillItem(BillItemBuilder b) {
        this.id          = 0;               
        this.billId      = 0;               
        this.product     = b.product;
        this.quantity    = b.quantity;
        this.totalPrice  = b.totalPrice;
    }

    public BillItem(int id, int billId, Product product, int quantity, double totalPrice) {
        this.id          = id;
        this.billId      = billId;
        this.product     = product;
        this.quantity    = quantity;
        this.totalPrice  = totalPrice;
    }

    public int getId()           { return id; }
    public int getBillId()       { return billId; }
    public Product getProduct()  { return product; }
    public int getQuantity()     { return quantity; }
    public double getTotalPrice(){ return totalPrice; }

    public static class BillItemBuilder {
        private final Product product;
        private final int quantity;
        private final double totalPrice;

        public BillItemBuilder(Product product, int quantity, PricingStrategy strategy) {
            if (product == null) {
                throw new IllegalArgumentException("Product cannot be null");
            }
            if (quantity <= 0) {
                throw new IllegalArgumentException("Quantity must be > 0");
            }
            this.product     = product;
            this.quantity    = quantity;
            this.totalPrice  = strategy.calculate(product, quantity);
        }

        public BillItem build() {
            return new BillItem(this);
        }
    }
}
