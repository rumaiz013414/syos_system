package com.syos.command;

import java.util.Scanner;

import com.syos.singleton.InventoryManager;
import com.syos.util.CommonVariables;

public class MoveToShelfCommand implements Command {
	private final InventoryManager inventoryManager;
	private final Scanner scanner;

	public MoveToShelfCommand(InventoryManager inventoryManager, Scanner scanner) {
		this.inventoryManager = inventoryManager;
		this.scanner = scanner;
	}

	@Override
	public void execute() {
		System.out.println("\n=== Move Stock to Shelf ===");

		String productCode = getProductCodeInput();
		if (productCode == null) {
			return;
		}

		int quantity = getQuantityInput();
		if (quantity == -1) {
			return;
		}

		try {
			inventoryManager.moveToShelf(productCode, quantity);
		} catch (IllegalArgumentException e) {
			System.out.println("Failed to move to shelf: " + e.getMessage());
		} catch (IllegalStateException e) {
			System.out.println("Operation failed: " + e.getMessage());
		} catch (RuntimeException e) {
			System.out.println("An unexpected error occurred: " + e.getMessage());
			// e.printStackTrace();
		}
	}

	private String getProductCodeInput() {
		String code;
		while (true) {
			System.out.print("Product code: ");
			code = scanner.nextLine().trim();
			if (code.isEmpty()) {
				System.out.println("Error: Product code cannot be empty.");
			} else {
				return code;
			}
		}
	}

	private int getQuantityInput() {
		int quantity;
		while (true) {
			System.out.print("Quantity: ");
			String qtyInput = scanner.nextLine().trim();
			try {
				quantity = Integer.parseInt(qtyInput);
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
}