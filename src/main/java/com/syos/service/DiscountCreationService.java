package com.syos.service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException; // More specific exception for date parsing
import java.util.Scanner;

import com.syos.enums.DiscountType;
import com.syos.repository.DiscountRepository;

public class DiscountCreationService {
	private final Scanner scanner;
	private final DiscountRepository discountRepo; // Make it final and inject

	// Constructor with injected repository
	public DiscountCreationService(Scanner scanner, DiscountRepository discountRepo) {
		this.scanner = scanner;
		this.discountRepo = discountRepo;
	}

	public int createDiscount() {
		System.out.println("\n=== Create New Discount ===");

		// 1) Discount name
		String discountName;
		while (true) {
			System.out.print("Enter discount name (e.g. \"10% OFF SUMMER\"): ");
			discountName = scanner.nextLine().trim();
			if (discountName.isEmpty()) {
				System.out.println("Error: Discount name cannot be empty.");
			} else {
				break; // Valid name, exit loop
			}
		}

		// 2) Discount type (PERCENT or AMOUNT)
		DiscountType discountType;
		while (true) { // Loop for valid type input
			System.out.print("Discount type (PERCENT or AMOUNT): ");
			String typeInput = scanner.nextLine().trim().toUpperCase();
			try {
				discountType = DiscountType.valueOf(typeInput);
				break; // Exit loop if valid
			} catch (IllegalArgumentException e) {
				System.out.println("Error: Invalid discount type. Use PERCENT or AMOUNT.");
			}
		}

		// 3) Discount value
		double discountValue;
		while (true) { // Loop for valid value input
			System.out.print("Discount value ("
					+ (discountType == DiscountType.PERCENT ? "percentage, e.g. 10" : "flat amount per unit, e.g. 5")
					+ "): ");
			String valueInput = scanner.nextLine().trim();
			try {
				discountValue = Double.parseDouble(valueInput);
				if (discountValue < 0) {
					System.out.println("Error: Discount value must be non-negative.");
				} else if (discountType == DiscountType.PERCENT && discountValue > 100) {
					System.out.println("Error: Percentage cannot exceed 100.");
				} else {
					break; // Exit loop if valid
				}
			} catch (NumberFormatException e) {
				System.out.println("Error: Invalid number format for discount value.");
			}
		}

		// 4) Start date
		LocalDate startDate;
		while (true) { // Loop for valid start date
			System.out.print("Start date (YYYY-MM-DD): ");
			String startDateInput = scanner.nextLine().trim();
			try {
				startDate = LocalDate.parse(startDateInput);
				break; // Exit loop if valid
			} catch (DateTimeParseException e) { // Use more specific exception
				System.out.println("Error: Invalid start date format. Please use YYYY-MM-DD.");
			}
		}

		// 5) End date
		LocalDate endDate;
		while (true) { // Loop for valid end date
			System.out.print("End date (YYYY-MM-DD): ");
			String endDateInput = scanner.nextLine().trim();
			try {
				endDate = LocalDate.parse(endDateInput);
				if (endDate.isBefore(startDate)) {
					System.out.println("Error: End date cannot be before start date.");
				} else {
					break; // Exit loop if valid
				}
			} catch (DateTimeParseException e) { // Use more specific exception
				System.out.println("Error: Invalid end date format. Please use YYYY-MM-DD.");
			}
		}

		// Create discount
		int discountId = -1; // Default to -1 indicating failure
		try {
			discountId = discountRepo.createDiscount(discountName, discountType, discountValue, startDate, endDate);
			if (discountId != -1) {
				System.out.printf("Success: Discount '%s' (ID=%d) created from %s to %s.%n", discountName, discountId,
						startDate, endDate);
			} else {
				System.out.println("Error: Failed to create discount (repository returned -1)."); // More specific error
			}
		} catch (RuntimeException e) { // Catch any unexpected runtime errors from the repository
			System.out.println("Error: An unexpected error occurred while creating the discount: " + e.getMessage());
			// e.printStackTrace(); // Keep for debugging, remove for production
		}
		return discountId;
	}
}