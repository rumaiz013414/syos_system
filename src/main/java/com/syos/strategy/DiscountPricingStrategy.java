package com.syos.strategy;

import com.syos.model.Discount;
import com.syos.model.Product;
import com.syos.repository.DiscountRepository;
import com.syos.singleton.InventoryManager;

import java.time.LocalDate;
import java.util.List;

public class DiscountPricingStrategy implements PricingStrategy {
	private final PricingStrategy basePriceStrategy;
	private final DiscountRepository discountRepository = new DiscountRepository();
	private final InventoryManager inventoryManager = InventoryManager.getInstance(null);
	private static final double MIN_TOTAL_PRICE = 0.0;

	public DiscountPricingStrategy(PricingStrategy basePriceStrategy) {
		this.basePriceStrategy = basePriceStrategy;
	}

	@Override
	public double calculate(Product product, int quantity) {
		double baseTotal = basePriceStrategy.calculate(product, quantity);
		int availableStock = inventoryManager.getQuantityOnShelf(product.getCode());

		// no discount if product is not in stock
		if (availableStock <= 0) {
			return baseTotal;
		}

		// fetch active discounts for the product
		List<Discount> activeDiscounts = discountRepository.findActiveDiscounts(product.getCode(), LocalDate.now());
		if (activeDiscounts.isEmpty()) {
			return baseTotal;
		}

		double bestDiscountedTotal = baseTotal;
		for (Discount discount : activeDiscounts) {
			double discountedTotal;
			switch (discount.getType()) {
			case PERCENT:
				discountedTotal = baseTotal * (1 - discount.getValue() / 100.0);
				break;
			case AMOUNT:
				discountedTotal = baseTotal - discount.getValue();
				break;
			default:
				discountedTotal = baseTotal;
			}

			if (discountedTotal < bestDiscountedTotal) {
				bestDiscountedTotal = discountedTotal;
			}
		}

		// prevent negative totals
		return Math.max(MIN_TOTAL_PRICE, bestDiscountedTotal);
	}
}
