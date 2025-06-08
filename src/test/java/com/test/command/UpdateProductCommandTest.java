package com.test.command;

import com.syos.command.UpdateProductCommand;
import com.syos.model.Product;
import com.syos.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UpdateProductCommandTest {

    @Mock
    private ProductService productService;

    @InjectMocks
    private UpdateProductCommand updateProductCommand;

    private ByteArrayOutputStream outputStreamCaptor;
    private InputStream originalSystemIn;
    private PrintStream originalSystemOut;

    @BeforeEach
    void setUp() {
        // Capture System.out for assertions
        originalSystemOut = System.out;
        outputStreamCaptor = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStreamCaptor));

        // Store original System.in
        originalSystemIn = System.in;

        // Initialize UpdateProductCommand with mocked ProductService and a dummy Scanner
        // The Scanner will be re-initialized in each test method for specific input
        // Since @InjectMocks creates the instance, we need to set the scanner through reflection
        // or re-initialize it for each test where input is needed.
        // For simplicity in this test, we'll manually create UpdateProductCommand in tests
        // where input is needed, ensuring the Scanner is correctly set up.
    }

    // Helper method to simulate user input
    private void provideInput(String data) {
        System.setIn(new ByteArrayInputStream(data.getBytes()));
        updateProductCommand = new UpdateProductCommand(productService, new Scanner(System.in));
    }

    // Helper method to reset System.in and System.out
    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        System.setOut(originalSystemOut);
        System.setIn(originalSystemIn);
        if (updateProductCommand != null && updateProductCommand.scanner != null) {
            updateProductCommand.scanner.close(); // Close the scanner created in tests
        }
    }

    @Test
    @DisplayName("Should update product name successfully")
    void shouldUpdateProductNameSuccessfully() {
        String productCode = "P001";
        String newName = "New Product Name";
        Product product = new Product(productCode, "Old Name", 10.0);

        provideInput(productCode + System.lineSeparator() + newName + System.lineSeparator());

        when(productService.findProductByCode(productCode)).thenReturn(product);
        when(productService.updateProductName(productCode, newName)).thenReturn(product);

        updateProductCommand.execute();

        verify(productService, times(1)).findProductByCode(productCode);
        verify(productService, times(1)).updateProductName(productCode, newName);
        assertTrue(outputStreamCaptor.toString().contains("Product name for 'P001' updated successfully to 'New Product Name'!"));
    }

    @Test
    @DisplayName("Should display error if product not found")
    void shouldDisplayErrorIfProductNotFound() {
        String productCode = "P999";

        provideInput(productCode + System.lineSeparator() + "Some Name" + System.lineSeparator());

        when(productService.findProductByCode(productCode)).thenReturn(null);

        updateProductCommand.execute();

        verify(productService, times(1)).findProductByCode(productCode);
        verify(productService, never()).updateProductName(anyString(), anyString());
        assertTrue(outputStreamCaptor.toString().contains("Product with code '" + productCode + "' not found."));
    }

    @Test
    @DisplayName("Should display error if new name is empty")
    void shouldDisplayErrorIfNewNameIsEmpty() {
        String productCode = "P002";
        String emptyName = "";
        Product product = new Product(productCode, "Existing Name", 15.0);

        provideInput(productCode + System.lineSeparator() + emptyName + System.lineSeparator());

        when(productService.findProductByCode(productCode)).thenReturn(product);

        updateProductCommand.execute();

        verify(productService, times(1)).findProductByCode(productCode);
        verify(productService, never()).updateProductName(anyString(), anyString()); // updateProductName should not be called
        assertTrue(outputStreamCaptor.toString().contains("Product name cannot be empty. No changes made."));
    }

    @Test
    @DisplayName("Should handle IllegalArgumentException from ProductService")
    void shouldHandleIllegalArgumentException() {
        String productCode = "P003";
        String newName = "A".repeat(101); // Exceeds MAX_PRODUCT_NAME_LENGTH
        Product product = new Product(productCode, "Current Name", 20.0);

        provideInput(productCode + System.lineSeparator() + newName + System.lineSeparator());

        when(productService.findProductByCode(productCode)).thenReturn(product);
        doThrow(new IllegalArgumentException("Product name must be at most 100 characters"))
                .when(productService).updateProductName(productCode, newName);

        updateProductCommand.execute();

        verify(productService, times(1)).findProductByCode(productCode);
        verify(productService, times(1)).updateProductName(productCode, newName);
        assertTrue(outputStreamCaptor.toString().contains("Error: Product name must be at most 100 characters"));
    }

    @Test
    @DisplayName("Should handle RuntimeException from ProductService")
    void shouldHandleRuntimeException() {
        String productCode = "P004";
        String newName = "Valid Name";
        Product product = new Product(productCode, "Current Name", 25.0);

        provideInput(productCode + System.lineSeparator() + newName + System.lineSeparator());

        when(productService.findProductByCode(productCode)).thenReturn(product);
        doThrow(new RuntimeException("Database connection failed"))
                .when(productService).updateProductName(productCode, newName);

        updateProductCommand.execute();

        verify(productService, times(1)).findProductByCode(productCode);
        verify(productService, times(1)).updateProductName(productCode, newName);
        assertTrue(outputStreamCaptor.toString().contains("An unexpected error occurred while updating the product name: Database connection failed"));
    }
}