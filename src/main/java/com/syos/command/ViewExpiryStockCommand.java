package com.syos.command;

import com.syos.model.StockBatch;
import com.syos.singleton.InventoryManager;
import java.util.List;
import java.util.Scanner;

public class ViewExpiryStockCommand implements Command {
    private final InventoryManager inventoryManager;
    private final Scanner scanner;
    private final String NL = System.lineSeparator(); // Consistent newline

    public ViewExpiryStockCommand(InventoryManager inventoryManager, Scanner scanner) {
        this.inventoryManager = inventoryManager;
        this.scanner = scanner;
    }

    @Override
    public void execute() {
        System.out.println(NL + "--- View Close to Expiry Stocks on Shelf ---");
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

        // This list will contain product codes to iterate through.
        // If no filter, we get all product codes to check their shelf quantity and expiring batches.
        List<String> productCodesToIterate;
        if (productCodeFilter.isEmpty()) {
            productCodesToIterate = inventoryManager.getAllProductCodes();
        } else {
            productCodesToIterate = List.of(productCodeFilter);
        }

        // Table header and separator definition (adjusted for new length)
        String TABLE_SEPARATOR = "------------------------------------------------------------------------------------------"; // 90 hyphens
        String TABLE_HEADER_FORMAT = "%-15s %-15s %-15s %-15s %-15s %-15s%n";

        StringBuilder tableContent = new StringBuilder(); // To build the main table rows
        boolean anyProductDisplayedInTable = false; // Flag if any row was actually added to tableContent

        for (String productCode : productCodesToIterate) {
            int quantityOnShelf = inventoryManager.getQuantityOnShelf(productCode);
            List<StockBatch> expiringBatches = inventoryManager.getExpiringBatchesForProduct(productCode, daysThreshold);

            // NEW LOGIC: Skip this product entirely if its shelf quantity is 0
            if (quantityOnShelf == 0) {
                continue;
            }

            // A product is displayed if it has expiring batches AND positive shelf quantity
            // OR if it was specifically filtered AND has positive shelf quantity (even if no expiring batches)
            if (!expiringBatches.isEmpty()) { // Case: Product has expiring batches AND shelf qty > 0 (checked above)
                anyProductDisplayedInTable = true;
                StockBatch firstBatch = expiringBatches.get(0);
                tableContent.append(String.format(TABLE_HEADER_FORMAT,
                        productCode,
                        quantityOnShelf,
                        firstBatch.getId(),
                        firstBatch.getExpiryDate(),
                        firstBatch.getPurchaseDate(),
                        firstBatch.getQuantityRemaining()));

                for (int i = 1; i < expiringBatches.size(); i++) {
                    StockBatch batch = expiringBatches.get(i);
                    tableContent.append(String.format(TABLE_HEADER_FORMAT,
                            "", "", // Blank for subsequent rows
                            batch.getId(),
                            batch.getExpiryDate(),
                            batch.getPurchaseDate(),
                            batch.getQuantityRemaining()));
                }
            } else if (!productCodeFilter.isEmpty()) { // Case: Specific product code was entered, has shelf quantity > 0 (checked above), but no expiring batches
                anyProductDisplayedInTable = true; // This counts as a row for display
                tableContent.append(String.format(TABLE_HEADER_FORMAT,
                        productCode,
                        quantityOnShelf,
                        "N/A", "N/A", "N/A", "N/A")); // Indicate no expiring batches
            }
        }

        // Print results based on whether any rows were generated
        if (anyProductDisplayedInTable) {
            System.out.printf("%n--- Products with Batches Expiring in Next %d Days ---%n", daysThreshold);
            System.out.println(TABLE_SEPARATOR);
            System.out.printf(TABLE_HEADER_FORMAT,
                    "Product Code", "Shelf Qty", "Batch ID", "Exp. Date", "Purch. Date", "Batch Rem. Qty");
            System.out.println(TABLE_SEPARATOR);
            System.out.print(tableContent.toString()); // Print all accumulated rows
            System.out.println(TABLE_SEPARATOR);
        } else {
            // This message covers cases where no products met the display criteria:
            // 1. No product codes were returned by getAllProductCodes initially (if no filter).
            // 2. All relevant products had 0 shelf quantity.
            // 3. No products had expiring batches within the threshold (if no filter).
            // 4. A specific product was filtered, but it had 0 shelf quantity OR no expiring batches (and thus no displayable row).
            if (!productCodeFilter.isEmpty()) {
                System.out.printf("No products found with batches expiring within %d days for product code '%s' or product has zero shelf quantity.%n", daysThreshold, productCodeFilter);
            } else {
                System.out.printf("No products found with batches expiring within %d days or products have zero shelf quantity.%n", daysThreshold);
            }
        }
        System.out.println("Note: 'Batch Rem. Qty' refers to stock remaining in back-store. 'Shelf Qty' is total on shelf.");
    }
}