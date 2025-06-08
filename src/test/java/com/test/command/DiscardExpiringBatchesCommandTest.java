package com.test.command;

import com.syos.command.DiscardExpiringBatchesCommand;
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
class DiscardExpiringBatchesCommandTest {

	@Mock
	private InventoryManager inventoryManager;
	@Mock
	private Scanner scanner;

	private DiscardExpiringBatchesCommand discardExpiringBatchesCommand;

	private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
	private final PrintStream originalOut = System.out;

	@BeforeEach
	void setUp() {
		System.setOut(new PrintStream(outContent));
		discardExpiringBatchesCommand = new DiscardExpiringBatchesCommand(inventoryManager, scanner);
	}

	@AfterEach
	void restoreStreams() {
		System.setOut(originalOut);
	}

	@Test
	@DisplayName("Should successfully discard a partial quantity from an expiring batch")
	void shouldSuccessfullyDiscardPartialQuantity() {
		// Arrange
		int daysThreshold = 7;
		int batchId = 101;
		int quantityToDiscard = 5;
		// Corrected StockBatch constructor usage: id, productCode, purchaseDate,
		// expiryDate, quantityRemaining
		StockBatch expiringBatch = new StockBatch(batchId, "PROD001", LocalDate.now().minusMonths(1),
				LocalDate.now().plusDays(5), 45);
		List<StockBatch> expiringBatches = Arrays.asList(expiringBatch);

		when(scanner.nextLine()).thenReturn(String.valueOf(daysThreshold)) // For daysThreshold input
				.thenReturn(String.valueOf(batchId)) // For batchId input
				.thenReturn(String.valueOf(quantityToDiscard)); // For quantityToDiscard input

		when(inventoryManager.getAllExpiringBatches(daysThreshold)).thenReturn(expiringBatches);
		doNothing().when(inventoryManager).discardBatchQuantity(batchId, quantityToDiscard);

		// Act
		discardExpiringBatchesCommand.execute();

		// Assert
		verify(scanner, times(3)).nextLine();
		verify(inventoryManager, times(1)).getAllExpiringBatches(daysThreshold);
		verify(inventoryManager, times(1)).discardBatchQuantity(batchId, quantityToDiscard);

		String output = outContent.toString();
		assertTrue(output.contains("--- Discard Expiring Stock Batches (Back-Store) ---"));
		assertTrue(output.contains("Enter expiry threshold in days"));
		assertTrue(output.contains(String.format("--- Batches Expiring in Next %d Days ---", daysThreshold)));
		assertTrue(output.contains(String.format("Batch ID: %d, Product: %s, Current Remaining Quantity: %d", batchId,
				expiringBatch.getProductCode(), expiringBatch.getQuantityRemaining())));
		assertTrue(output.contains("Enter quantity to discard from this batch"));
		assertTrue(output.contains(
				String.format("Successfully discarded %d units from Batch ID %d.", quantityToDiscard, batchId)));
	}

	@Test
	@DisplayName("Should successfully discard all remaining quantity from an expiring batch when 0 is entered")
	void shouldSuccessfullyDiscardAllQuantity() {
		// Arrange
		int daysThreshold = 7;
		int batchId = 102;
		int remainingQuantity = 30;
		// Corrected StockBatch constructor usage
		StockBatch expiringBatch = new StockBatch(batchId, "PROD002", LocalDate.now().minusWeeks(2),
				LocalDate.now().plusDays(2), remainingQuantity);
		List<StockBatch> expiringBatches = Arrays.asList(expiringBatch);

		when(scanner.nextLine()).thenReturn(String.valueOf(daysThreshold)).thenReturn(String.valueOf(batchId))
				.thenReturn("0"); // User enters 0 to discard all

		when(inventoryManager.getAllExpiringBatches(daysThreshold)).thenReturn(expiringBatches);
		doNothing().when(inventoryManager).discardBatchQuantity(batchId, remainingQuantity); // Should discard all 30

		// Act
		discardExpiringBatchesCommand.execute();

		// Assert
		verify(scanner, times(3)).nextLine();
		verify(inventoryManager, times(1)).getAllExpiringBatches(daysThreshold);
		verify(inventoryManager, times(1)).discardBatchQuantity(batchId, remainingQuantity);

		String output = outContent.toString();
		assertTrue(output.contains(
				String.format("Successfully discarded %d units from Batch ID %d.", remainingQuantity, batchId)));
	}

	@Test
	@DisplayName("Should handle invalid expiry threshold input (non-numeric)")
	void shouldHandleInvalidExpiryThresholdNonNumeric() {
		// Arrange
		String invalidThreshold = "abc";
		when(scanner.nextLine()).thenReturn(invalidThreshold);

		// Act
		discardExpiringBatchesCommand.execute();

		// Assert
		verify(scanner, times(1)).nextLine();
		verifyNoInteractions(inventoryManager); // No calls to inventoryManager
		String output = outContent.toString();
		assertTrue(output.contains("Invalid input. Please enter a number for days threshold."));
	}

	@Test
	@DisplayName("Should handle invalid expiry threshold input (negative)")
	void shouldHandleInvalidExpiryThresholdNegative() {
		// Arrange
		int negativeThreshold = -5;
		when(scanner.nextLine()).thenReturn(String.valueOf(negativeThreshold));

		// Act
		discardExpiringBatchesCommand.execute();

		// Assert
		verify(scanner, times(1)).nextLine();
		verifyNoInteractions(inventoryManager);
		String output = outContent.toString();
		assertTrue(output.contains("Expiry threshold must be a non-negative number."));
	}

	@Test
	@DisplayName("Should inform if no expiring batches are found")
	void shouldInformNoExpiringBatches() {
		// Arrange
		int daysThreshold = 10;
		when(scanner.nextLine()).thenReturn(String.valueOf(daysThreshold));
		when(inventoryManager.getAllExpiringBatches(daysThreshold)).thenReturn(Collections.emptyList());

		// Act
		discardExpiringBatchesCommand.execute();

		// Assert
		verify(scanner, times(1)).nextLine();
		verify(inventoryManager, times(1)).getAllExpiringBatches(daysThreshold);
		String output = outContent.toString();
		assertTrue(output.contains(String.format(
				"No stock batches found expiring within %d days in the back-store to discard.", daysThreshold)));
	}

	@Test
	@DisplayName("Should handle invalid batch ID input (non-numeric)")
	void shouldHandleInvalidBatchIdNonNumeric() {
		// Arrange
		int daysThreshold = 7;
		String invalidBatchId = "xyz";
		// Corrected StockBatch constructor usage
		StockBatch expiringBatch = new StockBatch(101, "PROD001", LocalDate.now().minusMonths(1),
				LocalDate.now().plusDays(5), 45);
		List<StockBatch> expiringBatches = Arrays.asList(expiringBatch);

		when(scanner.nextLine()).thenReturn(String.valueOf(daysThreshold)).thenReturn(invalidBatchId);

		when(inventoryManager.getAllExpiringBatches(daysThreshold)).thenReturn(expiringBatches);

		// Act
		discardExpiringBatchesCommand.execute();

		// Assert
		verify(scanner, times(2)).nextLine();
		verify(inventoryManager, times(1)).getAllExpiringBatches(daysThreshold);
		verifyNoMoreInteractions(inventoryManager); // No discard call
		String output = outContent.toString();
		assertTrue(output.contains("Invalid input. Please enter a number for Batch ID."));
	}

	@Test
	@DisplayName("Should cancel discard operation if batch ID is 0")
	void shouldCancelDiscardOperation() {
		// Arrange
		int daysThreshold = 7;
		// Corrected StockBatch constructor usage
		StockBatch expiringBatch = new StockBatch(101, "PROD001", LocalDate.now().minusMonths(1),
				LocalDate.now().plusDays(5), 45);
		List<StockBatch> expiringBatches = Arrays.asList(expiringBatch);

		when(scanner.nextLine()).thenReturn(String.valueOf(daysThreshold)).thenReturn("0"); // User enters 0 to cancel

		when(inventoryManager.getAllExpiringBatches(daysThreshold)).thenReturn(expiringBatches);

		// Act
		discardExpiringBatchesCommand.execute();

		// Assert
		verify(scanner, times(2)).nextLine();
		verify(inventoryManager, times(1)).getAllExpiringBatches(daysThreshold);
		verifyNoMoreInteractions(inventoryManager); // No discard call
		String output = outContent.toString();
		assertTrue(output.contains("Discard operation cancelled."));
	}

	@Test
	@DisplayName("Should handle batch ID not found in the listed expiring batches")
	void shouldHandleBatchIdNotFoundInList() {
		// Arrange
		int daysThreshold = 7;
		int nonExistentBatchId = 999;
		// Corrected StockBatch constructor usage
		StockBatch expiringBatch = new StockBatch(101, "PROD001", LocalDate.now().minusMonths(1),
				LocalDate.now().plusDays(5), 45);
		List<StockBatch> expiringBatches = Arrays.asList(expiringBatch);

		when(scanner.nextLine()).thenReturn(String.valueOf(daysThreshold))
				.thenReturn(String.valueOf(nonExistentBatchId));

		when(inventoryManager.getAllExpiringBatches(daysThreshold)).thenReturn(expiringBatches);

		// Act
		discardExpiringBatchesCommand.execute();

		// Assert
		verify(scanner, times(2)).nextLine();
		verify(inventoryManager, times(1)).getAllExpiringBatches(daysThreshold);
		verifyNoMoreInteractions(inventoryManager);
		String output = outContent.toString();
		assertTrue(
				output.contains("Batch ID not found or not in the expiring list. Please select an ID from the list."));
	}

	@Test
	@DisplayName("Should handle invalid quantity to discard (non-numeric)")
	void shouldHandleInvalidQuantityNonNumeric() {
		// Arrange
		int daysThreshold = 7;
		int batchId = 101;
		String invalidQuantity = "abc";
		// Corrected StockBatch constructor usage
		StockBatch expiringBatch = new StockBatch(batchId, "PROD001", LocalDate.now().minusMonths(1),
				LocalDate.now().plusDays(5), 45);
		List<StockBatch> expiringBatches = Arrays.asList(expiringBatch);

		when(scanner.nextLine()).thenReturn(String.valueOf(daysThreshold)).thenReturn(String.valueOf(batchId))
				.thenReturn(invalidQuantity);

		when(inventoryManager.getAllExpiringBatches(daysThreshold)).thenReturn(expiringBatches);

		// Act
		discardExpiringBatchesCommand.execute();

		// Assert
		verify(scanner, times(3)).nextLine();
		verify(inventoryManager, times(1)).getAllExpiringBatches(daysThreshold);
		verifyNoMoreInteractions(inventoryManager);
		String output = outContent.toString();
		assertTrue(output.contains("Invalid input. Please enter a number for quantity."));
	}

	@Test
	@DisplayName("Should handle invalid quantity to discard (negative)")
	void shouldHandleInvalidQuantityNegative() {
		// Arrange
		int daysThreshold = 7;
		int batchId = 101;
		int negativeQuantity = -5;
		// Corrected StockBatch constructor usage
		StockBatch expiringBatch = new StockBatch(batchId, "PROD001", LocalDate.now().minusMonths(1),
				LocalDate.now().plusDays(5), 45);
		List<StockBatch> expiringBatches = Arrays.asList(expiringBatch);

		when(scanner.nextLine()).thenReturn(String.valueOf(daysThreshold)).thenReturn(String.valueOf(batchId))
				.thenReturn(String.valueOf(negativeQuantity));

		when(inventoryManager.getAllExpiringBatches(daysThreshold)).thenReturn(expiringBatches);

		// Act
		discardExpiringBatchesCommand.execute();

		// Assert
		verify(scanner, times(3)).nextLine();
		verify(inventoryManager, times(1)).getAllExpiringBatches(daysThreshold);
		verifyNoMoreInteractions(inventoryManager);
		String output = outContent.toString();
		assertTrue(output.contains("Quantity to discard must be non-negative."));
	}

	@Test
	@DisplayName("Should handle quantity to discard exceeding remaining quantity")
	void shouldHandleQuantityExceedingRemaining() {
		// Arrange
		int daysThreshold = 7;
		int batchId = 101;
		int quantityToDiscard = 50; // More than remaining 45
		// Corrected StockBatch constructor usage
		StockBatch expiringBatch = new StockBatch(batchId, "PROD001", LocalDate.now().minusMonths(1),
				LocalDate.now().plusDays(5), 45);
		List<StockBatch> expiringBatches = Arrays.asList(expiringBatch);

		when(scanner.nextLine()).thenReturn(String.valueOf(daysThreshold)).thenReturn(String.valueOf(batchId))
				.thenReturn(String.valueOf(quantityToDiscard));

		when(inventoryManager.getAllExpiringBatches(daysThreshold)).thenReturn(expiringBatches);

		// Act
		discardExpiringBatchesCommand.execute();

		// Assert
		verify(scanner, times(3)).nextLine();
		verify(inventoryManager, times(1)).getAllExpiringBatches(daysThreshold);
		verifyNoMoreInteractions(inventoryManager);
		String output = outContent.toString();
		assertTrue(output.contains(String.format("Cannot discard %d units. Only %d remaining in batch %d.",
				quantityToDiscard, expiringBatch.getQuantityRemaining(), batchId)));
	}

	@Test
	@DisplayName("Should handle IllegalArgumentException from inventoryManager.discardBatchQuantity")
	void shouldHandleIllegalArgumentException() {
		// Arrange
		int daysThreshold = 7;
		int batchId = 101;
		int quantityToDiscard = 5;
		String errorMessage = "Batch or quantity invalid for discard.";
		// Corrected StockBatch constructor usage
		StockBatch expiringBatch = new StockBatch(batchId, "PROD001", LocalDate.now().minusMonths(1),
				LocalDate.now().plusDays(5), 45);
		List<StockBatch> expiringBatches = Arrays.asList(expiringBatch);

		when(scanner.nextLine()).thenReturn(String.valueOf(daysThreshold)).thenReturn(String.valueOf(batchId))
				.thenReturn(String.valueOf(quantityToDiscard));

		when(inventoryManager.getAllExpiringBatches(daysThreshold)).thenReturn(expiringBatches);
		doThrow(new IllegalArgumentException(errorMessage)).when(inventoryManager).discardBatchQuantity(batchId,
				quantityToDiscard);

		// Act
		discardExpiringBatchesCommand.execute();

		// Assert
		verify(scanner, times(3)).nextLine();
		verify(inventoryManager, times(1)).getAllExpiringBatches(daysThreshold);
		verify(inventoryManager, times(1)).discardBatchQuantity(batchId, quantityToDiscard);
		String output = outContent.toString();
		assertTrue(output.contains("Error discarding stock: " + errorMessage));
	}

	@Test
	@DisplayName("Should handle generic Exception from inventoryManager.discardBatchQuantity")
	void shouldHandleGenericException() {
		// Arrange
		int daysThreshold = 7;
		int batchId = 101;
		int quantityToDiscard = 5;
		String errorMessage = "Database connection lost!";
		// Corrected StockBatch constructor usage
		StockBatch expiringBatch = new StockBatch(batchId, "PROD001", LocalDate.now().minusMonths(1),
				LocalDate.now().plusDays(5), 45);
		List<StockBatch> expiringBatches = Arrays.asList(expiringBatch);

		when(scanner.nextLine()).thenReturn(String.valueOf(daysThreshold)).thenReturn(String.valueOf(batchId))
				.thenReturn(String.valueOf(quantityToDiscard));

		when(inventoryManager.getAllExpiringBatches(daysThreshold)).thenReturn(expiringBatches);
		doThrow(new RuntimeException(errorMessage)).when(inventoryManager).discardBatchQuantity(batchId,
				quantityToDiscard);

		// Act
		discardExpiringBatchesCommand.execute();

		// Assert
		verify(scanner, times(3)).nextLine();
		verify(inventoryManager, times(1)).getAllExpiringBatches(daysThreshold);
		verify(inventoryManager, times(1)).discardBatchQuantity(batchId, quantityToDiscard);
		String output = outContent.toString();
		assertTrue(output.contains("An unexpected error occurred: " + errorMessage));
	}

	// --- NEW TESTS FOR ADDITIONAL COVERAGE ---

	@Test
	@DisplayName("Should display multiple expiring batches in correct table format and proceed with discard")
	void shouldDisplayMultipleExpiringBatchesCorrectly() {
		// Arrange
		int daysThreshold = 14;
		StockBatch batch1 = new StockBatch(201, "PROD005", LocalDate.now().minusMonths(3), LocalDate.now().plusDays(10),
				100);
		StockBatch batch2 = new StockBatch(202, "PROD006", LocalDate.now().minusMonths(1), LocalDate.now().plusDays(5),
				75);
		List<StockBatch> expiringBatches = Arrays.asList(batch1, batch2);
		int quantityToDiscard = 10;

		when(scanner.nextLine()).thenReturn(String.valueOf(daysThreshold)) // Threshold input
				.thenReturn(String.valueOf(batch1.getId())) // Select batch1
				.thenReturn(String.valueOf(quantityToDiscard)); // Discard quantity

		when(inventoryManager.getAllExpiringBatches(daysThreshold)).thenReturn(expiringBatches);
		doNothing().when(inventoryManager).discardBatchQuantity(batch1.getId(), quantityToDiscard); // Mock discard for
																									// batch1

		// Act
		discardExpiringBatchesCommand.execute();

		// Assert
		String output = outContent.toString();
		assertTrue(output.contains(String.format("--- Batches Expiring in Next %d Days ---", daysThreshold)));
		assertTrue(output.contains(String.format("%-10s %-15s %-15s %-15s %-15s", "Batch ID", "Product Code",
				"Expiry Date", "Purch Date", "Remaining Qty")));
		// Verify both batches are listed
		assertTrue(output.contains(String.format("%-10d %-15s %-15s %-15s %-15d", batch1.getId(),
				batch1.getProductCode(), batch1.getExpiryDate().toString(), batch1.getPurchaseDate().toString(),
				batch1.getQuantityRemaining())));
		assertTrue(output.contains(String.format("%-10d %-15s %-15s %-15s %-15d", batch2.getId(),
				batch2.getProductCode(), batch2.getExpiryDate().toString(), batch2.getPurchaseDate().toString(),
				batch2.getQuantityRemaining())));
		// Verify the successful discard message for the selected batch
		assertTrue(output.contains(
				String.format("Successfully discarded %d units from Batch ID %d.", quantityToDiscard, batch1.getId())));
		verify(inventoryManager, times(1)).discardBatchQuantity(batch1.getId(), quantityToDiscard);
	}

	@Test
	@DisplayName("Should display batch with zero remaining quantity correctly and prevent positive discard")
	void shouldHandleBatchWithZeroRemainingQuantity() {
		// Arrange
		int daysThreshold = 7;
		int batchId = 301;
		int initialQuantity = 0; // Zero remaining
		StockBatch expiringBatch = new StockBatch(batchId, "PROD007", LocalDate.now().minusMonths(6),
				LocalDate.now().plusDays(1), initialQuantity);
		List<StockBatch> expiringBatches = Collections.singletonList(expiringBatch);

		when(scanner.nextLine()).thenReturn(String.valueOf(daysThreshold)).thenReturn(String.valueOf(batchId))
				.thenReturn("1"); // User tries to discard 1 unit (should be prevented)

		when(inventoryManager.getAllExpiringBatches(daysThreshold)).thenReturn(expiringBatches);

		// Act
		discardExpiringBatchesCommand.execute();

		// Assert
		String output = outContent.toString();
		assertTrue(output.contains(String.format("%-10d %-15s %-15s %-15s %-15d", expiringBatch.getId(),
				expiringBatch.getProductCode(), expiringBatch.getExpiryDate().toString(),
				expiringBatch.getPurchaseDate().toString(), expiringBatch.getQuantityRemaining())));
		assertTrue(output.contains(String.format("Current Remaining Quantity: %d", initialQuantity)));
		assertTrue(output.contains(
				String.format("Cannot discard %d units. Only %d remaining in batch %d.", 1, initialQuantity, batchId)));

		verify(inventoryManager, never()).discardBatchQuantity(anyInt(), anyInt()); // Ensure no discard happens
	}

	@Test
	@DisplayName("Should allow discarding 0 quantity from batch with 0 remaining (no-op with success)")
	void shouldAllowZeroDiscardOnZeroRemaining() {
		// Tests if discarding 0 from an already depleted batch doesn't cause errors
		int daysThreshold = 5;
		int batchId = 400;
		int remainingQuantity = 0;
		int quantityToDiscard = 0;
		StockBatch expiringBatch = new StockBatch(batchId, "PROD008", LocalDate.now().minusDays(10),
				LocalDate.now().plusDays(1), remainingQuantity);
		List<StockBatch> batches = Collections.singletonList(expiringBatch);

		when(scanner.nextLine()).thenReturn(String.valueOf(daysThreshold)).thenReturn(String.valueOf(batchId))
				.thenReturn(String.valueOf(quantityToDiscard));

		when(inventoryManager.getAllExpiringBatches(daysThreshold)).thenReturn(batches);
		doNothing().when(inventoryManager).discardBatchQuantity(batchId, quantityToDiscard);

		discardExpiringBatchesCommand.execute();

		String output = outContent.toString();
		assertTrue(output.contains("Successfully discarded 0 units from Batch ID 400."));
		verify(inventoryManager).discardBatchQuantity(batchId, quantityToDiscard);
	}

	@Test
	@DisplayName("Should allow discarding all when quantity entered equals remaining")
	void shouldAllowDiscardEqualToRemaining() {
		// Tests discarding a quantity exactly equal to what's remaining in the batch
		int daysThreshold = 3;
		int batchId = 500;
		int remaining = 20;
		StockBatch expiringBatch = new StockBatch(batchId, "PROD009", LocalDate.now().minusDays(40),
				LocalDate.now().plusDays(2), remaining);
		List<StockBatch> batches = Collections.singletonList(expiringBatch);

		when(scanner.nextLine()).thenReturn(String.valueOf(daysThreshold)).thenReturn(String.valueOf(batchId))
				.thenReturn(String.valueOf(remaining));

		when(inventoryManager.getAllExpiringBatches(daysThreshold)).thenReturn(batches);
		doNothing().when(inventoryManager).discardBatchQuantity(batchId, remaining);

		discardExpiringBatchesCommand.execute();

		String output = outContent.toString();
		assertTrue(output.contains("Successfully discarded 20 units from Batch ID 500."));
		verify(inventoryManager).discardBatchQuantity(batchId, remaining);
	}

}