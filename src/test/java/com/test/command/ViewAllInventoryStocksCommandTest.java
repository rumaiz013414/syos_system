package com.test.command;

import com.syos.command.ViewAllInventoryStocksCommand;
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
class ViewAllInventoryStocksCommandTest {

	@Mock
	private InventoryManager inventoryManager;
	@Mock
	private Scanner scanner;

	private ViewAllInventoryStocksCommand viewAllInventoryStocksCommand;

	private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
	private final PrintStream originalOut = System.out;
	private final String NL = System.lineSeparator();

	@BeforeEach
	void setUp() {
		System.setOut(new PrintStream(outContent));
		viewAllInventoryStocksCommand = new ViewAllInventoryStocksCommand(inventoryManager, scanner);
	}

	@AfterEach
	void restoreStreams() {
		System.setOut(originalOut);
	}

	@Test
	@DisplayName("Should display message when no products with back-store stock are found")
	void shouldDisplayNoProductsMessageWhenInventoryIsEmpty() {
		when(inventoryManager.getAllProductCodes()).thenReturn(Collections.emptyList());

		viewAllInventoryStocksCommand.execute();

		verify(inventoryManager, times(1)).getAllProductCodes();
		verify(inventoryManager, never()).getBatchesForProduct(anyString());

		String expectedOutput = NL + "--- All Inventory Stock Batches ---" + NL
				+ "No products with back-store stock found." + NL;
		assertEquals(expectedOutput, outContent.toString());
	}

	@Test
	@DisplayName("Should display all products and their batches correctly")
	void shouldDisplayProductsAndBatchesCorrectly() {
		List<String> productCodes = Arrays.asList("PROD001", "PROD002");

		StockBatch batch1_prod1 = new StockBatch(1, "PROD001", LocalDate.of(2024, 1, 15), LocalDate.of(2025, 1, 15),
				100);
		StockBatch batch2_prod1 = new StockBatch(2, "PROD001", LocalDate.of(2024, 3, 10), LocalDate.of(2025, 3, 10),
				50);
		List<StockBatch> batches_prod1 = Arrays.asList(batch1_prod1, batch2_prod1);

		StockBatch batch1_prod2 = new StockBatch(3, "PROD002", LocalDate.of(2024, 6, 20), LocalDate.of(2025, 6, 20),
				25);
		List<StockBatch> batches_prod2 = Collections.singletonList(batch1_prod2);

		when(inventoryManager.getAllProductCodes()).thenReturn(productCodes);
		when(inventoryManager.getBatchesForProduct("PROD001")).thenReturn(batches_prod1);
		when(inventoryManager.getBatchesForProduct("PROD002")).thenReturn(batches_prod2);

		viewAllInventoryStocksCommand.execute();

		verify(inventoryManager, times(1)).getAllProductCodes();
		verify(inventoryManager, times(1)).getBatchesForProduct("PROD001");
		verify(inventoryManager, times(1)).getBatchesForProduct("PROD002");

		StringBuilder expectedOutput = new StringBuilder();
		expectedOutput.append(NL).append("--- All Inventory Stock Batches ---").append(NL);

		expectedOutput.append(NL).append("Product Code: PROD001").append(NL);
		expectedOutput.append(String.format("%-5s %-15s %-15s %-10s %-10s", "ID", "Purchase Date", "Expiry Date",
				"Quantity", "Remaining")).append(NL);
		expectedOutput.append("----- --------------- --------------- ---------- ----------").append(NL);
		expectedOutput.append(String.format("%-5d %-15s %-15s %-10d %-10d", batch1_prod1.getId(),
				batch1_prod1.getPurchaseDate().toString(), batch1_prod1.getExpiryDate().toString(),
				batch1_prod1.getQuantityRemaining(), batch1_prod1.getQuantityRemaining())).append(NL);
		expectedOutput.append(String.format("%-5d %-15s %-15s %-10d %-10d", batch2_prod1.getId(),
				batch2_prod1.getPurchaseDate().toString(), batch2_prod1.getExpiryDate().toString(),
				batch2_prod1.getQuantityRemaining(), batch2_prod1.getQuantityRemaining())).append(NL);

		expectedOutput.append(NL).append("Product Code: PROD002").append(NL);
		expectedOutput.append(String.format("%-5s %-15s %-15s %-10s %-10s", "ID", "Purchase Date", "Expiry Date",
				"Quantity", "Remaining")).append(NL);
		expectedOutput.append("----- --------------- --------------- ---------- ----------").append(NL);
		expectedOutput.append(String.format("%-5d %-15s %-15s %-10d %-10d", batch1_prod2.getId(),
				batch1_prod2.getPurchaseDate().toString(), batch1_prod2.getExpiryDate().toString(),
				batch1_prod2.getQuantityRemaining(), batch1_prod2.getQuantityRemaining())).append(NL);

		expectedOutput.append("------------------------------------").append(NL);

		assertEquals(expectedOutput.toString(), outContent.toString());
	}

	@Test
	@DisplayName("Should correctly handle products with no stock batches")
	void shouldHandleProductWithNoBatches() {
		List<String> productCodes = Arrays.asList("PROD001", "PROD002", "PROD003");

		StockBatch batch1_prod1 = new StockBatch(1, "PROD001", LocalDate.of(2024, 1, 15), LocalDate.of(2025, 1, 15),
				100);
		List<StockBatch> batches_prod1 = Collections.singletonList(batch1_prod1);

		List<StockBatch> batches_prod2_empty = Collections.emptyList();

		StockBatch batch1_prod3 = new StockBatch(4, "PROD003", LocalDate.of(2024, 10, 1), LocalDate.of(2025, 10, 1),
				75);
		List<StockBatch> batches_prod3 = Collections.singletonList(batch1_prod3);

		when(inventoryManager.getAllProductCodes()).thenReturn(productCodes);
		when(inventoryManager.getBatchesForProduct("PROD001")).thenReturn(batches_prod1);
		when(inventoryManager.getBatchesForProduct("PROD002")).thenReturn(batches_prod2_empty);
		when(inventoryManager.getBatchesForProduct("PROD003")).thenReturn(batches_prod3);

		viewAllInventoryStocksCommand.execute();

		verify(inventoryManager, times(1)).getAllProductCodes();
		verify(inventoryManager, times(1)).getBatchesForProduct("PROD001");
		verify(inventoryManager, times(1)).getBatchesForProduct("PROD002");
		verify(inventoryManager, times(1)).getBatchesForProduct("PROD003");

		StringBuilder expectedOutput = new StringBuilder();
		expectedOutput.append(NL).append("--- All Inventory Stock Batches ---").append(NL);

		expectedOutput.append(NL).append("Product Code: PROD001").append(NL);
		expectedOutput.append(String.format("%-5s %-15s %-15s %-10s %-10s", "ID", "Purchase Date", "Expiry Date",
				"Quantity", "Remaining")).append(NL);
		expectedOutput.append("----- --------------- --------------- ---------- ----------").append(NL);
		expectedOutput.append(String.format("%-5d %-15s %-15s %-10d %-10d", batch1_prod1.getId(),
				batch1_prod1.getPurchaseDate().toString(), batch1_prod1.getExpiryDate().toString(),
				batch1_prod1.getQuantityRemaining(), batch1_prod1.getQuantityRemaining())).append(NL);

		expectedOutput.append(NL).append("Product Code: PROD003").append(NL);
		expectedOutput.append(String.format("%-5s %-15s %-15s %-10s %-10s", "ID", "Purchase Date", "Expiry Date",
				"Quantity", "Remaining")).append(NL);
		expectedOutput.append("----- --------------- --------------- ---------- ----------").append(NL);
		expectedOutput.append(String.format("%-5d %-15s %-15s %-10d %-10d", batch1_prod3.getId(),
				batch1_prod3.getPurchaseDate().toString(), batch1_prod3.getExpiryDate().toString(),
				batch1_prod3.getQuantityRemaining(), batch1_prod3.getQuantityRemaining())).append(NL);

		expectedOutput.append("------------------------------------").append(NL);

		assertEquals(expectedOutput.toString(), outContent.toString());
	}
}