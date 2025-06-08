package com.syos.command;

import com.syos.model.StockBatch;
import com.syos.singleton.InventoryManager;
import com.syos.util.CommonVariables;

import java.util.List;
import java.util.Scanner;

public class RemoveCloseToExpiryStockCommand implements Command {
    private final InventoryManager inventoryManager;
    private final Scanner scanner;

    public RemoveCloseToExpiryStockCommand(InventoryManager inventoryManager, Scanner scanner) {
        this.inventoryManager = inventoryManager;
        this.scanner = scanner;
    }

    @Override
    public void execute() {
        System.out.println("\n--- Remove Close to Expiry Stocks from Shelf ---");
        System.out.print("Enter expiry threshold in days (e.g., 30 for stocks expiring in next 30 days) to see what might need removal: ");
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

        List<String> productsWithExpiringBatches = inventoryManager.getAllProductCodesWithExpiringBatches(daysThreshold);

        if (productsWithExpiringBatches.isEmpty()) {
            System.out.printf("No products found with batches expiring within %d days. Nothing to consider for removal.%n", daysThreshold);
            return;
        }

        System.out.printf("%nProducts identified with batches expiring in next %d days:%n", daysThreshold);
        for (String productCode : productsWithExpiringBatches) {
            int shelfQuantity = inventoryManager.getQuantityOnShelf(productCode);
            System.out.printf("  - %s (Current Shelf Qty: %d)%n", productCode, shelfQuantity);
            List<StockBatch> expiringBatches = inventoryManager.getExpiringBatchesForProduct(productCode, daysThreshold);
            for (StockBatch batch : expiringBatches) {
                System.out.printf("    Batch ID: %d, Exp. Date: %s, Remaining Qty (Back-Store): %d%n",
                        batch.getId(), batch.getExpiryDate(), batch.getQuantityRemaining());
            }
        }

        System.out.print("\nEnter product code to remove from shelf (from the list above): ");
        String productCodeToRemove = scanner.nextLine().trim();

        if (productCodeToRemove.isEmpty() || !productsWithExpiringBatches.contains(productCodeToRemove)) {
            System.out.println("Invalid or unlisted product code. No stock removed.");
            return;
        }

        int currentShelfQuantity = inventoryManager.getQuantityOnShelf(productCodeToRemove);
        if (currentShelfQuantity == CommonVariables.MINIMUMQUANTITY) {
            System.out.printf("Product %s is not currently on the shelf. No stock removed.%n", productCodeToRemove);
            return;
        }

        System.out.printf("Current quantity of %s on shelf: %d%n", productCodeToRemove, currentShelfQuantity);
        System.out.print("Enter quantity to remove from shelf: ");
        int quantityToRemove;
        try {
            quantityToRemove = Integer.parseInt(scanner.nextLine().trim());
            if (quantityToRemove <= CommonVariables.MINIMUMQUANTITY || quantityToRemove > currentShelfQuantity) {
                System.out.printf("Invalid quantity. Must be positive and not exceed current shelf quantity (%d).%n", currentShelfQuantity);
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Please enter a number for quantity.");
            return;
        }

        try {
            inventoryManager.removeQuantityFromShelf(productCodeToRemove, quantityToRemove);
            System.out.printf("Successfully removed %d units of %s from shelf, assumed to be close-to-expiry stock.%n", quantityToRemove, productCodeToRemove);
        } catch (IllegalArgumentException e) {
            System.out.println("Error removing stock: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("An unexpected error occurred: " + e.getMessage());
        }
    }
}