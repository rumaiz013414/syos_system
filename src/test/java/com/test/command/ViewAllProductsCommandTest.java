package com.test.command;

import com.syos.command.ViewAllProductsCommand;
import com.syos.model.Product;
import com.syos.repository.ProductRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ViewAllProductsCommandTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private Scanner scanner; // Scanner is injected but not used in this command's execute()

    private ViewAllProductsCommand viewAllProductsCommand;

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final String NL = System.lineSeparator(); // Use system-dependent newline

    // Adjusted separator string for consistency with printf output width (52 hyphens)
    private final String SEPARATOR_LINE = "----------------------------------------------------";

    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(outContent));
        viewAllProductsCommand = new ViewAllProductsCommand(productRepository, scanner);
    }

    @AfterEach
    void restoreStreams() {
        System.setOut(originalOut);
    }

    @Test
    @DisplayName("Should display message when no products are found in the system")
    void shouldDisplayNoProductsMessageWhenNoProductsExist() {
        // Arrange
        when(productRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        viewAllProductsCommand.execute();

        // Assert
        verify(productRepository, times(1)).findAll();
        String expectedOutput = NL + "--- Viewing All Products ---" + NL +
                                "No products found in the system." + NL;
        assertEquals(expectedOutput, outContent.toString());
    }

    @Test
    @DisplayName("Should display products correctly when multiple products exist")
    void shouldDisplayProductsCorrectlyWhenMultipleProductsExist() {
        // Arrange
        Product product1 = new Product("P001", "Laptop", 1200.50);
        Product product2 = new Product("P002", "Mouse", 25.00);
        Product product3 = new Product("P003", "Keyboard (Gaming)", 75.99); // Longer name
        List<Product> products = Arrays.asList(product1, product2, product3);
        
        when(productRepository.findAll()).thenReturn(products);

        // Act
        viewAllProductsCommand.execute();

        // Assert
        verify(productRepository, times(1)).findAll();

        StringBuilder expectedOutput = new StringBuilder();
        expectedOutput.append(NL).append("--- Viewing All Products ---").append(NL);
        expectedOutput.append(SEPARATOR_LINE).append(NL);
        expectedOutput.append(String.format("%-15s %-25s %-10s%n", "Product Code", "Product Name", "Price (LKR)"));
        expectedOutput.append(SEPARATOR_LINE).append(NL);

        expectedOutput.append(String.format("%-15s %-25s %-10.2f%n",
                product1.getCode(), product1.getName(), product1.getPrice()));
        expectedOutput.append(String.format("%-15s %-25s %-10.2f%n",
                product2.getCode(), product2.getName(), product2.getPrice()));
        expectedOutput.append(String.format("%-15s %-25s %-10.2f%n",
                product3.getCode(), product3.getName(), product3.getPrice()));
        
        expectedOutput.append(SEPARATOR_LINE).append(NL);

        assertEquals(expectedOutput.toString(), outContent.toString());
    }

    @Test
    @DisplayName("Should display a single product correctly")
    void shouldDisplaySingleProductCorrectly() {
        // Arrange
        Product singleProduct = new Product("P004", "Webcam HD", 49.99);
        List<Product> products = Collections.singletonList(singleProduct);
        
        when(productRepository.findAll()).thenReturn(products);

        // Act
        viewAllProductsCommand.execute();

        // Assert
        verify(productRepository, times(1)).findAll();

        StringBuilder expectedOutput = new StringBuilder();
        expectedOutput.append(NL).append("--- Viewing All Products ---").append(NL);
        expectedOutput.append(SEPARATOR_LINE).append(NL);
        expectedOutput.append(String.format("%-15s %-25s %-10s%n", "Product Code", "Product Name", "Price (LKR)"));
        expectedOutput.append(SEPARATOR_LINE).append(NL);

        expectedOutput.append(String.format("%-15s %-25s %-10.2f%n",
                singleProduct.getCode(), singleProduct.getName(), singleProduct.getPrice()));
        
        expectedOutput.append(SEPARATOR_LINE).append(NL);

        assertEquals(expectedOutput.toString(), outContent.toString());
    }

    @Test
    @DisplayName("Should display error message when repository throws a RuntimeException")
    void shouldDisplayErrorMessageWhenRepositoryThrowsException() {
        // Arrange
        String errorMessage = "Database connection failed";
        when(productRepository.findAll()).thenThrow(new RuntimeException(errorMessage));

        // Act
        viewAllProductsCommand.execute();

        // Assert
        verify(productRepository, times(1)).findAll();
        String expectedOutput = NL + "--- Viewing All Products ---" + NL +
                                "Error retrieving products: " + errorMessage + NL;
        assertEquals(expectedOutput, outContent.toString());
    }
}