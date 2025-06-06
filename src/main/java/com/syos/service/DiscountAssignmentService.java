package com.syos.service;

import java.util.Scanner;

import com.syos.model.Discount;
import com.syos.model.Product;
import com.syos.repository.DiscountRepository;
import com.syos.repository.ProductRepository;

public class DiscountAssignmentService {
	private final Scanner scanner;
	private final DiscountRepository discountRepo;
	private final ProductRepository productRepo;

	public DiscountAssignmentService(Scanner scanner, DiscountRepository discountRepo, ProductRepository productRepo) {
		this.scanner = scanner;
		this.discountRepo = discountRepo;
		this.productRepo = productRepo;
	}

	public void assignDiscountToProduct() {
		System.out.println("\n=== Assign Existing Discount to Product ===");
		System.out.print("Enter product code: ");
		String productCode = scanner.nextLine().trim();
		if (productCode.isEmpty()) {
			System.out.println("Product code cannot be empty.");
			return;
		}

		Product product = productRepo.findByCode(productCode);
		if (product == null) {
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