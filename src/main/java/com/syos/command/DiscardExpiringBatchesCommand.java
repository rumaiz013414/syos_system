package com.syos.command;

import com.syos.model.StockBatch;
import com.syos.singleton.InventoryManager;
import com.syos.util.CommonVariables;

import java.util.List;
import java.util.Scanner;

public class DiscardExpiringBatchesCommand implements Command {
	private final InventoryManager inventoryManager;
	private final Scanner scanner;

	private static final String LINE_SEPARATOR = "--------------------------------------------------------------------------------------------------";

	public DiscardExpiringBatchesCommand(InventoryManager inventoryManager, Scanner scanner) {
		this.inventoryManager = inventoryManager;
		this.scanner = scanner;
	}

	@Override
	public void execute() {
		System.out.println("\n--- Discard Expiring Stock Batches (Back-Store) ---");

		int daysThreshold = getDaysThresholdInput();
		if (daysThreshold == -1) {
			return;
		}

		List<StockBatch> expiringBatches = inventoryManager.getAllExpiringBatches(daysThreshold);

		if (expiringBatches.isEmpty()) {
			System.out.printf("No stock batches found expiring within %d days in the back-store to discard.%n",
					daysThreshold);
			return;
		}

		displayExpiringBatches(expiringBatches, daysThreshold);

		int batchId = getBatchIdInput();
		if (batchId == 0) {
			System.out.println("Discard operation cancelled.");
			return;
		}

		StockBatch selectedBatch = findSelectedBatch(expiringBatches, batchId);
		if (selectedBatch == null) {
			System.out.println("Batch ID not found or not in the expiring list. Please select an ID from the list.");
			return;
		}

		System.out.printf("Selected Batch ID: %d, Product: %s, Current Remaining Quantity: %d%n", selectedBatch.getId(),
				selectedBatch.getProductCode(), selectedBatch.getQuantityRemaining());

		int quantityToDiscard = getQuantityToDiscardInput(selectedBatch);
		if (quantityToDiscard == -1) {
			return;
		}

		performDiscardOperation(batchId, quantityToDiscard);
	}

	private int getDaysThresholdInput() {
		while (true) {
			System.out.print(
					"Enter expiry threshold in days (e.g., 0 for already expired, 7 for next 7 days) to list batches: ");
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
		System.out.printf("%n--- Batches Expiring in Next %d Days ---%n", daysThreshold);
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

	private int getBatchIdInput() {
		while (true) {
			System.out.print("\nEnter Batch ID to discard (0 to cancel): ");
			String input = scanner.nextLine().trim();
			try {
				return Integer.parseInt(input);
			} catch (NumberFormatException e) {
				System.out.println("Invalid input. Please enter a number for Batch ID.");
			}
		}
	}

	private StockBatch findSelectedBatch(List<StockBatch> expiringBatches, int batchId) {
		for (StockBatch batch : expiringBatches) {
			if (batch.getId() == batchId) {
				return batch;
			}
		}
		return null;
	}

	private int getQuantityToDiscardInput(StockBatch selectedBatch) {
		while (true) {
			System.out.print("Enter quantity to discard from this batch (0 to discard all remaining): ");
			String input = scanner.nextLine().trim();
			try {
				int quantityToDiscard = Integer.parseInt(input);
				if (quantityToDiscard < CommonVariables.MINIMUMQUANTITY) {
					System.out.println("Quantity to discard must be non-negative.");
				} else {
					if (quantityToDiscard == CommonVariables.MINIMUMQUANTITY) {
						return selectedBatch.getQuantityRemaining();
					}
					if (quantityToDiscard > selectedBatch.getQuantityRemaining()) {
						System.out.printf("Cannot discard %d units. Only %d remaining in batch %d.%n",
								quantityToDiscard, selectedBatch.getQuantityRemaining(), selectedBatch.getId());
					} else {
						return quantityToDiscard;
					}
				}
			} catch (NumberFormatException e) {
				System.out.println("Invalid input. Please enter a number for quantity.");
			}
		}
	}

	private void performDiscardOperation(int batchId, int quantityToDiscard) {
		try {
			inventoryManager.discardBatchQuantity(batchId, quantityToDiscard);
			System.out.printf("Successfully discarded %d units from Batch ID %d.%n", quantityToDiscard, batchId);
		} catch (IllegalArgumentException e) {
			System.out.println("Error discarding stock: " + e.getMessage());
		} catch (Exception e) {
			System.out.println("An unexpected error occurred: " + e.getMessage());
		}
	}
}