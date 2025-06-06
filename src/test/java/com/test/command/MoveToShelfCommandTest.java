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
        // Redirect System.out to capture console output
        System.setOut(new PrintStream(outContent));
        moveToShelfCommand = new MoveToShelfCommand(inventoryManager, scanner);
    }

    @AfterEach
    void restoreStreams() {
        // Restore original System.out after each test
        System.setOut(originalOut);
    }

    @Test
    @DisplayName("Should successfully move stock to shelf with valid inputs")
    void shouldSuccessfullyMoveStockToShelf() {
        // Arrange
        String productCode = "PROD001";
        int quantity = 10;

        // Mock user inputs for product code and quantity
        when(scanner.nextLine())
                .thenReturn(productCode)
                .thenReturn(String.valueOf(quantity));

        // Mock the InventoryManager to do nothing upon successful move
        // The command itself doesn't print success, InventoryManager does.
        doNothing().when(inventoryManager).moveToShelf(productCode, quantity);

        // Act
        moveToShelfCommand.execute();

        // Assert
        // Verify that scanner.nextLine() was called twice (for code and quantity)
        verify(scanner, times(2)).nextLine();
        // Verify that InventoryManager.moveToShelf() was called exactly once with the correct arguments
        verify(inventoryManager, times(1)).moveToShelf(productCode, quantity);

        // Verify the console output for the command's prompts and header
        String output = outContent.toString();
        assertTrue(output.contains("=== Move Stock to Shelf ==="), "Output should contain the command header.");
        assertTrue(output.contains("Product code:"), "Output should contain the product code prompt.");
        assertTrue(output.contains("Quantity:"), "Output should contain the quantity prompt.");
        // The success message is printed by InventoryManager, not by this command, so we don't assert it here.
    }

    @Test
    @DisplayName("Should re-prompt for product code if initial input is empty, then succeed")
    void shouldRepromptForEmptyProductCode() {
        // Arrange
        String productCode = "PROD002";
        int quantity = 5;

        // Mock inputs: empty, then valid product code, then quantity
        when(scanner.nextLine())
                .thenReturn("") // First input: empty product code
                .thenReturn(productCode) // Second input: valid product code
                .thenReturn(String.valueOf(quantity)); // Third input: quantity

        doNothing().when(inventoryManager).moveToShelf(productCode, quantity);

        // Act
        moveToShelfCommand.execute();

        // Assert
        // Verify scanner.nextLine() was called three times (empty, valid code, quantity)
        verify(scanner, times(3)).nextLine();
        // Verify InventoryManager.moveToShelf() was called once with valid inputs
        verify(inventoryManager, times(1)).moveToShelf(productCode, quantity);

        // Verify error message for empty product code is displayed
        String output = outContent.toString();
        assertTrue(output.contains("Error: Product code cannot be empty."), "Should display error for empty product code.");
    }

    @Test
    @DisplayName("Should re-prompt for quantity if initial input is non-numeric, then succeed")
    void shouldRepromptForNonNumericQuantity() {
        // Arrange
        String productCode = "PROD003";
        String invalidQuantityInput = "abc";
        int quantity = 15;

        // Mock inputs: product code, non-numeric quantity, then valid quantity
        when(scanner.nextLine())
                .thenReturn(productCode)
                .thenReturn(invalidQuantityInput)
                .thenReturn(String.valueOf(quantity));

        doNothing().when(inventoryManager).moveToShelf(productCode, quantity);

        // Act
        moveToShelfCommand.execute();

        // Assert
        // Verify scanner.nextLine() was called three times (code, invalid qty, valid qty)
        verify(scanner, times(3)).nextLine();
        verify(inventoryManager, times(1)).moveToShelf(productCode, quantity);

        // Verify error message for invalid quantity is displayed
        String output = outContent.toString();
        assertTrue(output.contains("Error: Invalid quantity. Please enter a positive integer."), "Should display error for non-numeric quantity.");
    }

    @Test
    @DisplayName("Should re-prompt for quantity if initial input is zero, then succeed")
    void shouldRepromptForZeroQuantity() {
        // Arrange
        String productCode = "PROD004";
        int quantity = 20;

        // Mock inputs: product code, zero quantity, then valid quantity
        when(scanner.nextLine())
                .thenReturn(productCode)
                .thenReturn("0") // Quantity is zero
                .thenReturn(String.valueOf(quantity));

        doNothing().when(inventoryManager).moveToShelf(productCode, quantity);

        // Act
        moveToShelfCommand.execute();

        // Assert
        // Verify scanner.nextLine() was called three times (code, zero qty, valid qty)
        verify(scanner, times(3)).nextLine();
        verify(inventoryManager, times(1)).moveToShelf(productCode, quantity);

        // Verify error message for non-positive quantity is displayed
        String output = outContent.toString();
        assertTrue(output.contains("Error: Quantity must be positive."), "Should display error for zero quantity.");
    }

    @Test
    @DisplayName("Should re-prompt for quantity if initial input is negative, then succeed")
    void shouldRepromptForNegativeQuantity() {
        // Arrange
        String productCode = "PROD005";
        int quantity = 25;

        // Mock inputs: product code, negative quantity, then valid quantity
        when(scanner.nextLine())
                .thenReturn(productCode)
                .thenReturn("-5") // Quantity is negative
                .thenReturn(String.valueOf(quantity));

        doNothing().when(inventoryManager).moveToShelf(productCode, quantity);

        // Act
        moveToShelfCommand.execute();

        // Assert
        // Verify scanner.nextLine() was called three times (code, negative qty, valid qty)
        verify(scanner, times(3)).nextLine();
        verify(inventoryManager, times(1)).moveToShelf(productCode, quantity);

        // Verify error message for non-positive quantity is displayed
        String output = outContent.toString();
        assertTrue(output.contains("Error: Quantity must be positive."), "Should display error for negative quantity.");
    }

    @Test
    @DisplayName("Should handle IllegalArgumentException thrown by InventoryManager")
    void shouldHandleIllegalArgumentException() {
        // Arrange
        String productCode = "PROD006";
        int quantity = 30;
        String errorMessage = "Insufficient stock in inventory for PROD006.";

        // Mock inputs
        when(scanner.nextLine())
                .thenReturn(productCode)
                .thenReturn(String.valueOf(quantity));

        // Mock InventoryManager to throw IllegalArgumentException
        doThrow(new IllegalArgumentException(errorMessage))
                .when(inventoryManager).moveToShelf(productCode, quantity);

        // Act
        moveToShelfCommand.execute();

        // Assert
        verify(scanner, times(2)).nextLine();
        verify(inventoryManager, times(1)).moveToShelf(productCode, quantity);

        // Verify that the command prints the specific error message from the exception
        String output = outContent.toString();
        assertTrue(output.contains("Failed to move to shelf: " + errorMessage), "Should display error message from IllegalArgumentException.");
    }

    @Test
    @DisplayName("Should handle IllegalStateException thrown by InventoryManager")
    void shouldHandleIllegalStateException() {
        // Arrange
        String productCode = "PROD007";
        int quantity = 40;
        String errorMessage = "Shelf capacity reached or product not suitable for shelf.";

        // Mock inputs
        when(scanner.nextLine())
                .thenReturn(productCode)
                .thenReturn(String.valueOf(quantity));

        // Mock InventoryManager to throw IllegalStateException
        doThrow(new IllegalStateException(errorMessage))
                .when(inventoryManager).moveToShelf(productCode, quantity);

        // Act
        moveToShelfCommand.execute();

        // Assert
        verify(scanner, times(2)).nextLine();
        verify(inventoryManager, times(1)).moveToShelf(productCode, quantity);

        // Verify that the command prints the specific error message from the exception
        String output = outContent.toString();
        assertTrue(output.contains("Operation failed: " + errorMessage), "Should display error message from IllegalStateException.");
    }

    @Test
    @DisplayName("Should handle generic RuntimeException thrown by InventoryManager")
    void shouldHandleGenericRuntimeException() {
        // Arrange
        String productCode = "PROD008";
        int quantity = 50;
        String errorMessage = "Unexpected database error during shelf move.";

        // Mock inputs
        when(scanner.nextLine())
                .thenReturn(productCode)
                .thenReturn(String.valueOf(quantity));

        // Mock InventoryManager to throw a generic RuntimeException
        doThrow(new RuntimeException(errorMessage))
                .when(inventoryManager).moveToShelf(productCode, quantity);

        // Act
        moveToShelfCommand.execute();

        // Assert
        verify(scanner, times(2)).nextLine();
        verify(inventoryManager, times(1)).moveToShelf(productCode, quantity);

        // Verify that the command prints the generic unexpected error message
        String output = outContent.toString();
        assertTrue(output.contains("An unexpected error occurred: " + errorMessage), "Should display generic unexpected error message.");
    }

    @Test
    @DisplayName("Should re-prompt for product code multiple times if empty input persists")
    void shouldRepromptMultipleTimesForEmptyProductCode() {
        // Arrange
        String productCode = "PROD009";
        int quantity = 7;

        // Mock multiple empty attempts before a valid product code and quantity
        when(scanner.nextLine())
                .thenReturn("")     // First empty
                .thenReturn("   ")  // Second empty (just spaces, which become empty after trim)
                .thenReturn("")     // Third empty
                .thenReturn(productCode) // Finally valid product code
                .thenReturn(String.valueOf(quantity)); // Valid quantity

        doNothing().when(inventoryManager).moveToShelf(productCode, quantity);

        // Act
        moveToShelfCommand.execute();

        // Assert
        // Verify scanner.nextLine() was called 5 times (3 empty + valid code + quantity)
        verify(scanner, times(5)).nextLine();
        verify(inventoryManager, times(1)).moveToShelf(productCode, quantity);

        String output = outContent.toString();
        // Verify that the error message for empty code appears multiple times
        long errorCount = output.lines()
                                .filter(line -> line.contains("Error: Product code cannot be empty."))
                                .count();
        assertTrue(errorCount >= 3, "Expected at least 3 empty product code error messages.");
        assertTrue(output.contains("Product code:"), "Ensure product code prompt appears multiple times.");
        assertTrue(output.contains("Quantity:"), "Ensure quantity prompt appears.");
    }

    @Test
    @DisplayName("Should re-prompt for quantity multiple times if invalid input persists")
    void shouldRepromptMultipleTimesForInvalidQuantity() {
        // Arrange
        String productCode = "PROD010";
        int quantity = 12;

        // Mock multiple invalid quantity inputs before a valid one
        when(scanner.nextLine())
                .thenReturn(productCode)        // Product code
                .thenReturn("invalid_text")     // Non-numeric
                .thenReturn("-10")              // Negative
                .thenReturn("0")                // Zero
                .thenReturn(String.valueOf(quantity)); // Finally valid quantity

        doNothing().when(inventoryManager).moveToShelf(productCode, quantity);

        // Act
        moveToShelfCommand.execute();

        // Assert
        // Verify scanner.nextLine() was called 5 times (code + 3 invalid qty + valid qty)
        verify(scanner, times(5)).nextLine();
        verify(inventoryManager, times(1)).moveToShelf(productCode, quantity);

        String output = outContent.toString();
        assertTrue(output.contains("Error: Invalid quantity. Please enter a positive integer."), "Should contain invalid numeric error for 'invalid_text'.");
        assertTrue(output.contains("Error: Quantity must be positive."), "Should contain positive quantity error for '-10' and '0'.");

        // Check that the quantity prompt appears for each invalid attempt and the final valid input
        long quantityPromptCount = output.lines()
                                         .filter(line -> line.contains("Quantity:"))
                                         .count();
        assertTrue(quantityPromptCount >= 4, "Expected at least 4 quantity prompts (initial + 3 reprompts).");
    }

    @Test
    @DisplayName("Should handle product code with leading/trailing spaces correctly (trimming)")
    void shouldHandleProductCodeWithSpaces() {
        // Arrange
        String productCodeWithSpaces = "  PROD011  ";
        String expectedProductCode = "PROD011"; // This is what it should be after trimming
        int quantity = 20;

        // Mock inputs with spaces
        when(scanner.nextLine())
                .thenReturn(productCodeWithSpaces)
                .thenReturn(String.valueOf(quantity));

        // Mock InventoryManager expecting the TRIMMED product code
        doNothing().when(inventoryManager).moveToShelf(expectedProductCode, quantity);

        // Act
        moveToShelfCommand.execute();

        // Assert
        verify(scanner, times(2)).nextLine();
        // Verify that moveToShelf was called with the CORRECTLY TRIMMED product code
        verify(inventoryManager, times(1)).moveToShelf(expectedProductCode, quantity);

        String output = outContent.toString();
        assertTrue(output.contains("=== Move Stock to Shelf ==="), "Output should contain the command header.");
        assertTrue(output.contains("Product code:"), "Output should contain the product code prompt.");
        assertTrue(output.contains("Quantity:"), "Output should contain the quantity prompt.");
    }

    @Test
    @DisplayName("Should handle quantity input with leading/trailing spaces correctly (trimming before parsing)")
    void shouldHandleQuantityWithSpaces() {
        // Arrange
        String productCode = "PROD012";
        String quantityWithSpaces = "  30   ";
        int expectedQuantity = 30; // This is what it should be after parsing trimmed string

        // Mock inputs with spaces
        when(scanner.nextLine())
                .thenReturn(productCode)
                .thenReturn(quantityWithSpaces);

        // Mock InventoryManager expecting the CORRECTLY PARSED quantity
        doNothing().when(inventoryManager).moveToShelf(productCode, expectedQuantity);

        // Act
        moveToShelfCommand.execute();

        // Assert
        verify(scanner, times(2)).nextLine();
        // Verify that moveToShelf was called with the CORRECTLY PARSED integer quantity
        verify(inventoryManager, times(1)).moveToShelf(productCode, expectedQuantity);

        String output = outContent.toString();
        assertTrue(output.contains("=== Move Stock to Shelf ==="), "Output should contain the command header.");
        assertTrue(output.contains("Product code:"), "Output should contain the product code prompt.");
        assertTrue(output.contains("Quantity:"), "Output should contain the quantity prompt.");
    }
}