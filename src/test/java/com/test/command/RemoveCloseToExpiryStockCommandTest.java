package com.test.command;

import com.syos.command.RemoveCloseToExpiryStockCommand;
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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RemoveCloseToExpiryStockCommandTest {

    @Mock
    private InventoryManager inventoryManager;
    @Mock
    private Scanner scanner;

    private RemoveCloseToExpiryStockCommand removeCloseToExpiryStockCommand;

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(outContent));
        removeCloseToExpiryStockCommand = new RemoveCloseToExpiryStockCommand(inventoryManager, scanner);
    }

    @AfterEach
    void restoreStreams() {
        System.setOut(originalOut);
    }

    // --- Happy Path Test ---

    @Test
    @DisplayName("Should successfully remove specified quantity of close-to-expiry stock from shelf")
    void shouldSuccessfullyRemoveCloseToExpiryStock() {
        // Arrange
        int daysThreshold = 30;
        String productCode1 = "PROD001";
        String productCode2 = "PROD002";
        int qtyToRemove = 5;
        int currentShelfQtyProd1 = 10;

        List<String> expiringProducts = Arrays.asList(productCode1, productCode2);
        StockBatch batch1_prod1 = new StockBatch(1, productCode1, LocalDate.now().minusMonths(6), LocalDate.now().plusDays(10), 20);
        StockBatch batch2_prod1 = new StockBatch(2, productCode1, LocalDate.now().minusMonths(3), LocalDate.now().plusDays(25), 15);
        List<StockBatch> expiringBatchesProd1 = Arrays.asList(batch1_prod1, batch2_prod1);

        // Mocking for productCode1 (first product in the list) during initial display and final check
        when(inventoryManager.getQuantityOnShelf(productCode1)).thenReturn(currentShelfQtyProd1);
        when(inventoryManager.getExpiringBatchesForProduct(productCode1, daysThreshold)).thenReturn(expiringBatchesProd1);

        // Mocking for productCode2 (second product in the list, just for display)
        when(inventoryManager.getQuantityOnShelf(productCode2)).thenReturn(5); // Some arbitrary quantity for display
        when(inventoryManager.getExpiringBatchesForProduct(productCode2, daysThreshold)).thenReturn(Collections.emptyList()); // Or some batches

        when(scanner.nextLine())
                .thenReturn(String.valueOf(daysThreshold)) // Input for daysThreshold
                .thenReturn(productCode1)                 // Input for productCodeToRemove
                .thenReturn(String.valueOf(qtyToRemove));  // Input for quantityToRemove

        when(inventoryManager.getAllProductCodesWithExpiringBatches(daysThreshold)).thenReturn(expiringProducts);
        
        doNothing().when(inventoryManager).removeQuantityFromShelf(productCode1, qtyToRemove);

        // Act
        removeCloseToExpiryStockCommand.execute();

        // Assert
        verify(scanner, times(3)).nextLine(); // Threshold, Product Code, Quantity
        verify(inventoryManager, times(1)).getAllProductCodesWithExpiringBatches(daysThreshold);
        
        // Verify calls during the listing loop for both products
        // productCode1: Called once during listing, once for the final check = total 2 times
        verify(inventoryManager, times(2)).getQuantityOnShelf(productCode1); 
        verify(inventoryManager, times(1)).getExpiringBatchesForProduct(productCode1, daysThreshold);

        // productCode2: Called once during listing only
        verify(inventoryManager, times(1)).getQuantityOnShelf(productCode2);
        verify(inventoryManager, times(1)).getExpiringBatchesForProduct(productCode2, daysThreshold);
        
        // Verify the call for the specific product selected for removal (PROD001)
        verify(inventoryManager, times(1)).removeQuantityFromShelf(productCode1, qtyToRemove);

        String output = outContent.toString();
        assertTrue(output.contains("--- Remove Close to Expiry Stocks from Shelf ---"));
        assertTrue(output.contains("Enter expiry threshold in days (e.g., 30 for stocks expiring in next 30 days) to see what might need removal:"));
        assertTrue(output.contains(String.format("Products identified with batches expiring in next %d days:", daysThreshold)));
        assertTrue(output.contains(String.format(" - %s (Current Shelf Qty: %d)", productCode1, currentShelfQtyProd1)));
        assertTrue(output.contains(String.format("Batch ID: %d, Exp. Date: %s, Remaining Qty (Back-Store): %d", batch1_prod1.getId(), batch1_prod1.getExpiryDate(), batch1_prod1.getQuantityRemaining())));
        assertTrue(output.contains("Enter product code to remove from shelf (from the list above):"));
        assertTrue(output.contains(String.format("Current quantity of %s on shelf: %d", productCode1, currentShelfQtyProd1)));
        assertTrue(output.contains("Enter quantity to remove from shelf:"));
        assertTrue(output.contains(String.format("Successfully removed %d units of %s from shelf, assumed to be close-to-expiry stock.", qtyToRemove, productCode1)));
    }

    // --- daysThreshold Input Validation Tests ---

    @Test
    @DisplayName("Should display error for non-numeric daysThreshold input")
    void shouldDisplayErrorForNonNumericDaysThreshold() {
        // Arrange
        when(scanner.nextLine()).thenReturn("abc"); // Invalid input

        // Act
        removeCloseToExpiryStockCommand.execute();

        // Assert
        verify(scanner, times(1)).nextLine();
        verify(inventoryManager, never()).getAllProductCodesWithExpiringBatches(anyInt()); // Should not proceed
        String output = outContent.toString();
        assertTrue(output.contains("Invalid input. Please enter a number for days threshold."));
    }

    @Test
    @DisplayName("Should display error for negative daysThreshold input")
    void shouldDisplayErrorForNegativeDaysThreshold() {
        // Arrange
        when(scanner.nextLine()).thenReturn("-10"); // Invalid input

        // Act
        removeCloseToExpiryStockCommand.execute();

        // Assert
        verify(scanner, times(1)).nextLine();
        verify(inventoryManager, never()).getAllProductCodesWithExpiringBatches(anyInt()); // Should not proceed
        String output = outContent.toString();
        assertTrue(output.contains("Expiry threshold must be a non-negative number."));
    }

    // --- No Expiring Batches Found Test ---

    @Test
    @DisplayName("Should inform user if no products have close-to-expiry batches")
    void shouldInformIfNoExpiringBatchesFound() {
        // Arrange
        int daysThreshold = 60;
        when(scanner.nextLine()).thenReturn(String.valueOf(daysThreshold));
        when(inventoryManager.getAllProductCodesWithExpiringBatches(daysThreshold)).thenReturn(Collections.emptyList()); // No expiring products

        // Act
        removeCloseToExpiryStockCommand.execute();

        // Assert
        verify(scanner, times(1)).nextLine();
        verify(inventoryManager, times(1)).getAllProductCodesWithExpiringBatches(daysThreshold);
        verify(inventoryManager, never()).getQuantityOnShelf(anyString()); // Should not proceed further
        String output = outContent.toString();
        assertTrue(output.contains(String.format("No products found with batches expiring within %d days. Nothing to consider for removal.%n", daysThreshold)));
    }

    // --- productCodeToRemove Validation Tests ---

    @Test
    @DisplayName("Should display error if product code to remove is empty")
    void shouldDisplayErrorForEmptyProductCodeToRemove() {
        // Arrange
        int daysThreshold = 30;
        String productCode1 = "PROD001";
        List<String> expiringProducts = Collections.singletonList(productCode1);

        when(scanner.nextLine())
                .thenReturn(String.valueOf(daysThreshold))
                .thenReturn(""); // Empty product code input

        when(inventoryManager.getAllProductCodesWithExpiringBatches(daysThreshold)).thenReturn(expiringProducts);
        // Mock for the product listing loop (which will call these methods)
        when(inventoryManager.getQuantityOnShelf(productCode1)).thenReturn(10);
        when(inventoryManager.getExpiringBatchesForProduct(productCode1, daysThreshold)).thenReturn(Collections.emptyList());

        // Act
        removeCloseToExpiryStockCommand.execute();

        // Assert
        verify(scanner, times(2)).nextLine(); // Threshold, Product Code
        verify(inventoryManager, times(1)).getAllProductCodesWithExpiringBatches(daysThreshold);
        // Verify that listing methods were called
        verify(inventoryManager, times(1)).getQuantityOnShelf(productCode1);
        verify(inventoryManager, times(1)).getExpiringBatchesForProduct(productCode1, daysThreshold);
        verify(inventoryManager, never()).removeQuantityFromShelf(anyString(), anyInt()); // Should not proceed to remove
        String output = outContent.toString();
        assertTrue(output.contains("Invalid or unlisted product code. No stock removed."));
    }

    @Test
    @DisplayName("Should display error if product code to remove is not in the listed expiring products")
    void shouldDisplayErrorForUnlistedProductCodeToRemove() {
        // Arrange
        int daysThreshold = 30;
        String productCode1 = "PROD001";
        String unlistedProductCode = "UNLISTED_PROD";
        List<String> expiringProducts = Collections.singletonList(productCode1);

        when(scanner.nextLine())
                .thenReturn(String.valueOf(daysThreshold))
                .thenReturn(unlistedProductCode); // Unlisted product code

        when(inventoryManager.getAllProductCodesWithExpiringBatches(daysThreshold)).thenReturn(expiringProducts);
        // Mock for the product listing loop
        when(inventoryManager.getQuantityOnShelf(productCode1)).thenReturn(10);
        when(inventoryManager.getExpiringBatchesForProduct(productCode1, daysThreshold)).thenReturn(Collections.emptyList());

        // Act
        removeCloseToExpiryStockCommand.execute();

        // Assert
        verify(scanner, times(2)).nextLine(); // Threshold, Product Code
        verify(inventoryManager, times(1)).getAllProductCodesWithExpiringBatches(daysThreshold);
        // Verify that listing methods were called
        verify(inventoryManager, times(1)).getQuantityOnShelf(productCode1);
        verify(inventoryManager, times(1)).getExpiringBatchesForProduct(productCode1, daysThreshold);
        verify(inventoryManager, never()).removeQuantityFromShelf(anyString(), anyInt()); // Should not proceed to remove
        String output = outContent.toString();
        assertTrue(output.contains("Invalid or unlisted product code. No stock removed."));
    }

    @Test
    @DisplayName("Should inform user if selected product is not on shelf (quantity is zero)")
    void shouldInformIfProductNotOnShelf() {
        // Arrange
        int daysThreshold = 30;
        String productCode = "PROD001";
        List<String> expiringProducts = Collections.singletonList(productCode);

        when(scanner.nextLine())
                .thenReturn(String.valueOf(daysThreshold))
                .thenReturn(productCode); // This is the *same* product code that will be listed and then selected

        when(inventoryManager.getAllProductCodesWithExpiringBatches(daysThreshold)).thenReturn(expiringProducts);
        // Mock getExpiringBatchesForProduct for the display loop
        when(inventoryManager.getExpiringBatchesForProduct(productCode, daysThreshold)).thenReturn(Collections.emptyList());
        // Mock getQuantityOnShelf for both the listing loop and the explicit check
        when(inventoryManager.getQuantityOnShelf(productCode)).thenReturn(0); // Product is not on shelf

        // Act
        removeCloseToExpiryStockCommand.execute();

        // Assert
        verify(scanner, times(2)).nextLine(); // Threshold, Product Code
        verify(inventoryManager, times(1)).getAllProductCodesWithExpiringBatches(daysThreshold);
        // getQuantityOnShelf for productCode will be called twice:
        // 1. During the loop to display products: inventoryManager.getQuantityOnShelf(productCode)
        // 2. For the specific product selected for removal: inventoryManager.getQuantityOnShelf(productCodeToRemove)
        verify(inventoryManager, times(2)).getQuantityOnShelf(productCode);
        verify(inventoryManager, times(1)).getExpiringBatchesForProduct(productCode, daysThreshold); // Called during display loop
        verify(inventoryManager, never()).removeQuantityFromShelf(anyString(), anyInt()); // Should not proceed to remove
        String output = outContent.toString();
        assertTrue(output.contains(String.format("Product %s is not currently on the shelf. No stock removed.", productCode)));
    }

    @Test
    @DisplayName("Should handle product code to remove with leading/trailing spaces")
    void shouldHandleProductCodeToRemoveWithSpaces() {
        // Arrange
        int daysThreshold = 30;
        String productCodeWithSpaces = "  PROD001  ";
        String expectedProductCode = "PROD001";
        int qtyToRemove = 5;
        int currentShelfQty = 10;

        List<String> expiringProducts = Collections.singletonList(expectedProductCode); // List contains trimmed code

        when(scanner.nextLine())
                .thenReturn(String.valueOf(daysThreshold))
                .thenReturn(productCodeWithSpaces) // Input with spaces
                .thenReturn(String.valueOf(qtyToRemove));

        when(inventoryManager.getAllProductCodesWithExpiringBatches(daysThreshold)).thenReturn(expiringProducts);
        // Mock calls for initial display loop
        when(inventoryManager.getQuantityOnShelf(expectedProductCode)).thenReturn(currentShelfQty); // This will be called twice (once for listing, once for check)
        when(inventoryManager.getExpiringBatchesForProduct(expectedProductCode, daysThreshold)).thenReturn(Collections.emptyList()); // For display
        doNothing().when(inventoryManager).removeQuantityFromShelf(expectedProductCode, qtyToRemove);

        // Act
        removeCloseToExpiryStockCommand.execute();

        // Assert
        verify(inventoryManager, times(2)).getQuantityOnShelf(expectedProductCode); // Verify trimmed code used
        verify(inventoryManager, times(1)).getExpiringBatchesForProduct(expectedProductCode, daysThreshold);
        verify(inventoryManager, times(1)).removeQuantityFromShelf(expectedProductCode, qtyToRemove); // Verify trimmed code used
        assertTrue(outContent.toString().contains("Successfully removed"));
    }


    // --- quantityToRemove Input Validation Tests ---

    @Test
    @DisplayName("Should display error for non-numeric quantityToRemove input")
    void shouldDisplayErrorForNonNumericQuantityToRemove() {
        // Arrange
        int daysThreshold = 30;
        String productCode = "PROD001";
        int currentShelfQty = 10;
        List<String> expiringProducts = Collections.singletonList(productCode);

        when(scanner.nextLine())
                .thenReturn(String.valueOf(daysThreshold))
                .thenReturn(productCode)
                .thenReturn("xyz"); // Invalid input for quantity

        when(inventoryManager.getAllProductCodesWithExpiringBatches(daysThreshold)).thenReturn(expiringProducts);
        when(inventoryManager.getQuantityOnShelf(productCode)).thenReturn(currentShelfQty);
        when(inventoryManager.getExpiringBatchesForProduct(productCode, daysThreshold)).thenReturn(Collections.emptyList());

        // Act
        removeCloseToExpiryStockCommand.execute();

        // Assert
        verify(scanner, times(3)).nextLine(); // Threshold, Product Code, Quantity
        // Verify listing-related calls
        verify(inventoryManager, times(2)).getQuantityOnShelf(productCode); // Once for list, once for check
        verify(inventoryManager, times(1)).getExpiringBatchesForProduct(productCode, daysThreshold);
        verify(inventoryManager, never()).removeQuantityFromShelf(anyString(), anyInt());
        String output = outContent.toString();
        assertTrue(output.contains("Invalid input. Please enter a number for quantity."));
    }

    @Test
    @DisplayName("Should display error for zero quantityToRemove input")
    void shouldDisplayErrorForZeroQuantityToRemove() {
        // Arrange
        int daysThreshold = 30;
        String productCode = "PROD001";
        int currentShelfQty = 10;
        List<String> expiringProducts = Collections.singletonList(productCode);

        when(scanner.nextLine())
                .thenReturn(String.valueOf(daysThreshold))
                .thenReturn(productCode)
                .thenReturn("0"); // Invalid input: zero quantity

        when(inventoryManager.getAllProductCodesWithExpiringBatches(daysThreshold)).thenReturn(expiringProducts);
        when(inventoryManager.getQuantityOnShelf(productCode)).thenReturn(currentShelfQty);
        when(inventoryManager.getExpiringBatchesForProduct(productCode, daysThreshold)).thenReturn(Collections.emptyList());

        // Act
        removeCloseToExpiryStockCommand.execute();

        // Assert
        verify(scanner, times(3)).nextLine();
        // Verify listing-related calls
        verify(inventoryManager, times(2)).getQuantityOnShelf(productCode); // Once for list, once for check
        verify(inventoryManager, times(1)).getExpiringBatchesForProduct(productCode, daysThreshold);
        verify(inventoryManager, never()).removeQuantityFromShelf(anyString(), anyInt());
        String output = outContent.toString();
        assertTrue(output.contains(String.format("Invalid quantity. Must be positive and not exceed current shelf quantity (%d).", currentShelfQty)));
    }

    @Test
    @DisplayName("Should display error for negative quantityToRemove input")
    void shouldDisplayErrorForNegativeQuantityToRemove() {
        // Arrange
        int daysThreshold = 30;
        String productCode = "PROD001";
        int currentShelfQty = 10;
        List<String> expiringProducts = Collections.singletonList(productCode);

        when(scanner.nextLine())
                .thenReturn(String.valueOf(daysThreshold))
                .thenReturn(productCode)
                .thenReturn("-5"); // Invalid input: negative quantity

        when(inventoryManager.getAllProductCodesWithExpiringBatches(daysThreshold)).thenReturn(expiringProducts);
        when(inventoryManager.getQuantityOnShelf(productCode)).thenReturn(currentShelfQty);
        when(inventoryManager.getExpiringBatchesForProduct(productCode, daysThreshold)).thenReturn(Collections.emptyList());

        // Act
        removeCloseToExpiryStockCommand.execute();

        // Assert
        verify(scanner, times(3)).nextLine();
        // Verify listing-related calls
        verify(inventoryManager, times(2)).getQuantityOnShelf(productCode); // Once for list, once for check
        verify(inventoryManager, times(1)).getExpiringBatchesForProduct(productCode, daysThreshold);
        verify(inventoryManager, never()).removeQuantityFromShelf(anyString(), anyInt());
        String output = outContent.toString();
        assertTrue(output.contains(String.format("Invalid quantity. Must be positive and not exceed current shelf quantity (%d).", currentShelfQty)));
    }

    @Test
    @DisplayName("Should display error if quantityToRemove exceeds current shelf quantity")
    void shouldDisplayErrorIfQuantityToRemoveExceedsShelfQuantity() {
        // Arrange
        int daysThreshold = 30;
        String productCode = "PROD001";
        int currentShelfQty = 10;
        int qtyToRemove = 15; // Exceeds current shelf quantity
        List<String> expiringProducts = Collections.singletonList(productCode);

        when(scanner.nextLine())
                .thenReturn(String.valueOf(daysThreshold))
                .thenReturn(productCode)
                .thenReturn(String.valueOf(qtyToRemove));

        when(inventoryManager.getAllProductCodesWithExpiringBatches(daysThreshold)).thenReturn(expiringProducts);
        when(inventoryManager.getQuantityOnShelf(productCode)).thenReturn(currentShelfQty);
        when(inventoryManager.getExpiringBatchesForProduct(productCode, daysThreshold)).thenReturn(Collections.emptyList());

        // Act
        removeCloseToExpiryStockCommand.execute();

        // Assert
        verify(scanner, times(3)).nextLine();
        // Verify listing-related calls
        verify(inventoryManager, times(2)).getQuantityOnShelf(productCode); // Once for list, once for check
        verify(inventoryManager, times(1)).getExpiringBatchesForProduct(productCode, daysThreshold);
        verify(inventoryManager, never()).removeQuantityFromShelf(anyString(), anyInt());
        String output = outContent.toString();
        assertTrue(output.contains(String.format("Invalid quantity. Must be positive and not exceed current shelf quantity (%d).", currentShelfQty)));
    }

    @Test
    @DisplayName("Should handle quantityToRemove with leading/trailing spaces")
    void shouldHandleQuantityToRemoveWithSpaces() {
        // Arrange
        int daysThreshold = 30;
        String productCode = "PROD001";
        String qtyToRemoveWithSpaces = "  5  ";
        int expectedQtyToRemove = 5;
        int currentShelfQty = 10;

        List<String> expiringProducts = Collections.singletonList(productCode);

        when(scanner.nextLine())
                .thenReturn(String.valueOf(daysThreshold))
                .thenReturn(productCode)
                .thenReturn(qtyToRemoveWithSpaces); // Input with spaces

        when(inventoryManager.getAllProductCodesWithExpiringBatches(daysThreshold)).thenReturn(expiringProducts);
        // Mock calls for initial display loop
        when(inventoryManager.getQuantityOnShelf(productCode)).thenReturn(currentShelfQty); // This will be called twice (once for listing, once for check)
        when(inventoryManager.getExpiringBatchesForProduct(productCode, daysThreshold)).thenReturn(Collections.emptyList());
        doNothing().when(inventoryManager).removeQuantityFromShelf(productCode, expectedQtyToRemove);

        // Act
        removeCloseToExpiryStockCommand.execute();

        // Assert
        verify(inventoryManager, times(2)).getQuantityOnShelf(productCode); // Verify trimmed code used
        verify(inventoryManager, times(1)).getExpiringBatchesForProduct(productCode, daysThreshold);
        verify(inventoryManager, times(1)).removeQuantityFromShelf(productCode, expectedQtyToRemove); // Verify trimmed code used
        assertTrue(outContent.toString().contains("Successfully removed"));
    }


    // --- Error Handling for InventoryManager.removeQuantityFromShelf ---

    @Test
    @DisplayName("Should display error if IllegalArgumentException occurs during removal")
    void shouldHandleIllegalArgumentExceptionOnRemoval() {
        // Arrange
        int daysThreshold = 30;
        String productCode = "PROD001";
        int qtyToRemove = 5;
        int currentShelfQty = 10;
        String errorMessage = "Cannot remove more than available quantity on shelf.";

        List<String> expiringProducts = Collections.singletonList(productCode);

        when(scanner.nextLine())
                .thenReturn(String.valueOf(daysThreshold))
                .thenReturn(productCode)
                .thenReturn(String.valueOf(qtyToRemove));

        when(inventoryManager.getAllProductCodesWithExpiringBatches(daysThreshold)).thenReturn(expiringProducts);
        // Mock calls for initial display loop
        when(inventoryManager.getQuantityOnShelf(productCode)).thenReturn(currentShelfQty); // This will be called twice
        when(inventoryManager.getExpiringBatchesForProduct(productCode, daysThreshold)).thenReturn(Collections.emptyList());
        doThrow(new IllegalArgumentException(errorMessage))
                .when(inventoryManager).removeQuantityFromShelf(productCode, qtyToRemove);

        // Act
        removeCloseToExpiryStockCommand.execute();

        // Assert
        verify(scanner, times(3)).nextLine();
        verify(inventoryManager, times(2)).getQuantityOnShelf(productCode);
        verify(inventoryManager, times(1)).getExpiringBatchesForProduct(productCode, daysThreshold);
        verify(inventoryManager, times(1)).removeQuantityFromShelf(productCode, qtyToRemove);
        String output = outContent.toString();
        assertTrue(output.contains("Error removing stock: " + errorMessage));
    }

    @Test
    @DisplayName("Should display unexpected error if generic Exception occurs during removal")
    void shouldHandleGenericExceptionOnRemoval() {
        // Arrange
        int daysThreshold = 30;
        String productCode = "PROD001";
        int qtyToRemove = 5;
        int currentShelfQty = 10;
        String errorMessage = "Database error during shelf removal.";

        List<String> expiringProducts = Collections.singletonList(productCode);

        when(scanner.nextLine())
                .thenReturn(String.valueOf(daysThreshold))
                .thenReturn(productCode)
                .thenReturn(String.valueOf(qtyToRemove));

        when(inventoryManager.getAllProductCodesWithExpiringBatches(daysThreshold)).thenReturn(expiringProducts);
        // Mock calls for initial display loop
        when(inventoryManager.getQuantityOnShelf(productCode)).thenReturn(currentShelfQty); // This will be called twice
        when(inventoryManager.getExpiringBatchesForProduct(productCode, daysThreshold)).thenReturn(Collections.emptyList());
        doThrow(new RuntimeException(errorMessage)) // Using RuntimeException for generic Exception
                .when(inventoryManager).removeQuantityFromShelf(productCode, qtyToRemove);

        // Act
        removeCloseToExpiryStockCommand.execute();

        // Assert
        verify(scanner, times(3)).nextLine();
        verify(inventoryManager, times(2)).getQuantityOnShelf(productCode);
        verify(inventoryManager, times(1)).getExpiringBatchesForProduct(productCode, daysThreshold);
        verify(inventoryManager, times(1)).removeQuantityFromShelf(productCode, qtyToRemove);
        String output = outContent.toString();
        assertTrue(output.contains("An unexpected error occurred: " + errorMessage));
    }
}