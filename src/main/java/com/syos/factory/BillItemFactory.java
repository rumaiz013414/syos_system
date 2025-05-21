package com.syos.factory;

import com.syos.model.BillItem;
import com.syos.model.Product;
import com.syos.strategy.PricingStrategy;

public class BillItemFactory {
    private final PricingStrategy strategy;

    public BillItemFactory(PricingStrategy strategy) {
        this.strategy = strategy;
    }

    public BillItem create(Product product, int quantity) {
        return new BillItem.BillItemBuilder(product, quantity, strategy)
                .build();
    }
}
