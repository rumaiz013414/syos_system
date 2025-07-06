package com.syos.service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
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

		String discountName = getDiscountNameInput();
		if (discountName == null)
			return -1;

		DiscountType discountType = getDiscountTypeInput();
		if (discountType == null)
			return -1;

		double discountValue = getDiscountValueInput(discountType);
		if (discountValue == -1.0)
			return -1;

		LocalDate startDate = getDateInput("Start date (YYYY-MM-DD): ");
		if (startDate == null)
			return -1;

		LocalDate endDate = getEndDateInput(startDate);
		if (endDate == null)
			return -1;

		return performDiscountCreation(discountName, discountType, discountValue, startDate, endDate);
	}

	private String getDiscountNameInput() {
		String discountName;
		while (true) {
			System.out.print("Enter discount name (e.g. \"10% OFF SUMMER\"): ");
			discountName = scanner.nextLine().trim();
			if (discountName.isEmpty()) {
				System.out.println("Error: Discount name cannot be empty.");
			} else {
				return discountName;
			}
		}
	}

	private DiscountType getDiscountTypeInput() {
		DiscountType discountType;
		while (true) {
			System.out.print("Discount type (PERCENT or AMOUNT): ");
			String typeInput = scanner.nextLine().trim().toUpperCase();
			try {
				discountType = DiscountType.valueOf(typeInput);
				return discountType;
			} catch (IllegalArgumentException e) {
				System.out.println("Error: Invalid discount type. Use PERCENT or AMOUNT.");
			}
		}
	}

	private double getDiscountValueInput(DiscountType discountType) {
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
				} else if (discountType == DiscountType.PERCENT && discountValue > CommonVariables.oneHundredPercent) {

					System.out.println("Error: Percentage cannot exceed 100.");
				} else {
					return discountValue;
				}
			} catch (NumberFormatException e) {
				System.out.println("Error: Invalid number format for discount value.");
			}
		}
	}

	private LocalDate getDateInput(String prompt) {
		LocalDate date;
		while (true) {
			System.out.print(prompt);
			String dateInput = scanner.nextLine().trim();
			try {
				date = LocalDate.parse(dateInput);
				return date;
			} catch (DateTimeParseException e) {
				System.out.println("Error: Invalid date format. Please use YYYY-MM-DD.");
			}
		}
	}

	private LocalDate getEndDateInput(LocalDate startDate) {
		LocalDate endDate;
		while (true) {
			endDate = getDateInput("End date (YYYY-MM-DD): ");
			if (endDate == null) {
				return null;
			}
			if (endDate.isBefore(startDate)) {
				System.out.println("Error: End date cannot be before start date.");
			} else {
				return endDate;
			}
		}
	}

	private int performDiscountCreation(String discountName, DiscountType discountType, double discountValue,
			LocalDate startDate, LocalDate endDate) {
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