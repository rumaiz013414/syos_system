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
		System.out.print("Product code: ");
		String code = scanner.nextLine().trim();
		if (code.isEmpty()) {
			System.out.println("Product code cannot be empty.");
			return;
		}

		System.out.print("Quantity: ");
		int qty;
		try {
			qty = Integer.parseInt(scanner.nextLine().trim());
			if (qty <= 0) {
				System.out.println("Quantity must be positive.");
				return;
			}
		} catch (NumberFormatException e) {
			System.out.println("Invalid quantity. Please enter a positive integer.");
			return;
		}

		try {
			inventoryManager.moveToShelf(code, qty);
			System.out.printf("Moved up to %d units of %s to shelf.%n", qty, code);
		} catch (Exception e) {
			System.out.println("Failed to move to shelf: " + e.getMessage());
		}
	}
}
