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
	private final String newLine = System.lineSeparator();

	public ViewAllProductsWithDiscountsCommand(DiscountRepository discountRepository,
			ProductRepository productRepository) {
		this.discountRepository = discountRepository;
		this.productRepository = productRepository;
	}

	String lineSeperator = "------------------------------------------------------------------------------------------------------";

	@Override
	public void execute() {
		System.out.println(newLine + "--- Products with Active Discounts ---");
		List<Product> products = productRepository.findAll();
		if (products.isEmpty()) {
			System.out.println("No products have been registered yet.");
			return;
		}
		LocalDate today = LocalDate.now();
		boolean hasDiscountsToDisplay = false;

		StringBuilder tableRows = new StringBuilder();

		for (Product product : products) {
			List<Discount> activeDiscounts = discountRepository.findDiscountsByProductCode(product.getCode(), today);

			if (!activeDiscounts.isEmpty()) {
				hasDiscountsToDisplay = true;
				String discountsDisplay = activeDiscounts.stream().map(discount -> {
					String value = (discount.getType() == DiscountType.PERCENT)
							? String.format("%.2f%%", discount.getValue())
							: String.format("%.2f", discount.getValue());
					return discount.getName() + " (" + value + ")";
				}).collect(Collectors.joining("; "));

				tableRows.append(String.format("%-15s %-30s %-10.2f %s%n", product.getCode(), product.getName(),
						product.getPrice(), discountsDisplay));
			}
		}

		if (hasDiscountsToDisplay) {
			System.out.printf("%-15s %-30s %-10s %s%n", "Product Code", "Product Name", "Price", "Active Discounts");
			System.out.println(lineSeperator);
			System.out.print(tableRows.toString());
			System.out.println(lineSeperator);
		} else {
			System.out.println("No products currently have active discounts.");
		}
	}
}