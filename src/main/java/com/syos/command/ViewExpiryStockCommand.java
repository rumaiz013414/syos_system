package com.syos.command;

import com.syos.model.StockBatch;
import com.syos.singleton.InventoryManager;
import java.util.List;
import java.util.Scanner;
import java.util.Map;
import java.util.HashMap;

public class ViewExpiryStockCommand implements Command {
    private final InventoryManager inventoryManager;
    private final Scanner scanner;

    public ViewExpiryStockCommand(InventoryManager inventoryManager, Scanner scanner) {
        this.inventoryManager = inventoryManager;
        this.scanner = scanner;
    }

    @Override
    public void execute() {
        System.out.println("\n--- View Close to Expiry Stocks on Shelf ---");
        System.out.print("Enter expiry threshold in days (e.g., 30 for stocks expiring in next 30 days): ");
        int daysThreshold;
        try {
            daysThreshold = Integer.parseInt(scanner.nextLine().trim());
            if (daysThreshold < 0) {
                System.out.println("Expiry threshold must be a non-negative number.");
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Please enter a number for days threshold.");
            return;
        }

        System.out.print("Enter product code to filter (or leave blank to view all products): ");
        String productCodeFilter = scanner.nextLine().trim();

        List<String> productCodesToDisplay;
        if (productCodeFilter.isEmpty()) {
            // Get all product codes that *might* have expiring batches
            productCodesToDisplay = inventoryManager.getAllProductCodesWithExpiringBatches(daysThreshold);
        } else {
            productCodesToDisplay = List.of(productCodeFilter);
        }

        if (productCodesToDisplay.isEmpty()) {
            System.out.printf("No products found with batches expiring within %d days", daysThreshold);
            if (!productCodeFilter.isEmpty()) {
                System.out.printf(" for product code '%s'.%n", productCodeFilter);
            } else {
                System.out.println(".");
            }
            return;
        }

        System.out.printf("%n--- Products with Batches Expiring in Next %d Days ---%n", daysThreshold);
        System.out.println("-------------------------------------------------------------------------------------------------------------");
        System.out.printf("%-15s %-15s %-15s %-15s %-15s %-15s%n",
                          "Product Code", "Shelf Qty", "Batch ID", "Exp. Date", "Purch. Date", "Batch Rem. Qty");
        System.out.println("-------------------------------------------------------------------------------------------------------------");

        boolean foundAnyExpiring = false;

        for (String productCode : productCodesToDisplay) {
            List<StockBatch> expiringBatches = inventoryManager.getExpiringBatchesForProduct(productCode, daysThreshold);

            if (!expiringBatches.isEmpty()) {
                foundAnyExpiring = true;
                int quantityOnShelf = inventoryManager.getQuantityOnShelf(productCode);

                // Print the first row for this product with its shelf quantity
                StockBatch firstBatch = expiringBatches.get(0);
                System.out.printf("%-15s %-15d %-15d %-15s %-15s %-15d%n",
                                  productCode,
                                  quantityOnShelf,
                                  firstBatch.getId(),
                                  firstBatch.getExpiryDate(),
                                  firstBatch.getPurchaseDate(),
                                  firstBatch.getQuantityRemaining());

                // Print subsequent batches for the same product, leaving Product Code and Shelf Qty blank
                for (int i = 1; i < expiringBatches.size(); i++) {
                    StockBatch batch = expiringBatches.get(i);
                    System.out.printf("%-15s %-15s %-15d %-15s %-15s %-15d%n",
                                      "", // Blank for subsequent rows of the same product
                                      "", // Blank for subsequent rows of the same product
                                      batch.getId(),
                                      batch.getExpiryDate(),
                                      batch.getPurchaseDate(),
                                      batch.getQuantityRemaining());
                }
            } else if (!productCodeFilter.isEmpty()) {
                // If a specific product was requested but has no expiring batches, show its shelf qty
                int quantityOnShelf = inventoryManager.getQuantityOnShelf(productCode);
                System.out.printf("%-15s %-15d %-15s %-15s %-15s %-15s%n",
                                  productCode,
                                  quantityOnShelf,
                                  "N/A", "N/A", "N/A", "N/A"); // Indicate no expiring batches
            }
        }

        if (!foundAnyExpiring && productCodeFilter.isEmpty()) {
            System.out.println("No products found with batches expiring within the specified threshold.");
        }
        System.out.println("-------------------------------------------------------------------------------------------------------------");
        System.out.println("Note: 'Batch Rem. Qty' refers to stock remaining in back-store. 'Shelf Qty' is total on shelf.");
    }
}