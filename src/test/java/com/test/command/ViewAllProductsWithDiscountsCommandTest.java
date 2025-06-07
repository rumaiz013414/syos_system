package com.test.command;

import com.syos.command.ViewAllProductsWithDiscountsCommand;
import com.syos.model.Product;
import com.syos.model.Discount;
import com.syos.repository.DiscountRepository;
import com.syos.repository.ProductRepository;
import com.syos.enums.DiscountType;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ViewAllProductsWithDiscountsCommandTest {

    @Mock
    private DiscountRepository discountRepository;
    @Mock
    private ProductRepository productRepository;

    private ViewAllProductsWithDiscountsCommand viewAllProductsWithDiscountsCommand;

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final String NL = System.lineSeparator(); // Use system-dependent newline

    // Define the separator line used in the command for consistency
    private final String TABLE_SEPARATOR = "------------------------------------------------------------------------------------------------------";

    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(outContent));
        viewAllProductsWithDiscountsCommand = new ViewAllProductsWithDiscountsCommand(discountRepository, productRepository);
    }

    @AfterEach
    void restoreStreams() {
        System.setOut(originalOut);
    }

    @Test
    @DisplayName("Should display 'No products registered' message when product repository is empty")
    void shouldDisplayNoProductsRegisteredMessageWhenProductRepositoryIsEmpty() {
        // Arrange
        when(productRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        viewAllProductsWithDiscountsCommand.execute();

        // Assert
        verify(productRepository, times(1)).findAll();
        // Ensure discountRepository methods are NOT called as no products exist
        verify(discountRepository, never()).findDiscountsByProductCode(anyString(), any(LocalDate.class));

        String expectedOutput = NL + "--- Products with Active Discounts ---" + NL +
                                "No products have been registered yet." + NL;
        assertEquals(expectedOutput, outContent.toString());
    }

    @Test
    @DisplayName("Should display 'No products currently have active discounts' when products exist but no active discounts are found")
    void shouldDisplayNoActiveDiscountsMessageWhenNoProductHasActiveDiscounts() {
        // Arrange
        Product product1 = new Product("P001", "Laptop", 1200.00);
        Product product2 = new Product("P002", "Mouse", 25.00);
        List<Product> products = Arrays.asList(product1, product2);

        when(productRepository.findAll()).thenReturn(products);
        // Mock that no active discounts are found for any product
        when(discountRepository.findDiscountsByProductCode(eq("P001"), any(LocalDate.class))).thenReturn(Collections.emptyList());
        when(discountRepository.findDiscountsByProductCode(eq("P002"), any(LocalDate.class))).thenReturn(Collections.emptyList());

        // Act
        viewAllProductsWithDiscountsCommand.execute();

        // Assert
        verify(productRepository, times(1)).findAll();
        verify(discountRepository, times(1)).findDiscountsByProductCode(eq("P001"), any(LocalDate.class));
        verify(discountRepository, times(1)).findDiscountsByProductCode(eq("P002"), any(LocalDate.class));

        String expectedOutput = NL + "--- Products with Active Discounts ---" + NL +
                                "No products currently have active discounts." + NL;
        assertEquals(expectedOutput, outContent.toString());
    }

    @Test
    @DisplayName("Should display products with active discounts correctly, including mixed types and multiple discounts")
    void shouldDisplayProductsWithActiveDiscountsCorrectly() {
        // Arrange
        Product product1 = new Product("P001", "Laptop", 1200.00);
        Product product2 = new Product("P002", "Mouse", 25.00);
        Product product3 = new Product("P003", "Keyboard", 75.00); // This product will have no discounts

        List<Product> products = Arrays.asList(product1, product2, product3);

        // Discounts for Product 1
        Discount discount1_p1 = new Discount(1, "Summer Sale", DiscountType.PERCENT, 10.5, LocalDate.now().minusDays(5), LocalDate.now().plusDays(5));
        Discount discount2_p1 = new Discount(2, "Student Offer", DiscountType.AMOUNT, 50.0, LocalDate.now().minusDays(2), LocalDate.now().plusDays(10));
        List<Discount> discounts_p1 = Arrays.asList(discount1_p1, discount2_p1);

        // Discounts for Product 2
        Discount discount1_p2 = new Discount(3, "Weekend Deal", DiscountType.PERCENT, 5.0, LocalDate.now().minusDays(1), LocalDate.now().plusDays(1));
        List<Discount> discounts_p2 = Collections.singletonList(discount1_p2);

        // Product 3 has no active discounts
        List<Discount> discounts_p3 = Collections.emptyList();

        when(productRepository.findAll()).thenReturn(products);
        when(discountRepository.findDiscountsByProductCode(eq("P001"), any(LocalDate.class))).thenReturn(discounts_p1);
        when(discountRepository.findDiscountsByProductCode(eq("P002"), any(LocalDate.class))).thenReturn(discounts_p2);
        when(discountRepository.findDiscountsByProductCode(eq("P003"), any(LocalDate.class))).thenReturn(discounts_p3);

        // Act
        viewAllProductsWithDiscountsCommand.execute();

        // Assert
        verify(productRepository, times(1)).findAll();
        verify(discountRepository, times(1)).findDiscountsByProductCode(eq("P001"), any(LocalDate.class));
        verify(discountRepository, times(1)).findDiscountsByProductCode(eq("P002"), any(LocalDate.class));
        verify(discountRepository, times(1)).findDiscountsByProductCode(eq("P003"), any(LocalDate.class)); // Even if it has no discounts, findDiscountsByProductCode is called

        StringBuilder expectedOutput = new StringBuilder();
        expectedOutput.append(NL).append("--- Products with Active Discounts ---").append(NL);
        expectedOutput.append(String.format("%-15s %-30s %-10s %s%n", "Product Code", "Product Name", "Price", "Active Discounts"));
        expectedOutput.append(TABLE_SEPARATOR).append(NL);

        // Expected output for Product 1
        String discountsDisplay_p1 = "Summer Sale (10.50%); Student Offer (50.00)";
        expectedOutput.append(String.format("%-15s %-30s %-10.2f %s%n",
                product1.getCode(), product1.getName(), product1.getPrice(), discountsDisplay_p1));

        // Expected output for Product 2
        String discountsDisplay_p2 = "Weekend Deal (5.00%)";
        expectedOutput.append(String.format("%-15s %-30s %-10.2f %s%n",
                product2.getCode(), product2.getName(), product2.getPrice(), discountsDisplay_p2));
        
        expectedOutput.append(TABLE_SEPARATOR).append(NL);

        assertEquals(expectedOutput.toString(), outContent.toString());
    }

    @Test
    @DisplayName("Should display all products with active discounts when all products have them")
    void shouldDisplayAllProductsWhenAllHaveDiscounts() {
        // Arrange
        Product product1 = new Product("P001", "Desk Chair", 150.00);
        Product product2 = new Product("P002", "Monitor", 300.00);
        List<Product> products = Arrays.asList(product1, product2);

        // Discounts for Product 1
        Discount discount1_p1 = new Discount(1, "Office Sale", DiscountType.PERCENT, 20.0, LocalDate.now().minusDays(5), LocalDate.now().plusDays(5));
        List<Discount> discounts_p1 = Collections.singletonList(discount1_p1);

        // Discounts for Product 2
        Discount discount1_p2 = new Discount(2, "Tech Deal", DiscountType.AMOUNT, 20.0, LocalDate.now().minusDays(1), LocalDate.now().plusDays(1));
        List<Discount> discounts_p2 = Collections.singletonList(discount1_p2);

        when(productRepository.findAll()).thenReturn(products);
        when(discountRepository.findDiscountsByProductCode(eq("P001"), any(LocalDate.class))).thenReturn(discounts_p1);
        when(discountRepository.findDiscountsByProductCode(eq("P002"), any(LocalDate.class))).thenReturn(discounts_p2);

        // Act
        viewAllProductsWithDiscountsCommand.execute();

        // Assert
        verify(productRepository, times(1)).findAll();
        verify(discountRepository, times(1)).findDiscountsByProductCode(eq("P001"), any(LocalDate.class));
        verify(discountRepository, times(1)).findDiscountsByProductCode(eq("P002"), any(LocalDate.class));

        StringBuilder expectedOutput = new StringBuilder();
        expectedOutput.append(NL).append("--- Products with Active Discounts ---").append(NL);
        expectedOutput.append(String.format("%-15s %-30s %-10s %s%n", "Product Code", "Product Name", "Price", "Active Discounts"));
        expectedOutput.append(TABLE_SEPARATOR).append(NL);

        // Expected output for Product 1
        String discountsDisplay_p1 = "Office Sale (20.00%)";
        expectedOutput.append(String.format("%-15s %-30s %-10.2f %s%n",
                product1.getCode(), product1.getName(), product1.getPrice(), discountsDisplay_p1));

        // Expected output for Product 2
        String discountsDisplay_p2 = "Tech Deal (20.00)";
        expectedOutput.append(String.format("%-15s %-30s %-10.2f %s%n",
                product2.getCode(), product2.getName(), product2.getPrice(), discountsDisplay_p2));
        
        expectedOutput.append(TABLE_SEPARATOR).append(NL);

        assertEquals(expectedOutput.toString(), outContent.toString());
    }
}