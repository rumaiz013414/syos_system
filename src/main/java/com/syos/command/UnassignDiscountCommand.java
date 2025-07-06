package com.syos.command;

import com.syos.repository.DiscountRepository;
import com.syos.repository.ProductRepository;
import com.syos.model.Product;
import com.syos.enums.DiscountType;
import com.syos.model.Discount;

import java.util.List;
import java.util.Scanner;
import java.time.LocalDate;

public class UnassignDiscountCommand implements Command {
	private final Scanner scanner;
	private final DiscountRepository discountRepository;
	private final ProductRepository productRepository;

	private static final String LINE_SEPARATOR = "--------------------------------------------------";

	public UnassignDiscountCommand(Scanner scanner, DiscountRepository discountRepository,
			ProductRepository productRepository) {
		this.scanner = scanner;
		this.discountRepository = discountRepository;
		this.productRepository = productRepository;
	}

	@Override
	public void execute() {
		System.out.println("\n--- Unassign Discount from Product ---");

		String productCode = getProductCodeInput();
		if (productCode == null) {
			return;
		}

		Product product = productRepository.findByCode(productCode);
		if (product == null) {
			System.out.println("Error: Product with code '" + productCode + "' not found.");
			return;
		}

		List<Discount> currentDiscounts = discountRepository.findDiscountsByProductCode(productCode, LocalDate.now());
		if (currentDiscounts.isEmpty()) {
			System.out.println("Product '" + product.getName() + "' (" + productCode
					+ ") currently has no active discounts to unassign.");
			return;
		}

		displayActiveDiscounts(product, productCode, currentDiscounts);

		int discountId = getDiscountIdInput();
		if (discountId == -1) {
			return;
		}

		Discount discountToUnassign = discountRepository.findById(discountId);
		if (discountToUnassign == null) {
			System.out.println("Error: Discount with ID " + discountId + " not found.");
			return;
		}

		if (!isDiscountAssignedToProduct(currentDiscounts, discountId)) {
			System.out.println("Error: Discount ID " + discountId + " is not currently assigned to product '"
					+ productCode + "'.");
			return;
		}

		performUnassignment(productCode, product, discountToUnassign);
	}

	private String getProductCodeInput() {
		System.out.print("Enter Product Code: ");
		String productCode = scanner.nextLine().trim();
		if (productCode.isEmpty()) {
			System.out.println("Error: Product code cannot be empty.");
			return null;
		}
		return productCode;
	}

	private void displayActiveDiscounts(Product product, String productCode, List<Discount> currentDiscounts) {
		System.out.println("\nActive discounts for " + product.getName() + " (" + productCode + "):");
		System.out.printf("%-5s %-20s %-10s %-10s%n", "ID", "Name", "Type", "Value");
		System.out.println(LINE_SEPARATOR);
		for (Discount discount : currentDiscounts) {
			String typeDisplay = (discount.getType() == DiscountType.PERCENT) ? "PERCENT" : "AMOUNT";
			String valueDisplay = String.format("%.2f", discount.getValue());
			System.out.printf("%-5d %-20s %-10s %-10s%n", discount.getId(), discount.getName(), typeDisplay,
					valueDisplay);
		}
		System.out.println(LINE_SEPARATOR);
	}

	private int getDiscountIdInput() {
		System.out.print("Enter Discount ID to unassign: ");
		try {
			return Integer.parseInt(scanner.nextLine().trim());
		} catch (NumberFormatException e) {
			System.out.println("Invalid Discount ID. Please enter a number.");
			return -1;
		}
	}

	private boolean isDiscountAssignedToProduct(List<Discount> currentDiscounts, int discountId) {
		return currentDiscounts.stream().anyMatch(discount -> discount.getId() == discountId);
	}

	private void performUnassignment(String productCode, Product product, Discount discountToUnassign) {
		boolean success = discountRepository.unassignDiscountFromProduct(productCode, discountToUnassign.getId());

		if (success) {
			System.out.println("Discount '" + discountToUnassign.getName() + "' (ID: " + discountToUnassign.getId()
					+ ") successfully unassigned from product '" + product.getName() + "' (" + productCode + ").");
		} else {
			System.out.println("Failed to unassign discount. This might happen if the assignment didn't exist.");
		}
	}
}