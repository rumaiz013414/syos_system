package com.syos.factory;

import com.syos.model.BillItem;
import com.syos.model.Product;
import com.syos.strategy.PricingStrategy;

public class BillItemFactory {
    private final PricingStrategy pricingStrategy;

    public BillItemFactory(PricingStrategy pricingStrategy) {
        this.pricingStrategy = pricingStrategy;
    }

    public BillItem create(Product product, int quantity) {
        return new BillItem.BillItemBuilder(product, quantity, pricingStrategy).build();
    }
}
