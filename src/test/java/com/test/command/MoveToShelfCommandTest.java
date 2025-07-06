package com.test.command;

import com.syos.command.MoveToShelfCommand;
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
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MoveToShelfCommandTest {

	@Mock
	private InventoryManager inventoryManager;
	@Mock
	private Scanner scanner;

	private MoveToShelfCommand moveToShelfCommand;

	private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
	private final PrintStream originalOut = System.out;

	@BeforeEach
	void setUp() {
		System.setOut(new PrintStream(outContent));
		moveToShelfCommand = new MoveToShelfCommand(inventoryManager, scanner);
	}

	@AfterEach
	void restoreStreams() {
		System.setOut(originalOut);
	}

	@Test
	@DisplayName("Should successfully move stock to shelf with valid inputs")
	void shouldSuccessfullyMoveStockToShelf() {
		String productCode = "PROD001";
		int quantity = 10;

		when(scanner.nextLine()).thenReturn(productCode).thenReturn(String.valueOf(quantity));

		doNothing().when(inventoryManager).moveToShelf(productCode, quantity);

		moveToShelfCommand.execute();

		verify(scanner, times(2)).nextLine();
		verify(inventoryManager, times(1)).moveToShelf(productCode, quantity);

		String output = outContent.toString();
		assertTrue(output.contains("=== Move Stock to Shelf ==="), "Output should contain the command header.");
		assertTrue(output.contains("Product code:"), "Output should contain the product code prompt.");
		assertTrue(output.contains("Quantity:"), "Output should contain the quantity prompt.");
	}

	@Test
	@DisplayName("Should re-prompt for product code if initial input is empty, then succeed")
	void shouldRepromptForEmptyProductCode() {
		String productCode = "PROD002";
		int quantity = 5;

		when(scanner.nextLine()).thenReturn("").thenReturn(productCode).thenReturn(String.valueOf(quantity));

		doNothing().when(inventoryManager).moveToShelf(productCode, quantity);

		moveToShelfCommand.execute();

		verify(scanner, times(3)).nextLine();
		verify(inventoryManager, times(1)).moveToShelf(productCode, quantity);

		String output = outContent.toString();
		assertTrue(output.contains("Error: Product code cannot be empty."),
				"Should display error for empty product code.");
	}

	@Test
	@DisplayName("Should re-prompt for quantity if initial input is non-numeric, then succeed")
	void shouldRepromptForNonNumericQuantity() {
		String productCode = "PROD003";
		String invalidQuantityInput = "abc";
		int quantity = 15;

		when(scanner.nextLine()).thenReturn(productCode).thenReturn(invalidQuantityInput)
				.thenReturn(String.valueOf(quantity));

		doNothing().when(inventoryManager).moveToShelf(productCode, quantity);

		moveToShelfCommand.execute();

		verify(scanner, times(3)).nextLine();
		verify(inventoryManager, times(1)).moveToShelf(productCode, quantity);

		String output = outContent.toString();
		assertTrue(output.contains("Error: Invalid quantity. Please enter a positive integer."),
				"Should display error for non-numeric quantity.");
	}

	@Test
	@DisplayName("Should re-prompt for quantity if initial input is zero, then succeed")
	void shouldRepromptForZeroQuantity() {
		String productCode = "PROD004";
		int quantity = 20;

		when(scanner.nextLine()).thenReturn(productCode).thenReturn("0").thenReturn(String.valueOf(quantity));

		doNothing().when(inventoryManager).moveToShelf(productCode, quantity);

		moveToShelfCommand.execute();

		verify(scanner, times(3)).nextLine();
		verify(inventoryManager, times(1)).moveToShelf(productCode, quantity);

		String output = outContent.toString();
		assertTrue(output.contains("Error: Quantity must be positive."), "Should display error for zero quantity.");
	}

	@Test
	@DisplayName("Should re-prompt for quantity if initial input is negative, then succeed")
	void shouldRepromptForNegativeQuantity() {
		String productCode = "PROD005";
		int quantity = 25;

		when(scanner.nextLine()).thenReturn(productCode).thenReturn("-5").thenReturn(String.valueOf(quantity));

		doNothing().when(inventoryManager).moveToShelf(productCode, quantity);

		moveToShelfCommand.execute();

		verify(scanner, times(3)).nextLine();
		verify(inventoryManager, times(1)).moveToShelf(productCode, quantity);

		String output = outContent.toString();
		assertTrue(output.contains("Error: Quantity must be positive."), "Should display error for negative quantity.");
	}

	@Test
	@DisplayName("Should handle IllegalArgumentException thrown by InventoryManager")
	void shouldHandleIllegalArgumentException() {
		String productCode = "PROD006";
		int quantity = 30;
		String errorMessage = "Insufficient stock in inventory for PROD006.";

		when(scanner.nextLine()).thenReturn(productCode).thenReturn(String.valueOf(quantity));

		doThrow(new IllegalArgumentException(errorMessage)).when(inventoryManager).moveToShelf(productCode, quantity);

		moveToShelfCommand.execute();

		verify(scanner, times(2)).nextLine();
		verify(inventoryManager, times(1)).moveToShelf(productCode, quantity);

		String output = outContent.toString();
		assertTrue(output.contains("Failed to move to shelf: " + errorMessage),
				"Should display error message from IllegalArgumentException.");
	}

	@Test
	@DisplayName("Should handle IllegalStateException thrown by InventoryManager")
	void shouldHandleIllegalStateException() {
		String productCode = "PROD007";
		int quantity = 40;
		String errorMessage = "Shelf capacity reached or product not suitable for shelf.";

		when(scanner.nextLine()).thenReturn(productCode).thenReturn(String.valueOf(quantity));

		doThrow(new IllegalStateException(errorMessage)).when(inventoryManager).moveToShelf(productCode, quantity);

		moveToShelfCommand.execute();

		verify(scanner, times(2)).nextLine();
		verify(inventoryManager, times(1)).moveToShelf(productCode, quantity);

		String output = outContent.toString();
		assertTrue(output.contains("Operation failed: " + errorMessage),
				"Should display error message from IllegalStateException.");
	}

	@Test
	@DisplayName("Should handle generic RuntimeException thrown by InventoryManager")
	void shouldHandleGenericRuntimeException() {
		String productCode = "PROD008";
		int quantity = 50;
		String errorMessage = "Unexpected database error during shelf move.";

		when(scanner.nextLine()).thenReturn(productCode).thenReturn(String.valueOf(quantity));

		doThrow(new RuntimeException(errorMessage)).when(inventoryManager).moveToShelf(productCode, quantity);

		moveToShelfCommand.execute();

		verify(scanner, times(2)).nextLine();
		verify(inventoryManager, times(1)).moveToShelf(productCode, quantity);

		String output = outContent.toString();
		assertTrue(output.contains("An unexpected error occurred: " + errorMessage),
				"Should display generic unexpected error message.");
	}

	@Test
	@DisplayName("Should re-prompt for product code multiple times if empty input persists")
	void shouldRepromptMultipleTimesForEmptyProductCode() {
		String productCode = "PROD009";
		int quantity = 7;

		when(scanner.nextLine()).thenReturn("").thenReturn("   ").thenReturn("").thenReturn(productCode)
				.thenReturn(String.valueOf(quantity));

		doNothing().when(inventoryManager).moveToShelf(productCode, quantity);

		moveToShelfCommand.execute();

		verify(scanner, times(5)).nextLine();
		verify(inventoryManager, times(1)).moveToShelf(productCode, quantity);

		String output = outContent.toString();
		long errorCount = output.lines().filter(line -> line.contains("Error: Product code cannot be empty.")).count();
		assertTrue(errorCount >= 3, "Expected at least 3 empty product code error messages.");
		assertTrue(output.contains("Product code:"), "Ensure product code prompt appears multiple times.");
		assertTrue(output.contains("Quantity:"), "Ensure quantity prompt appears.");
	}

	@Test
	@DisplayName("Should re-prompt for quantity multiple times if invalid input persists")
	void shouldRepromptMultipleTimesForInvalidQuantity() {
		String productCode = "PROD010";
		int quantity = 12;

		when(scanner.nextLine()).thenReturn(productCode).thenReturn("invalid_text").thenReturn("-10").thenReturn("0")
				.thenReturn(String.valueOf(quantity));

		doNothing().when(inventoryManager).moveToShelf(productCode, quantity);

		moveToShelfCommand.execute();

		verify(scanner, times(5)).nextLine();
		verify(inventoryManager, times(1)).moveToShelf(productCode, quantity);

		String output = outContent.toString();
		assertTrue(output.contains("Error: Invalid quantity. Please enter a positive integer."),
				"Should contain invalid numeric error for 'invalid_text'.");
		assertTrue(output.contains("Error: Quantity must be positive."),
				"Should contain positive quantity error for '-10' and '0'.");

		long quantityPromptCount = output.lines().filter(line -> line.contains("Quantity:")).count();
		assertTrue(quantityPromptCount >= 4, "Expected at least 4 quantity prompts (initial + 3 reprompts).");
	}

	@Test
	@DisplayName("Should handle product code with leading/trailing spaces correctly (trimming)")
	void shouldHandleProductCodeWithSpaces() {
		String productCodeWithSpaces = " PROD011 ";
		String expectedProductCode = "PROD011";
		int quantity = 20;

		when(scanner.nextLine()).thenReturn(productCodeWithSpaces).thenReturn(String.valueOf(quantity));

		doNothing().when(inventoryManager).moveToShelf(expectedProductCode, quantity);

		moveToShelfCommand.execute();

		verify(scanner, times(2)).nextLine();
		verify(inventoryManager, times(1)).moveToShelf(expectedProductCode, quantity);

		String output = outContent.toString();
		assertTrue(output.contains("=== Move Stock to Shelf ==="), "Output should contain the command header.");
		assertTrue(output.contains("Product code:"), "Output should contain the product code prompt.");
		assertTrue(output.contains("Quantity:"), "Output should contain the quantity prompt.");
	}

	@Test
	@DisplayName("Should handle quantity input with leading/trailing spaces correctly (trimming before parsing)")
	void shouldHandleQuantityWithSpaces() {
		String productCode = "PROD012";
		String quantityWithSpaces = " 30 ";
		int expectedQuantity = 30;

		when(scanner.nextLine()).thenReturn(productCode).thenReturn(quantityWithSpaces);

		doNothing().when(inventoryManager).moveToShelf(productCode, expectedQuantity);

		moveToShelfCommand.execute();

		verify(scanner, times(2)).nextLine();
		verify(inventoryManager, times(1)).moveToShelf(productCode, expectedQuantity);

		String output = outContent.toString();
		assertTrue(output.contains("=== Move Stock to Shelf ==="), "Output should contain the command header.");
		assertTrue(output.contains("Product code:"), "Output should contain the product code prompt.");
		assertTrue(output.contains("Quantity:"), "Output should contain the quantity prompt.");
	}
}