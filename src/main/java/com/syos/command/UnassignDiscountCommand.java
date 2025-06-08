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

	public UnassignDiscountCommand(Scanner scanner, DiscountRepository discountRepository,
			ProductRepository productRepository) {
		this.scanner = scanner;
		this.discountRepository = discountRepository;
		this.productRepository = productRepository;
	}

	String lineSeperator = "--------------------------------------------------";

	@Override
	public void execute() {
		System.out.println("\n--- Unassign Discount from Product ---");

		System.out.print("Enter Product Code: ");
		String productCode = scanner.nextLine().trim();

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

		System.out.println("\nActive discounts for " + product.getName() + " (" + productCode + "):");
		System.out.printf("%-5s %-20s %-10s %-10s%n", "ID", "Name", "Type", "Value");
		System.out.println(lineSeperator);
		for (Discount discount : currentDiscounts) {
			String typeDisplay = (discount.getType() == DiscountType.PERCENT) ? "PERCENT" : "AMOUNT";
			String valueDisplay = String.format("%.2f", discount.getValue());
			System.out.printf("%-5d %-20s %-10s %-10s%n", discount.getId(), discount.getName(), typeDisplay,
					valueDisplay);
		}
		System.out.println(lineSeperator);

		System.out.print("Enter Discount ID to unassign: ");
		int discountId;
		try {
			discountId = Integer.parseInt(scanner.nextLine().trim());
		} catch (NumberFormatException e) {
			System.out.println("Invalid Discount ID. Please enter a number.");
			return;
		}

		Discount discountToUnassign = discountRepository.findById(discountId);
		if (discountToUnassign == null) {
			System.out.println("Error: Discount with ID " + discountId + " not found.");
			return;
		}

		boolean isAssigned = currentDiscounts.stream().anyMatch(discount -> discount.getId() == discountId);
		if (!isAssigned) {
			System.out.println("Error: Discount ID " + discountId + " is not currently assigned to product '"
					+ productCode + "'.");
			return;
		}

		boolean success = discountRepository.unassignDiscountFromProduct(productCode, discountId);

		if (success) {
			System.out.println("Discount '" + discountToUnassign.getName() + "' (ID: " + discountId
					+ ") successfully unassigned from product '" + product.getName() + "' (" + productCode + ").");
		} else {
			System.out.println("Failed to unassign discount. This might happen if the assignment didn't exist.");
		}
	}
}