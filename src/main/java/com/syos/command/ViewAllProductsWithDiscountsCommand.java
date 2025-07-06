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

	private static final String LINE_SEPARATOR = "------------------------------------------------------------------------------------------------------";

	public ViewAllProductsWithDiscountsCommand(DiscountRepository discountRepository,
			ProductRepository productRepository) {
		this.discountRepository = discountRepository;
		this.productRepository = productRepository;
	}

	@Override
	public void execute() {
		System.out.println(newLine + "--- Products with Active Discounts ---");
		List<Product> products = productRepository.findAll();

		if (products.isEmpty()) {
			System.out.println("No products have been registered yet.");
			return;
		}

		displayProductsWithDiscounts(products);
	}

	private void displayProductsWithDiscounts(List<Product> products) {
		LocalDate today = LocalDate.now();
		StringBuilder tableRows = new StringBuilder();
		boolean hasDiscountsToDisplay = false;

		for (Product product : products) {
			List<Discount> activeDiscounts = discountRepository.findDiscountsByProductCode(product.getCode(), today);

			if (!activeDiscounts.isEmpty()) {
				hasDiscountsToDisplay = true;
				String discountsDisplay = formatDiscountsForDisplay(activeDiscounts);
				tableRows.append(String.format("%-15s %-30s %-10.2f %s%n", product.getCode(), product.getName(),
						product.getPrice(), discountsDisplay));
			}
		}

		if (hasDiscountsToDisplay) {
			printDiscountTable(tableRows);
		} else {
			System.out.println("No products currently have active discounts.");
		}
	}

	private String formatDiscountsForDisplay(List<Discount> discounts) {
		return discounts.stream().map(discount -> {
			String value = (discount.getType() == DiscountType.PERCENT) ? String.format("%.2f%%", discount.getValue())
					: String.format("%.2f", discount.getValue());
			return discount.getName() + " (" + value + ")";
		}).collect(Collectors.joining("; "));
	}

	private void printDiscountTable(StringBuilder tableRows) {
		System.out.printf("%-15s %-30s %-10s %s%n", "Product Code", "Product Name", "Price", "Active Discounts");
		System.out.println(LINE_SEPARATOR);
		System.out.print(tableRows.toString());
		System.out.println(LINE_SEPARATOR);
	}
}