package com.syos.command;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

import com.syos.singleton.InventoryManager;

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

		String code;
		while (true) {
			System.out.print("Product code: ");
			code = scanner.nextLine().trim();
			if (code.isEmpty()) {
				System.out.println("Error: Product code cannot be empty.");
			} else {
				break;
			}
		}

		int qty;
		while (true) {
			System.out.print("Quantity: ");
			String qtyInput = scanner.nextLine().trim();
			try {
				qty = Integer.parseInt(qtyInput);
				if (qty <= 0) {
					System.out.println("Error: Quantity must be positive.");
				} else {
					break;
				}
			} catch (NumberFormatException e) {
				System.out.println("Error: Invalid quantity. Please enter a positive integer.");
			}
		}

		LocalDate pd;
		while (true) {
			System.out.print("Purchase date (YYYY-MM-DD): ");
			String pdInput = scanner.nextLine().trim();
			try {
				pd = LocalDate.parse(pdInput);
				break;
			} catch (DateTimeParseException e) {
				System.out.println("Error: Invalid purchase date format. Please use YYYY-MM-DD.");
			}
		}

		LocalDate ed;
		while (true) {
			System.out.print("Expiry date (YYYY-MM-DD): ");
			String edInput = scanner.nextLine().trim();
			try {
				ed = LocalDate.parse(edInput);

				if (ed.isBefore(pd)) {
					System.out.println("Error: Expiry date cannot be before purchase date.");
				} else {
					break;
				}
			} catch (DateTimeParseException e) {
				System.out.println("Error: Invalid expiry date format. Please use YYYY-MM-DD.");
			}
		}

		try {
			inventoryManager.receiveStock(code, pd, ed, qty);
		} catch (IllegalArgumentException e) {
			System.out.println("Failed to receive stock: " + e.getMessage());
		} catch (RuntimeException e) {
			System.out.println("An unexpected error occurred: " + e.getMessage());
			e.printStackTrace();
		}
	}
}