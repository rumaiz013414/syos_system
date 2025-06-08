package com.syos.strategy;

import java.time.LocalDate;
import java.util.List;

import com.syos.model.Discount;
import com.syos.model.Product;
import com.syos.repository.DiscountRepository;
import com.syos.singleton.InventoryManager;
import com.syos.util.CommonVariables;

public class DiscountPricingStrategy implements PricingStrategy {
	private final PricingStrategy basePriceStrategy;
	private final DiscountRepository discountRepository = new DiscountRepository();
	private final InventoryManager inventoryManager = InventoryManager.getInstance(null);

	public DiscountPricingStrategy(PricingStrategy basePriceStrategy) {
		this.basePriceStrategy = basePriceStrategy;
	}

	@Override
	public double calculate(Product product, int quantity) {
		double baseTotal = basePriceStrategy.calculate(product, quantity);
		int availableStock = inventoryManager.getQuantityOnShelf(product.getCode());
		if (availableStock <= CommonVariables.MINIMUMQUANTITY) {
			return baseTotal;
		}
		List<Discount> activeDiscounts = discountRepository.findActiveDiscounts(product.getCode(), LocalDate.now());

		if (activeDiscounts.isEmpty()) {
			return baseTotal;
		}

		double bestDiscountedTotal = baseTotal;

		for (Discount discount : activeDiscounts) {
			double discountedTotal;
			switch (discount.getType()) {
			case PERCENT:

				discountedTotal = baseTotal * (CommonVariables.oneHundredPercent
						- (discount.getValue() / CommonVariables.percentageDevisor));
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

		return Math.max(CommonVariables.MIN_TOTAL_PRICE, bestDiscountedTotal);
	}
}
