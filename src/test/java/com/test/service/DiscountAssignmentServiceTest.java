package com.test.service;

import com.syos.model.Discount;
import com.syos.model.Product;
import com.syos.enums.DiscountType; // Assuming you have a DiscountType enum
import com.syos.repository.DiscountRepository;
import com.syos.repository.ProductRepository;
import com.syos.service.DiscountAssignmentService;

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

class DiscountAssignmentServiceTest {

    @Mock
    private Scanner mockScanner; // Mock the Scanner for user input
    @Mock
    private DiscountRepository mockDiscountRepo; // Mock the DiscountRepository
    @Mock
    private ProductRepository mockProductRepo; // Mock the ProductRepository

    @InjectMocks
    private DiscountAssignmentService discountAssignmentService; // Inject mocks into the service

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
        reset(mockScanner, mockDiscountRepo, mockProductRepo); // Clear mock interactions after each test
    }

    // --- Test Cases for Successful Discount Assignment ---

    @Test
    @DisplayName("Should successfully assign an existing discount to a product")
    void assignDiscountToProduct_Success() {
        // Arrange
        String productCode = "PROD001";
        int discountId = 1;
        Product mockProduct = new Product(productCode, "Test Product", 100.0); // Create a mock product
        Discount mockDiscount = new Discount(discountId, "Summer Sale", DiscountType.PERCENT, 10.0,
                LocalDate.now(), LocalDate.now().plusDays(30)); // Create a mock discount

        // Configure mock behavior
        when(mockScanner.nextLine())
                .thenReturn(productCode) // For product code input
                .thenReturn(String.valueOf(discountId)); // For discount ID input

        when(mockProductRepo.findByCode(productCode)).thenReturn(mockProduct);
        when(mockDiscountRepo.findById(discountId)).thenReturn(mockDiscount);
        doNothing().when(mockDiscountRepo).linkProductToDiscount(productCode, discountId);

        // Act
        discountAssignmentService.assignDiscountToProduct();

        // Assert
        String expectedOutputPart1 = "=== Assign Existing Discount to Product ===";
        String expectedOutputPart2 = "Enter product code:";
        String expectedOutputPart3 = "Enter discount ID:";
        String expectedOutputPart4 = String.format("Selected Discount: ID %d | Name: '%s' | Type: %s | Value: %.2f | Active: %s to %s",
                mockDiscount.getId(), mockDiscount.getName(), mockDiscount.getType(),
                mockDiscount.getValue(), mockDiscount.getStart(), mockDiscount.getEnd());
        String expectedOutputPart5 = String.format("Discount ID %d assigned to product %s.%n", discountId, productCode);

        String consoleOutput = outContent.toString();
        assertTrue(consoleOutput.contains(expectedOutputPart1));
        assertTrue(consoleOutput.contains(expectedOutputPart2));
        assertTrue(consoleOutput.contains(expectedOutputPart3));
        assertTrue(consoleOutput.contains(expectedOutputPart4));
        assertTrue(consoleOutput.contains(expectedOutputPart5));

        // Verify interactions with mocks
        verify(mockScanner, times(2)).nextLine(); // Called twice for product code and discount ID
        verify(mockProductRepo, times(1)).findByCode(productCode);
        verify(mockDiscountRepo, times(1)).findById(discountId);
        verify(mockDiscountRepo, times(1)).linkProductToDiscount(productCode, discountId);
    }

    // --- Test Cases for Invalid Product Code ---

    @Test
    @DisplayName("Should display error for empty product code")
    void assignDiscountToProduct_EmptyProductCode() {
        // Arrange
        when(mockScanner.nextLine()).thenReturn(""); // Empty input for product code

        // Act
        discountAssignmentService.assignDiscountToProduct();

        // Assert
        assertTrue(outContent.toString().contains("Product code cannot be empty."));

        // Verify no further interactions
        verify(mockScanner, times(1)).nextLine();
        verifyNoInteractions(mockProductRepo);
        verifyNoInteractions(mockDiscountRepo);
    }

    @Test
    @DisplayName("Should display error for blank product code")
    void assignDiscountToProduct_BlankProductCode() {
        // Arrange
        when(mockScanner.nextLine()).thenReturn("   "); // Blank input for product code

        // Act
        discountAssignmentService.assignDiscountToProduct();

        // Assert
        assertTrue(outContent.toString().contains("Product code cannot be empty."));

        // Verify no further interactions
        verify(mockScanner, times(1)).nextLine();
        verifyNoInteractions(mockProductRepo);
        verifyNoInteractions(mockDiscountRepo);
    }

    @Test
    @DisplayName("Should display error if product is not found")
    void assignDiscountToProduct_ProductNotFound() {
        // Arrange
        String productCode = "NONEXISTENT";
        when(mockScanner.nextLine()).thenReturn(productCode); // Product code input
        when(mockProductRepo.findByCode(productCode)).thenReturn(null); // Product not found

        // Act
        discountAssignmentService.assignDiscountToProduct();

        // Assert
        assertTrue(outContent.toString().contains("No such product: " + productCode));

        // Verify interactions
        verify(mockScanner, times(1)).nextLine();
        verify(mockProductRepo, times(1)).findByCode(productCode);
        verifyNoMoreInteractions(mockScanner); // Ensure scanner not called for discount ID
        verifyNoInteractions(mockDiscountRepo);
    }

    // --- Test Cases for Invalid Discount ID ---

    @Test
    @DisplayName("Should display error for invalid discount ID format (non-numeric)")
    void assignDiscountToProduct_InvalidDiscountIdFormat() {
        // Arrange
        String productCode = "PROD001";
        Product mockProduct = new Product(productCode, "Test Product", 100.0);

        when(mockScanner.nextLine())
                .thenReturn(productCode) // For product code input
                .thenReturn("abc"); // For discount ID input (non-numeric)

        when(mockProductRepo.findByCode(productCode)).thenReturn(mockProduct);

        // Act
        discountAssignmentService.assignDiscountToProduct();

        // Assert
        assertTrue(outContent.toString().contains("Invalid discount ID format. Please enter a number."));

        // Verify interactions
        verify(mockScanner, times(2)).nextLine();
        verify(mockProductRepo, times(1)).findByCode(productCode);
        verifyNoInteractions(mockDiscountRepo); // Discount repo should not be called
    }

    @Test
    @DisplayName("Should display error for non-positive discount ID")
    void assignDiscountToProduct_NonPositiveDiscountId() {
        // Arrange
        String productCode = "PROD001";
        Product mockProduct = new Product(productCode, "Test Product", 100.0);

        when(mockScanner.nextLine())
                .thenReturn(productCode) // For product code input
                .thenReturn("0"); // For discount ID input (non-positive)

        when(mockProductRepo.findByCode(productCode)).thenReturn(mockProduct);

        // Act
        discountAssignmentService.assignDiscountToProduct();

        // Assert
        assertTrue(outContent.toString().contains("Discount ID must be a positive number."));

        // Verify interactions
        verify(mockScanner, times(2)).nextLine();
        verify(mockProductRepo, times(1)).findByCode(productCode);
        verifyNoInteractions(mockDiscountRepo); // Discount repo should not be called
    }

    @Test
    @DisplayName("Should display error if discount is not found")
    void assignDiscountToProduct_DiscountNotFound() {
        // Arrange
        String productCode = "PROD001";
        int discountId = 99;
        Product mockProduct = new Product(productCode, "Test Product", 100.0);

        when(mockScanner.nextLine())
                .thenReturn(productCode)
                .thenReturn(String.valueOf(discountId));

        when(mockProductRepo.findByCode(productCode)).thenReturn(mockProduct);
        when(mockDiscountRepo.findById(discountId)).thenReturn(null); // Discount not found

        // Act
        discountAssignmentService.assignDiscountToProduct();

        // Assert
        assertTrue(outContent.toString().contains("No discount found with ID: " + discountId));
        assertTrue(outContent.toString().contains("Please create the discount first using 'Create new discount' option."));

        // Verify interactions
        verify(mockScanner, times(2)).nextLine();
        verify(mockProductRepo, times(1)).findByCode(productCode);
        verify(mockDiscountRepo, times(1)).findById(discountId);
        verify(mockDiscountRepo, never()).linkProductToDiscount(anyString(), anyInt()); // Link should not be called
    }

    // --- Test Cases for Repository Exceptions ---

    @Test
    @DisplayName("Should handle RuntimeException from productRepo.findByCode")
    void assignDiscountToProduct_ProductRepoFindThrowsException() {
        // Arrange
        String productCode = "PROD001";
        when(mockScanner.nextLine()).thenReturn(productCode);
        when(mockProductRepo.findByCode(productCode)).thenThrow(new RuntimeException("DB error during product lookup"));

        // Act & Assert
        // We expect the RuntimeException to propagate as the service doesn't catch it
        // However, if the service wraps it, we'd assert on the wrapper exception.
        // As per the provided service, it doesn't wrap or handle this specific exception.
        // So, this test will pass if the RuntimeException is thrown.
        RuntimeException thrown = assertThrows(RuntimeException.class, () ->
                discountAssignmentService.assignDiscountToProduct());

        assertEquals("DB error during product lookup", thrown.getMessage());

        // Verify interactions
        verify(mockScanner, times(1)).nextLine();
        verify(mockProductRepo, times(1)).findByCode(productCode);
        verifyNoInteractions(mockDiscountRepo);
    }


    @Test
    @DisplayName("Should handle RuntimeException from discountRepo.findById")
    void assignDiscountToProduct_DiscountRepoFindByIdThrowsException() {
        // Arrange
        String productCode = "PROD001";
        int discountId = 1;
        Product mockProduct = new Product(productCode, "Test Product", 100.0);

        when(mockScanner.nextLine())
                .thenReturn(productCode)
                .thenReturn(String.valueOf(discountId));

        when(mockProductRepo.findByCode(productCode)).thenReturn(mockProduct);
        when(mockDiscountRepo.findById(discountId)).thenThrow(new RuntimeException("DB error during discount lookup"));

        // Act & Assert
        // We expect the RuntimeException to propagate
        RuntimeException thrown = assertThrows(RuntimeException.class, () ->
                discountAssignmentService.assignDiscountToProduct());

        assertEquals("DB error during discount lookup", thrown.getMessage());

        // Verify interactions
        verify(mockScanner, times(2)).nextLine();
        verify(mockProductRepo, times(1)).findByCode(productCode);
        verify(mockDiscountRepo, times(1)).findById(discountId);
        verify(mockDiscountRepo, never()).linkProductToDiscount(anyString(), anyInt());
    }

    @Test
    @DisplayName("Should handle RuntimeException during discount assignment (linkProductToDiscount)")
    void assignDiscountToProduct_LinkProductToDiscountThrowsException() {
        // Arrange
        String productCode = "PROD001";
        int discountId = 1;
        Product mockProduct = new Product(productCode, "Test Product", 100.0);
        Discount mockDiscount = new Discount(discountId, "Summer Sale", DiscountType.PERCENT, 10.0,
                LocalDate.now(), LocalDate.now().plusDays(30));

        when(mockScanner.nextLine())
                .thenReturn(productCode)
                .thenReturn(String.valueOf(discountId));

        when(mockProductRepo.findByCode(productCode)).thenReturn(mockProduct);
        when(mockDiscountRepo.findById(discountId)).thenReturn(mockDiscount);
        doThrow(new RuntimeException("Failed to update database")).when(mockDiscountRepo).linkProductToDiscount(productCode, discountId);

        // Act
        discountAssignmentService.assignDiscountToProduct();

        // Assert
        assertTrue(outContent.toString().contains("Failed to assign discount: Failed to update database"));

        // Verify interactions
        verify(mockScanner, times(2)).nextLine();
        verify(mockProductRepo, times(1)).findByCode(productCode);
        verify(mockDiscountRepo, times(1)).findById(discountId);
        verify(mockDiscountRepo, times(1)).linkProductToDiscount(productCode, discountId); // Called once
    }
}