package com.syos.strategy;

import com.syos.model.Product;

public class NoDiscountStrategy implements PricingStrategy {

	@Override
	public double calculate(Product product, int quantity) {
		// TODO Auto-generated method stub
		return product.getPrice() * quantity;
	}

}
