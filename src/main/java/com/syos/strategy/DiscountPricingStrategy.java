package com.syos.strategy;

import com.syos.enums.DiscountType;
import com.syos.model.Discount;
import com.syos.model.Product;
import com.syos.repository.DiscountRepository;
import com.syos.singleton.InventoryManager;

import java.time.LocalDate;
import java.util.List;

public class DiscountPricingStrategy implements PricingStrategy {
	private final PricingStrategy baseStrategy;
	private final DiscountRepository discountRepo = new DiscountRepository();
	private final InventoryManager inventoryManager = InventoryManager.getInstance(null);

	public DiscountPricingStrategy(PricingStrategy baseStrategy) {
		this.baseStrategy = baseStrategy;
	}

	@Override
	public double calculate(Product product, int quantity) {
		double baseTotal = baseStrategy.calculate(product, quantity);
		int qtyOnShelf = inventoryManager.getQuantityOnShelf(product.getCode());
		if (qtyOnShelf <= 0) {
			return baseTotal;
		}

		List<Discount> discounts = discountRepo.findActiveDiscounts(product.getCode(), LocalDate.now());
		if (discounts.isEmpty()) {
			return baseTotal;
		}

		double bestReducedTotal = baseTotal;
		for (Discount d : discounts) {
			double reduced;
			if (d.getType() == DiscountType.PERCENT) {
				reduced = baseTotal * (1 - d.getValue() / 100.0);
			} else {
				reduced = baseTotal - (d.getValue() * quantity);
			}
			if (reduced < bestReducedTotal) {
				bestReducedTotal = reduced;
			}
		}

		return Math.max(0.0, bestReducedTotal);
	}
}
