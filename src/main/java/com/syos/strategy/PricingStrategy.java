package com.syos.strategy;

import com.syos.model.Product;

public interface PricingStrategy {
    double calculate(Product product, int quantity);
}
