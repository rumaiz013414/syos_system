package com.test.command;

import com.syos.command.ReceiveStockCommand;
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
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReceiveStockCommandTest {

	@Mock
	private InventoryManager inventoryManager;
	@Mock
	private Scanner scanner;

	private ReceiveStockCommand receiveStockCommand;

	private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
	private final PrintStream originalOut = System.out;

	@BeforeEach
	void setUp() {
		System.setOut(new PrintStream(outContent));
		receiveStockCommand = new ReceiveStockCommand(inventoryManager, scanner);
	}

	@AfterEach
	void restoreStreams() {
		System.setOut(originalOut);
	}

	@Test
	@DisplayName("Should successfully receive new stock with valid inputs")
	void shouldSuccessfullyReceiveNewStock() {
		// Arrange
		String productCode = "PROD001";
		int quantity = 100;
		LocalDate purchaseDate = LocalDate.now().minusDays(5);
		LocalDate expiryDate = LocalDate.now().plusMonths(6);

		when(scanner.nextLine()).thenReturn(productCode).thenReturn(String.valueOf(quantity))
				.thenReturn(purchaseDate.toString()).thenReturn(expiryDate.toString());

		doNothing().when(inventoryManager).receiveStock(productCode, purchaseDate, expiryDate, quantity);

		// Act
		receiveStockCommand.execute();

		// Assert
		verify(scanner, times(4)).nextLine(); // Code, Qty, PD, ED
		verify(inventoryManager, times(1)).receiveStock(productCode, purchaseDate, expiryDate, quantity);

		String output = outContent.toString();
		assertTrue(output.contains("=== Receive New Stock ==="), "Output should contain the command header.");
		assertTrue(output.contains("Product code:"), "Output should contain product code prompt.");
		assertTrue(output.contains("Quantity:"), "Output should contain quantity prompt.");
		assertTrue(output.contains("Purchase date (YYYY-MM-DD):"), "Output should contain purchase date prompt.");
		assertTrue(output.contains("Expiry date (YYYY-MM-DD):"), "Output should contain expiry date prompt.");
		// Success message is handled by InventoryManager, not this command.
	}

	@Test
	@DisplayName("Should handle product code with leading/trailing spaces correctly")
	void shouldHandleProductCodeWithSpaces() {
		// Arrange
		String productCodeWithSpaces = "  PROD002  ";
		String expectedProductCode = "PROD002"; // After trimming
		int quantity = 50;
		LocalDate purchaseDate = LocalDate.now().minusDays(10);
		LocalDate expiryDate = LocalDate.now().plusMonths(3);

		when(scanner.nextLine()).thenReturn(productCodeWithSpaces).thenReturn(String.valueOf(quantity))
				.thenReturn(purchaseDate.toString()).thenReturn(expiryDate.toString());

		doNothing().when(inventoryManager).receiveStock(expectedProductCode, purchaseDate, expiryDate, quantity);

		// Act
		receiveStockCommand.execute();

		// Assert
		verify(scanner, times(4)).nextLine();
		verify(inventoryManager, times(1)).receiveStock(expectedProductCode, purchaseDate, expiryDate, quantity);
		assertTrue(outContent.toString().contains("Product code:")); // General prompt check
	}

	@Test
	@DisplayName("Should re-prompt for product code if initial inputs are empty, then succeed")
	void shouldRepromptMultipleTimesForEmptyProductCode() {
		// Arrange
		String productCode = "PROD003";
		int quantity = 30;
		LocalDate purchaseDate = LocalDate.now().minusDays(2);
		LocalDate expiryDate = LocalDate.now().plusMonths(1);

		when(scanner.nextLine()).thenReturn("") // Empty 1
				.thenReturn("   ") // Spaces only (empty after trim)
				.thenReturn(productCode) // Valid
				.thenReturn(String.valueOf(quantity)).thenReturn(purchaseDate.toString())
				.thenReturn(expiryDate.toString());

		doNothing().when(inventoryManager).receiveStock(productCode, purchaseDate, expiryDate, quantity);

		// Act
		receiveStockCommand.execute();

		// Assert
		verify(scanner, times(6)).nextLine(); // 2 empty + valid code + qty + PD + ED
		verify(inventoryManager, times(1)).receiveStock(productCode, purchaseDate, expiryDate, quantity);

		String output = outContent.toString();
		long errorCount = output.lines().filter(line -> line.contains("Error: Product code cannot be empty.")).count();
		assertTrue(errorCount >= 2, "Expected at least 2 empty product code error messages.");
	}

	@Test
	@DisplayName("Should handle quantity input with leading/trailing spaces correctly")
	void shouldHandleQuantityWithSpaces() {
		// Arrange
		String productCode = "PROD004";
		String quantityWithSpaces = "  75   ";
		int expectedQuantity = 75;
		LocalDate purchaseDate = LocalDate.now().minusWeeks(1);
		LocalDate expiryDate = LocalDate.now().plusMonths(2);

		when(scanner.nextLine()).thenReturn(productCode).thenReturn(quantityWithSpaces)
				.thenReturn(purchaseDate.toString()).thenReturn(expiryDate.toString());

		doNothing().when(inventoryManager).receiveStock(productCode, purchaseDate, expiryDate, expectedQuantity);

		// Act
		receiveStockCommand.execute();

		// Assert
		verify(scanner, times(4)).nextLine();
		verify(inventoryManager, times(1)).receiveStock(productCode, purchaseDate, expiryDate, expectedQuantity);
		assertTrue(outContent.toString().contains("Quantity:")); // General prompt check
	}

	@Test
	@DisplayName("Should re-prompt for quantity if initial inputs are invalid, then succeed")
	void shouldRepromptMultipleTimesForInvalidQuantity() {
		// Arrange
		String productCode = "PROD005";
		int quantity = 10;
		LocalDate purchaseDate = LocalDate.now().minusDays(1);
		LocalDate expiryDate = LocalDate.now().plusMonths(1);

		when(scanner.nextLine()).thenReturn(productCode).thenReturn("not_a_number") // Invalid 1: non-numeric
				.thenReturn("-5") // Invalid 2: negative
				.thenReturn("0") // Invalid 3: zero
				.thenReturn(String.valueOf(quantity)) // Valid
				.thenReturn(purchaseDate.toString()).thenReturn(expiryDate.toString());

		doNothing().when(inventoryManager).receiveStock(productCode, purchaseDate, expiryDate, quantity);

		// Act
		receiveStockCommand.execute();

		// Assert
		verify(scanner, times(7)).nextLine(); // Code + 3 invalid qty + valid qty + PD + ED
		verify(inventoryManager, times(1)).receiveStock(productCode, purchaseDate, expiryDate, quantity);

		String output = outContent.toString();
		assertTrue(output.contains("Error: Invalid quantity. Please enter a positive integer."),
				"Should display error for non-numeric quantity.");
		assertTrue(output.contains("Error: Quantity must be positive."),
				"Should display error for non-positive quantity.");
		long quantityPromptCount = output.lines().filter(line -> line.contains("Quantity:")).count();
		assertTrue(quantityPromptCount >= 4, "Expected at least 4 quantity prompts.");
	}

	@Test
	@DisplayName("Should handle purchase date input with leading/trailing spaces correctly")
	void shouldHandlePurchaseDateWithSpaces() {
		// Arrange
		String productCode = "PROD006";
		int quantity = 20;
		String purchaseDateWithSpaces = "  " + LocalDate.now().minusDays(15).toString() + "  ";
		LocalDate expectedPurchaseDate = LocalDate.now().minusDays(15);
		LocalDate expiryDate = LocalDate.now().plusMonths(4);

		when(scanner.nextLine()).thenReturn(productCode).thenReturn(String.valueOf(quantity))
				.thenReturn(purchaseDateWithSpaces).thenReturn(expiryDate.toString());

		doNothing().when(inventoryManager).receiveStock(productCode, expectedPurchaseDate, expiryDate, quantity);

		// Act
		receiveStockCommand.execute();

		// Assert
		verify(scanner, times(4)).nextLine();
		verify(inventoryManager, times(1)).receiveStock(productCode, expectedPurchaseDate, expiryDate, quantity);
		assertTrue(outContent.toString().contains("Purchase date (YYYY-MM-DD):")); // General prompt check
	}

	@Test
	@DisplayName("Should re-prompt for purchase date if initial inputs are invalid format, then succeed")
	void shouldRepromptMultipleTimesForInvalidPurchaseDateFormat() {
		// Arrange
		String productCode = "PROD007";
		int quantity = 40;
		LocalDate purchaseDate = LocalDate.now().minusDays(3);
		LocalDate expiryDate = LocalDate.now().plusMonths(5);

		when(scanner.nextLine()).thenReturn(productCode).thenReturn(String.valueOf(quantity)).thenReturn("2024/01/01") // Invalid
																														// 1:
																														// wrong
																														// format
				.thenReturn("not_a_date") // Invalid 2: non-date string
				.thenReturn(purchaseDate.toString()) // Valid
				.thenReturn(expiryDate.toString());

		doNothing().when(inventoryManager).receiveStock(productCode, purchaseDate, expiryDate, quantity);

		// Act
		receiveStockCommand.execute();

		// Assert
		verify(scanner, times(6)).nextLine(); // Code + Qty + 2 invalid PD + valid PD + ED
		verify(inventoryManager, times(1)).receiveStock(productCode, purchaseDate, expiryDate, quantity);

		String output = outContent.toString();
		assertTrue(output.contains("Error: Invalid purchase date format. Please use YYYY-MM-DD."),
				"Should display invalid purchase date format error.");
		long pdPromptCount = output.lines().filter(line -> line.contains("Purchase date (YYYY-MM-DD):")).count();
		assertTrue(pdPromptCount >= 3, "Expected at least 3 purchase date prompts.");
	}

	@Test
	@DisplayName("Should handle expiry date input with leading/trailing spaces correctly")
	void shouldHandleExpiryDateWithSpaces() {
		// Arrange
		String productCode = "PROD008";
		int quantity = 60;
		LocalDate purchaseDate = LocalDate.now().minusDays(20);
		String expiryDateWithSpaces = "  " + LocalDate.now().plusMonths(6).toString() + "  ";
		LocalDate expectedExpiryDate = LocalDate.now().plusMonths(6);

		when(scanner.nextLine()).thenReturn(productCode).thenReturn(String.valueOf(quantity))
				.thenReturn(purchaseDate.toString()).thenReturn(expiryDateWithSpaces);

		doNothing().when(inventoryManager).receiveStock(productCode, purchaseDate, expectedExpiryDate, quantity);

		// Act
		receiveStockCommand.execute();

		// Assert
		verify(scanner, times(4)).nextLine();
		verify(inventoryManager, times(1)).receiveStock(productCode, purchaseDate, expectedExpiryDate, quantity);
		assertTrue(outContent.toString().contains("Expiry date (YYYY-MM-DD):")); // General prompt check
	}

	@Test
	@DisplayName("Should re-prompt for expiry date if initial inputs are invalid format, then succeed")
	void shouldRepromptMultipleTimesForInvalidExpiryDateFormat() {
		// Arrange
		String productCode = "PROD009";
		int quantity = 25;
		LocalDate purchaseDate = LocalDate.now().minusDays(7);
		LocalDate expiryDate = LocalDate.now().plusMonths(1);

		when(scanner.nextLine()).thenReturn(productCode).thenReturn(String.valueOf(quantity))
				.thenReturn(purchaseDate.toString()).thenReturn("2024-1-1") // Invalid 1: wrong format
				.thenReturn("not_a_date_at_all") // Invalid 2: non-date string
				.thenReturn(expiryDate.toString()); // Valid

		doNothing().when(inventoryManager).receiveStock(productCode, purchaseDate, expiryDate, quantity);

		// Act
		receiveStockCommand.execute();

		// Assert
		verify(scanner, times(6)).nextLine(); // Code + Qty + PD + 2 invalid ED + valid ED
		verify(inventoryManager, times(1)).receiveStock(productCode, purchaseDate, expiryDate, quantity);

		String output = outContent.toString();
		assertTrue(output.contains("Error: Invalid expiry date format. Please use YYYY-MM-DD."),
				"Should display invalid expiry date format error.");
		long edPromptCount = output.lines().filter(line -> line.contains("Expiry date (YYYY-MM-DD):")).count();
		assertTrue(edPromptCount >= 3, "Expected at least 3 expiry date prompts.");
	}

	@Test
	@DisplayName("Should re-prompt for expiry date if it is before purchase date, then succeed")
	void shouldRepromptForExpiryDateBeforePurchaseDate() {
		// Arrange
		String productCode = "PROD010";
		int quantity = 15;
		LocalDate purchaseDate = LocalDate.now().minusDays(10); // Purchase 10 days ago
		LocalDate invalidExpiryDate = purchaseDate.minusDays(1); // Expiry before purchase
		LocalDate validExpiryDate = LocalDate.now().plusMonths(1); // Valid expiry

		when(scanner.nextLine()).thenReturn(productCode).thenReturn(String.valueOf(quantity))
				.thenReturn(purchaseDate.toString()).thenReturn(invalidExpiryDate.toString()) // First ED input: before
																								// PD
				.thenReturn(validExpiryDate.toString()); // Second ED input: valid

		doNothing().when(inventoryManager).receiveStock(productCode, purchaseDate, validExpiryDate, quantity);

		// Act
		receiveStockCommand.execute();

		// Assert
		verify(scanner, times(5)).nextLine(); // Code + Qty + PD + invalid ED + valid ED
		verify(inventoryManager, times(1)).receiveStock(productCode, purchaseDate, validExpiryDate, quantity);

		String output = outContent.toString();
		assertTrue(output.contains("Error: Expiry date cannot be before purchase date."),
				"Should display error for expiry date before purchase date.");
		long edPromptCount = output.lines().filter(line -> line.contains("Expiry date (YYYY-MM-DD):")).count();
		assertTrue(edPromptCount >= 2, "Expected at least 2 expiry date prompts.");
	}

	@Test
	@DisplayName("Should allow expiry date to be the same as purchase date")
	void shouldAllowExpiryDateSameAsPurchaseDate() {
		// Arrange
		String productCode = "PROD011";
		int quantity = 5;
		LocalDate purchaseAndExpiryDate = LocalDate.now(); // Same date

		when(scanner.nextLine()).thenReturn(productCode).thenReturn(String.valueOf(quantity))
				.thenReturn(purchaseAndExpiryDate.toString()).thenReturn(purchaseAndExpiryDate.toString()); // Expiry is
																											// same as
																											// purchase

		doNothing().when(inventoryManager).receiveStock(productCode, purchaseAndExpiryDate, purchaseAndExpiryDate,
				quantity);

		// Act
		receiveStockCommand.execute();

		// Assert
		verify(scanner, times(4)).nextLine();
		verify(inventoryManager, times(1)).receiveStock(productCode, purchaseAndExpiryDate, purchaseAndExpiryDate,
				quantity);
		// No error messages expected
	}

	@Test
	@DisplayName("Should handle IllegalArgumentException from InventoryManager")
	void shouldHandleIllegalArgumentException() {
		// Arrange
		String productCode = "PROD012";
		int quantity = 1;
		LocalDate purchaseDate = LocalDate.now();
		LocalDate expiryDate = LocalDate.now().plusYears(1);
		String errorMessage = "Product code PROD012 not recognized or invalid.";

		when(scanner.nextLine()).thenReturn(productCode).thenReturn(String.valueOf(quantity))
				.thenReturn(purchaseDate.toString()).thenReturn(expiryDate.toString());

		doThrow(new IllegalArgumentException(errorMessage)).when(inventoryManager).receiveStock(productCode,
				purchaseDate, expiryDate, quantity);

		// Act
		receiveStockCommand.execute();

		// Assert
		verify(scanner, times(4)).nextLine();
		verify(inventoryManager, times(1)).receiveStock(productCode, purchaseDate, expiryDate, quantity);

		String output = outContent.toString();
		assertTrue(output.contains("Failed to receive stock: " + errorMessage),
				"Should display error message from IllegalArgumentException.");
	}

	@Test
	@DisplayName("Should handle generic RuntimeException from InventoryManager")
	void shouldHandleGenericRuntimeException() {
		// Arrange
		String productCode = "PROD013";
		int quantity = 2;
		LocalDate purchaseDate = LocalDate.now();
		LocalDate expiryDate = LocalDate.now().plusMonths(1);
		String errorMessage = "Database connection lost during stock reception.";

		when(scanner.nextLine()).thenReturn(productCode).thenReturn(String.valueOf(quantity))
				.thenReturn(purchaseDate.toString()).thenReturn(expiryDate.toString());

		doThrow(new RuntimeException(errorMessage)).when(inventoryManager).receiveStock(productCode, purchaseDate,
				expiryDate, quantity);

		// Act
		receiveStockCommand.execute();

		// Assert
		verify(scanner, times(4)).nextLine();
		verify(inventoryManager, times(1)).receiveStock(productCode, purchaseDate, expiryDate, quantity);

		String output = outContent.toString();
		assertTrue(output.contains("An unexpected error occurred: " + errorMessage),
				"Should display generic unexpected error message.");
		// The e.printStackTrace() call from the command will also be captured, leading
		// to more output.
		// For production, you might remove printStackTrace for cleaner logs.
	}
}