package com.test.command;

import com.syos.command.AssignDiscountCommand;
import com.syos.enums.DiscountType;
import com.syos.model.Discount;
import com.syos.model.Product;
import com.syos.repository.DiscountRepository;
import com.syos.repository.ProductRepository;
import org.junit.jupiter.api.*;
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
class AssignDiscountCommandTest {

    @Mock private Scanner scanner;
    @Mock private DiscountRepository discountRepository;
    @Mock private ProductRepository productRepository;

    private AssignDiscountCommand assignDiscountCommand;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(outContent));
        assignDiscountCommand = new AssignDiscountCommand(scanner, discountRepository, productRepository);
    }

    @AfterEach
    void restoreStreams() {
        System.setOut(originalOut);
    }

    @Test
    @DisplayName("Should successfully assign an existing discount with PERCENT type to a product")
    void shouldSuccessfullyAssignPercentDiscount() {
        String productCode = "P123";
        int discountId = 1;
        Product product = new Product(productCode, "Laptop", 1200.0);
        Discount discount = new Discount(discountId, "Summer Sale", DiscountType.PERCENT, 10.0,
                LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 30));

        when(scanner.nextLine()).thenReturn(productCode).thenReturn(String.valueOf(discountId));
        when(productRepository.findByCode(productCode)).thenReturn(product);
        when(discountRepository.findById(discountId)).thenReturn(discount);

        assignDiscountCommand.execute();

        String output = outContent.toString();
        assertTrue(output.contains("Discount ID 1 assigned to product P123."));
    }

    @Test
    @DisplayName("Should successfully assign an existing discount with AMOUNT type to a product")
    void shouldSuccessfullyAssignAmountDiscount() {
        String productCode = "P456";
        int discountId = 2;
        Product product = new Product(productCode, "Mouse", 25.0);
        Discount discount = new Discount(discountId, "Flat $5 Off", DiscountType.AMOUNT, 5.0,
                LocalDate.of(2025, 7, 1), LocalDate.of(2025, 7, 31));

        when(scanner.nextLine()).thenReturn(productCode).thenReturn(String.valueOf(discountId));
        when(productRepository.findByCode(productCode)).thenReturn(product);
        when(discountRepository.findById(discountId)).thenReturn(discount);

        assignDiscountCommand.execute();

        String output = outContent.toString();
        assertTrue(output.contains("Discount ID 2 assigned to product P456."));
    }

    @Test
    @DisplayName("Should handle empty product code input")
    void shouldHandleEmptyProductCode() {
        when(scanner.nextLine()).thenReturn("");

        assignDiscountCommand.execute();

        String output = outContent.toString();
        assertTrue(output.contains("Product code cannot be empty."));
    }

    @Test
    @DisplayName("Should handle product code not found")
    void shouldHandleProductCodeNotFound() {
        String productCode = "INVALID";
        when(scanner.nextLine()).thenReturn(productCode);
        when(productRepository.findByCode(productCode)).thenReturn(null);

        assignDiscountCommand.execute();

        String output = outContent.toString();
        assertTrue(output.contains("No such product: " + productCode));
    }

    @Test
    @DisplayName("Should handle invalid discount ID format")
    void shouldHandleInvalidDiscountIdFormat() {
        String productCode = "P123";
        Product product = new Product(productCode, "Laptop", 1200.0);

        when(scanner.nextLine()).thenReturn(productCode).thenReturn("abc");
        when(productRepository.findByCode(productCode)).thenReturn(product);

        assignDiscountCommand.execute();

        String output = outContent.toString();
        assertTrue(output.contains("Invalid discount ID format. Please enter a number."));
    }

    @Test
    @DisplayName("Should handle non-positive discount ID")
    void shouldHandleNonPositiveDiscountId() {
        String productCode = "P123";
        Product product = new Product(productCode, "Laptop", 1200.0);

        when(scanner.nextLine()).thenReturn(productCode).thenReturn("0");
        when(productRepository.findByCode(productCode)).thenReturn(product);

        assignDiscountCommand.execute();

        String output = outContent.toString();
        assertTrue(output.contains("Discount ID must be a positive number."));
    }

    @Test
    @DisplayName("Should handle discount ID not found")
    void shouldHandleDiscountIdNotFound() {
        String productCode = "P123";
        int discountId = 999;
        Product product = new Product(productCode, "Laptop", 1200.0);

        when(scanner.nextLine()).thenReturn(productCode).thenReturn(String.valueOf(discountId));
        when(productRepository.findByCode(productCode)).thenReturn(product);
        when(discountRepository.findById(discountId)).thenReturn(null);

        assignDiscountCommand.execute();

        String output = outContent.toString();
        assertTrue(output.contains("No discount found with ID: " + discountId));
        assertTrue(output.contains("Please create the discount first"));
    }

    @Test
    @DisplayName("Should handle exception during discount assignment")
    void shouldHandleLinkProductToDiscountException() {
        String productCode = "P123";
        int discountId = 1;
        Product product = new Product(productCode, "Laptop", 1200.0);
        Discount discount = new Discount(discountId, "Test Discount", DiscountType.PERCENT, 5.0,
                LocalDate.now(), LocalDate.now().plusDays(7));

        when(scanner.nextLine()).thenReturn(productCode).thenReturn(String.valueOf(discountId));
        when(productRepository.findByCode(productCode)).thenReturn(product);
        when(discountRepository.findById(discountId)).thenReturn(discount);
        doThrow(new RuntimeException("Conflict")).when(discountRepository).linkProductToDiscount(productCode, discountId);

        assignDiscountCommand.execute();

        String output = outContent.toString();
        assertTrue(output.contains("Failed to assign discount: Conflict"));
    }


    

 
}
