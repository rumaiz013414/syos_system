package com.test.command;

import com.syos.command.ViewStockCommand;
import com.syos.model.StockBatch;
import com.syos.singleton.InventoryManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ViewStockCommandTest {

    @Mock
    private InventoryManager inventoryManager;
    @Mock
    private Scanner scanner;

    private ViewStockCommand viewStockCommand;

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final String NL = System.lineSeparator(); // Use system-dependent newline

    // Define the separator lines and note message used in the command
    private final String TABLE_SEPARATOR = "-----------------------------------------------------------------------------------------------------------------";
    private final String TABLE_HEADER_FORMAT = "%-15s %-15s %-15s %-15s %-15s %-15s%n";
    private final String NOTE_MESSAGE = "Note: 'Shelf Qty' is the total quantity on the shelf. 'Batch Rem. Qty' is stock remaining in back-store batches.";

    @BeforeEach
    void setUp() {
        // Redirect System.out to capture console output
        System.setOut(new PrintStream(outContent));
        // Initialize the command with mocked dependencies
        viewStockCommand = new ViewStockCommand(inventoryManager, scanner);
    }

    @AfterEach
    void restoreStreams() {
        // Restore original System.out after each test
        System.setOut(originalOut);
    }

    // Helper method to get the common table header string
    private String getCommonTableHeader() {
        return TABLE_SEPARATOR + NL +
               String.format(TABLE_HEADER_FORMAT, "Product Code", "Shelf Qty", "Batch ID", "Purch. Date", "Exp. Date", "Batch Rem. Qty") +
               TABLE_SEPARATOR + NL;
    }


    @Test
    @DisplayName("Should display 'No products found' message when inventory is empty (no filter)")
    void testExecute_displayAll_noProductsFound() {
        // Arrange
        // Simulate empty input for product code, triggering display of all products
        when(scanner.nextLine()).thenReturn("");
        // Mock inventoryManager to return an empty list of product codes
        when(inventoryManager.getAllProductCodes()).thenReturn(Collections.emptyList());

        // Act
        viewStockCommand.execute();

        // Assert
        String expectedOutput = "Enter product code to view stock details (or leave blank to view all products): " +
                                "No products found in the system." + NL;
        assertEquals(expectedOutput, outContent.toString());
        // Verify that getAllProductCodes was called exactly once
        verify(inventoryManager, times(1)).getAllProductCodes();
        // Verify that stock details methods were never called as there are no products
        verify(inventoryManager, never()).getQuantityOnShelf(anyString());
        verify(inventoryManager, never()).getBatchesForProduct(anyString());
    }

    @Test
    @DisplayName("Should display all products with mixed stock details (no filter)")
    void testExecute_displayAll_mixedStockDetails() {
        // Arrange
        // Simulate empty input for product code, triggering display of all products
        when(scanner.nextLine()).thenReturn("");

        // Define product codes for the test scenario
        String prodA = "PROD_A"; // Product with multiple batches and shelf qty
        String prodB = "PROD_B"; // Product with single batch and shelf qty
        String prodC = "PROD_C"; // Product with no batches, positive shelf qty
        String prodD = "PROD_D"; // Product with no stock (zero shelf, no batches)
        String prodE = "PROD_E"; // Another product with no batches, positive shelf qty

        List<String> allProductCodes = Arrays.asList(prodA, prodB, prodC, prodD, prodE);
        when(inventoryManager.getAllProductCodes()).thenReturn(allProductCodes);

        // Mock data for Prod A
        LocalDate today = LocalDate.now();
        StockBatch batchA1 = new StockBatch(101, prodA, today.minusMonths(3), today.plusMonths(3), 50);
        StockBatch batchA2 = new StockBatch(102, prodA, today.minusMonths(1), today.plusMonths(1), 20);
        when(inventoryManager.getQuantityOnShelf(prodA)).thenReturn(75);
        when(inventoryManager.getBatchesForProduct(prodA)).thenReturn(Arrays.asList(batchA1, batchA2));

        // Mock data for Prod B
        StockBatch batchB1 = new StockBatch(201, prodB, today.minusMonths(6), today.plusMonths(6), 100);
        when(inventoryManager.getQuantityOnShelf(prodB)).thenReturn(100);
        when(inventoryManager.getBatchesForProduct(prodB)).thenReturn(Collections.singletonList(batchB1));

        // Mock data for Prod C (no batches, positive shelf qty)
        when(inventoryManager.getQuantityOnShelf(prodC)).thenReturn(15);
        when(inventoryManager.getBatchesForProduct(prodC)).thenReturn(Collections.emptyList());

        // Mock data for Prod D (no batches, zero shelf qty)
        when(inventoryManager.getQuantityOnShelf(prodD)).thenReturn(0);
        when(inventoryManager.getBatchesForProduct(prodD)).thenReturn(Collections.emptyList());

        // Mock data for Prod E (no batches, positive shelf qty)
        when(inventoryManager.getQuantityOnShelf(prodE)).thenReturn(25);
        when(inventoryManager.getBatchesForProduct(prodE)).thenReturn(Collections.emptyList());

        // Act
        viewStockCommand.execute();

        // Assert
        StringBuilder expectedOutput = new StringBuilder();
        expectedOutput.append("Enter product code to view stock details (or leave blank to view all products): ");
        expectedOutput.append(NL).append("--- Current Shelf and Back-Store Stock Details (All Products) ---").append(NL);
        expectedOutput.append(getCommonTableHeader());

        // Expected output for PROD_A
        expectedOutput.append(String.format(TABLE_HEADER_FORMAT,
                prodA, 75, batchA1.getId(), batchA1.getPurchaseDate().toString(), batchA1.getExpiryDate().toString(), batchA1.getQuantityRemaining()));
        expectedOutput.append(String.format(TABLE_HEADER_FORMAT,
                "", "", batchA2.getId(), batchA2.getPurchaseDate().toString(), batchA2.getExpiryDate().toString(), batchA2.getQuantityRemaining()));
        // Expected output for PROD_B
        expectedOutput.append(String.format(TABLE_HEADER_FORMAT,
                prodB, 100, batchB1.getId(), batchB1.getPurchaseDate().toString(), batchB1.getExpiryDate().toString(), batchB1.getQuantityRemaining()));
        // Expected output for PROD_C (no batches, positive shelf qty)
        expectedOutput.append(String.format(TABLE_HEADER_FORMAT,
                prodC, 15, "N/A", "N/A", "N/A", "N/A"));
        // Expected output for PROD_D (no batches, zero shelf qty)
        expectedOutput.append(String.format(TABLE_HEADER_FORMAT,
                prodD, 0, "N/A", "N/A", "N/A", "N/A"));
        // Expected output for PROD_E (no batches, positive shelf qty)
        expectedOutput.append(String.format(TABLE_HEADER_FORMAT,
                prodE, 25, "N/A", "N/A", "N/A", "N/A"));

        expectedOutput.append(TABLE_SEPARATOR).append(NL);
        expectedOutput.append(NOTE_MESSAGE).append(NL);

        assertEquals(expectedOutput.toString(), outContent.toString());

        // Verify manager calls for all products
        verify(inventoryManager, times(1)).getAllProductCodes();
        verify(inventoryManager, times(1)).getQuantityOnShelf(prodA);
        verify(inventoryManager, times(1)).getBatchesForProduct(prodA);
        verify(inventoryManager, times(1)).getQuantityOnShelf(prodB);
        verify(inventoryManager, times(1)).getBatchesForProduct(prodB);
        verify(inventoryManager, times(1)).getQuantityOnShelf(prodC);
        verify(inventoryManager, times(1)).getBatchesForProduct(prodC);
        verify(inventoryManager, times(1)).getQuantityOnShelf(prodD);
        verify(inventoryManager, times(1)).getBatchesForProduct(prodD);
        verify(inventoryManager, times(1)).getQuantityOnShelf(prodE);
        verify(inventoryManager, times(1)).getBatchesForProduct(prodE);
    }

 

    @Test
    @DisplayName("Should display stock details for a specific product with no batches but positive shelf quantity")
    void testExecute_displayFiltered_noBatches_positiveShelfQty() {
        // Arrange
        String productCode = "SHELF_ONLY_PROD";
        when(scanner.nextLine()).thenReturn(productCode); // Filter by this product code

        when(inventoryManager.getQuantityOnShelf(productCode)).thenReturn(30);
        when(inventoryManager.getBatchesForProduct(productCode)).thenReturn(Collections.emptyList());

        // Act
        viewStockCommand.execute();

        // Assert
        StringBuilder expectedOutput = new StringBuilder();
        expectedOutput.append("Enter product code to view stock details (or leave blank to view all products): ");
        expectedOutput.append(String.format(NL + "--- Current Stock Details for Product: %s ---%n", productCode));
        expectedOutput.append(getCommonTableHeader());
        expectedOutput.append(String.format(TABLE_HEADER_FORMAT, productCode, 30, "N/A", "N/A", "N/A", "N/A"));
        expectedOutput.append(TABLE_SEPARATOR).append(NL);
        expectedOutput.append(NOTE_MESSAGE).append(NL);

        assertEquals(expectedOutput.toString(), outContent.toString());
        // Verify that getAllProductCodes is not called when a specific product code is provided
        verify(inventoryManager, never()).getAllProductCodes();
        verify(inventoryManager, times(1)).getQuantityOnShelf(productCode);
        verify(inventoryManager, times(1)).getBatchesForProduct(productCode);
    }

    @Test
    @DisplayName("Should handle unexpected exception when retrieving details for a filtered product")
    void testExecute_displayFiltered_unexpectedException() {
        // Arrange
        String productCode = "ERROR_PROD";
        when(scanner.nextLine()).thenReturn(productCode);

        // Mock a scenario where getQuantityOnShelf works, but getBatchesForProduct throws an unexpected error
        when(inventoryManager.getQuantityOnShelf(productCode)).thenReturn(10);
        when(inventoryManager.getBatchesForProduct(productCode)).thenThrow(new RuntimeException("Network error"));

        // Act
        viewStockCommand.execute();

        // Assert
        StringBuilder expectedOutput = new StringBuilder();
        expectedOutput.append("Enter product code to view stock details (or leave blank to view all products): ");
        expectedOutput.append(String.format(NL + "--- Current Stock Details for Product: %s ---%n", productCode));
        expectedOutput.append(getCommonTableHeader());
        // Expected error message from the command
        expectedOutput.append(String.format("An unexpected error occurred for product %s: Network error%n", productCode));
        expectedOutput.append(TABLE_SEPARATOR).append(NL);
        expectedOutput.append(NOTE_MESSAGE).append(NL);

        assertEquals(expectedOutput.toString(), outContent.toString());
        verify(inventoryManager, never()).getAllProductCodes();
        verify(inventoryManager, times(1)).getQuantityOnShelf(productCode);
        verify(inventoryManager, times(1)).getBatchesForProduct(productCode);
    }

    @Test
    @DisplayName("Should display stock details for a specific product with a single batch")
    void testExecute_displayFiltered_singleBatch() {
        // Arrange
        String productCode = "SINGLE_PROD";
        when(scanner.nextLine()).thenReturn(productCode);

        LocalDate today = LocalDate.now();
        StockBatch batch1 = new StockBatch(401, productCode, today.minusMonths(1), today.plusMonths(5), 70);
        when(inventoryManager.getQuantityOnShelf(productCode)).thenReturn(70);
        when(inventoryManager.getBatchesForProduct(productCode)).thenReturn(Collections.singletonList(batch1));

        // Act
        viewStockCommand.execute();

        // Assert
        StringBuilder expectedOutput = new StringBuilder();
        expectedOutput.append("Enter product code to view stock details (or leave blank to view all products): ");
        expectedOutput.append(String.format(NL + "--- Current Stock Details for Product: %s ---%n", productCode));
        expectedOutput.append(getCommonTableHeader());
        expectedOutput.append(String.format(TABLE_HEADER_FORMAT, productCode, 70, batch1.getId(),
                batch1.getPurchaseDate().toString(), batch1.getExpiryDate().toString(), batch1.getQuantityRemaining()));
        expectedOutput.append(TABLE_SEPARATOR).append(NL);
        expectedOutput.append(NOTE_MESSAGE).append(NL);

        assertEquals(expectedOutput.toString(), outContent.toString());
        verify(inventoryManager, never()).getAllProductCodes();
        verify(inventoryManager, times(1)).getQuantityOnShelf(productCode);
        verify(inventoryManager, times(1)).getBatchesForProduct(productCode);
    }

    @Test
    @DisplayName("Should display stock details for a specific product with multiple batches")
    void testExecute_displayFiltered_multipleBatches() {
        // Arrange
        String productCode = "FILTER_PROD";
        when(scanner.nextLine()).thenReturn(productCode); // Filtered product code

        LocalDate today = LocalDate.now();
        StockBatch batch1 = new StockBatch(301, productCode, today.minusMonths(2), today.plusMonths(4), 40);
        StockBatch batch2 = new StockBatch(302, productCode, today.minusMonths(1), today.plusMonths(2), 15);
        when(inventoryManager.getQuantityOnShelf(productCode)).thenReturn(55);
        when(inventoryManager.getBatchesForProduct(productCode)).thenReturn(Arrays.asList(batch1, batch2));

        // Act
        viewStockCommand.execute();

        // Assert
        StringBuilder expectedOutput = new StringBuilder();
        expectedOutput.append("Enter product code to view stock details (or leave blank to view all products): ");
        expectedOutput.append(String.format(NL + "--- Current Stock Details for Product: %s ---%n", productCode));
        expectedOutput.append(getCommonTableHeader());
        expectedOutput.append(String.format(TABLE_HEADER_FORMAT, productCode, 55, batch1.getId(),
                batch1.getPurchaseDate().toString(), batch1.getExpiryDate().toString(), batch1.getQuantityRemaining()));
        expectedOutput.append(String.format(TABLE_HEADER_FORMAT, "", "", batch2.getId(),
                batch2.getPurchaseDate().toString(), batch2.getExpiryDate().toString(), batch2.getQuantityRemaining()));
        expectedOutput.append(TABLE_SEPARATOR).append(NL);
        expectedOutput.append(NOTE_MESSAGE).append(NL);

        assertEquals(expectedOutput.toString(), outContent.toString());
        verify(inventoryManager, never()).getAllProductCodes(); // Not called when a filter is provided
        verify(inventoryManager, times(1)).getQuantityOnShelf(productCode);
        verify(inventoryManager, times(1)).getBatchesForProduct(productCode);
    }


    @Test
    @DisplayName("Should display specific product not found error message when filtered product is not found (IllegalArgumentException)")
    void testExecute_displayFiltered_productNotFound() {
        // Arrange
        String productCode = "NON_EXISTENT_PROD";
        when(scanner.nextLine()).thenReturn(productCode);

        // Mock inventoryManager to throw IllegalArgumentException for a non-existent product
        when(inventoryManager.getQuantityOnShelf(productCode))
                .thenThrow(new IllegalArgumentException("Product not found: " + productCode));

        // Act
        viewStockCommand.execute();

        // Assert
        StringBuilder expectedOutput = new StringBuilder();
        expectedOutput.append("Enter product code to view stock details (or leave blank to view all products): ");
        expectedOutput.append(String.format(NL + "--- Current Stock Details for Product: %s ---%n", productCode));
        expectedOutput.append(getCommonTableHeader());
        // The command itself prints the error message for filtered product not found
        expectedOutput.append(String.format("Error: Product code '%s' not found or issue retrieving details: Product not found: %s%n",
                productCode, productCode));
        expectedOutput.append(TABLE_SEPARATOR).append(NL);
        expectedOutput.append(NOTE_MESSAGE).append(NL);

        assertEquals(expectedOutput.toString(), outContent.toString());
        verify(inventoryManager, never()).getAllProductCodes();
        verify(inventoryManager, times(1)).getQuantityOnShelf(productCode);
        verify(inventoryManager, never()).getBatchesForProduct(anyString()); // Batches method shouldn't be called if getQuantityOnShelf fails
    }

    @Test
    @DisplayName("Should display stock details with N/A for a single product with zero shelf and no batches")
    void testExecute_displayFiltered_zeroStockNoBatches() {
        // Arrange
        String productCode = "ZERO_STOCK_PROD";
        when(scanner.nextLine()).thenReturn(productCode);

        when(inventoryManager.getQuantityOnShelf(productCode)).thenReturn(0);
        when(inventoryManager.getBatchesForProduct(productCode)).thenReturn(Collections.emptyList());

        // Act
        viewStockCommand.execute();

        // Assert
        StringBuilder expectedOutput = new StringBuilder();
        expectedOutput.append("Enter product code to view stock details (or leave blank to view all products): ");
        expectedOutput.append(String.format(NL + "--- Current Stock Details for Product: %s ---%n", productCode));
        expectedOutput.append(getCommonTableHeader());
        // The command should now print the N/A row for zero stock, zero batches
        expectedOutput.append(String.format(TABLE_HEADER_FORMAT, productCode, 0, "N/A", "N/A", "N/A", "N/A"));
        expectedOutput.append(TABLE_SEPARATOR).append(NL);
        expectedOutput.append(NOTE_MESSAGE).append(NL);

        assertEquals(expectedOutput.toString(), outContent.toString());
        verify(inventoryManager, never()).getAllProductCodes();
        verify(inventoryManager, times(1)).getQuantityOnShelf(productCode);
        verify(inventoryManager, times(1)).getBatchesForProduct(productCode);
    }

    @Test
    @DisplayName("Should display 'No valid stock data' message when all products in the system cause an unexpected error (all products view)")
    void testExecute_displayAll_allProductsError() {
        // Arrange
        when(scanner.nextLine()).thenReturn(""); // Empty input for all products
        String prod1 = "PROD1";
        String prod2 = "PROD2";
        List<String> allProductCodes = Arrays.asList(prod1, prod2);
        when(inventoryManager.getAllProductCodes()).thenReturn(allProductCodes);

        // Mock both calls to throw an exception for each product
        when(inventoryManager.getQuantityOnShelf(anyString())).thenThrow(new RuntimeException("DB Connection Lost"));
        when(inventoryManager.getBatchesForProduct(anyString())).thenThrow(new RuntimeException("DB Connection Lost"));

        // Act
        viewStockCommand.execute();

        // Assert
        StringBuilder expectedOutput = new StringBuilder();
        expectedOutput.append("Enter product code to view stock details (or leave blank to view all products): ");
        expectedOutput.append(NL).append("--- Current Shelf and Back-Store Stock Details (All Products) ---").append(NL);
        expectedOutput.append(getCommonTableHeader());
        expectedOutput.append(String.format("An unexpected error occurred for product %s: DB Connection Lost%n", prod1));
        expectedOutput.append(String.format("An unexpected error occurred for product %s: DB Connection Lost%n", prod2));
        expectedOutput.append("No valid stock data found for the specified products, or issues occurred during retrieval.").append(NL); // Final message
        expectedOutput.append(TABLE_SEPARATOR).append(NL);
        expectedOutput.append(NOTE_MESSAGE).append(NL);

        assertEquals(expectedOutput.toString(), outContent.toString());

        verify(inventoryManager, times(1)).getAllProductCodes();
        verify(inventoryManager, times(1)).getQuantityOnShelf(prod1);
        verify(inventoryManager, times(1)).getBatchesForProduct(prod1);
        verify(inventoryManager, times(1)).getQuantityOnShelf(prod2);
        verify(inventoryManager, times(1)).getBatchesForProduct(prod2);
    }

    @Test
    @DisplayName("Should display 'No valid stock data' message when all products in the system are not found (all products view)")
    void testExecute_displayAll_allProductsNotFound() {
        // Arrange
        when(scanner.nextLine()).thenReturn(""); // Empty input for all products
        String prod1 = "MISSING_PROD1";
        String prod2 = "MISSING_PROD2";
        List<String> allProductCodes = Arrays.asList(prod1, prod2);
        when(inventoryManager.getAllProductCodes()).thenReturn(allProductCodes);

        // Mock both calls to throw IllegalArgumentException
        when(inventoryManager.getQuantityOnShelf(anyString())).thenThrow(new IllegalArgumentException("Product not found"));
        when(inventoryManager.getBatchesForProduct(anyString())).thenThrow(new IllegalArgumentException("Product not found"));


        // Act
        viewStockCommand.execute();

        // Assert
        StringBuilder expectedOutput = new StringBuilder();
        expectedOutput.append("Enter product code to view stock details (or leave blank to view all products): ");
        expectedOutput.append(NL).append("--- Current Shelf and Back-Store Stock Details (All Products) ---").append(NL);
        expectedOutput.append(getCommonTableHeader());
        // For 'all products' view, IllegalArgumentExceptions are generally not printed per product,
        // but the final "No valid stock data" message covers it.
        expectedOutput.append("No valid stock data found for the specified products, or issues occurred during retrieval.").append(NL);
        expectedOutput.append(TABLE_SEPARATOR).append(NL);
        expectedOutput.append(NOTE_MESSAGE).append(NL);

        assertEquals(expectedOutput.toString(), outContent.toString());

        verify(inventoryManager, times(1)).getAllProductCodes();
        verify(inventoryManager, times(1)).getQuantityOnShelf(prod1);
        verify(inventoryManager, never()).getBatchesForProduct(prod1); // Batches not called if quantity throws
        verify(inventoryManager, times(1)).getQuantityOnShelf(prod2);
        verify(inventoryManager, never()).getBatchesForProduct(prod2); // Batches not called if quantity throws
    }
}