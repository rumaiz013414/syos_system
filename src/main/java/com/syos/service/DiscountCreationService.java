package com.syos.service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException; // More specific exception for date parsing
import java.util.Scanner;

import com.syos.enums.DiscountType;
import com.syos.repository.DiscountRepository;
import com.syos.util.CommonVariables;

public class DiscountCreationService {
	private final Scanner scanner;
	private final DiscountRepository discountRepository;

	public DiscountCreationService(Scanner scanner, DiscountRepository discountRepository) {
		this.scanner = scanner;
		this.discountRepository = discountRepository;
	}

	public int createDiscount() {
		System.out.println("\n=== Create New Discount ===");
		String discountName;
		while (true) {
			System.out.print("Enter discount name (e.g. \"10% OFF SUMMER\"): ");
			discountName = scanner.nextLine().trim();
			if (discountName.isEmpty()) {
				System.out.println("Error: Discount name cannot be empty.");
			} else {
				break;
			}
		}

		DiscountType discountType;
		while (true) {
			System.out.print("Discount type (PERCENT or AMOUNT): ");
			String typeInput = scanner.nextLine().trim().toUpperCase();
			try {
				discountType = DiscountType.valueOf(typeInput);
				break;
			} catch (IllegalArgumentException e) {
				System.out.println("Error: Invalid discount type. Use PERCENT or AMOUNT.");
			}
		}

		double discountValue;
		while (true) {
			System.out.print("Discount value ("
					+ (discountType == DiscountType.PERCENT ? "percentage, e.g. 10" : "flat amount per unit, e.g. 5")
					+ "): ");
			String valueInput = scanner.nextLine().trim();
			try {
				discountValue = Double.parseDouble(valueInput);
				if (discountValue < CommonVariables.MINIMUMAMOUNT) {
					System.out.println("Error: Discount value must be non-negative.");
				} else if (discountType == DiscountType.PERCENT
						&& discountValue > CommonVariables.MAX_PRODUCT_NAME_LENGTH) {
					System.out.println("Error: Percentage cannot exceed 100.");
				} else {
					break;
				}
			} catch (NumberFormatException e) {
				System.out.println("Error: Invalid number format for discount value.");
			}
		}

		LocalDate startDate;
		while (true) {
			System.out.print("Start date (YYYY-MM-DD): ");
			String startDateInput = scanner.nextLine().trim();
			try {
				startDate = LocalDate.parse(startDateInput);
				break;
			} catch (DateTimeParseException e) {
				System.out.println("Error: Invalid start date format. Please use YYYY-MM-DD.");
			}
		}

		LocalDate endDate;
		while (true) {
			System.out.print("End date (YYYY-MM-DD): ");
			String endDateInput = scanner.nextLine().trim();
			try {
				endDate = LocalDate.parse(endDateInput);
				if (endDate.isBefore(startDate)) {
					System.out.println("Error: End date cannot be before start date.");
				} else {
					break;
				}
			} catch (DateTimeParseException e) {
				System.out.println("Error: Invalid end date format. Please use YYYY-MM-DD.");
			}
		}

		try {
			CommonVariables.discountId = discountRepository.createDiscount(discountName, discountType, discountValue,
					startDate, endDate);
			if (CommonVariables.discountId != -1) {
				System.out.printf("Success: Discount '%s' (ID=%d) created from %s to %s.%n", discountName,
						CommonVariables.discountId, startDate, endDate);
			} else {
				System.out.println("Error: Failed to create discount");
			}
		} catch (RuntimeException e) {
			System.out.println("Error: An unexpected error occurred while creating the discount: " + e.getMessage());
		}
		return CommonVariables.discountId;
	}
}