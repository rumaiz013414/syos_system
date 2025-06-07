package com.syos.command;

import com.syos.singleton.InventoryManager;
import com.syos.model.StockBatch;

import java.util.List;
import java.util.Scanner;

public class ViewAllInventoryStocksCommand implements Command {
	private final InventoryManager inventoryManager;
    private final String NL = System.lineSeparator(); // Declare NL for consistent use

	public ViewAllInventoryStocksCommand(InventoryManager inventoryManager, Scanner scanner) {
		this.inventoryManager = inventoryManager;
	}

	@Override
	public void execute() {
        // Use NL for the initial newline for consistency
		System.out.println(NL + "--- All Inventory Stock Batches ---");
		List<String> allProductCodes = inventoryManager.getAllProductCodes();

		if (allProductCodes.isEmpty()) {
			System.out.println("No products with back-store stock found.");
			return;
		}

		for (String productCode : allProductCodes) {
			List<StockBatch> batches = inventoryManager.getBatchesForProduct(productCode);

			if (!batches.isEmpty()) {
                // Use NL for the newline before Product Code
				System.out.println(NL + "Product Code: " + productCode);
				System.out.printf("%-5s %-15s %-15s %-10s %-10s%n", "ID", "Purchase Date", "Expiry Date", "Quantity",
						"Remaining");
				System.out.println("----- --------------- --------------- ---------- ----------");
				for (StockBatch batch : batches) {
					System.out.printf("%-5d %-15s %-15s %-10d %-10d%n", batch.getId(), batch.getPurchaseDate(),
							batch.getExpiryDate(), batch.getQuantityRemaining(), batch.getQuantityRemaining());
				}
			}
		}
		System.out.println("------------------------------------");
	}
}