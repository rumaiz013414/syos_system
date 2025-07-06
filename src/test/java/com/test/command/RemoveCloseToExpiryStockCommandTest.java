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


	@Test
	@DisplayName("Should successfully remove specified quantity of close-to-expiry stock from shelf")
	void shouldSuccessfullyRemoveCloseToExpiryStock() {
		int daysThreshold = 30;
		String productCode1 = "PROD001";
		String productCode2 = "PROD002";
		int qtyToRemove = 5;
		int currentShelfQtyProd1 = 10;

		List<String> expiringProducts = Arrays.asList(productCode1, productCode2);
		StockBatch batch1_prod1 = new StockBatch(1, productCode1, LocalDate.now().minusMonths(6),
				LocalDate.now().plusDays(10), 20);
		StockBatch batch2_prod1 = new StockBatch(2, productCode1, LocalDate.now().minusMonths(3),
				LocalDate.now().plusDays(25), 15);
		List<StockBatch> expiringBatchesProd1 = Arrays.asList(batch1_prod1, batch2_prod1);

		when(inventoryManager.getQuantityOnShelf(productCode1)).thenReturn(currentShelfQtyProd1);
		when(inventoryManager.getExpiringBatchesForProduct(productCode1, daysThreshold))
				.thenReturn(expiringBatchesProd1);

		when(inventoryManager.getQuantityOnShelf(productCode2)).thenReturn(5);
		when(inventoryManager.getExpiringBatchesForProduct(productCode2, daysThreshold))
				.thenReturn(Collections.emptyList());

		when(scanner.nextLine()).thenReturn(String.valueOf(daysThreshold)).thenReturn(productCode1)
				.thenReturn(String.valueOf(qtyToRemove));

		when(inventoryManager.getAllProductCodesWithExpiringBatches(daysThreshold)).thenReturn(expiringProducts);

		doNothing().when(inventoryManager).removeQuantityFromShelf(productCode1, qtyToRemove);

		removeCloseToExpiryStockCommand.execute();

		verify(scanner, times(3)).nextLine();
		verify(inventoryManager, times(1)).getAllProductCodesWithExpiringBatches(daysThreshold);

		verify(inventoryManager, times(2)).getQuantityOnShelf(productCode1);
		verify(inventoryManager, times(1)).getExpiringBatchesForProduct(productCode1, daysThreshold);

		verify(inventoryManager, times(1)).getQuantityOnShelf(productCode2);
		verify(inventoryManager, times(1)).getExpiringBatchesForProduct(productCode2, daysThreshold);

		verify(inventoryManager, times(1)).removeQuantityFromShelf(productCode1, qtyToRemove);

		String output = outContent.toString();
		assertTrue(output.contains("--- Remove Close to Expiry Stocks from Shelf ---"));
		assertTrue(output.contains(
				"Enter expiry threshold in days (e.g., 30 for stocks expiring in next 30 days) to see what might need removal:"));
		assertTrue(output
				.contains(String.format("Products identified with batches expiring in next %d days:", daysThreshold)));
		assertTrue(output.contains(String.format(" - %s (Current Shelf Qty: %d)", productCode1, currentShelfQtyProd1)));
		assertTrue(output.contains(String.format("Batch ID: %d, Exp. Date: %s, Remaining Qty (Back-Store): %d",
				batch1_prod1.getId(), batch1_prod1.getExpiryDate(), batch1_prod1.getQuantityRemaining())));
		assertTrue(output.contains("Enter product code to remove from shelf (from the list above):"));
		assertTrue(output
				.contains(String.format("Current quantity of %s on shelf: %d", productCode1, currentShelfQtyProd1)));
		assertTrue(output.contains("Enter quantity to remove from shelf:"));
		assertTrue(output.contains(
				String.format("Successfully removed %d units of %s from shelf, assumed to be close-to-expiry stock.",
						qtyToRemove, productCode1)));
	}
	
	//daysThreshold Input Validation Tests 

	@Test
	@DisplayName("Should display error for non-numeric daysThreshold input")
	void shouldDisplayErrorForNonNumericDaysThreshold() {
		when(scanner.nextLine()).thenReturn("abc");

		removeCloseToExpiryStockCommand.execute();

		verify(scanner, times(1)).nextLine();
		verify(inventoryManager, never()).getAllProductCodesWithExpiringBatches(anyInt());
		String output = outContent.toString();
		assertTrue(output.contains("Invalid input. Please enter a number for days threshold."));
	}

	@Test
	@DisplayName("Should display error for negative daysThreshold input")
	void shouldDisplayErrorForNegativeDaysThreshold() {
		when(scanner.nextLine()).thenReturn("-10");

		removeCloseToExpiryStockCommand.execute();

		verify(scanner, times(1)).nextLine();
		verify(inventoryManager, never()).getAllProductCodesWithExpiringBatches(anyInt());
		String output = outContent.toString();
		assertTrue(output.contains("Expiry threshold must be a non-negative number."));
	}

	// No Expiring Batches Found Test

	@Test
	@DisplayName("Should inform user if no products have close-to-expiry batches")
	void shouldInformIfNoExpiringBatchesFound() {
		int daysThreshold = 60;
		when(scanner.nextLine()).thenReturn(String.valueOf(daysThreshold));
		when(inventoryManager.getAllProductCodesWithExpiringBatches(daysThreshold)).thenReturn(Collections.emptyList());

		removeCloseToExpiryStockCommand.execute();

		verify(scanner, times(1)).nextLine();
		verify(inventoryManager, times(1)).getAllProductCodesWithExpiringBatches(daysThreshold);
		verify(inventoryManager, never()).getQuantityOnShelf(anyString());
		String output = outContent.toString();
		assertTrue(output.contains(String.format(
				"No products found with batches expiring within %d days. Nothing to consider for removal.%n",
				daysThreshold)));
	}

	// productCodeToRemove Validation Tests 

	@Test
	@DisplayName("Should display error if product code to remove is empty")
	void shouldDisplayErrorForEmptyProductCodeToRemove() {
		int daysThreshold = 30;
		String productCode1 = "PROD001";
		List<String> expiringProducts = Collections.singletonList(productCode1);

		when(scanner.nextLine()).thenReturn(String.valueOf(daysThreshold)).thenReturn("");

		when(inventoryManager.getAllProductCodesWithExpiringBatches(daysThreshold)).thenReturn(expiringProducts);
		when(inventoryManager.getQuantityOnShelf(productCode1)).thenReturn(10);
		when(inventoryManager.getExpiringBatchesForProduct(productCode1, daysThreshold))
				.thenReturn(Collections.emptyList());

		removeCloseToExpiryStockCommand.execute();

		verify(scanner, times(2)).nextLine();
		verify(inventoryManager, times(1)).getAllProductCodesWithExpiringBatches(daysThreshold);
		verify(inventoryManager, times(1)).getQuantityOnShelf(productCode1);
		verify(inventoryManager, times(1)).getExpiringBatchesForProduct(productCode1, daysThreshold);
		verify(inventoryManager, never()).removeQuantityFromShelf(anyString(), anyInt());
		String output = outContent.toString();
		assertTrue(output.contains("Invalid or unlisted product code. No stock removed."));
	}

	@Test
	@DisplayName("Should display error if product code to remove is not in the listed expiring products")
	void shouldDisplayErrorForUnlistedProductCodeToRemove() {
		int daysThreshold = 30;
		String productCode1 = "PROD001";
		String unlistedProductCode = "UNLISTED_PROD";
		List<String> expiringProducts = Collections.singletonList(productCode1);

		when(scanner.nextLine()).thenReturn(String.valueOf(daysThreshold)).thenReturn(unlistedProductCode);

		when(inventoryManager.getAllProductCodesWithExpiringBatches(daysThreshold)).thenReturn(expiringProducts);
		when(inventoryManager.getQuantityOnShelf(productCode1)).thenReturn(10);
		when(inventoryManager.getExpiringBatchesForProduct(productCode1, daysThreshold))
				.thenReturn(Collections.emptyList());

		removeCloseToExpiryStockCommand.execute();

		verify(scanner, times(2)).nextLine();
		verify(inventoryManager, times(1)).getAllProductCodesWithExpiringBatches(daysThreshold);
		verify(inventoryManager, times(1)).getQuantityOnShelf(productCode1);
		verify(inventoryManager, times(1)).getExpiringBatchesForProduct(productCode1, daysThreshold);
		verify(inventoryManager, never()).removeQuantityFromShelf(anyString(), anyInt());
		String output = outContent.toString();
		assertTrue(output.contains("Invalid or unlisted product code. No stock removed."));
	}

	@Test
	@DisplayName("Should inform user if selected product is not on shelf (quantity is zero)")
	void shouldInformIfProductNotOnShelf() {
		int daysThreshold = 30;
		String productCode = "PROD001";
		List<String> expiringProducts = Collections.singletonList(productCode);

		when(scanner.nextLine()).thenReturn(String.valueOf(daysThreshold)).thenReturn(productCode);

		when(inventoryManager.getAllProductCodesWithExpiringBatches(daysThreshold)).thenReturn(expiringProducts);
		when(inventoryManager.getExpiringBatchesForProduct(productCode, daysThreshold))
				.thenReturn(Collections.emptyList());
		when(inventoryManager.getQuantityOnShelf(productCode)).thenReturn(0);

		removeCloseToExpiryStockCommand.execute();

		verify(scanner, times(2)).nextLine();
		verify(inventoryManager, times(1)).getAllProductCodesWithExpiringBatches(daysThreshold);
		verify(inventoryManager, times(2)).getQuantityOnShelf(productCode);
		verify(inventoryManager, times(1)).getExpiringBatchesForProduct(productCode, daysThreshold);
		verify(inventoryManager, never()).removeQuantityFromShelf(anyString(), anyInt());
		String output = outContent.toString();
		assertTrue(output
				.contains(String.format("Product %s is not currently on the shelf. No stock removed.", productCode)));
	}

	@Test
	@DisplayName("Should handle product code to remove with leading/trailing spaces")
	void shouldHandleProductCodeToRemoveWithSpaces() {
		int daysThreshold = 30;
		String productCodeWithSpaces = " PROD001 ";
		String expectedProductCode = "PROD001";
		int qtyToRemove = 5;
		int currentShelfQty = 10;

		List<String> expiringProducts = Collections.singletonList(expectedProductCode);

		when(scanner.nextLine()).thenReturn(String.valueOf(daysThreshold)).thenReturn(productCodeWithSpaces)
				.thenReturn(String.valueOf(qtyToRemove));

		when(inventoryManager.getAllProductCodesWithExpiringBatches(daysThreshold)).thenReturn(expiringProducts);
		when(inventoryManager.getQuantityOnShelf(expectedProductCode)).thenReturn(currentShelfQty);
		when(inventoryManager.getExpiringBatchesForProduct(expectedProductCode, daysThreshold))
				.thenReturn(Collections.emptyList());
		doNothing().when(inventoryManager).removeQuantityFromShelf(expectedProductCode, qtyToRemove);

		removeCloseToExpiryStockCommand.execute();

		verify(inventoryManager, times(2)).getQuantityOnShelf(expectedProductCode);
		verify(inventoryManager, times(1)).getExpiringBatchesForProduct(expectedProductCode, daysThreshold);
		verify(inventoryManager, times(1)).removeQuantityFromShelf(expectedProductCode, qtyToRemove);
		assertTrue(outContent.toString().contains("Successfully removed"));
	}

	//quantityToRemove Input Validation Tests 

	@Test
	@DisplayName("Should display error for non-numeric quantityToRemove input")
	void shouldDisplayErrorForNonNumericQuantityToRemove() {
		int daysThreshold = 30;
		String productCode = "PROD001";
		int currentShelfQty = 10;
		List<String> expiringProducts = Collections.singletonList(productCode);

		when(scanner.nextLine()).thenReturn(String.valueOf(daysThreshold)).thenReturn(productCode).thenReturn("xyz");

		when(inventoryManager.getAllProductCodesWithExpiringBatches(daysThreshold)).thenReturn(expiringProducts);
		when(inventoryManager.getQuantityOnShelf(productCode)).thenReturn(currentShelfQty);
		when(inventoryManager.getExpiringBatchesForProduct(productCode, daysThreshold))
				.thenReturn(Collections.emptyList());

		removeCloseToExpiryStockCommand.execute();

		verify(scanner, times(3)).nextLine();
		verify(inventoryManager, times(2)).getQuantityOnShelf(productCode);
		verify(inventoryManager, times(1)).getExpiringBatchesForProduct(productCode, daysThreshold);
		verify(inventoryManager, never()).removeQuantityFromShelf(anyString(), anyInt());
		String output = outContent.toString();
		assertTrue(output.contains("Invalid input. Please enter a number for quantity."));
	}

	@Test
	@DisplayName("Should display error for zero quantityToRemove input")
	void shouldDisplayErrorForZeroQuantityToRemove() {
		int daysThreshold = 30;
		String productCode = "PROD001";
		int currentShelfQty = 10;
		List<String> expiringProducts = Collections.singletonList(productCode);

		when(scanner.nextLine()).thenReturn(String.valueOf(daysThreshold)).thenReturn(productCode).thenReturn("0");

		when(inventoryManager.getAllProductCodesWithExpiringBatches(daysThreshold)).thenReturn(expiringProducts);
		when(inventoryManager.getQuantityOnShelf(productCode)).thenReturn(currentShelfQty);
		when(inventoryManager.getExpiringBatchesForProduct(productCode, daysThreshold))
				.thenReturn(Collections.emptyList());

		removeCloseToExpiryStockCommand.execute();

		verify(scanner, times(3)).nextLine();
		verify(inventoryManager, times(2)).getQuantityOnShelf(productCode);
		verify(inventoryManager, times(1)).getExpiringBatchesForProduct(productCode, daysThreshold);
		verify(inventoryManager, never()).removeQuantityFromShelf(anyString(), anyInt());
		String output = outContent.toString();
		assertTrue(output.contains(String.format(
				"Invalid quantity. Must be positive and not exceed current shelf quantity (%d).", currentShelfQty)));
	}

	@Test
	@DisplayName("Should display error for negative quantityToRemove input")
	void shouldDisplayErrorForNegativeQuantityToRemove() {
		int daysThreshold = 30;
		String productCode = "PROD001";
		int currentShelfQty = 10;
		List<String> expiringProducts = Collections.singletonList(productCode);

		when(scanner.nextLine()).thenReturn(String.valueOf(daysThreshold)).thenReturn(productCode).thenReturn("-5");

		when(inventoryManager.getAllProductCodesWithExpiringBatches(daysThreshold)).thenReturn(expiringProducts);
		when(inventoryManager.getQuantityOnShelf(productCode)).thenReturn(currentShelfQty);
		when(inventoryManager.getExpiringBatchesForProduct(productCode, daysThreshold))
				.thenReturn(Collections.emptyList());

		removeCloseToExpiryStockCommand.execute();

		verify(scanner, times(3)).nextLine();
		verify(inventoryManager, times(2)).getQuantityOnShelf(productCode);
		verify(inventoryManager, times(1)).getExpiringBatchesForProduct(productCode, daysThreshold);
		verify(inventoryManager, never()).removeQuantityFromShelf(anyString(), anyInt());
		String output = outContent.toString();
		assertTrue(output.contains(String.format(
				"Invalid quantity. Must be positive and not exceed current shelf quantity (%d).", currentShelfQty)));
	}

	@Test
	@DisplayName("Should display error if quantityToRemove exceeds current shelf quantity")
	void shouldDisplayErrorIfQuantityToRemoveExceedsShelfQuantity() {
		int daysThreshold = 30;
		String productCode = "PROD001";
		int currentShelfQty = 10;
		int qtyToRemove = 15;
		List<String> expiringProducts = Collections.singletonList(productCode);

		when(scanner.nextLine()).thenReturn(String.valueOf(daysThreshold)).thenReturn(productCode)
				.thenReturn(String.valueOf(qtyToRemove));

		when(inventoryManager.getAllProductCodesWithExpiringBatches(daysThreshold)).thenReturn(expiringProducts);
		when(inventoryManager.getQuantityOnShelf(productCode)).thenReturn(currentShelfQty);
		when(inventoryManager.getExpiringBatchesForProduct(productCode, daysThreshold))
				.thenReturn(Collections.emptyList());

		removeCloseToExpiryStockCommand.execute();

		verify(scanner, times(3)).nextLine();
		verify(inventoryManager, times(2)).getQuantityOnShelf(productCode);
		verify(inventoryManager, times(1)).getExpiringBatchesForProduct(productCode, daysThreshold);
		verify(inventoryManager, never()).removeQuantityFromShelf(anyString(), anyInt());
		String output = outContent.toString();
		assertTrue(output.contains(String.format(
				"Invalid quantity. Must be positive and not exceed current shelf quantity (%d).", currentShelfQty)));
	}

	@Test
	@DisplayName("Should handle quantityToRemove with leading/trailing spaces")
	void shouldHandleQuantityToRemoveWithSpaces() {
		int daysThreshold = 30;
		String productCode = "PROD001";
		String qtyToRemoveWithSpaces = " 5 ";
		int expectedQtyToRemove = 5;
		int currentShelfQty = 10;

		List<String> expiringProducts = Collections.singletonList(productCode);

		when(scanner.nextLine()).thenReturn(String.valueOf(daysThreshold)).thenReturn(productCode)
				.thenReturn(qtyToRemoveWithSpaces);

		when(inventoryManager.getAllProductCodesWithExpiringBatches(daysThreshold)).thenReturn(expiringProducts);
		when(inventoryManager.getQuantityOnShelf(productCode)).thenReturn(currentShelfQty);
		when(inventoryManager.getExpiringBatchesForProduct(productCode, daysThreshold))
				.thenReturn(Collections.emptyList());
		doNothing().when(inventoryManager).removeQuantityFromShelf(productCode, expectedQtyToRemove);

		removeCloseToExpiryStockCommand.execute();

		verify(inventoryManager, times(2)).getQuantityOnShelf(productCode);
		verify(inventoryManager, times(1)).getExpiringBatchesForProduct(productCode, daysThreshold);
		verify(inventoryManager, times(1)).removeQuantityFromShelf(productCode, expectedQtyToRemove);
		assertTrue(outContent.toString().contains("Successfully removed"));
	}

	// Error Handling for InventoryManager.removeQuantityFromShelf

	@Test
	@DisplayName("Should display error if IllegalArgumentException occurs during removal")
	void shouldHandleIllegalArgumentExceptionOnRemoval() {
		int daysThreshold = 30;
		String productCode = "PROD001";
		int qtyToRemove = 5;
		int currentShelfQty = 10;
		String errorMessage = "Cannot remove more than available quantity on shelf.";

		List<String> expiringProducts = Collections.singletonList(productCode);

		when(scanner.nextLine()).thenReturn(String.valueOf(daysThreshold)).thenReturn(productCode)
				.thenReturn(String.valueOf(qtyToRemove));

		when(inventoryManager.getAllProductCodesWithExpiringBatches(daysThreshold)).thenReturn(expiringProducts);
		when(inventoryManager.getQuantityOnShelf(productCode)).thenReturn(currentShelfQty);
		when(inventoryManager.getExpiringBatchesForProduct(productCode, daysThreshold))
				.thenReturn(Collections.emptyList());
		doThrow(new IllegalArgumentException(errorMessage)).when(inventoryManager).removeQuantityFromShelf(productCode,
				qtyToRemove);

		removeCloseToExpiryStockCommand.execute();

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
		int daysThreshold = 30;
		String productCode = "PROD001";
		int qtyToRemove = 5;
		int currentShelfQty = 10;
		String errorMessage = "Database error during shelf removal.";

		List<String> expiringProducts = Collections.singletonList(productCode);

		when(scanner.nextLine()).thenReturn(String.valueOf(daysThreshold)).thenReturn(productCode)
				.thenReturn(String.valueOf(qtyToRemove));

		when(inventoryManager.getAllProductCodesWithExpiringBatches(daysThreshold)).thenReturn(expiringProducts);
		when(inventoryManager.getQuantityOnShelf(productCode)).thenReturn(currentShelfQty);
		when(inventoryManager.getExpiringBatchesForProduct(productCode, daysThreshold))
				.thenReturn(Collections.emptyList());
		doThrow(new RuntimeException(errorMessage)).when(inventoryManager).removeQuantityFromShelf(productCode,
				qtyToRemove);

		removeCloseToExpiryStockCommand.execute();

		verify(scanner, times(3)).nextLine();
		verify(inventoryManager, times(2)).getQuantityOnShelf(productCode);
		verify(inventoryManager, times(1)).getExpiringBatchesForProduct(productCode, daysThreshold);
		verify(inventoryManager, times(1)).removeQuantityFromShelf(productCode, qtyToRemove);
		String output = outContent.toString();
		assertTrue(output.contains("An unexpected error occurred: " + errorMessage));
	}
}