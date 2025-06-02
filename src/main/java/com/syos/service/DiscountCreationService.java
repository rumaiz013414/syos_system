package com.syos.service;

import com.syos.enums.DiscountType;
import com.syos.repository.DiscountRepository;

import java.time.LocalDate;
import java.util.Scanner;

public class DiscountCreationService {
	private final Scanner scanner;
	private final DiscountRepository discountRepo = new DiscountRepository();

	public DiscountCreationService(Scanner scanner) {
		this.scanner = scanner;
	}

	public int createDiscount() {
		System.out.println("\n=== Create New Discount ===");

		// 1) Discount name
		System.out.print("Enter discount name (e.g. \"10% OFF SUMMER\"): ");
		String discountName = scanner.nextLine().trim();
		if (discountName.isEmpty()) {
			System.out.println("Discount name cannot be empty.");
			return -1;
		}

		System.out.print("Discount type (PERCENT or AMOUNT): ");
		String typeInput = scanner.nextLine().trim().toUpperCase();
		DiscountType discountType;
		try {
			discountType = DiscountType.valueOf(typeInput);
		} catch (IllegalArgumentException e) {
			System.out.println("Invalid discount type. Use PERCENT or AMOUNT.");
			return -1;
		}

		System.out.print("Discount value ("
				+ (discountType == DiscountType.PERCENT ? "percentage, e.g. 10" : "flat amount per unit, e.g. 5")
				+ "): ");
		String valueInput = scanner.nextLine().trim();
		double discountValue;
		try {
			discountValue = Double.parseDouble(valueInput);
			if (discountValue < 0) {
				System.out.println("Discount value must be non-negative.");
				return -1;
			}
			if (discountType == DiscountType.PERCENT && discountValue > 100) {
				System.out.println("Percentage cannot exceed 100.");
				return -1;
			}
		} catch (NumberFormatException e) {
			System.out.println("Invalid number format for discount value.");
			return -1;
		}

		System.out.print("Start date (YYYY-MM-DD): ");
		String startDateInput = scanner.nextLine().trim();
		LocalDate startDate;
		try {
			startDate = LocalDate.parse(startDateInput);
		} catch (Exception e) {
			System.out.println("Invalid start date format.");
			return -1;
		}

		System.out.print("End date (YYYY-MM-DD): ");
		String endDateInput = scanner.nextLine().trim();
		LocalDate endDate;
		try {
			endDate = LocalDate.parse(endDateInput);
		} catch (Exception e) {
			System.out.println("Invalid end date format.");
			return -1;
		}

		if (endDate.isBefore(startDate)) {
			System.out.println("End date cannot be before start date.");
			return -1;
		}

		int discountId = discountRepo.createDiscount(discountName, discountType, discountValue, startDate, endDate);
		if (discountId != -1) {
			System.out.printf("Discount '%s' (ID=%d) created from %s to %s.%n", discountName, discountId, startDate,
					endDate);
		} else {
			System.out.println("Failed to create discount.");
		}
		return discountId;
	}
}