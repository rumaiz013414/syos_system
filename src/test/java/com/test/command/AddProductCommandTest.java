package com.test.command; // Ensure your test package matches where you keep your test files

import com.syos.command.AddProductCommand;
import com.syos.model.Product;
import com.syos.repository.ProductRepository; // Import the new dependency
import com.syos.service.ProductService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Scanner;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddProductCommandTest {

	@Mock
	private ProductService productService;

	@Mock
	private Scanner scanner;

	@Mock // New mock for ProductRepository
	private ProductRepository productRepository;

	@InjectMocks // AddProductCommand now needs productRepository injected
	private AddProductCommand addProductCommand;

	private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
	private final PrintStream originalOut = System.out;

	@BeforeEach
	void setUp() {
		System.setOut(new PrintStream(outContent));
		// If not using @InjectMocks, you'd manually initialize here:
		// addProductCommand = new AddProductCommand(productService, scanner,
		// productRepository);
	}

	@AfterEach
	void restoreStreams() {
		System.setOut(originalOut);
	}

	// --- 1. Happy Path Test ---
	@Test
	@DisplayName("Should successfully add a product with valid input and unique code")
	void shouldSuccessfullyAddProduct() {
		// Arrange
		String code = "P001";
		String name = "Test Product";
		String priceInput = "10.50";
		double price = 10.50;

		Product mockProduct = new Product(code, name, price);

		// Simulate user input
		when(scanner.nextLine()).thenReturn(code) // First call: product code
				.thenReturn(name) // Second call: product name
				.thenReturn(priceInput); // Third call: product price

		// Simulate product code being unique (not found in repository)
		when(productRepository.findByCode(code)).thenReturn(null);

		// Simulate successful product addition by service
		when(productService.addProduct(code, name, price)).thenReturn(mockProduct);

		// Act
		addProductCommand.execute();

		// Assert
		// Verify interactions
		verify(productRepository, times(1)).findByCode(code);
		verify(productService, times(1)).addProduct(code, name, price);

		// Verify console output
		String output = outContent.toString();
		assertTrue(output.contains("=== Add New Product ==="));
		assertTrue(
				output.contains(String.format("Success: Product added! Details: %s | %s | %.2f", code, name, price)));
	}

	@ParameterizedTest
	@MethodSource("invalidInputTestCases")
	@DisplayName("Should prompt for valid input until all fields are correct")
	void shouldHandleInvalidInputsAndEventuallySucceed(String initialCode, String finalCode, String initialName,
			String finalName, String initialPrice, String finalPrice, String expectedErrorMessageCode,
			String expectedErrorMessageName, String expectedErrorMessagePrice) {

		// Arrange
		double finalPriceDouble = Double.parseDouble(finalPrice);
		Product mockProduct = new Product(finalCode, finalName, finalPriceDouble);

		when(scanner.nextLine()).thenReturn(initialCode).thenReturn(finalCode).thenReturn(initialName)
				.thenReturn(finalName).thenReturn(initialPrice).thenReturn(finalPrice);

		// Mock repository: initialCode might or might not be null, but finalCode must
		// be null
		lenient().when(productRepository.findByCode(initialCode)).thenReturn(null); // Be lenient, not all initial codes
																					// are checked for uniqueness
		when(productRepository.findByCode(finalCode)).thenReturn(null);

		// Mock service
		when(productService.addProduct(finalCode, finalName, finalPriceDouble)).thenReturn(mockProduct);

		// Act
		addProductCommand.execute();

		// Assert
		// Verify addProduct was called only with the final, valid inputs
		verify(productService, times(1)).addProduct(finalCode, finalName, finalPriceDouble);

		// Verify error messages appeared for invalid inputs
		String output = outContent.toString();
		if (expectedErrorMessageCode != null) {
			assertTrue(output.contains(expectedErrorMessageCode),
					"Output should contain code error: " + expectedErrorMessageCode);
		}
		if (expectedErrorMessageName != null) {
			assertTrue(output.contains(expectedErrorMessageName),
					"Output should contain name error: " + expectedErrorMessageName);
		}
		if (expectedErrorMessagePrice != null) {
			assertTrue(output.contains(expectedErrorMessagePrice),
					"Output should contain price error: " + expectedErrorMessagePrice);
		}
		// Verify success message
		assertTrue(output.contains(String.format("Success: Product added! Details: %s | %s | %.2f", finalCode,
				finalName, finalPriceDouble)));
	}

	// Method to provide arguments for the parameterized test (invalid inputs
	// followed by valid)
	private static Stream<Arguments> invalidInputTestCases() {
		// Each argument set:
		// {initialCode (invalid), finalCode (valid), initialName (invalid), finalName
		// (valid),
		// initialPrice (invalid), finalPrice (valid), expectedCodeError,
		// expectedNameError, expectedPriceError}
		return Stream.of(
				Arguments.of("", "V001", "Name", "ValidName", "10.00", "10.00", "Product code cannot be empty.", null,
						null),
				Arguments.of("AA", "V002", "Name", "ValidName", "10.00", "10.00",
						"Product code must be between 3 and 10 characters long.", null, null),
				Arguments.of("P!@#", "V003", "Name", "ValidName", "10.00", "10.00",
						"Product code can only contain letters and numbers.", null, null),
				Arguments.of("V004", "", "ValidName", "ValidName", "10.00", "10.00", null,
						"Product name cannot be empty.", null),
				Arguments.of("V005", "V005", "A", "ValidName", "10.00", "10.00", null,
						"Product name must be between 2 and 50 characters long.", null),
				Arguments.of("V006", "V006", "ValidName", "ValidName", "invalid", "10.00", null, null,
						"Invalid price format."),
				Arguments.of("V007", "V007", "ValidName", "ValidName", "0.50", "1.00", null, null,
						"Price cannot be negative. Minimum price is 1.00.") // Price < MIN_PRICE
		);
	}

	// --- 4. Test for Service Layer Exceptions ---
	@Test
	@DisplayName("Should handle IllegalArgumentException from ProductService.addProduct")
	void shouldHandleProductServiceIllegalArgumentException() {
		// Arrange
		String code = "P007";
		String name = "Error Product";
		String priceInput = "20.00";
		double price = 20.00;
		String errorMessage = "Invalid arguments provided to service!";

		when(scanner.nextLine()).thenReturn(code).thenReturn(name).thenReturn(priceInput);

		when(productRepository.findByCode(code)).thenReturn(null); // Ensure unique code first

		// Mock ProductService to throw IllegalArgumentException
		when(productService.addProduct(code, name, price)).thenThrow(new IllegalArgumentException(errorMessage));

		// Act
		addProductCommand.execute();

		// Assert
		verify(productService, times(1)).addProduct(code, name, price);
		String output = outContent.toString();
		assertTrue(output.contains("Failed to add product: " + errorMessage));
	}

	@Test
	@DisplayName("Should handle IllegalStateException from ProductService.addProduct")
	void shouldHandleProductServiceIllegalStateException() {
		// Arrange
		String code = "P008";
		String name = "Error Product";
		String priceInput = "20.00";
		double price = 20.00;
		String errorMessage = "Database connection lost!";

		when(scanner.nextLine()).thenReturn(code).thenReturn(name).thenReturn(priceInput);

		when(productRepository.findByCode(code)).thenReturn(null);

		// Mock ProductService to throw IllegalStateException
		when(productService.addProduct(code, name, price)).thenThrow(new IllegalStateException(errorMessage));

		// Act
		addProductCommand.execute();

		// Assert
		verify(productService, times(1)).addProduct(code, name, price);
		String output = outContent.toString();
		assertTrue(output.contains("Operation failed: " + errorMessage));
	}

	@Test
	@DisplayName("Should handle generic RuntimeException from ProductService.addProduct")
	void shouldHandleProductServiceGenericRuntimeException() {
		// Arrange
		String code = "P009";
		String name = "Error Product";
		String priceInput = "20.00";
		double price = 20.00;
		String errorMessage = "Something completely unexpected happened!";

		when(scanner.nextLine()).thenReturn(code).thenReturn(name).thenReturn(priceInput);

		when(productRepository.findByCode(code)).thenReturn(null);

		// Mock ProductService to throw a generic RuntimeException
		when(productService.addProduct(code, name, price)).thenThrow(new RuntimeException(errorMessage));

		// Act
		addProductCommand.execute();

		// Assert
		verify(productService, times(1)).addProduct(code, name, price);
		String output = outContent.toString();
		assertTrue(output.contains("An unexpected error occurred while adding the product: " + errorMessage));
		
	}

	// --- 5. Additional Test: Price Exactly at MIN_PRICE (1.00) ---
	@Test
	@DisplayName("Should add product successfully with price exactly at MIN_PRICE (1.00)")
	void shouldAddProductWithMinPrice() {
		// Arrange
		String code = "P010";
		String name = "Min Price Item";
		String priceInput = "1.00"; // Exactly MIN_PRICE
		double price = 1.00;

		Product mockProduct = new Product(code, name, price);

		when(scanner.nextLine()).thenReturn(code).thenReturn(name).thenReturn(priceInput);

		when(productRepository.findByCode(code)).thenReturn(null);
		when(productService.addProduct(code, name, price)).thenReturn(mockProduct);

		// Act
		addProductCommand.execute();

		// Assert
		verify(productService, times(1)).addProduct(code, name, price);
		String output = outContent.toString();
		assertTrue(
				output.contains(String.format("Success: Product added! Details: %s | %s | %.2f", code, name, price)));
	}

	@Test
	@DisplayName("Should prompt for new code if product code already exists")
	void shouldHandleDuplicateProductCode() {
		// Arrange
		String duplicateCode = "DUP01";
		String newCode = "NEW01";
		String name = "New Product";
		String priceInput = "25.00";
		double price = 25.00;

		Product existingProduct = new Product(duplicateCode, "Existing Product", 5.00);
		Product newProduct = new Product(newCode, name, price);

		when(scanner.nextLine()).thenReturn(duplicateCode) // First input: duplicate code
				.thenReturn(newCode) // Second input: unique code (allows loop to break)
				.thenReturn(name)
				.thenReturn(priceInput); 

		// Mock the ProductRepository behavior:
		// 1. When findByCode is called with the duplicateCode, it should return an
		// existing product.
		// 2. When findByCode is called with the newCode, it should return null
		// (indicating it's unique).
		when(productRepository.findByCode(duplicateCode)).thenReturn(existingProduct);
		when(productRepository.findByCode(newCode)).thenReturn(null);

		// Mock the ProductService behavior for the final successful addition
		when(productService.addProduct(newCode, name, price)).thenReturn(newProduct);

		// Act
		addProductCommand.execute();

		// Assert
		// Verify that findByCode was called for both the duplicate attempt and the
		// successful attempt.
		verify(productRepository, times(1)).findByCode(duplicateCode);
		verify(productRepository, times(1)).findByCode(newCode);

		// Verify that productService.addProduct was called exactly once, with the new,
		// unique product details.
		verify(productService, times(1)).addProduct(newCode, name, price);
		// And importantly, verify that productService.addProduct was *never* called
		// with the duplicate code.
		// We use `eq(duplicateCode)` to match the specific string, combined with
		// `anyString()` and `anyDouble()`
		// for the other arguments to adhere to Mockito's matcher rules.
		verify(productService, never()).addProduct(eq(duplicateCode), anyString(), anyDouble());

		// Verify the console output contains the error message and the success message.
		String output = outContent.toString();
		assertTrue(output.contains("Error: Product code already exists. Please choose a different one."));
		assertTrue(output
				.contains(String.format("Success: Product added! Details: %s | %s | %.2f", newCode, name, price)));
	}
}