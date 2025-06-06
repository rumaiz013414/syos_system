package com.test;

import com.syos.command.ReceiveStockCommand;
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
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Scanner;
import java.util.stream.Stream;

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

	// --- Success Scenario ---
	@Test
	@DisplayName("Should successfully receive stock with valid inputs")
	void shouldSuccessfullyReceiveStock() {
		// Arrange
		String productCode = "PROD001";
		String quantityInput = "100";
		int quantity = 100;
		String purchaseDateInput = "2024-01-01";
		LocalDate purchaseDate = LocalDate.parse(purchaseDateInput);
		String expiryDateInput = "2025-01-01";
		LocalDate expiryDate = LocalDate.parse(expiryDateInput);

		when(scanner.nextLine()).thenReturn(productCode).thenReturn(quantityInput).thenReturn(purchaseDateInput)
				.thenReturn(expiryDateInput);
		// Mock the inventoryManager's behavior for a successful receive
		doNothing().when(inventoryManager).receiveStock(productCode, purchaseDate, expiryDate, quantity);

		// Act
		receiveStockCommand.execute();

		// Assert
		verify(scanner, times(4)).nextLine(); // Code, Qty, PD, ED
		verify(inventoryManager, times(1)).receiveStock(productCode, purchaseDate, expiryDate, quantity);
		String output = outContent.toString();
		assertTrue(output.contains("=== Receive New Stock ==="));
		// The success message is now printed by InventoryManager, so we verify that the
		// command started
	}

	// --- Validation Failure Scenarios (Parameterized) ---
	@ParameterizedTest
	@MethodSource("invalidInputTestCases")
	@DisplayName("Should handle invalid inputs and re-prompt until valid data is entered")
	void shouldHandleInvalidInputsAndEventuallySucceed(String[] scannerInputs, // All inputs for scanner.nextLine() in
																				// sequence
			String expectedFinalCode, int expectedFinalQuantity, LocalDate expectedFinalPurchaseDate,
			LocalDate expectedFinalExpiryDate, String[] expectedErrorMessages) {

		// Arrange
		when(scanner.nextLine()).thenReturn(scannerInputs[0], Stream.of(scannerInputs).skip(1).toArray(String[]::new));
		doNothing().when(inventoryManager).receiveStock(expectedFinalCode, expectedFinalPurchaseDate,
				expectedFinalExpiryDate, expectedFinalQuantity);

		// Act
		receiveStockCommand.execute();

		// Assert
		verify(inventoryManager, times(1)).receiveStock(expectedFinalCode, expectedFinalPurchaseDate,
				expectedFinalExpiryDate, expectedFinalQuantity);

		String output = outContent.toString();
		for (String errorMessage : expectedErrorMessages) {
			if (errorMessage != null) {
				assertTrue(output.contains(errorMessage), "Output should contain error: " + errorMessage);
			}
		}
		assertTrue(output.contains("=== Receive New Stock ==="));
	}

	private static Stream<Arguments> invalidInputTestCases() {

		return Stream.of(
				// 1. Invalid Code -> Valid Code
				Arguments.of(new String[] { "", "PROD002", "10", "2024-01-01", "2025-01-01" }, "PROD002", 10,
						LocalDate.of(2024, 1, 1), LocalDate.of(2025, 1, 1),
						new String[] { "Error: Product code cannot be empty." }),

				// 2. Invalid Quantity Format -> Valid Quantity
				Arguments.of(new String[] { "PROD003", "abc", "5", "2024-01-01", "2025-01-01" }, "PROD003", 5,
						LocalDate.of(2024, 1, 1), LocalDate.of(2025, 1, 1),
						new String[] { "Error: Invalid quantity. Please enter a positive integer." }),

				// 3. Non-positive Quantity -> Valid Quantity
				Arguments.of(new String[] { "PROD004", "0", "15", "2024-01-01", "2025-01-01" }, "PROD004", 15,
						LocalDate.of(2024, 1, 1), LocalDate.of(2025, 1, 1),
						new String[] { "Error: Quantity must be positive." }),
				Arguments.of(new String[] { "PROD005", "-5", "20", "2024-01-01", "2025-01-01" }, "PROD005", 20,
						LocalDate.of(2024, 1, 1), LocalDate.of(2025, 1, 1),
						new String[] { "Error: Quantity must be positive." }),

				// 4. Invalid Purchase Date Format -> Valid Purchase Date
				Arguments.of(new String[] { "PROD006", "10", "2024/01/01", "2024-01-01", "2025-01-01" }, "PROD006", 10,
						LocalDate.of(2024, 1, 1), LocalDate.of(2025, 1, 1),
						new String[] { "Error: Invalid purchase date format. Please use YYYY-MM-DD." }),

				// 5. Invalid Expiry Date Format -> Valid Expiry Date
				Arguments.of(new String[] { "PROD007", "10", "2024-01-01", "01-01-2025", "2025-01-01" }, "PROD007", 10,
						LocalDate.of(2024, 1, 1), LocalDate.of(2025, 1, 1),
						new String[] { "Error: Invalid expiry date format. Please use YYYY-MM-DD." }),

				// 6. Expiry Date Before Purchase Date -> Valid Expiry Date
				Arguments.of(new String[] { "PROD008", "10", "2024-01-10", "2024-01-09", "2024-01-11" }, "PROD008", 10,
						LocalDate.of(2024, 1, 10), LocalDate.of(2024, 1, 11),
						new String[] { "Error: Expiry date cannot be before purchase date." }),

				// 7. All Invalid Inputs (chained) -> All Valid Inputs
				Arguments.of(new String[] { "", "PROD009", // Code
						"xyz", "-1", "50", // Quantity
						"bad-pd", "2024-01-01", // Purchase Date
						"bad-ed", "2023-01-01", "2025-01-01" // Expiry Date (first invalid, then before PD, then valid)
				}, "PROD009", 50, LocalDate.of(2024, 1, 1), LocalDate.of(2025, 1, 1),
						new String[] { "Error: Product code cannot be empty.",
								"Error: Invalid quantity. Please enter a positive integer.",
								"Error: Quantity must be positive.",
								"Error: Invalid purchase date format. Please use YYYY-MM-DD.",
								"Error: Invalid expiry date format. Please use YYYY-MM-DD.",
								"Error: Expiry date cannot be before purchase date." }));
	}

	// --- Exception Handling Scenarios from InventoryManager ---
	@Test
	@DisplayName("Should handle IllegalArgumentException from InventoryManager.receiveStock")
	void shouldHandleIllegalArgumentExceptionFromManager() {
		// Arrange
		String productCode = "PROD_ERR1";
		String quantityInput = "10";
		int quantity = 10;
		String purchaseDateInput = "2024-01-01";
		LocalDate purchaseDate = LocalDate.parse(purchaseDateInput);
		String expiryDateInput = "2025-01-01";
		LocalDate expiryDate = LocalDate.parse(expiryDateInput);
		String errorMessage = "Internal validation failed in manager.";

		when(scanner.nextLine()).thenReturn(productCode).thenReturn(quantityInput).thenReturn(purchaseDateInput)
				.thenReturn(expiryDateInput);
		doThrow(new IllegalArgumentException(errorMessage)).when(inventoryManager).receiveStock(productCode,
				purchaseDate, expiryDate, quantity);

		// Act
		receiveStockCommand.execute();

		// Assert
		verify(scanner, times(4)).nextLine();
		verify(inventoryManager, times(1)).receiveStock(productCode, purchaseDate, expiryDate, quantity);
		String output = outContent.toString();
		assertTrue(output.contains("Failed to receive stock: " + errorMessage));
	}

	@Test
	@DisplayName("Should handle generic RuntimeException from InventoryManager.receiveStock")
	void shouldHandleGenericRuntimeExceptionFromManager() {
		// Arrange
		String productCode = "PROD_ERR2";
		String quantityInput = "10";
		int quantity = 10;
		String purchaseDateInput = "2024-01-01";
		LocalDate purchaseDate = LocalDate.parse(purchaseDateInput);
		String expiryDateInput = "2025-01-01";
		LocalDate expiryDate = LocalDate.parse(expiryDateInput);
		String errorMessage = "Database error during batch creation.";

		when(scanner.nextLine()).thenReturn(productCode).thenReturn(quantityInput).thenReturn(purchaseDateInput)
				.thenReturn(expiryDateInput);
		doThrow(new RuntimeException(errorMessage)).when(inventoryManager).receiveStock(productCode, purchaseDate,
				expiryDate, quantity);

		// Act
		receiveStockCommand.execute();

		// Assert
		verify(scanner, times(4)).nextLine();
		verify(inventoryManager, times(1)).receiveStock(productCode, purchaseDate, expiryDate, quantity);
		String output = outContent.toString();
		assertTrue(output.contains("An unexpected error occurred: " + errorMessage));
	}
}