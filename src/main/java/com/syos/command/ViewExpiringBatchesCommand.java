package com.syos.command;

import com.syos.model.StockBatch;
import com.syos.singleton.InventoryManager;
import com.syos.util.CommonVariables;

import java.util.List;
import java.util.Scanner;

public class ViewExpiringBatchesCommand implements Command {
	private final InventoryManager inventoryManager;
	private final Scanner scanner;
	private final String newLine = System.lineSeparator();

	private static final String LINE_SEPARATOR = "--------------------------------------------------------------------------";

	public ViewExpiringBatchesCommand(InventoryManager inventoryManager, Scanner scanner) {
		this.inventoryManager = inventoryManager;
		this.scanner = scanner;
	}

	@Override
	public void execute() {
		System.out.println(newLine + "--- View Expiring Stock Batches (Back-Store) ---");

		int daysThreshold = getDaysThresholdInput();
		if (daysThreshold == -1) {
			return;
		}

		List<StockBatch> expiringBatches = inventoryManager.getAllExpiringBatches(daysThreshold);

		if (expiringBatches.isEmpty()) {
			System.out.printf("No stock batches found expiring within %d days in the back-store.%n", daysThreshold);
			return;
		}

		displayExpiringBatches(expiringBatches, daysThreshold);
	}

	private int getDaysThresholdInput() {
		while (true) {
			System.out.print("Enter expiry threshold in days (e.g., 30 for batches expiring in next 30 days): ");
			String input = scanner.nextLine().trim();
			try {
				int daysThreshold = Integer.parseInt(input);
				if (daysThreshold < CommonVariables.MININUMDAYS) {
					System.out.println("Expiry threshold must be a non-negative number.");
				} else {
					return daysThreshold;
				}
			} catch (NumberFormatException e) {
				System.out.println("Invalid input. Please enter a number for days threshold.");
			}
		}
	}

	private void displayExpiringBatches(List<StockBatch> expiringBatches, int daysThreshold) {
		System.out.printf("%n--- Stock Batches Expiring in Next %d Days ---%n", daysThreshold);
		System.out.println(LINE_SEPARATOR);
		System.out.printf("%-10s %-15s %-15s %-15s %-15s%n", "Batch ID", "Product Code", "Expiry Date", "Purch Date",
				"Remaining Qty");
		System.out.println(LINE_SEPARATOR);
		for (StockBatch batch : expiringBatches) {
			System.out.printf("%-10d %-15s %-15s %-15s %-15d%n", batch.getId(), batch.getProductCode(),
					batch.getExpiryDate(), batch.getPurchaseDate(), batch.getQuantityRemaining());
		}
		System.out.println(LINE_SEPARATOR);
	}
}