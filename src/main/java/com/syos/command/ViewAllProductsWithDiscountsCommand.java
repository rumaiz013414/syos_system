package com.syos.command;

import com.syos.model.Product;
import com.syos.model.Discount;
import com.syos.repository.DiscountRepository;
import com.syos.repository.ProductRepository;
import com.syos.enums.DiscountType;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class ViewAllProductsWithDiscountsCommand implements Command {
	private final DiscountRepository discountRepository;
	private final ProductRepository productRepository;

	public ViewAllProductsWithDiscountsCommand(DiscountRepository discountRepository,
			ProductRepository productRepository) {
		this.discountRepository = discountRepository;
		this.productRepository = productRepository;
	}

	@Override
	public void execute() {
		System.out.println("\n--- Products with Active Discounts ---"); // Always print the main title

		List<Product> products = productRepository.findAll();

		if (products.isEmpty()) {
			System.out.println("No products have been registered yet.");
			return;
		}

		LocalDate today = LocalDate.now();
		boolean hasDiscountsToDisplay = false;

		// Use StringBuilder only for the rows that will be printed
		StringBuilder tableRows = new StringBuilder();

		for (Product product : products) {
			List<Discount> activeDiscounts = discountRepository.findDiscountsByProductCode(product.getCode(), today);

			if (!activeDiscounts.isEmpty()) {
				hasDiscountsToDisplay = true; // At least one product has a discount
				String discountsDisplay = activeDiscounts.stream().map(d -> {
					String value = (d.getType() == DiscountType.PERCENT) ? String.format("%.2f%%", d.getValue())
							: String.format("%.2f", d.getValue());
					return d.getName() + " (" + value + ")";
				}).collect(Collectors.joining("; "));

				tableRows.append(String.format("%-15s %-30s %-10.2f %s%n", product.getCode(), product.getName(),
						product.getPrice(), discountsDisplay));
			}
		}

		// Print header and footer only if there are rows to display
		if (hasDiscountsToDisplay) {
			System.out.printf("%-15s %-30s %-10s %s%n", "Product Code", "Product Name", "Price", "Active Discounts");
			System.out.println(
					"------------------------------------------------------------------------------------------------------");
			System.out.print(tableRows.toString()); // Print all accumulated rows
			System.out.println(
					"------------------------------------------------------------------------------------------------------");
		} else {
			System.out.println("No products currently have active discounts.");
		}
	}
}