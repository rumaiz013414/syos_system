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
	private final String NL = System.lineSeparator();

	private final String TABLE_SEPARATOR = "-----------------------------------------------------------------------------------------------------------------";
	private final String TABLE_HEADER_FORMAT = "%-15s %-15s %-15s %-15s %-15s %-15s%n";
	private final String NOTE_MESSAGE = "Note: 'Shelf Qty' is the total quantity on the shelf. 'Batch Rem. Qty' is stock remaining in back-store batches.";

	@BeforeEach
	void setUp() {
		System.setOut(new PrintStream(outContent));
		viewStockCommand = new ViewStockCommand(inventoryManager, scanner);
	}

	@AfterEach
	void restoreStreams() {
		System.setOut(originalOut);
	}

	private String getCommonTableHeader() {
		return TABLE_SEPARATOR + NL + String.format(TABLE_HEADER_FORMAT, "Product Code", "Shelf Qty", "Batch ID",
				"Purch. Date", "Exp. Date", "Batch Rem. Qty") + TABLE_SEPARATOR + NL;
	}

	@Test
	@DisplayName("Should display 'No products found' message when inventory is empty (no filter)")
	void testExecute_displayAll_noProductsFound() {
		when(scanner.nextLine()).thenReturn("");
		when(inventoryManager.getAllProductCodes()).thenReturn(Collections.emptyList());

		viewStockCommand.execute();

		String expectedOutput = "Enter product code to view stock details (or leave blank to view all products): "
				+ "No products found in the system." + NL;
		assertEquals(expectedOutput, outContent.toString());
		verify(inventoryManager, times(1)).getAllProductCodes();
		verify(inventoryManager, never()).getQuantityOnShelf(anyString());
		verify(inventoryManager, never()).getBatchesForProduct(anyString());
	}

	@Test
	@DisplayName("Should display all products with mixed stock details (no filter)")
	void testExecute_displayAll_mixedStockDetails() {
		when(scanner.nextLine()).thenReturn("");

		String prodA = "PROD_A";
		String prodB = "PROD_B";
		String prodC = "PROD_C";
		String prodD = "PROD_D";
		String prodE = "PROD_E";

		List<String> allProductCodes = Arrays.asList(prodA, prodB, prodC, prodD, prodE);
		when(inventoryManager.getAllProductCodes()).thenReturn(allProductCodes);

		LocalDate today = LocalDate.now();
		StockBatch batchA1 = new StockBatch(101, prodA, today.minusMonths(3), today.plusMonths(3), 50);
		StockBatch batchA2 = new StockBatch(102, prodA, today.minusMonths(1), today.plusMonths(1), 20);
		when(inventoryManager.getQuantityOnShelf(prodA)).thenReturn(75);
		when(inventoryManager.getBatchesForProduct(prodA)).thenReturn(Arrays.asList(batchA1, batchA2));

		StockBatch batchB1 = new StockBatch(201, prodB, today.minusMonths(6), today.plusMonths(6), 100);
		when(inventoryManager.getQuantityOnShelf(prodB)).thenReturn(100);
		when(inventoryManager.getBatchesForProduct(prodB)).thenReturn(Collections.singletonList(batchB1));

		when(inventoryManager.getQuantityOnShelf(prodC)).thenReturn(15);
		when(inventoryManager.getBatchesForProduct(prodC)).thenReturn(Collections.emptyList());

		when(inventoryManager.getQuantityOnShelf(prodD)).thenReturn(0);
		when(inventoryManager.getBatchesForProduct(prodD)).thenReturn(Collections.emptyList());

		when(inventoryManager.getQuantityOnShelf(prodE)).thenReturn(25);
		when(inventoryManager.getBatchesForProduct(prodE)).thenReturn(Collections.emptyList());

		viewStockCommand.execute();

		StringBuilder expectedOutput = new StringBuilder();
		expectedOutput.append("Enter product code to view stock details (or leave blank to view all products): ");
		expectedOutput.append(NL).append("--- Current Shelf and Back-Store Stock Details (All Products) ---")
				.append(NL);
		expectedOutput.append(getCommonTableHeader());

		expectedOutput.append(
				String.format(TABLE_HEADER_FORMAT, prodA, 75, batchA1.getId(), batchA1.getPurchaseDate().toString(),
						batchA1.getExpiryDate().toString(), batchA1.getQuantityRemaining()));
		expectedOutput.append(
				String.format(TABLE_HEADER_FORMAT, "", "", batchA2.getId(), batchA2.getPurchaseDate().toString(),
						batchA2.getExpiryDate().toString(), batchA2.getQuantityRemaining()));
		expectedOutput.append(
				String.format(TABLE_HEADER_FORMAT, prodB, 100, batchB1.getId(), batchB1.getPurchaseDate().toString(),
						batchB1.getExpiryDate().toString(), batchB1.getQuantityRemaining()));
		expectedOutput.append(String.format(TABLE_HEADER_FORMAT, prodC, 15, "N/A", "N/A", "N/A", "N/A"));
		expectedOutput.append(String.format(TABLE_HEADER_FORMAT, prodD, 0, "N/A", "N/A", "N/A", "N/A"));
		expectedOutput.append(String.format(TABLE_HEADER_FORMAT, prodE, 25, "N/A", "N/A", "N/A", "N/A"));

		expectedOutput.append(TABLE_SEPARATOR).append(NL);
		expectedOutput.append(NOTE_MESSAGE).append(NL);

		assertEquals(expectedOutput.toString(), outContent.toString());

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
		String productCode = "SHELF_ONLY_PROD";
		when(scanner.nextLine()).thenReturn(productCode);

		when(inventoryManager.getQuantityOnShelf(productCode)).thenReturn(30);
		when(inventoryManager.getBatchesForProduct(productCode)).thenReturn(Collections.emptyList());

		viewStockCommand.execute();

		StringBuilder expectedOutput = new StringBuilder();
		expectedOutput.append("Enter product code to view stock details (or leave blank to view all products): ");
		expectedOutput.append(String.format(NL + "--- Current Stock Details for Product: %s ---%n", productCode));
		expectedOutput.append(getCommonTableHeader());
		expectedOutput.append(String.format(TABLE_HEADER_FORMAT, productCode, 30, "N/A", "N/A", "N/A", "N/A"));
		expectedOutput.append(TABLE_SEPARATOR).append(NL);
		expectedOutput.append(NOTE_MESSAGE).append(NL);

		assertEquals(expectedOutput.toString(), outContent.toString());
		verify(inventoryManager, never()).getAllProductCodes();
		verify(inventoryManager, times(1)).getQuantityOnShelf(productCode);
		verify(inventoryManager, times(1)).getBatchesForProduct(productCode);
	}

	@Test
	@DisplayName("Should handle unexpected exception when retrieving details for a filtered product")
	void testExecute_displayFiltered_unexpectedException() {
		String productCode = "ERROR_PROD";
		when(scanner.nextLine()).thenReturn(productCode);

		when(inventoryManager.getQuantityOnShelf(productCode)).thenReturn(10);
		when(inventoryManager.getBatchesForProduct(productCode)).thenThrow(new RuntimeException("Network error"));

		viewStockCommand.execute();

		StringBuilder expectedOutput = new StringBuilder();
		expectedOutput.append("Enter product code to view stock details (or leave blank to view all products): ");
		expectedOutput.append(String.format(NL + "--- Current Stock Details for Product: %s ---%n", productCode));
		expectedOutput.append(getCommonTableHeader());
		expectedOutput
				.append(String.format("An unexpected error occurred for product %s: Network error%n", productCode));
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
		String productCode = "SINGLE_PROD";
		when(scanner.nextLine()).thenReturn(productCode);

		LocalDate today = LocalDate.now();
		StockBatch batch1 = new StockBatch(401, productCode, today.minusMonths(1), today.plusMonths(5), 70);
		when(inventoryManager.getQuantityOnShelf(productCode)).thenReturn(70);
		when(inventoryManager.getBatchesForProduct(productCode)).thenReturn(Collections.singletonList(batch1));

		viewStockCommand.execute();

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
		String productCode = "FILTER_PROD";
		when(scanner.nextLine()).thenReturn(productCode);

		LocalDate today = LocalDate.now();
		StockBatch batch1 = new StockBatch(301, productCode, today.minusMonths(2), today.plusMonths(4), 40);
		StockBatch batch2 = new StockBatch(302, productCode, today.minusMonths(1), today.plusMonths(2), 15);
		when(inventoryManager.getQuantityOnShelf(productCode)).thenReturn(55);
		when(inventoryManager.getBatchesForProduct(productCode)).thenReturn(Arrays.asList(batch1, batch2));

		viewStockCommand.execute();

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
		verify(inventoryManager, never()).getAllProductCodes();
		verify(inventoryManager, times(1)).getQuantityOnShelf(productCode);
		verify(inventoryManager, times(1)).getBatchesForProduct(productCode);
	}

	@Test
	@DisplayName("Should display specific product not found error message when filtered product is not found (IllegalArgumentException)")
	void testExecute_displayFiltered_productNotFound() {
		String productCode = "NON_EXISTENT_PROD";
		when(scanner.nextLine()).thenReturn(productCode);

		when(inventoryManager.getQuantityOnShelf(productCode))
				.thenThrow(new IllegalArgumentException("Product not found: " + productCode));

		viewStockCommand.execute();

		StringBuilder expectedOutput = new StringBuilder();
		expectedOutput.append("Enter product code to view stock details (or leave blank to view all products): ");
		expectedOutput.append(String.format(NL + "--- Current Stock Details for Product: %s ---%n", productCode));
		expectedOutput.append(getCommonTableHeader());
		expectedOutput.append(
				String.format("Error: Product code '%s' not found or issue retrieving details: Product not found: %s%n",
						productCode, productCode));
		expectedOutput.append(TABLE_SEPARATOR).append(NL);
		expectedOutput.append(NOTE_MESSAGE).append(NL);

		assertEquals(expectedOutput.toString(), outContent.toString());
		verify(inventoryManager, never()).getAllProductCodes();
		verify(inventoryManager, times(1)).getQuantityOnShelf(productCode);
		verify(inventoryManager, never()).getBatchesForProduct(anyString());
	}

	@Test
	@DisplayName("Should display stock details with N/A for a single product with zero shelf and no batches")
	void testExecute_displayFiltered_zeroStockNoBatches() {
		String productCode = "ZERO_STOCK_PROD";
		when(scanner.nextLine()).thenReturn(productCode);

		when(inventoryManager.getQuantityOnShelf(productCode)).thenReturn(0);
		when(inventoryManager.getBatchesForProduct(productCode)).thenReturn(Collections.emptyList());

		viewStockCommand.execute();

		StringBuilder expectedOutput = new StringBuilder();
		expectedOutput.append("Enter product code to view stock details (or leave blank to view all products): ");
		expectedOutput.append(String.format(NL + "--- Current Stock Details for Product: %s ---%n", productCode));
		expectedOutput.append(getCommonTableHeader());
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
		when(scanner.nextLine()).thenReturn("");
		String prod1 = "PROD1";
		String prod2 = "PROD2";
		List<String> allProductCodes = Arrays.asList(prod1, prod2);
		when(inventoryManager.getAllProductCodes()).thenReturn(allProductCodes);

		when(inventoryManager.getQuantityOnShelf(anyString())).thenThrow(new RuntimeException("DB Connection Lost"));
		when(inventoryManager.getBatchesForProduct(anyString())).thenThrow(new RuntimeException("DB Connection Lost"));

		viewStockCommand.execute();

		StringBuilder expectedOutput = new StringBuilder();
		expectedOutput.append("Enter product code to view stock details (or leave blank to view all products): ");
		expectedOutput.append(NL).append("--- Current Shelf and Back-Store Stock Details (All Products) ---")
				.append(NL);
		expectedOutput.append(getCommonTableHeader());
		expectedOutput
				.append(String.format("An unexpected error occurred for product %s: DB Connection Lost%n", prod1));
		expectedOutput
				.append(String.format("An unexpected error occurred for product %s: DB Connection Lost%n", prod2));
		expectedOutput
				.append("No valid stock data found for the specified products, or issues occurred during retrieval.")
				.append(NL);
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
		when(scanner.nextLine()).thenReturn("");
		String prod1 = "MISSING_PROD1";
		String prod2 = "MISSING_PROD2";
		List<String> allProductCodes = Arrays.asList(prod1, prod2);
		when(inventoryManager.getAllProductCodes()).thenReturn(allProductCodes);

		when(inventoryManager.getQuantityOnShelf(anyString()))
				.thenThrow(new IllegalArgumentException("Product not found"));
		when(inventoryManager.getBatchesForProduct(anyString()))
				.thenThrow(new IllegalArgumentException("Product not found"));

		viewStockCommand.execute();

		StringBuilder expectedOutput = new StringBuilder();
		expectedOutput.append("Enter product code to view stock details (or leave blank to view all products): ");
		expectedOutput.append(NL).append("--- Current Shelf and Back-Store Stock Details (All Products) ---")
				.append(NL);
		expectedOutput.append(getCommonTableHeader());
		expectedOutput
				.append("No valid stock data found for the specified products, or issues occurred during retrieval.")
				.append(NL);
		expectedOutput.append(TABLE_SEPARATOR).append(NL);
		expectedOutput.append(NOTE_MESSAGE).append(NL);

		assertEquals(expectedOutput.toString(), outContent.toString());

		verify(inventoryManager, times(1)).getAllProductCodes();
		verify(inventoryManager, times(1)).getQuantityOnShelf(prod1);
		verify(inventoryManager, never()).getBatchesForProduct(prod1);
		verify(inventoryManager, times(1)).getQuantityOnShelf(prod2);
		verify(inventoryManager, never()).getBatchesForProduct(prod2);
	}
}