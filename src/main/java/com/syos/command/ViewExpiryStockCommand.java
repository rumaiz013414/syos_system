package com.syos.command;

import com.syos.model.StockBatch;
import com.syos.singleton.InventoryManager;
import com.syos.util.CommonVariables;

import java.util.List;
import java.util.Scanner;

public class ViewExpiryStockCommand implements Command {
	private final InventoryManager inventoryManager;
	private final Scanner scanner;
	private final String newLine = System.lineSeparator();

	public ViewExpiryStockCommand(InventoryManager inventoryManager, Scanner scanner) {
		this.inventoryManager = inventoryManager;
		this.scanner = scanner;
	}

	String lineSeperator = "------------------------------------------------------------------------------------------";

	String tableHeader = "%-15s %-15s %-15s %-15s %-15s %-15s%n";

	@Override
	public void execute() {
		System.out.println(newLine + "--- View Close to Expiry Stocks on Shelf ---");
		System.out.print("Enter expiry threshold in days (e.g., 30 for stocks expiring in next 30 days): ");
		int daysThreshold;
		try {
			daysThreshold = Integer.parseInt(scanner.nextLine().trim());
			if (daysThreshold < CommonVariables.MININUMDAYS) {
				System.out.println("Expiry threshold must be a non-negative number.");
				return;
			}
		} catch (NumberFormatException e) {
			System.out.println("Invalid input. Please enter a number for days threshold.");
			return;
		}

		System.out.print("Enter product code to filter (or leave blank to view all products): ");
		String productCodeFilter = scanner.nextLine().trim();

		List<String> productCodesToIterate;
		if (productCodeFilter.isEmpty()) {
			productCodesToIterate = inventoryManager.getAllProductCodes();
		} else {
			productCodesToIterate = List.of(productCodeFilter);
		}

		StringBuilder tableContent = new StringBuilder();
		boolean anyProductDisplayedInTable = false;

		for (String productCode : productCodesToIterate) {
			int quantityOnShelf = inventoryManager.getQuantityOnShelf(productCode);
			List<StockBatch> expiringBatches = inventoryManager.getExpiringBatchesForProduct(productCode,
					daysThreshold);

			if (quantityOnShelf == CommonVariables.MINIMUMQUANTITY) {
				continue;
			}

			if (!expiringBatches.isEmpty()) {
				anyProductDisplayedInTable = true;
				StockBatch firstBatch = expiringBatches.get(CommonVariables.MININUMDAYS);
				tableContent.append(String.format(tableHeader, productCode, quantityOnShelf, firstBatch.getId(),
						firstBatch.getExpiryDate(), firstBatch.getPurchaseDate(), firstBatch.getQuantityRemaining()));

				for (int iterate = 1; iterate < expiringBatches.size(); iterate++) {
					StockBatch batch = expiringBatches.get(iterate);
					tableContent.append(String.format(tableHeader, "", "", batch.getId(), batch.getExpiryDate(),
							batch.getPurchaseDate(), batch.getQuantityRemaining()));
				}
			} else if (!productCodeFilter.isEmpty()) {

				anyProductDisplayedInTable = true;
				tableContent
						.append(String.format(tableHeader, productCode, quantityOnShelf, "N/A", "N/A", "N/A", "N/A"));

			}
		}

		if (anyProductDisplayedInTable) {
			System.out.printf("%n--- Products with Batches Expiring in Next %d Days ---%n", daysThreshold);
			System.out.println(lineSeperator);
			System.out.printf(tableHeader, "Product Code", "Shelf Qty", "Batch ID", "Exp. Date", "Purch. Date",
					"Batch Rem. Qty");
			System.out.println(lineSeperator);
			System.out.print(tableContent.toString());
			System.out.println(lineSeperator);
		} else {
			if (!productCodeFilter.isEmpty()) {
				System.out.printf(
						"No products found with batches expiring within %d days for product code '%s' or product has zero shelf quantity.%n",
						daysThreshold, productCodeFilter);
			} else {
				System.out.printf(
						"No products found with batches expiring within %d days or products have zero shelf quantity.%n",
						daysThreshold);
			}
		}
		System.out.println(
				"Note: 'Batch Rem. Qty' refers to stock remaining in back-store. 'Shelf Qty' is total on shelf.");
	}
}