package com.test.command;

import com.syos.command.UnassignDiscountCommand;
import com.syos.model.Discount;
import com.syos.model.Product; // Correct Product model import
import com.syos.repository.DiscountRepository;
import com.syos.repository.ProductRepository;
import com.syos.enums.DiscountType; // Make sure to import DiscountType enum

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
class UnassignDiscountCommandTest {

    @Mock
    private Scanner scanner;
    @Mock
    private DiscountRepository discountRepository;
    @Mock
    private ProductRepository productRepository;

    private UnassignDiscountCommand unassignDiscountCommand;

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    void setUp() {
        // Redirect System.out to capture console output
        System.setOut(new PrintStream(outContent));
        unassignDiscountCommand = new UnassignDiscountCommand(scanner, discountRepository, productRepository);
    }

    @AfterEach
    void restoreStreams() {
        // Restore original System.out after each test
        System.setOut(originalOut);
    }

    // --- Happy Path Tests ---

    @Test
    @DisplayName("Should successfully unassign an active discount from a product")
    void shouldSuccessfullyUnassignDiscount() {
        // Arrange
        String productCode = "PROD001";
        // CORRECTED Product constructor call
        Product product = new Product(productCode, "Laptop", 1200.0);
        int discountIdToUnassign = 1;
        Discount discountToUnassign = new Discount(discountIdToUnassign, "Summer Sale", DiscountType.PERCENT, 15.0, LocalDate.now().minusDays(5), LocalDate.now().plusDays(5));
        Discount otherActiveDiscount = new Discount(2, "Student Discount", DiscountType.AMOUNT, 50.0, LocalDate.now().minusDays(10), LocalDate.now().plusDays(10));
        List<Discount> activeDiscounts = Arrays.asList(discountToUnassign, otherActiveDiscount);

        when(scanner.nextLine())
                .thenReturn(productCode)
                .thenReturn(String.valueOf(discountIdToUnassign)); // User enters ID to unassign

        when(productRepository.findByCode(productCode)).thenReturn(product);
        when(discountRepository.findDiscountsByProductCode(productCode, LocalDate.now())).thenReturn(activeDiscounts);
        when(discountRepository.findById(discountIdToUnassign)).thenReturn(discountToUnassign);
        when(discountRepository.unassignDiscountFromProduct(productCode, discountIdToUnassign)).thenReturn(true);

        // Act
        unassignDiscountCommand.execute();

        // Assert
        verify(productRepository, times(1)).findByCode(productCode);
        verify(discountRepository, times(1)).findDiscountsByProductCode(productCode, LocalDate.now());
        verify(discountRepository, times(1)).findById(discountIdToUnassign);
        verify(discountRepository, times(1)).unassignDiscountFromProduct(productCode, discountIdToUnassign);

        String output = outContent.toString();
        assertTrue(output.contains("--- Unassign Discount from Product ---"));
        assertTrue(output.contains("Enter Product Code:"));
        assertTrue(output.contains("Active discounts for " + product.getName() + " (" + productCode + "):"));
        assertTrue(output.contains(String.format("%-5d %-20s", discountToUnassign.getId(), discountToUnassign.getName()))); // Check if discount was listed
        assertTrue(output.contains("Enter Discount ID to unassign:"));
        assertTrue(output.contains("Discount '" + discountToUnassign.getName() + "' (ID: " + discountIdToUnassign + ") successfully unassigned from product '" + product.getName() + "' (" + productCode + ")."));
    }

    // --- Product Validation Tests ---

    @Test
    @DisplayName("Should display error if product is not found")
    void shouldDisplayErrorIfProductNotFound() {
        // Arrange
        String productCode = "NONEXISTENT_PROD";
        when(scanner.nextLine()).thenReturn(productCode);
        when(productRepository.findByCode(productCode)).thenReturn(null);

        // Act
        unassignDiscountCommand.execute();

        // Assert
        verify(productRepository, times(1)).findByCode(productCode);
        verify(discountRepository, never()).findDiscountsByProductCode(anyString(), any(LocalDate.class)); // Should not proceed
        verify(scanner, times(1)).nextLine(); // Only product code input
        
        String output = outContent.toString();
        assertTrue(output.contains("Error: Product with code '" + productCode + "' not found."));
    }

    @Test
    @DisplayName("Should handle product code with leading/trailing spaces correctly")
    void shouldHandleProductCodeWithSpaces() {
        // Arrange
        String productCodeWithSpaces = "  PROD002  ";
        String expectedProductCode = "PROD002"; // After trimming
        // CORRECTED Product constructor call
        Product product = new Product(expectedProductCode, "Keyboard", 75.0);
        int discountId = 1;
        Discount discount = new Discount(discountId, "Black Friday", DiscountType.PERCENT, 20.0, LocalDate.now().minusDays(1), LocalDate.now().plusDays(1));
        List<Discount> activeDiscounts = Collections.singletonList(discount);

        when(scanner.nextLine())
                .thenReturn(productCodeWithSpaces)
                .thenReturn(String.valueOf(discountId));

        when(productRepository.findByCode(expectedProductCode)).thenReturn(product);
        when(discountRepository.findDiscountsByProductCode(expectedProductCode, LocalDate.now())).thenReturn(activeDiscounts);
        when(discountRepository.findById(discountId)).thenReturn(discount);
        when(discountRepository.unassignDiscountFromProduct(expectedProductCode, discountId)).thenReturn(true);

        // Act
        unassignDiscountCommand.execute();

        // Assert
        verify(productRepository, times(1)).findByCode(expectedProductCode); // Verify trimmed code used
        verify(discountRepository, times(1)).unassignDiscountFromProduct(expectedProductCode, discountId);
        assertTrue(outContent.toString().contains("successfully unassigned"));
    }

    // --- No Active Discounts Tests ---

    @Test
    @DisplayName("Should inform user if product has no active discounts")
    void shouldInformIfNoActiveDiscounts() {
        // Arrange
        String productCode = "PROD003";
        // CORRECTED Product constructor call
        Product product = new Product(productCode, "Mouse", 25.0);

        when(scanner.nextLine()).thenReturn(productCode);
        when(productRepository.findByCode(productCode)).thenReturn(product);
        when(discountRepository.findDiscountsByProductCode(productCode, LocalDate.now())).thenReturn(Collections.emptyList()); // No active discounts

        // Act
        unassignDiscountCommand.execute();

        // Assert
        verify(productRepository, times(1)).findByCode(productCode);
        verify(discountRepository, times(1)).findDiscountsByProductCode(productCode, LocalDate.now());
        verify(discountRepository, never()).findById(anyInt()); // Should not proceed to ask for discount ID
        verify(scanner, times(1)).nextLine(); // Only product code input

        String output = outContent.toString();
        assertTrue(output.contains("Product '" + product.getName() + "' (" + productCode + ") currently has no active discounts to unassign."));
    }

    // --- Discount ID Input Validation Tests ---

    @Test
    @DisplayName("Should display error for invalid (non-numeric) Discount ID input")
    void shouldDisplayErrorForInvalidDiscountIdInput() {
        // Arrange
        String productCode = "PROD004";
        // CORRECTED Product constructor call
        Product product = new Product(productCode, "Monitor", 300.0);
        List<Discount> activeDiscounts = Collections.singletonList(new Discount(1, "Test Discount", DiscountType.PERCENT, 10.0, LocalDate.now().minusDays(1), LocalDate.now().plusDays(1)));

        when(scanner.nextLine())
                .thenReturn(productCode)
                .thenReturn("invalid_id"); // Non-numeric discount ID

        when(productRepository.findByCode(productCode)).thenReturn(product);
        when(discountRepository.findDiscountsByProductCode(productCode, LocalDate.now())).thenReturn(activeDiscounts);

        // Act
        unassignDiscountCommand.execute();

        // Assert
        verify(productRepository, times(1)).findByCode(productCode);
        verify(discountRepository, times(1)).findDiscountsByProductCode(productCode, LocalDate.now());
        verify(discountRepository, never()).findById(anyInt()); // Should not attempt to find discount
        verify(discountRepository, never()).unassignDiscountFromProduct(anyString(), anyInt()); // Should not attempt to unassign

        String output = outContent.toString();
        assertTrue(output.contains("Invalid Discount ID. Please enter a number."));
    }

    @Test
    @DisplayName("Should handle discount ID with leading/trailing spaces correctly")
    void shouldHandleDiscountIdWithSpaces() {
        // Arrange
        String productCode = "PROD005";
        // CORRECTED Product constructor call
        Product product = new Product(productCode, "Webcam", 50.0);
        String discountIdWithSpaces = "  123  ";
        int expectedDiscountId = 123; // After trimming
        Discount discount = new Discount(expectedDiscountId, "Flash Sale", DiscountType.AMOUNT, 5.0, LocalDate.now().minusDays(1), LocalDate.now().plusDays(1));
        List<Discount> activeDiscounts = Collections.singletonList(discount);

        when(scanner.nextLine())
                .thenReturn(productCode)
                .thenReturn(discountIdWithSpaces);

        when(productRepository.findByCode(productCode)).thenReturn(product);
        when(discountRepository.findDiscountsByProductCode(productCode, LocalDate.now())).thenReturn(activeDiscounts);
        when(discountRepository.findById(expectedDiscountId)).thenReturn(discount);
        when(discountRepository.unassignDiscountFromProduct(productCode, expectedDiscountId)).thenReturn(true);

        // Act
        unassignDiscountCommand.execute();

        // Assert
        verify(discountRepository, times(1)).findById(expectedDiscountId); // Verify trimmed ID used
        verify(discountRepository, times(1)).unassignDiscountFromProduct(productCode, expectedDiscountId);
        assertTrue(outContent.toString().contains("successfully unassigned"));
    }

    // --- Discount Existence and Assignment Validation Tests ---

    @Test
    @DisplayName("Should display error if discount with given ID is not found")
    void shouldDisplayErrorIfDiscountNotFound() {
        // Arrange
        String productCode = "PROD006";
        // CORRECTED Product constructor call
        Product product = new Product(productCode, "Headphones", 100.0);
        int nonExistentDiscountId = 999;
        List<Discount> activeDiscounts = Collections.singletonList(new Discount(1, "Existing Discount", DiscountType.PERCENT, 10.0, LocalDate.now().minusDays(1), LocalDate.now().plusDays(1)));

        when(scanner.nextLine())
                .thenReturn(productCode)
                .thenReturn(String.valueOf(nonExistentDiscountId));

        when(productRepository.findByCode(productCode)).thenReturn(product);
        when(discountRepository.findDiscountsByProductCode(productCode, LocalDate.now())).thenReturn(activeDiscounts);
        when(discountRepository.findById(nonExistentDiscountId)).thenReturn(null); // Discount not found

        // Act
        unassignDiscountCommand.execute();

        // Assert
        verify(productRepository, times(1)).findByCode(productCode);
        verify(discountRepository, times(1)).findDiscountsByProductCode(productCode, LocalDate.now());
        verify(discountRepository, times(1)).findById(nonExistentDiscountId);
        verify(discountRepository, never()).unassignDiscountFromProduct(anyString(), anyInt()); // Should not attempt to unassign

        String output = outContent.toString();
        assertTrue(output.contains("Error: Discount with ID " + nonExistentDiscountId + " not found."));
    }

    @Test
    @DisplayName("Should display error if discount ID is not assigned to the product")
    void shouldDisplayErrorIfDiscountNotAssignedToProduct() {
        // Arrange
        String productCode = "PROD007";
        // CORRECTED Product constructor call
        Product product = new Product(productCode, "Charger", 20.0);
        int discountIdNotAssigned = 3;
        Discount foundDiscount = new Discount(discountIdNotAssigned, "Another Store Discount", DiscountType.AMOUNT, 2.0, LocalDate.now().minusDays(5), LocalDate.now().plusDays(5));
        
        // Active discounts for PROD007, but not ID 3
        List<Discount> activeDiscounts = Collections.singletonList(new Discount(1, "Existing Discount", DiscountType.PERCENT, 10.0, LocalDate.now().minusDays(1), LocalDate.now().plusDays(1)));

        when(scanner.nextLine())
                .thenReturn(productCode)
                .thenReturn(String.valueOf(discountIdNotAssigned));

        when(productRepository.findByCode(productCode)).thenReturn(product);
        when(discountRepository.findDiscountsByProductCode(productCode, LocalDate.now())).thenReturn(activeDiscounts);
        when(discountRepository.findById(discountIdNotAssigned)).thenReturn(foundDiscount);

        // Act
        unassignDiscountCommand.execute();

        // Assert
        verify(productRepository, times(1)).findByCode(productCode);
        verify(discountRepository, times(1)).findDiscountsByProductCode(productCode, LocalDate.now());
        verify(discountRepository, times(1)).findById(discountIdNotAssigned);
        verify(discountRepository, never()).unassignDiscountFromProduct(anyString(), anyInt()); // Should not attempt to unassign

        String output = outContent.toString();
        assertTrue(output.contains("Error: Discount ID " + discountIdNotAssigned + " is not currently assigned to product '" + productCode + "'."));
    }

    // --- Unassignment Failure Test ---

    @Test
    @DisplayName("Should display failure message if unassignDiscountFromProduct returns false")
    void shouldDisplayFailureIfRepositoryUnassignFails() {
        // Arrange
        String productCode = "PROD008";
        // CORRECTED Product constructor call
        Product product = new Product(productCode, "USB Drive", 15.0);
        int discountId = 1;
        Discount discountToUnassign = new Discount(discountId, "Winter Sale", DiscountType.AMOUNT, 3.0, LocalDate.now().minusDays(5), LocalDate.now().plusDays(5));
        List<Discount> activeDiscounts = Collections.singletonList(discountToUnassign);

        when(scanner.nextLine())
                .thenReturn(productCode)
                .thenReturn(String.valueOf(discountId));

        when(productRepository.findByCode(productCode)).thenReturn(product);
        when(discountRepository.findDiscountsByProductCode(productCode, LocalDate.now())).thenReturn(activeDiscounts);
        when(discountRepository.findById(discountId)).thenReturn(discountToUnassign);
        when(discountRepository.unassignDiscountFromProduct(productCode, discountId)).thenReturn(false); // Simulate unassignment failure

        // Act
        unassignDiscountCommand.execute();

        // Assert
        verify(productRepository, times(1)).findByCode(productCode);
        verify(discountRepository, times(1)).findDiscountsByProductCode(productCode, LocalDate.now());
        verify(discountRepository, times(1)).findById(discountId);
        verify(discountRepository, times(1)).unassignDiscountFromProduct(productCode, discountId);

        String output = outContent.toString();
        assertTrue(output.contains("Failed to unassign discount. This might happen if the assignment didn't exist."));
    }
}