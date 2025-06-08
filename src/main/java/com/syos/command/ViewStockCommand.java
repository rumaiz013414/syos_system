package com.syos.command;

import com.syos.singleton.InventoryManager;
import com.syos.util.CommonVariables;

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

	String lineSeperator = "-----------------------------------------------------------------------------------------------------------------";

	@Override
	public void execute() {
		System.out.print("Enter product code to view stock details (or leave blank to view all products): ");
		String productCode = scanner.nextLine().trim();

		if (productCode.isEmpty()) {
			displayAllStockDetailsInTable();
		} else {
			displayStockDetailsInTable(productCode);
		}
	}

	private void displayAllStockDetailsInTable() {
		try {
			List<String> allProductCodes = inventoryManager.getAllProductCodes();

			if (allProductCodes.isEmpty()) {
				System.out.println("No products found in the system.");
				return;
			}

			System.out.println("\n--- Current Shelf and Back-Store Stock Details (All Products) ---");
			printStockTable(allProductCodes);

		} catch (Exception e) {
			System.out.println("Error fetching all stock information: " + e.getMessage());
		}
	}

	private void displayStockDetailsInTable(String productCode) {
		List<String> singleProductList = List.of(productCode);
		System.out.printf("\n--- Current Stock Details for Product: %s ---%n", productCode);
		printStockTable(singleProductList);
	}

	private void printStockTable(List<String> productCodes) {
		System.out.println(lineSeperator);
		System.out.printf("%-15s %-15s %-15s %-15s %-15s %-15s%n", "Product Code", "Shelf Qty", "Batch ID",
				"Purch. Date", "Exp. Date", "Batch Rem. Qty");
		System.out.println(lineSeperator);

		boolean anyProductFoundWithStock = false;

		for (String productCode : productCodes) {
			try {
				int quantityOnShelf = inventoryManager.getQuantityOnShelf(productCode);
				List<StockBatch> batches = inventoryManager.getBatchesForProduct(productCode);

				if (batches.isEmpty() && quantityOnShelf == CommonVariables.MINIMUMQUANTITY) {
					if (productCodes.size() == CommonVariables.PRODUCTQUANTITY) {
						System.out.printf("%-15s %-15d %-15s %-15s %-15s %-15s%n", productCode, 0, "N/A", "N/A", "N/A",
								"N/A");
						anyProductFoundWithStock = true;
					}
				}

				anyProductFoundWithStock = true;

				if (batches.isEmpty()) {
					System.out.printf("%-15s %-15d %-15s %-15s %-15s %-15s%n", productCode, quantityOnShelf, "N/A",
							"N/A", "N/A", "N/A");
				} else {
					StockBatch firstBatch = batches.get(0);
					System.out.printf("%-15s %-15d %-15d %-15s %-15s %-15d%n", productCode, quantityOnShelf,
							firstBatch.getId(), firstBatch.getPurchaseDate(), firstBatch.getExpiryDate(),
							firstBatch.getQuantityRemaining());

					for (int i = 1; i < batches.size(); i++) {
						StockBatch batch = batches.get(i);
						System.out.printf("%-15s %-15s %-15d %-15s %-15s %-15d%n", "", "", batch.getId(),
								batch.getPurchaseDate(), batch.getExpiryDate(), batch.getQuantityRemaining());
					}
				}
			} catch (IllegalArgumentException e) {
				if (productCodes.size() == 1) {
					System.out.printf("Error: Product code '%s' not found or issue retrieving details: %s%n",
							productCode, e.getMessage());
				} else {
				}
			} catch (Exception e) {
				System.out.printf("An unexpected error occurred for product %s: %s%n", productCode, e.getMessage());
			}
		}

		if (!anyProductFoundWithStock && !productCodes.isEmpty()) {
			System.out.println("No stock data found for the selected products.");
		}
		System.out.println(lineSeperator);
		System.out.println(
				"Note: 'Shelf Qty' is the total quantity on the shelf. 'Batch Rem. Qty' is stock remaining in back-store batches.");
	}
}