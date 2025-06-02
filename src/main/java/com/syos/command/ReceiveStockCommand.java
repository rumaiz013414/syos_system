package com.syos.command;

import java.time.LocalDate;
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

		System.out.print("Purchase date (YYYY-MM-DD): ");
		LocalDate pd;
		try {
			pd = LocalDate.parse(scanner.nextLine().trim());
		} catch (Exception e) {
			System.out.println("Invalid date format. Use YYYY-MM-DD.");
			return;
		}

		System.out.print("Expiry date  (YYYY-MM-DD): ");
		LocalDate ed;
		try {
			ed = LocalDate.parse(scanner.nextLine().trim());
		} catch (Exception e) {
			System.out.println("Invalid date format. Use YYYY-MM-DD.");
			return;
		}

		if (ed.isBefore(pd)) {
			System.out.println("Expiry date cannot be before purchase date.");
			return;
		}

		try {
			inventoryManager.receiveStock(code, pd, ed, qty);
			System.out.printf("Received %d units of %s (expires %s)%n", qty, code, ed);
		} catch (Exception e) {
			System.out.println("Failed to receive stock: " + e.getMessage());
		}
	}
}
