package com.test.service;

import com.syos.enums.DiscountType;
import com.syos.repository.DiscountRepository;
import com.syos.service.DiscountCreationService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDate;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DiscountCreationServiceTest {

    @Mock
    private Scanner mockScanner; // Mock the Scanner for user input
    @Mock
    private DiscountRepository mockDiscountRepo; // Mock the DiscountRepository

    @InjectMocks
    private DiscountCreationService discountCreationService; // Inject mocks into the service under test

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this); // Initialize mocks
        System.setOut(new PrintStream(outContent)); // Redirect System.out to capture console output
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut); // Restore original System.out
        reset(mockScanner, mockDiscountRepo); // Clear mock interactions after each test
    }

    // --- Test Cases for Successful Discount Creation ---

    @Test
    @DisplayName("Should successfully create a discount with valid inputs")
    void createDiscount_Success() {
        // Arrange
        String name = "New Year Sale";
        String type = "PERCENT";
        String value = "15.0";
        String startDate = "2025-01-01";
        String endDate = "2025-01-31";
        int generatedId = 101;

        // Configure mock Scanner to return predefined inputs sequentially
        when(mockScanner.nextLine())
                .thenReturn(name)       // 1) Discount name
                .thenReturn(type)       // 2) Discount type
                .thenReturn(value)      // 3) Discount value
                .thenReturn(startDate)  // 4) Start date
                .thenReturn(endDate);   // 5) End date

        // Configure mock DiscountRepository to return a generated ID
        when(mockDiscountRepo.createDiscount(
                eq(name), eq(DiscountType.PERCENT), eq(15.0), eq(LocalDate.parse(startDate)), eq(LocalDate.parse(endDate))
        )).thenReturn(generatedId);

        // Act
        int resultId = discountCreationService.createDiscount();

        // Assert
        assertEquals(generatedId, resultId, "The returned ID should match the generated ID");

        String consoleOutput = outContent.toString();
        assertTrue(consoleOutput.contains("=== Create New Discount ==="), "Header should be present");
        assertTrue(consoleOutput.contains("Enter discount name (e.g. \"10% OFF SUMMER\"):"), "Name prompt should be present");
        assertTrue(consoleOutput.contains("Discount type (PERCENT or AMOUNT):"), "Type prompt should be present");
        assertTrue(consoleOutput.contains("Discount value (percentage, e.g. 10):"), "Value prompt should be present");
        assertTrue(consoleOutput.contains("Start date (YYYY-MM-DD):"), "Start date prompt should be present");
        assertTrue(consoleOutput.contains("End date (YYYY-MM-DD):"), "End date prompt should be present");
        assertTrue(consoleOutput.contains(
                String.format("Success: Discount '%s' (ID=%d) created from %s to %s.", name, generatedId, startDate, endDate)
        ), "Success message should be present");

        // Verify that scanner.nextLine() was called for each input
        verify(mockScanner, times(5)).nextLine();
        // Verify that createDiscount was called with correct arguments
        verify(mockDiscountRepo, times(1)).createDiscount(
                name, DiscountType.PERCENT, 15.0, LocalDate.parse(startDate), LocalDate.parse(endDate)
        );
    }

    // --- Test Cases for Discount Name Validation ---

    @Test
    @DisplayName("Should re-prompt for discount name if empty then succeed")
    void createDiscount_EmptyNameThenValid() {
        // Arrange
        String validName = "Valid Name";
        // Simulate empty input then valid input
        when(mockScanner.nextLine())
                .thenReturn("") // First try: empty
                .thenReturn(validName) // Second try: valid
                .thenReturn("PERCENT")
                .thenReturn("10")
                .thenReturn("2025-06-01")
                .thenReturn("2025-06-30");

        when(mockDiscountRepo.createDiscount(any(), any(), anyDouble(), any(), any())).thenReturn(1);

        // Act
        discountCreationService.createDiscount();

        // Assert
        String consoleOutput = outContent.toString();
        assertTrue(consoleOutput.contains("Error: Discount name cannot be empty."));
        // Ensure it prompted again for the name
        int firstPromptIndex = consoleOutput.indexOf("Enter discount name (e.g. \"10% OFF SUMMER\"):");
        int secondPromptIndex = consoleOutput.indexOf("Enter discount name (e.g. \"10% OFF SUMMER\"):", firstPromptIndex + 1);
        assertTrue(secondPromptIndex != -1, "Should prompt for name again after empty input");

        // Verify scanner was called more times due to re-prompt
        verify(mockScanner, times(6)).nextLine(); // 1 (empty) + 1 (valid name) + 4 (type, value, dates) = 6
    }

    // --- Test Cases for Discount Type Validation ---

    @Test
    @DisplayName("Should re-prompt for discount type if invalid then succeed")
    void createDiscount_InvalidTypeThenValid() {
        // Arrange
        String validName = "Valid Name";
        String validType = "AMOUNT";
        // Simulate invalid input then valid input
        when(mockScanner.nextLine())
                .thenReturn(validName)
                .thenReturn("INVALID_TYPE") // First try: invalid
                .thenReturn(validType)      // Second try: valid
                .thenReturn("50.0")
                .thenReturn("2025-06-01")
                .thenReturn("2025-06-30");

        when(mockDiscountRepo.createDiscount(any(), any(), anyDouble(), any(), any())).thenReturn(1);

        // Act
        discountCreationService.createDiscount();

        // Assert
        String consoleOutput = outContent.toString();
        assertTrue(consoleOutput.contains("Error: Invalid discount type. Use PERCENT or AMOUNT."));
        // Ensure it prompted again for the type
        int firstPromptIndex = consoleOutput.indexOf("Discount type (PERCENT or AMOUNT):");
        int secondPromptIndex = consoleOutput.indexOf("Discount type (PERCENT or AMOUNT):", firstPromptIndex + 1);
        assertTrue(secondPromptIndex != -1, "Should prompt for type again after invalid input");

        verify(mockScanner, times(6)).nextLine(); // 1(name) + 1(invalid type) + 1(valid type) + 3(value, dates) = 6
    }

    // --- Test Cases for Discount Value Validation ---

    @Test
    @DisplayName("Should re-prompt for discount value if non-numeric then succeed")
    void createDiscount_NonNumericValueThenValid() {
        // Arrange
        String validName = "Sale";
        String validType = "PERCENT";
        String validValue = "10.0";
        when(mockScanner.nextLine())
                .thenReturn(validName)
                .thenReturn(validType)
                .thenReturn("not-a-number") // Invalid value
                .thenReturn(validValue)    // Valid value
                .thenReturn("2025-06-01")
                .thenReturn("2025-06-30");

        when(mockDiscountRepo.createDiscount(any(), any(), anyDouble(), any(), any())).thenReturn(1);

        // Act
        discountCreationService.createDiscount();

        // Assert
        String consoleOutput = outContent.toString();
        assertTrue(consoleOutput.contains("Error: Invalid number format for discount value."));
        verify(mockScanner, times(6)).nextLine(); // 1(name) + 1(type) + 1(invalid val) + 1(valid val) + 2(dates) = 6
    }

    @Test
    @DisplayName("Should re-prompt for discount value if negative then succeed")
    void createDiscount_NegativeValueThenValid() {
        // Arrange
        String validName = "Sale";
        String validType = "AMOUNT";
        String validValue = "5.0";
        when(mockScanner.nextLine())
                .thenReturn(validName)
                .thenReturn(validType)
                .thenReturn("-10.0") // Invalid value (negative)
                .thenReturn(validValue) // Valid value
                .thenReturn("2025-06-01")
                .thenReturn("2025-06-30");

        when(mockDiscountRepo.createDiscount(any(), any(), anyDouble(), any(), any())).thenReturn(1);

        // Act
        discountCreationService.createDiscount();

        // Assert
        String consoleOutput = outContent.toString();
        assertTrue(consoleOutput.contains("Error: Discount value must be non-negative."));
        verify(mockScanner, times(6)).nextLine();
    }

    @Test
    @DisplayName("Should re-prompt for percentage value if greater than 100 then succeed")
    void createDiscount_PercentageGreaterThan100ThenValid() {
        // Arrange
        String validName = "Big Sale";
        String validType = "PERCENT";
        String validValue = "20.0";
        when(mockScanner.nextLine())
                .thenReturn(validName)
                .thenReturn(validType)
                .thenReturn("120") // Invalid percentage
                .thenReturn(validValue) // Valid percentage
                .thenReturn("2025-06-01")
                .thenReturn("2025-06-30");

        when(mockDiscountRepo.createDiscount(any(), any(), anyDouble(), any(), any())).thenReturn(1);

        // Act
        discountCreationService.createDiscount();

        // Assert
        String consoleOutput = outContent.toString();
        assertTrue(consoleOutput.contains("Error: Percentage cannot exceed 100."));
        verify(mockScanner, times(6)).nextLine();
    }

    // --- Test Cases for Date Validation ---

    @Test
    @DisplayName("Should re-prompt for start date if invalid format then succeed")
    void createDiscount_InvalidStartDateFormatThenValid() {
        // Arrange
        String validName = "Sale";
        String validType = "PERCENT";
        String validValue = "10";
        String validStartDate = "2025-06-01";
        String validEndDate = "2025-06-30";
        when(mockScanner.nextLine())
                .thenReturn(validName)
                .thenReturn(validType)
                .thenReturn(validValue)
                .thenReturn("01-06-2025") // Invalid format
                .thenReturn(validStartDate) // Valid format
                .thenReturn(validEndDate);

        when(mockDiscountRepo.createDiscount(any(), any(), anyDouble(), any(), any())).thenReturn(1);

        // Act
        discountCreationService.createDiscount();

        // Assert
        String consoleOutput = outContent.toString();
        assertTrue(consoleOutput.contains("Error: Invalid start date format. Please use YYYY-MM-DD."));
        verify(mockScanner, times(6)).nextLine();
    }

    @Test
    @DisplayName("Should re-prompt for end date if invalid format then succeed")
    void createDiscount_InvalidEndDateFormatThenValid() {
        // Arrange
        String validName = "Sale";
        String validType = "PERCENT";
        String validValue = "10";
        String validStartDate = "2025-06-01";
        String validEndDate = "2025-06-30";
        when(mockScanner.nextLine())
                .thenReturn(validName)
                .thenReturn(validType)
                .thenReturn(validValue)
                .thenReturn(validStartDate)
                .thenReturn("01/06/2025") // Invalid format
                .thenReturn(validEndDate); // Valid format

        when(mockDiscountRepo.createDiscount(any(), any(), anyDouble(), any(), any())).thenReturn(1);

        // Act
        discountCreationService.createDiscount();

        // Assert
        String consoleOutput = outContent.toString();
        assertTrue(consoleOutput.contains("Error: Invalid end date format. Please use YYYY-MM-DD."));
        verify(mockScanner, times(6)).nextLine();
    }

    @Test
    @DisplayName("Should re-prompt for end date if before start date then succeed")
    void createDiscount_EndDateBeforeStartDateThenValid() {
        // Arrange
        String validName = "Sale";
        String validType = "PERCENT";
        String validValue = "10";
        String startDate = "2025-06-10";
        String invalidEndDate = "2025-06-01"; // Before start date
        String validEndDate = "2025-06-30";
        when(mockScanner.nextLine())
                .thenReturn(validName)
                .thenReturn(validType)
                .thenReturn(validValue)
                .thenReturn(startDate)
                .thenReturn(invalidEndDate)
                .thenReturn(validEndDate);

        when(mockDiscountRepo.createDiscount(any(), any(), anyDouble(), any(), any())).thenReturn(1);

        // Act
        discountCreationService.createDiscount();

        // Assert
        String consoleOutput = outContent.toString();
        assertTrue(consoleOutput.contains("Error: End date cannot be before start date."));
        verify(mockScanner, times(6)).nextLine();
    }

}