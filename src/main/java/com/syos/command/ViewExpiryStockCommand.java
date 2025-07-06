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

	private static final String LINE_SEPARATOR = "------------------------------------------------------------------------------------------";
	private static final String TABLE_HEADER_FORMAT = "%-15s %-15s %-15s %-15s %-15s %-15s%n";

	public ViewExpiryStockCommand(InventoryManager inventoryManager, Scanner scanner) {
		this.inventoryManager = inventoryManager;
		this.scanner = scanner;
	}

	@Override
	public void execute() {
		System.out.println(newLine + "--- View Close to Expiry Stocks on Shelf ---");

		int daysThreshold = getDaysThresholdInput();
		if (daysThreshold == -1) {
			return;
		}

		String productCodeFilter = getProductCodeFilterInput();

		List<String> productCodesToIterate = getProductCodesToIterate(productCodeFilter);

		displayExpiryStockDetails(productCodesToIterate, daysThreshold, productCodeFilter);
	}

	private int getDaysThresholdInput() {
		while (true) {
			System.out.print("Enter expiry threshold in days (e.g., 30 for stocks expiring in next 30 days): ");
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

	private String getProductCodeFilterInput() {
		System.out.print("Enter product code to filter (or leave blank to view all products): ");
		return scanner.nextLine().trim();
	}

	private List<String> getProductCodesToIterate(String productCodeFilter) {
		if (productCodeFilter.isEmpty()) {
			return inventoryManager.getAllProductCodes();
		} else {
			return List.of(productCodeFilter);
		}
	}

	private void displayExpiryStockDetails(List<String> productCodesToIterate, int daysThreshold,
			String productCodeFilter) {
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
				appendBatchDetailsToTable(tableContent, productCode, quantityOnShelf, expiringBatches);
			} else if (!productCodeFilter.isEmpty()) {
				anyProductDisplayedInTable = true;
				tableContent.append(
						String.format(TABLE_HEADER_FORMAT, productCode, quantityOnShelf, "N/A", "N/A", "N/A", "N/A"));
			}
		}

		if (anyProductDisplayedInTable) {
			printFormattedTable(tableContent, daysThreshold);
		} else {
			printNoProductsFoundMessage(daysThreshold, productCodeFilter);
		}
		System.out.println(
				"Note: 'Batch Rem. Qty' refers to stock remaining in back-store. 'Shelf Qty' is total on shelf.");
	}

	private void appendBatchDetailsToTable(StringBuilder tableContent, String productCode, int quantityOnShelf,
			List<StockBatch> expiringBatches) {
		StockBatch firstBatch = expiringBatches.get(CommonVariables.MININUMDAYS);
		tableContent.append(String.format(TABLE_HEADER_FORMAT, productCode, quantityOnShelf, firstBatch.getId(),
				firstBatch.getExpiryDate(), firstBatch.getPurchaseDate(), firstBatch.getQuantityRemaining()));

		for (int iterate = 1; iterate < expiringBatches.size(); iterate++) {
			StockBatch batch = expiringBatches.get(iterate);
			tableContent.append(String.format(TABLE_HEADER_FORMAT, "", "", batch.getId(), batch.getExpiryDate(),
					batch.getPurchaseDate(), batch.getQuantityRemaining()));
		}
	}

	private void printFormattedTable(StringBuilder tableContent, int daysThreshold) {
		System.out.printf("%n--- Products with Batches Expiring in Next %d Days ---%n", daysThreshold);
		System.out.println(LINE_SEPARATOR);
		System.out.printf(TABLE_HEADER_FORMAT, "Product Code", "Shelf Qty", "Batch ID", "Exp. Date", "Purch. Date",
				"Batch Rem. Qty");
		System.out.println(LINE_SEPARATOR);
		System.out.print(tableContent.toString());
		System.out.println(LINE_SEPARATOR);
	}

	private void printNoProductsFoundMessage(int daysThreshold, String productCodeFilter) {
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
}