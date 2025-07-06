package com.syos.command;

import com.syos.singleton.InventoryManager;
import com.syos.model.StockBatch;

import java.util.List;
import java.util.Scanner;

public class ViewAllInventoryStocksCommand implements Command {
	private final InventoryManager inventoryManager;
	private final String newLine = System.lineSeparator();

	public ViewAllInventoryStocksCommand(InventoryManager inventoryManager, Scanner scanner) {
		this.inventoryManager = inventoryManager;
	}

	@Override
	public void execute() {
		System.out.println(newLine + "--- All Inventory Stock Batches ---");
		List<String> allProductCodes = inventoryManager.getAllProductCodes();

		if (allProductCodes.isEmpty()) {
			System.out.println("No products with back-store stock found.");
			return;
		}

		displayAllProductBatches(allProductCodes);
		System.out.println("------------------------------------");
	}

	private void displayAllProductBatches(List<String> allProductCodes) {
		for (String productCode : allProductCodes) {
			List<StockBatch> batches = inventoryManager.getBatchesForProduct(productCode);

			if (!batches.isEmpty()) {
				System.out.println(newLine + "Product Code: " + productCode);
				printBatchHeader();
				printBatchDetails(batches);
			}
		}
	}

	private void printBatchHeader() {
		System.out.printf("%-5s %-15s %-15s %-10s %-10s%n", "ID", "Purchase Date", "Expiry Date", "Quantity",
				"Remaining");
		System.out.println("----- --------------- --------------- ---------- ----------");
	}

	private void printBatchDetails(List<StockBatch> batches) {
		for (StockBatch batch : batches) {
			System.out.printf("%-5d %-15s %-15s %-10d %-10d%n", batch.getId(), batch.getPurchaseDate(),
					batch.getExpiryDate(), batch.getQuantityRemaining(), batch.getQuantityRemaining());
		}
	}
}