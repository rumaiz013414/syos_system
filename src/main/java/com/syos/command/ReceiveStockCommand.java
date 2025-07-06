package com.syos.command;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

import com.syos.singleton.InventoryManager;
import com.syos.util.CommonVariables;

public class ReceiveStockCommand implements Command {
	private final InventoryManager inventoryManager;
	private final Scanner scanner;

	public ReceiveStockCommand(InventoryManager inventoryManager, Scanner scanner) {
		this.inventoryManager = inventoryManager;
		this.scanner = scanner;
	}

	@Override
	public void execute() {
		System.out.println("\n=== Receive New Stock ===");

		String productCode = getProductCodeInput();
		if (productCode == null) {
			return;
		}

		int quantity = getQuantityInput();
		if (quantity == -1) {
			return;
		}

		LocalDate purchaseDate = getDateInput("Purchase date (YYYY-MM-DD): ");
		if (purchaseDate == null) {
			return;
		}

		LocalDate expiryDate = getExpiryDateInput(purchaseDate);
		if (expiryDate == null) {
			return;
		}

		try {
			inventoryManager.receiveStock(productCode, purchaseDate, expiryDate, quantity);
		} catch (IllegalArgumentException e) {
			System.out.println("Failed to receive stock: " + e.getMessage());
		} catch (RuntimeException e) {
			System.out.println("An unexpected error occurred: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private String getProductCodeInput() {
		String productCode;
		while (true) {
			System.out.print("Product code: ");
			productCode = scanner.nextLine().trim();
			if (productCode.isEmpty()) {
				System.out.println("Error: Product code cannot be empty.");
			} else {
				return productCode;
			}
		}
	}

	private int getQuantityInput() {
		int quantity;
		while (true) {
			System.out.print("Quantity: ");
			String quantityInput = scanner.nextLine().trim();
			try {
				quantity = Integer.parseInt(quantityInput);
				if (quantity <= CommonVariables.MINIMUMQUANTITY) {
					System.out.println("Error: Quantity must be positive.");
				} else {
					return quantity;
				}
			} catch (NumberFormatException e) {
				System.out.println("Error: Invalid quantity. Please enter a positive integer.");
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

	private LocalDate getExpiryDateInput(LocalDate purchaseDate) {
		LocalDate expiryDate;
		while (true) {
			expiryDate = getDateInput("Expiry date (YYYY-MM-DD): ");
			if (expiryDate == null) {
				return null;
			}
			if (expiryDate.isBefore(purchaseDate)) {
				System.out.println("Error: Expiry date cannot be before purchase date.");
			} else {
				return expiryDate;
			}
		}
	}
}