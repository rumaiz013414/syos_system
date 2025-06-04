package com.syos.command;

import com.syos.model.StockBatch;
import com.syos.singleton.InventoryManager;
import java.util.List;
import java.util.Scanner;

public class ViewExpiryStockCommand implements Command {
    private final InventoryManager inventoryManager;
    private final Scanner scanner;

    public ViewExpiryStockCommand(InventoryManager inventoryManager, Scanner scanner) {
        this.inventoryManager = inventoryManager;
        this.scanner = scanner;
    }

    @Override
    public void execute() {
        System.out.println("\n--- View Close to Expiry Stocks ---");
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
            productCodesToDisplay = inventoryManager.getAllProductCodesWithExpiringBatches(daysThreshold);
        } else {
            productCodesToDisplay = List.of(productCodeFilter); // Only check this specific product
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
        boolean foundAny = false;
        for (String productCode : productCodesToDisplay) {
            List<StockBatch> expiringBatches = inventoryManager.getExpiringBatchesForProduct(productCode, daysThreshold);

            if (!expiringBatches.isEmpty()) {
                foundAny = true;
                int quantityOnShelf = inventoryManager.getQuantityOnShelf(productCode);
                System.out.printf("%nProduct Code: %s%n", productCode);
                System.out.printf("  Quantity on Shelf: %d%n", quantityOnShelf);
                System.out.println("  Expiring Batches (Back-Store & Potentially on Shelf):");
                for (StockBatch batch : expiringBatches) {
                    System.out.printf("    Batch ID: %d, Exp. Date: %s, Remaining Qty (Back-Store): %d%n",
                            batch.getId(), batch.getExpiryDate(), batch.getQuantityRemaining());
                }
            } else if (!productCodeFilter.isEmpty()) {
                // If a specific product was requested but has no expiring batches
                System.out.printf("%nProduct Code: %s - No batches expiring within %d days.%n", productCode, daysThreshold);
            }
        }

        if (!foundAny && productCodeFilter.isEmpty()) {
            System.out.printf("No products found with batches expiring within %d days.%n", daysThreshold);
        }
    }
}