package com.syos.command;

import java.util.Scanner;

import com.syos.singleton.InventoryManager;

public class MoveToShelfCommand implements Command {
	private final InventoryManager inventoryManager;
	private final Scanner scanner;

	public MoveToShelfCommand(InventoryManager inventoryManager, Scanner scanner) {
		this.inventoryManager = inventoryManager;
		this.scanner = scanner;
	}

	@Override
	public void execute() {
		System.out.println("\n=== Move Stock to Shelf ==="); // Added header

		// --- Product Code Input and Validation ---
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

		// --- Quantity Input and Validation ---
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

		// --- Execute Move and Handle Exceptions ---
		try {
			inventoryManager.moveToShelf(code, qty);
			// Success message is now printed by InventoryManager itself for consistency
			// System.out.printf("Moved %d units of %s to shelf.%n", qty, code); // Removed,
			// as InventoryManager prints it
		} catch (IllegalArgumentException e) {
			System.out.println("Failed to move to shelf: " + e.getMessage());
		} catch (IllegalStateException e) {
			System.out.println("Operation failed: " + e.getMessage());
		} catch (RuntimeException e) {
			System.out.println("An unexpected error occurred: " + e.getMessage());
			// e.printStackTrace(); // Keep for debugging, remove for production
		}
	}
}