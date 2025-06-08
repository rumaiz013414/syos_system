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

		String productCode;
		while (true) {
			System.out.print("Product code: ");
			productCode = scanner.nextLine().trim();
			if (productCode.isEmpty()) {
				System.out.println("Error: Product code cannot be empty.");
			} else {
				break;
			}
		}

		int quantity;
		while (true) {
			System.out.print("Quantity: ");
			String quantityInput = scanner.nextLine().trim();
			try {
				quantity = Integer.parseInt(quantityInput);
				if (quantity <= CommonVariables.MINIMUMQUANTITY) {
					System.out.println("Error: Quantity must be positive.");
				} else {
					break;
				}
			} catch (NumberFormatException e) {
				System.out.println("Error: Invalid quantity. Please enter a positive integer.");
			}
		}

		LocalDate purchaseDate;
		while (true) {
			System.out.print("Purchase date (YYYY-MM-DD): ");
			String purchaseDateInput = scanner.nextLine().trim();
			try {
				purchaseDate = LocalDate.parse(purchaseDateInput);
				break;
			} catch (DateTimeParseException e) {
				System.out.println("Error: Invalid purchase date format. Please use YYYY-MM-DD.");
			}
		}

		LocalDate expiryDate;
		while (true) {
			System.out.print("Expiry date (YYYY-MM-DD): ");
			String expiryDateInput = scanner.nextLine().trim();
			try {
				expiryDate = LocalDate.parse(expiryDateInput);

				if (expiryDate.isBefore(purchaseDate)) {
					System.out.println("Error: Expiry date cannot be before purchase date.");
				} else {
					break;
				}
			} catch (DateTimeParseException e) {
				System.out.println("Error: Invalid expiry date format. Please use YYYY-MM-DD.");
			}
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
}