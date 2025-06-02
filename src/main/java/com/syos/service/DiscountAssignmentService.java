package com.syos.service;

import com.syos.model.Discount;
import com.syos.repository.DiscountRepository;
import com.syos.repository.ProductRepository;

import java.util.Scanner;

public class DiscountAssignmentService {
	private final Scanner scanner;
	private final DiscountRepository discountRepo = new DiscountRepository();
	private final ProductRepository productRepo = new ProductRepository();

	public DiscountAssignmentService(Scanner scanner) {
		this.scanner = scanner;
	}

	public void assignDiscountToProduct() {
		System.out.println("\n=== Assign Existing Discount to Product ===");

		// 1) Product code
		System.out.print("Enter product code: ");
		String productCode = scanner.nextLine().trim();
		if (productCode.isEmpty()) {
			System.out.println("Product code cannot be empty.");
			return;
		}
		if (productRepo.findByCode(productCode) == null) {
			System.out.println("No such product: " + productCode);
			return;
		}

		System.out.print("Enter discount ID: ");
		int discountId;
		try {
			discountId = Integer.parseInt(scanner.nextLine().trim());
			if (discountId <= 0) {
				System.out.println("Discount ID must be a positive number.");
				return;
			}

			// ADDED VALIDATION: Check if the discount ID exists
			Discount existingDiscount = discountRepo.findById(discountId);
			if (existingDiscount == null) {
				System.out.println("No discount found with ID: " + discountId);
				System.out.println("Please create the discount first using 'Create new discount' option.");
				return;
			}

			System.out.printf("Selected Discount: ID %d | Name: '%s' | Type: %s | Value: %.2f | Active: %s to %s%n",
					existingDiscount.getId(), existingDiscount.getName(), existingDiscount.getType(),
					existingDiscount.getValue(), existingDiscount.getStart(), existingDiscount.getEnd());

		} catch (NumberFormatException e) {
			System.out.println("Invalid discount ID format. Please enter a number.");
			return;
		}

		try {
			discountRepo.linkProductToDiscount(productCode, discountId);
			System.out.printf("Discount ID %d assigned to product %s.%n", discountId, productCode);
		} catch (RuntimeException e) {

			System.out.println("Failed to assign discount: " + e.getMessage());
			e.printStackTrace();
		}
	}
}