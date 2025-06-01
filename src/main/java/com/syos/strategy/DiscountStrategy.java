package com.syos.strategy;

import com.syos.model.Product;

public interface DiscountStrategy {
    double apply(Product product, int quantity);
}
