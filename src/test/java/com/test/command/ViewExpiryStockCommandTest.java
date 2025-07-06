package com.test.command;

import com.syos.command.ViewExpiryStockCommand;
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
class ViewExpiryStockCommandTest {

	@Mock
	private InventoryManager inventoryManager;
	@Mock
	private Scanner scanner;

	private ViewExpiryStockCommand viewExpiryStockCommand;

	private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
	private final PrintStream originalOut = System.out;
	private final String NL = System.lineSeparator();

	private final String TABLE_SEPARATOR = "------------------------------------------------------------------------------------------";
	private final String NOTE_MESSAGE = "Note: 'Batch Rem. Qty' refers to stock remaining in back-store. 'Shelf Qty' is total on shelf.";

	@BeforeEach
	void setUp() {
		System.setOut(new PrintStream(outContent));
		viewExpiryStockCommand = new ViewExpiryStockCommand(inventoryManager, scanner);
	}

	@AfterEach
	void restoreStreams() {
		System.setOut(originalOut);
	}

	private String getExpectedTableHeader(int daysThreshold) {
		return String.format("%n--- Products with Batches Expiring in Next %d Days ---%n", daysThreshold)
				+ TABLE_SEPARATOR + NL + String.format("%-15s %-15s %-15s %-15s %-15s %-15s%n", "Product Code",
						"Shelf Qty", "Batch ID", "Exp. Date", "Purch. Date", "Batch Rem. Qty")
				+ TABLE_SEPARATOR + NL;
	}

	@Test
	@DisplayName("Should display error message for non-numeric expiry threshold input")
	void testExecute_invalidInput_notANumber() {
		when(scanner.nextLine()).thenReturn("abc");

		viewExpiryStockCommand.execute();

		String expectedOutput = NL + "--- View Close to Expiry Stocks on Shelf ---" + NL
				+ "Enter expiry threshold in days (e.g., 30 for stocks expiring in next 30 days): "
				+ "Invalid input. Please enter a number for days threshold." + NL;
		assertEquals(expectedOutput, outContent.toString());
		verifyNoInteractions(inventoryManager);
	}

	@Test
	@DisplayName("Should display error message for negative expiry threshold input")
	void testExecute_invalidInput_negativeNumber() {
		when(scanner.nextLine()).thenReturn("-5");

		viewExpiryStockCommand.execute();

		String expectedOutput = NL + "--- View Close to Expiry Stocks on Shelf ---" + NL
				+ "Enter expiry threshold in days (e.g., 30 for stocks expiring in next 30 days): "
				+ "Expiry threshold must be a non-negative number." + NL;
		assertEquals(expectedOutput, outContent.toString());
		verifyNoInteractions(inventoryManager);
	}

	@Test
	@DisplayName("Should display message when products exist but none meet expiring + positive shelf quantity criteria (no filter)")
	void testExecute_noDisplayableExpiringBatches_noFilter() {
		int daysThreshold = 30;
		when(scanner.nextLine()).thenReturn(String.valueOf(daysThreshold)).thenReturn("");

		String product1Code = "PROD001";
		String product2Code = "PROD002";
		List<String> productCodes = Arrays.asList(product1Code, product2Code);

		when(inventoryManager.getAllProductCodes()).thenReturn(productCodes);

		StockBatch batch1_p1 = new StockBatch(1, product1Code, LocalDate.now().minusMonths(1),
				LocalDate.now().plusDays(10), 10);
		when(inventoryManager.getExpiringBatchesForProduct(product1Code, daysThreshold))
				.thenReturn(Collections.singletonList(batch1_p1));
		when(inventoryManager.getQuantityOnShelf(product1Code)).thenReturn(0);

		when(inventoryManager.getExpiringBatchesForProduct(product2Code, daysThreshold))
				.thenReturn(Collections.emptyList());
		when(inventoryManager.getQuantityOnShelf(product2Code)).thenReturn(5);

		viewExpiryStockCommand.execute();

		String expectedOutput = NL + "--- View Close to Expiry Stocks on Shelf ---" + NL
				+ "Enter expiry threshold in days (e.g., 30 for stocks expiring in next 30 days): "
				+ "Enter product code to filter (or leave blank to view all products): "
				+ String.format(
						"No products found with batches expiring within %d days or products have zero shelf quantity.%n",
						daysThreshold)
				+ NOTE_MESSAGE + NL;
		assertEquals(expectedOutput, outContent.toString());
		verify(inventoryManager, times(1)).getAllProductCodes();
		verify(inventoryManager, times(1)).getExpiringBatchesForProduct(product1Code, daysThreshold);
		verify(inventoryManager, times(1)).getQuantityOnShelf(product1Code);
		verify(inventoryManager, times(1)).getExpiringBatchesForProduct(product2Code, daysThreshold);
		verify(inventoryManager, times(1)).getQuantityOnShelf(product2Code);
	}

	@Test
	@DisplayName("Should display message when no product codes are found in the inventory at all (no filter)")
	void testExecute_noProductCodesFoundInInventoryManager_noFilter() {
		int daysThreshold = 30;
		when(scanner.nextLine()).thenReturn(String.valueOf(daysThreshold)).thenReturn("");

		when(inventoryManager.getAllProductCodes()).thenReturn(Collections.emptyList());

		viewExpiryStockCommand.execute();

		String expectedOutput = NL + "--- View Close to Expiry Stocks on Shelf ---" + NL
				+ "Enter expiry threshold in days (e.g., 30 for stocks expiring in next 30 days): "
				+ "Enter product code to filter (or leave blank to view all products): "
				+ String.format(
						"No products found with batches expiring within %d days or products have zero shelf quantity.%n",
						daysThreshold)
				+ NOTE_MESSAGE + NL;
		assertEquals(expectedOutput, outContent.toString());
		verify(inventoryManager, times(1)).getAllProductCodes();
		verify(inventoryManager, never()).getExpiringBatchesForProduct(anyString(), anyInt());
		verify(inventoryManager, never()).getQuantityOnShelf(anyString());
	}

	@Test
	@DisplayName("Should display message when filtered product has no expiring batches AND zero shelf quantity")
	void testExecute_noExpiringBatchesForFilteredProduct_zeroShelfQty() {
		int daysThreshold = 30;
		String productCode = "PROD001";
		when(scanner.nextLine()).thenReturn(String.valueOf(daysThreshold)).thenReturn(productCode);

		when(inventoryManager.getExpiringBatchesForProduct(productCode, daysThreshold))
				.thenReturn(Collections.emptyList());
		when(inventoryManager.getQuantityOnShelf(productCode)).thenReturn(0);

		viewExpiryStockCommand.execute();

		String expectedOutput = NL + "--- View Close to Expiry Stocks on Shelf ---" + NL
				+ "Enter expiry threshold in days (e.g., 30 for stocks expiring in next 30 days): "
				+ "Enter product code to filter (or leave blank to view all products): "
				+ String.format(
						"No products found with batches expiring within %d days for product code '%s' or product has zero shelf quantity.%n",
						daysThreshold, productCode)
				+ NOTE_MESSAGE + NL;
		assertEquals(expectedOutput, outContent.toString());
		verify(inventoryManager, times(1)).getExpiringBatchesForProduct(productCode, daysThreshold);
		verify(inventoryManager, times(1)).getQuantityOnShelf(productCode);
		verify(inventoryManager, never()).getAllProductCodes();
	}

	@Test
	@DisplayName("Should display filtered product with positive shelf quantity but no expiring batches (shows N/A)")
	void testExecute_filteredProduct_positiveShelfQty_noExpiringBatches() {
		int daysThreshold = 30;
		String productCode = "PROD_NO_EXP";
		when(scanner.nextLine()).thenReturn(String.valueOf(daysThreshold)).thenReturn(productCode);

		when(inventoryManager.getExpiringBatchesForProduct(productCode, daysThreshold))
				.thenReturn(Collections.emptyList());
		when(inventoryManager.getQuantityOnShelf(productCode)).thenReturn(10);

		viewExpiryStockCommand.execute();

		StringBuilder expectedOutput = new StringBuilder();
		expectedOutput.append(NL).append("--- View Close to Expiry Stocks on Shelf ---").append(NL);
		expectedOutput.append("Enter expiry threshold in days (e.g., 30 for stocks expiring in next 30 days): ");
		expectedOutput.append("Enter product code to filter (or leave blank to view all products): ");
		expectedOutput.append(getExpectedTableHeader(daysThreshold));
		expectedOutput.append(
				String.format("%-15s %-15d %-15s %-15s %-15s %-15s%n", productCode, 10, "N/A", "N/A", "N/A", "N/A"));
		expectedOutput.append(TABLE_SEPARATOR).append(NL);
		expectedOutput.append(NOTE_MESSAGE).append(NL);

		assertEquals(expectedOutput.toString(), outContent.toString());
		verify(inventoryManager, never()).getAllProductCodes();
		verify(inventoryManager, times(1)).getExpiringBatchesForProduct(productCode, daysThreshold);
		verify(inventoryManager, times(1)).getQuantityOnShelf(productCode);
	}

	@Test
	@DisplayName("Should display a single expiring batch for a product with positive shelf quantity (no filter)")
	void testExecute_singleExpiringBatch_positiveShelfQty_noFilter() {
		int daysThreshold = 60;
		String productCode = "PROD001";
		LocalDate expiryDate = LocalDate.now().plusDays(45);
		LocalDate purchaseDate = LocalDate.now().minusMonths(2);
		when(scanner.nextLine()).thenReturn(String.valueOf(daysThreshold)).thenReturn("");

		StockBatch batch = new StockBatch(1, productCode, purchaseDate, expiryDate, 25);
		List<StockBatch> expiringBatches = Collections.singletonList(batch);

		when(inventoryManager.getAllProductCodes()).thenReturn(Collections.singletonList(productCode));
		when(inventoryManager.getExpiringBatchesForProduct(productCode, daysThreshold)).thenReturn(expiringBatches);
		when(inventoryManager.getQuantityOnShelf(productCode)).thenReturn(100);

		viewExpiryStockCommand.execute();

		StringBuilder expectedOutput = new StringBuilder();
		expectedOutput.append(NL).append("--- View Close to Expiry Stocks on Shelf ---").append(NL);
		expectedOutput.append("Enter expiry threshold in days (e.g., 30 for stocks expiring in next 30 days): ");
		expectedOutput.append("Enter product code to filter (or leave blank to view all products): ");
		expectedOutput.append(getExpectedTableHeader(daysThreshold));
		expectedOutput.append(String.format("%-15s %-15d %-15d %-15s %-15s %-15d%n", productCode, 100, batch.getId(),
				batch.getExpiryDate().toString(), batch.getPurchaseDate().toString(), batch.getQuantityRemaining()));
		expectedOutput.append(TABLE_SEPARATOR).append(NL);
		expectedOutput.append(NOTE_MESSAGE).append(NL);

		assertEquals(expectedOutput.toString(), outContent.toString());
		verify(inventoryManager, times(1)).getAllProductCodes();
		verify(inventoryManager, times(1)).getExpiringBatchesForProduct(productCode, daysThreshold);
		verify(inventoryManager, times(1)).getQuantityOnShelf(productCode);
	}

	@Test
	@DisplayName("Should display a single expiring batch for a product with positive shelf quantity (with filter)")
	void testExecute_singleExpiringBatch_positiveShelfQty_withFilter() {
		int daysThreshold = 60;
		String productCode = "PROD001";
		LocalDate expiryDate = LocalDate.now().plusDays(45);
		LocalDate purchaseDate = LocalDate.now().minusMonths(2);
		when(scanner.nextLine()).thenReturn(String.valueOf(daysThreshold)).thenReturn(productCode);

		when(inventoryManager.getExpiringBatchesForProduct(productCode, daysThreshold))
				.thenReturn(Collections.singletonList(new StockBatch(1, productCode, purchaseDate, expiryDate, 25)));
		when(inventoryManager.getQuantityOnShelf(productCode)).thenReturn(100);

		viewExpiryStockCommand.execute();

		StringBuilder expectedOutput = new StringBuilder();
		expectedOutput.append(NL).append("--- View Close to Expiry Stocks on Shelf ---").append(NL);
		expectedOutput.append("Enter expiry threshold in days (e.g., 30 for stocks expiring in next 30 days): ");
		expectedOutput.append("Enter product code to filter (or leave blank to view all products): ");
		expectedOutput.append(getExpectedTableHeader(daysThreshold));
		expectedOutput.append(String.format("%-15s %-15d %-15d %-15s %-15s %-15d%n", productCode, 100, 1,
				expiryDate.toString(), purchaseDate.toString(), 25));
		expectedOutput.append(TABLE_SEPARATOR).append(NL);
		expectedOutput.append(NOTE_MESSAGE).append(NL);

		assertEquals(expectedOutput.toString(), outContent.toString());
		verify(inventoryManager, never()).getAllProductCodes();
		verify(inventoryManager, times(1)).getExpiringBatchesForProduct(productCode, daysThreshold);
		verify(inventoryManager, times(1)).getQuantityOnShelf(productCode);
	}

	@Test
	@DisplayName("Should display multiple expiring batches for one product with positive shelf quantity (no filter)")
	void testExecute_multipleExpiringBatchesForOneProduct_positiveShelfQty_noFilter() {
		int daysThreshold = 90;
		String productCode = "PROD002";
		LocalDate today = LocalDate.now();
		when(scanner.nextLine()).thenReturn(String.valueOf(daysThreshold)).thenReturn("");

		StockBatch batch1 = new StockBatch(10, productCode, today.minusMonths(3), today.plusDays(50), 50);
		StockBatch batch2 = new StockBatch(11, productCode, today.minusMonths(2), today.plusDays(80), 30);
		List<StockBatch> expiringBatches = Arrays.asList(batch1, batch2);

		when(inventoryManager.getAllProductCodes()).thenReturn(Collections.singletonList(productCode));
		when(inventoryManager.getExpiringBatchesForProduct(productCode, daysThreshold)).thenReturn(expiringBatches);
		when(inventoryManager.getQuantityOnShelf(productCode)).thenReturn(80);

		viewExpiryStockCommand.execute();

		StringBuilder expectedOutput = new StringBuilder();
		expectedOutput.append(NL).append("--- View Close to Expiry Stocks on Shelf ---").append(NL);
		expectedOutput.append("Enter expiry threshold in days (e.g., 30 for stocks expiring in next 30 days): ");
		expectedOutput.append("Enter product code to filter (or leave blank to view all products): ");
		expectedOutput.append(getExpectedTableHeader(daysThreshold));

		expectedOutput.append(String.format("%-15s %-15d %-15d %-15s %-15s %-15d%n", productCode, 80, batch1.getId(),
				batch1.getExpiryDate().toString(), batch1.getPurchaseDate().toString(), batch1.getQuantityRemaining()));
		expectedOutput.append(String.format("%-15s %-15s %-15d %-15s %-15s %-15d%n", "", "", batch2.getId(),
				batch2.getExpiryDate().toString(), batch2.getPurchaseDate().toString(), batch2.getQuantityRemaining()));
		expectedOutput.append(TABLE_SEPARATOR).append(NL);
		expectedOutput.append(NOTE_MESSAGE).append(NL);

		assertEquals(expectedOutput.toString(), outContent.toString());
		verify(inventoryManager, times(1)).getAllProductCodes();
		verify(inventoryManager, times(1)).getExpiringBatchesForProduct(productCode, daysThreshold);
		verify(inventoryManager, times(1)).getQuantityOnShelf(productCode);
	}

	@Test
	@DisplayName("Should display multiple products with expiring batches and positive shelf quantities (no filter)")
	void testExecute_multipleProducts_expiringBatches_positiveShelfQty_noFilter() {
		int daysThreshold = 120;
		String prodA = "PRODAAA";
		String prodB = "PRODBBB";
		String prodC = "PRODCCC";
		String prodD = "PRODDDD";
		LocalDate today = LocalDate.now();

		when(scanner.nextLine()).thenReturn(String.valueOf(daysThreshold)).thenReturn("");

		List<String> allProductCodes = Arrays.asList(prodA, prodB, prodC, prodD);
		when(inventoryManager.getAllProductCodes()).thenReturn(allProductCodes);

		StockBatch batchA1 = new StockBatch(1, prodA, today.minusMonths(4), today.plusDays(60), 10);
		StockBatch batchA2 = new StockBatch(2, prodA, today.minusMonths(3), today.plusDays(90), 15);
		when(inventoryManager.getExpiringBatchesForProduct(prodA, daysThreshold))
				.thenReturn(Arrays.asList(batchA1, batchA2));
		when(inventoryManager.getQuantityOnShelf(prodA)).thenReturn(25);

		StockBatch batchB1 = new StockBatch(3, prodB, today.minusMonths(6), today.plusDays(30), 5);
		when(inventoryManager.getExpiringBatchesForProduct(prodB, daysThreshold))
				.thenReturn(Collections.singletonList(batchB1));
		when(inventoryManager.getQuantityOnShelf(prodB)).thenReturn(5);

		StockBatch batchC1 = new StockBatch(4, prodC, today.minusMonths(1), today.plusDays(10), 30);
		when(inventoryManager.getExpiringBatchesForProduct(prodC, daysThreshold))
				.thenReturn(Collections.singletonList(batchC1));
		when(inventoryManager.getQuantityOnShelf(prodC)).thenReturn(0);

		when(inventoryManager.getExpiringBatchesForProduct(prodD, daysThreshold)).thenReturn(Collections.emptyList());
		when(inventoryManager.getQuantityOnShelf(prodD)).thenReturn(10);

		viewExpiryStockCommand.execute();

		StringBuilder expectedOutput = new StringBuilder();
		expectedOutput.append(NL).append("--- View Close to Expiry Stocks on Shelf ---").append(NL);
		expectedOutput.append("Enter expiry threshold in days (e.g., 30 for stocks expiring in next 30 days): ");
		expectedOutput.append("Enter product code to filter (or leave blank to view all products): ");
		expectedOutput.append(getExpectedTableHeader(daysThreshold));

		expectedOutput.append(String.format("%-15s %-15d %-15d %-15s %-15s %-15d%n", prodA, 25, batchA1.getId(),
				batchA1.getExpiryDate().toString(), batchA1.getPurchaseDate().toString(),
				batchA1.getQuantityRemaining()));
		expectedOutput.append(String.format("%-15s %-15s %-15d %-15s %-15s %-15d%n", "", "", batchA2.getId(),
				batchA2.getExpiryDate().toString(), batchA2.getPurchaseDate().toString(),
				batchA2.getQuantityRemaining()));
		expectedOutput.append(String.format("%-15s %-15d %-15d %-15s %-15s %-15d%n", prodB, 5, batchB1.getId(),
				batchB1.getExpiryDate().toString(), batchB1.getPurchaseDate().toString(),
				batchB1.getQuantityRemaining()));

		expectedOutput.append(TABLE_SEPARATOR).append(NL);
		expectedOutput.append(NOTE_MESSAGE).append(NL);

		assertEquals(expectedOutput.toString(), outContent.toString());

		verify(inventoryManager, times(1)).getAllProductCodes();
		verify(inventoryManager, times(1)).getExpiringBatchesForProduct(prodA, daysThreshold);
		verify(inventoryManager, times(1)).getQuantityOnShelf(prodA);
		verify(inventoryManager, times(1)).getExpiringBatchesForProduct(prodB, daysThreshold);
		verify(inventoryManager, times(1)).getQuantityOnShelf(prodB);
		verify(inventoryManager, times(1)).getExpiringBatchesForProduct(prodC, daysThreshold);
		verify(inventoryManager, times(1)).getQuantityOnShelf(prodC);
		verify(inventoryManager, times(1)).getExpiringBatchesForProduct(prodD, daysThreshold);
		verify(inventoryManager, times(1)).getQuantityOnShelf(prodD);
	}
}