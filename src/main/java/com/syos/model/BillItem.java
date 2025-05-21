package com.syos.model;

import com.syos.strategy.PricingStrategy;

public class BillItem {
    private final Product product;
    private final int quantity;
    private final double totalPrice;

    private BillItem(BillItemBuilder b) {
        this.product    = b.product;
        this.quantity   = b.quantity;
        this.totalPrice = b.totalPrice;
    }

    public Product getProduct()    { return product; }
    public int getQuantity()       { return quantity; }
    public double getTotalPrice()  { return totalPrice; }

    // builder for BillItem, applying a pricing strategy under the hood
    public static class BillItemBuilder {
        private final Product product;
        private final int quantity;
        private double totalPrice;

        public BillItemBuilder(Product product, int quantity, PricingStrategy strategy) {
            if (product == null || quantity <= 0) {
                throw new IllegalArgumentException("Product & positive qty required");
            }
            this.product    = product;
            this.quantity   = quantity;
            this.totalPrice = strategy.calculate(product, quantity);
        }

        public BillItem build() {
            return new BillItem(this);
        }
    }
}
