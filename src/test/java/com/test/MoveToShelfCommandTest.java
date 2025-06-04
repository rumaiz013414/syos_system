package com.test;

import com.syos.command.MoveToShelfCommand;
import com.syos.singleton.InventoryManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Scanner;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MoveToShelfCommandTest {

	@Mock
	private InventoryManager inventoryManager; // Mock the singleton
	@Mock
	private Scanner scanner; // Mock user input

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

	// --- Success Scenario ---
	@Test
	@DisplayName("Should successfully move stock to shelf with valid inputs")
	void shouldSuccessfullyMoveToShelf() {
		// Arrange
		String productCode = "PROD001";
		String quantityInput = "10";
		int quantity = 10;

		when(scanner.nextLine()).thenReturn(productCode).thenReturn(quantityInput);
		// Mock the inventoryManager's behavior for a successful move
		doNothing().when(inventoryManager).moveToShelf(productCode, quantity);

		// Act
		moveToShelfCommand.execute();

		// Assert
		verify(scanner, times(2)).nextLine();
		verify(inventoryManager, times(1)).moveToShelf(productCode, quantity);
		// The success message is now printed by InventoryManager, so we verify that the
		// command started
		String output = outContent.toString();
		assertTrue(output.contains("=== Move Stock to Shelf ==="));
		// We don't verify the specific success message here, as it comes from
		// InventoryManager
		// and is tested in InventoryManagerTest.
	}

	// --- Validation Failure Scenarios ---
	@ParameterizedTest
	@MethodSource("invalidInputTestCases")
	@DisplayName("Should handle invalid inputs and re-prompt until valid")
	void shouldHandleInvalidInputsAndEventuallySucceed(String[] scannerInputs, // All inputs for scanner.nextLine() in
																				// sequence
			String expectedFinalCode, int expectedFinalQuantity, String[] expectedErrorMessages) {

		// Arrange
		when(scanner.nextLine()).thenReturn(scannerInputs[0], Stream.of(scannerInputs).skip(1).toArray(String[]::new));
		doNothing().when(inventoryManager).moveToShelf(expectedFinalCode, expectedFinalQuantity);

		// Act
		moveToShelfCommand.execute();

		// Assert
		verify(inventoryManager, times(1)).moveToShelf(expectedFinalCode, expectedFinalQuantity);

		String output = outContent.toString();
		for (String errorMessage : expectedErrorMessages) {
			if (errorMessage != null) {
				assertTrue(output.contains(errorMessage), "Output should contain error: " + errorMessage);
			}
		}
		assertTrue(output.contains("=== Move Stock to Shelf ==="));
	}

	private static Stream<Arguments> invalidInputTestCases() {
		return Stream.of(
				// Invalid Code -> Valid Code
				Arguments.of(new String[] { "", "PROD002", "10" }, "PROD002", 10,
						new String[] { "Error: Product code cannot be empty." }),
				// Invalid Quantity Format -> Valid Quantity
				Arguments.of(new String[] { "PROD003", "abc", "5" }, "PROD003", 5,
						new String[] { "Error: Invalid quantity. Please enter a positive integer." }),
				// Non-positive Quantity -> Valid Quantity
				Arguments.of(new String[] { "PROD004", "0", "15" }, "PROD004", 15,
						new String[] { "Error: Quantity must be positive." }),
				Arguments.of(new String[] { "PROD005", "-5", "20" }, "PROD005", 20,
						new String[] { "Error: Quantity must be positive." }),
				// All invalid -> All valid
				Arguments.of(new String[] { "", "PROD006", "xyz", "-1", "25" }, "PROD006", 25,
						new String[] { "Error: Product code cannot be empty.",
								"Error: Invalid quantity. Please enter a positive integer.",
								"Error: Quantity must be positive." }));
	}

	// --- Exception Handling Scenarios from InventoryManager ---
	@Test
	@DisplayName("Should handle IllegalArgumentException from InventoryManager.moveToShelf")
	void shouldHandleIllegalArgumentExceptionFromManager() {
		// Arrange
		String productCode = "PROD007";
		String quantityInput = "10";
		int quantity = 10;
		String errorMessage = "Insufficient stock in back-store.";

		when(scanner.nextLine()).thenReturn(productCode).thenReturn(quantityInput);
		doThrow(new IllegalArgumentException(errorMessage)).when(inventoryManager).moveToShelf(productCode, quantity);

		// Act
		moveToShelfCommand.execute();

		// Assert
		verify(scanner, times(2)).nextLine();
		verify(inventoryManager, times(1)).moveToShelf(productCode, quantity);
		String output = outContent.toString();
		assertTrue(output.contains("Failed to move to shelf: " + errorMessage));
	}

	@Test
	@DisplayName("Should handle IllegalStateException from InventoryManager.moveToShelf")
	void shouldHandleIllegalStateExceptionFromManager() {
		// Arrange
		String productCode = "PROD008";
		String quantityInput = "10";
		int quantity = 10;
		String errorMessage = "Shelf strategy returned null batch unexpectedly.";

		when(scanner.nextLine()).thenReturn(productCode).thenReturn(quantityInput);
		doThrow(new IllegalStateException(errorMessage)).when(inventoryManager).moveToShelf(productCode, quantity);

		// Act
		moveToShelfCommand.execute();

		// Assert
		verify(scanner, times(2)).nextLine();
		verify(inventoryManager, times(1)).moveToShelf(productCode, quantity);
		String output = outContent.toString();
		assertTrue(output.contains("Operation failed: " + errorMessage));
	}

	@Test
	@DisplayName("Should handle generic RuntimeException from InventoryManager.moveToShelf")
	void shouldHandleGenericRuntimeExceptionFromManager() {
		// Arrange
		String productCode = "PROD009";
		String quantityInput = "10";
		int quantity = 10;
		String errorMessage = "Database error during stock update.";

		when(scanner.nextLine()).thenReturn(productCode).thenReturn(quantityInput);
		doThrow(new RuntimeException(errorMessage)).when(inventoryManager).moveToShelf(productCode, quantity);

		// Act
		moveToShelfCommand.execute();

		// Assert
		verify(scanner, times(2)).nextLine();
		verify(inventoryManager, times(1)).moveToShelf(productCode, quantity);
		String output = outContent.toString();
		assertTrue(output.contains("An unexpected error occurred: " + errorMessage));
	}
}