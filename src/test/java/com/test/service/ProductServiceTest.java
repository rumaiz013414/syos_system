package com.test.service;

import com.syos.model.Product;
import com.syos.repository.ProductRepository;
import com.syos.service.ProductService;
import com.syos.util.CommonVariables;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {

    @Mock
    private ProductRepository productRepository; // Mock the dependency

    @InjectMocks
    private ProductService productService; // Inject mocks into ProductService

    private MockedConstruction<ProductRepository> mockedProductRepositoryConstruction;

    @BeforeEach
    void setUp() {
        mockedProductRepositoryConstruction = mockConstruction(ProductRepository.class, (mock, context) -> {
            productRepository = mock;
        });
        productService = new ProductService();
    }

    @AfterEach
    void tearDown() {
        if (mockedProductRepositoryConstruction != null) {
            mockedProductRepositoryConstruction.close();
        }
    }

    // --- Tests for addProduct method ---

    @Test
    @DisplayName("Should add a product successfully")
    void shouldAddProductSuccessfully() {
        String code = "P001";
        String name = "Test Product";
        double price = 10.0;

        when(productRepository.findByCode(code)).thenReturn(null);

        Product result = productService.addProduct(code, name, price);

        assertNotNull(result);
        assertEquals(code, result.getCode());
        assertEquals(name, result.getName());
        assertEquals(price, result.getPrice());
        verify(productRepository, times(1)).findByCode(code);
        verify(productRepository, times(1)).add(any(Product.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException if product code is too long")
    void shouldThrowExceptionIfCodeTooLong() {
        // Corrected: Declare longCode as effectively final in one step
        final String longCode = "P" + "0".repeat(CommonVariables.MAX_CODE_LENGTH) + "1";
        String name = "Test Product";
        double price = 10.0;

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            productService.addProduct(longCode, name, price);
        });

        assertTrue(thrown.getMessage().contains("Product code must be at most 10 characters"));
        verify(productRepository, never()).findByCode(anyString());
        verify(productRepository, never()).add(any(Product.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException if product name is too long")
    void shouldThrowExceptionIfNameTooLong() {
        String code = "P002";
        // Corrected: Declare longName as effectively final in one step
        final String longName = "A".repeat(CommonVariables.MAX_PRODUCT_NAME_LENGTH) + "B";
        double price = 10.0;

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            productService.addProduct(code, longName, price);
        });

        assertTrue(thrown.getMessage().contains("Product name must be at most 100 characters"));
        verify(productRepository, never()).findByCode(anyString());
        verify(productRepository, never()).add(any(Product.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException if product code already exists")
    void shouldThrowExceptionIfCodeExists() {
        String code = "P003";
        String name = "Existing Product";
        double price = 10.0;

        when(productRepository.findByCode(code)).thenReturn(new Product(code, name, price));

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            productService.addProduct(code, "New Name", 20.0);
        });

        assertTrue(thrown.getMessage().contains("Product code already exists: " + code));
        verify(productRepository, times(1)).findByCode(code);
        verify(productRepository, never()).add(any(Product.class));
    }

    // --- Tests for updateProductName method ---

    @Test
    @DisplayName("Should update product name successfully")
    void shouldUpdateProductNameSuccessfully() {
        String code = "UP001";
        String oldName = "Old Name";
        String newName = "Updated Product Name";
        double price = 50.0;
        Product existingProduct = new Product(code, oldName, price);

        when(productRepository.findByCode(code)).thenReturn(existingProduct);
        doNothing().when(productRepository).update(any(Product.class));

        Product result = productService.updateProductName(code, newName);

        assertNotNull(result);
        assertEquals(code, result.getCode());
        assertEquals(newName, result.getName());
        assertEquals(price, result.getPrice());
        verify(productRepository, times(1)).findByCode(code);
        verify(productRepository, times(1)).update(argThat(product ->
                product.getCode().equals(code) && product.getName().equals(newName) && product.getPrice() == price));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException if new name is too long")
    void shouldThrowExceptionIfNewNameTooLong() {
        String code = "UP002";
        // Corrected: Declare longNewName as effectively final in one step
        final String longNewName = "Z".repeat(CommonVariables.MAX_PRODUCT_NAME_LENGTH + 1);
        Product existingProduct = new Product(code, "Current Name", 30.0);

        when(productRepository.findByCode(code)).thenReturn(existingProduct);

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            productService.updateProductName(code, longNewName);
        });

        assertTrue(thrown.getMessage().contains("Product name must be at most 100 characters"));
//        verify(productRepository, times(1)).findByCode(code);
        verify(productRepository, never()).update(any(Product.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException if product not found for update")
    void shouldThrowExceptionIfProductNotFoundForUpdate() {
        String code = "NONEXISTENT";
        String newName = "Some Name";

        when(productRepository.findByCode(code)).thenReturn(null);

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            productService.updateProductName(code, newName);
        });

        assertTrue(thrown.getMessage().contains("Product with code " + code + " not found."));
        verify(productRepository, times(1)).findByCode(code);
        verify(productRepository, never()).update(any(Product.class));
    }

    // --- Tests for findProductByCode method ---

    @Test
    @DisplayName("Should find product by code successfully")
    void shouldFindProductByCodeSuccessfully() {
        String code = "F001";
        Product expectedProduct = new Product(code, "Found Product", 75.0);

        when(productRepository.findByCode(code)).thenReturn(expectedProduct);

        Product result = productService.findProductByCode(code);

        assertNotNull(result);
        assertEquals(expectedProduct, result);
        verify(productRepository, times(1)).findByCode(code);
    }

    @Test
    @DisplayName("Should return null if product not found by code")
    void shouldReturnNullIfProductNotFoundByCode() {
        String code = "F999";

        when(productRepository.findByCode(code)).thenReturn(null);

        Product result = productService.findProductByCode(code);

        assertNull(result);
        verify(productRepository, times(1)).findByCode(code);
    }
}