package com.syos.command;

import com.syos.singleton.InventoryManager;
import java.util.Scanner;
import java.util.List;
import com.syos.model.StockBatch;

public class ViewStockCommand implements Command {
	private final InventoryManager inventoryManager;
	private final Scanner scanner;

	public ViewStockCommand(InventoryManager inventoryManager, Scanner scanner) {
		this.inventoryManager = inventoryManager;
		this.scanner = scanner;
	}

	@Override
	public void execute() {
		System.out.print("Enter product code to view stock details (or leave blank to view all products): ");
		String productCode = scanner.nextLine().trim();

		if (productCode.isEmpty()) {
			System.out.println("Viewing all products on shelf and their batches:");
			displayAllStockDetails();
		} else {
			displayStockDetails(productCode);
		}
	}

	private void displayAllStockDetails() {
		try {
			List<String> allProductCodes = inventoryManager.getAllProductCodes();
			if (allProductCodes.isEmpty()) {
				System.out.println("No products found in the system.");
				return;
			}

			System.out.println("--- Current Shelf and Batch Stock (All Products) ---");
			for (String code : allProductCodes) {
				displayStockDetails(code);
			}
		} catch (Exception e) {
			System.out.println("Error fetching all stock information: " + e.getMessage());
		}
	}

	private void displayStockDetails(String productCode) {
		try {
			int quantityOnShelf = inventoryManager.getQuantityOnShelf(productCode);
			System.out.printf("Product Code: %s, Quantity on Shelf: %d%n", productCode, quantityOnShelf);

			List<StockBatch> batches = inventoryManager.getBatchesForProduct(productCode);
			if (batches.isEmpty()) {
				System.out.println("  No batches found for this product.");
			} else {
				System.out.println("  Batches:");
				for (StockBatch batch : batches) {
					System.out.printf("    Batch ID: %d, Purchase Date: %s, Expiry Date: %s, Remaining Quantity: %d%n",
							batch.getId(), batch.getPurchaseDate(), batch.getExpiryDate(),
							batch.getQuantityRemaining());
				}
			}
		} catch (IllegalArgumentException e) {
			System.out.println("Error: " + e.getMessage());
		} catch (Exception e) {
			System.out.println("An unexpected error occurred: " + e.getMessage());
		}
	}
}