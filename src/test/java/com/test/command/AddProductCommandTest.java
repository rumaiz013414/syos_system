package com.test.command;

import com.syos.command.AddProductCommand;
import com.syos.model.Product;
import com.syos.repository.ProductRepository;
import com.syos.service.ProductService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddProductCommandTest {

    @Mock
    private ProductService productService;

    @Mock
    private Scanner scanner;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private AddProductCommand addProductCommand;

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    void restoreStreams() {
        System.setOut(originalOut);
    }

    @Test
    @DisplayName("Should add product with valid input and unique code")
    void shouldSuccessfullyAddProduct() {
        String code = "P001";
        String name = "Test Product";
        String priceInput = "10.50";
        double price = 10.50;
        Product mockProduct = new Product(code, name, price);

        when(scanner.nextLine()).thenReturn(code, name, priceInput);
        when(productRepository.findByCode(code)).thenReturn(null);
        when(productService.addProduct(code, name, price)).thenReturn(mockProduct);

        addProductCommand.execute();

        verify(productRepository).findByCode(code);
        verify(productService).addProduct(code, name, price);
        String output = outContent.toString();
        assertTrue(output.contains("Success: Product added! Details: P001 | Test Product | 10.50"));
    }

    @Test
    @DisplayName("Should retry if duplicate product code is entered")
    void shouldHandleDuplicateProductCode() {
        String duplicateCode = "DUP01";
        String newCode = "NEW01";
        String name = "New Product";
        String priceInput = "25.00";
        double price = 25.00;

        Product existingProduct = new Product(duplicateCode, "Existing Product", 5.00);
        Product newProduct = new Product(newCode, name, price);

        when(scanner.nextLine()).thenReturn(duplicateCode, newCode, name, priceInput);
        when(productRepository.findByCode(duplicateCode)).thenReturn(existingProduct);
        when(productRepository.findByCode(newCode)).thenReturn(null);
        when(productService.addProduct(newCode, name, price)).thenReturn(newProduct);

        addProductCommand.execute();

        verify(productRepository).findByCode(duplicateCode);
        verify(productRepository).findByCode(newCode);
        verify(productService).addProduct(newCode, name, price);
        verify(productService, never()).addProduct(eq(duplicateCode), anyString(), anyDouble());

        String output = outContent.toString();
        assertTrue(output.contains("Error: Product code already exists. Please choose a different one."));
        assertTrue(output.contains("Success: Product added! Details: NEW01 | New Product | 25.00"));
    }

    @Test
    @DisplayName("Should handle IllegalArgumentException from service")
    void shouldHandleProductServiceIllegalArgumentException() {
        String code = "P007";
        String name = "Error Product";
        String priceInput = "20.00";
        double price = 20.00;

        when(scanner.nextLine()).thenReturn(code, name, priceInput);
        when(productRepository.findByCode(code)).thenReturn(null);
        when(productService.addProduct(code, name, price))
                .thenThrow(new IllegalArgumentException("Invalid arguments provided to service!"));

        addProductCommand.execute();

        verify(productService).addProduct(code, name, price);
        String output = outContent.toString();
        assertTrue(output.contains("Failed to add product: Invalid arguments provided to service!"));
    }

    @Test
    @DisplayName("Should handle IllegalStateException from service")
    void shouldHandleProductServiceIllegalStateException() {
        String code = "P008";
        String name = "Error Product";
        String priceInput = "20.00";
        double price = 20.00;

        when(scanner.nextLine()).thenReturn(code, name, priceInput);
        when(productRepository.findByCode(code)).thenReturn(null);
        when(productService.addProduct(code, name, price))
                .thenThrow(new IllegalStateException("Database connection lost!"));

        addProductCommand.execute();

        verify(productService).addProduct(code, name, price);
        String output = outContent.toString();
        assertTrue(output.contains("Operation failed: Database connection lost!"));
    }

    @Test
    @DisplayName("Should handle unexpected RuntimeException from service")
    void shouldHandleProductServiceGenericRuntimeException() {
        String code = "P009";
        String name = "Error Product";
        String priceInput = "20.00";
        double price = 20.00;

        when(scanner.nextLine()).thenReturn(code, name, priceInput);
        when(productRepository.findByCode(code)).thenReturn(null);
        when(productService.addProduct(code, name, price))
                .thenThrow(new RuntimeException("Something completely unexpected happened!"));

        addProductCommand.execute();

        verify(productService).addProduct(code, name, price);
        String output = outContent.toString();
        assertTrue(output.contains("An unexpected error occurred while adding the product: Something completely unexpected happened!"));
    }

    @Test
    @DisplayName("Should accept product with price exactly at minimum allowed")
    void shouldAddProductWithMinPrice() {
        String code = "P010";
        String name = "Min Price Item";
        String priceInput = "1.00";
        double price = 1.00;
        Product mockProduct = new Product(code, name, price);

        when(scanner.nextLine()).thenReturn(code, name, priceInput);
        when(productRepository.findByCode(code)).thenReturn(null);
        when(productService.addProduct(code, name, price)).thenReturn(mockProduct);

        addProductCommand.execute();

        verify(productService).addProduct(code, name, price);
        String output = outContent.toString();
        assertTrue(output.contains("Success: Product added! Details: P010 | Min Price Item | 1.00"));
    }

    @Test
    @DisplayName("Should retry multiple duplicate codes before accepting unique one")
    void shouldRetryUntilValidUniqueCodeEntered() {
        String duplicate1 = "CODE1";
        String duplicate2 = "CODE2";
        String unique = "UNIQUE1";
        String name = "Final Product";
        String price = "15.00";
        double parsedPrice = 15.00;

        when(scanner.nextLine()).thenReturn(duplicate1, duplicate2, unique, name, price);
        when(productRepository.findByCode(duplicate1)).thenReturn(new Product(duplicate1, "Old", 5.0));
        when(productRepository.findByCode(duplicate2)).thenReturn(new Product(duplicate2, "Old", 5.0));
        when(productRepository.findByCode(unique)).thenReturn(null);
        when(productService.addProduct(unique, name, parsedPrice)).thenReturn(new Product(unique, name, parsedPrice));

        addProductCommand.execute();

        verify(productService).addProduct(unique, name, parsedPrice);
        String output = outContent.toString();
        assertTrue(output.contains("Error: Product code already exists. Please choose a different one."));
    }

    @Test
    @DisplayName("Should retry if blank name is entered")
    void shouldHandleBlankName() {
        String code = "PROD01";
        String name = "Product Name";
        String price = "30.00";
        double parsedPrice = 30.00;

        when(scanner.nextLine()).thenReturn(code, "", name, price);
        when(productRepository.findByCode(code)).thenReturn(null);
        when(productService.addProduct(code, name, parsedPrice)).thenReturn(new Product(code, name, parsedPrice));

        addProductCommand.execute();

        String output = outContent.toString();
        assertTrue(output.contains("Product name cannot be empty."));
        assertTrue(output.contains("Success: Product added! Details: PROD01 | Product Name | 30.00"));
    }

    @Test
    @DisplayName("Should retry if price format is invalid")
    void shouldHandleInvalidPriceFormat() {
        String code = "PRC01";
        String name = "Test Price";
        String invalidPrice = "abc";
        String validPrice = "50.00";
        double parsedPrice = 50.00;

        when(scanner.nextLine()).thenReturn(code, name, invalidPrice, validPrice);
        when(productRepository.findByCode(code)).thenReturn(null);
        when(productService.addProduct(code, name, parsedPrice)).thenReturn(new Product(code, name, parsedPrice));

        addProductCommand.execute();

        String output = outContent.toString();
        assertTrue(output.contains("Invalid price format."));
        assertTrue(output.contains("Success: Product added! Details: PRC01 | Test Price | 50.00"));
    }
}
