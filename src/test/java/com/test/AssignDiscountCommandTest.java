package com.test;

import com.syos.command.AssignDiscountCommand;
import com.syos.enums.DiscountType; // Make sure this import is present
import com.syos.model.Discount;
import com.syos.model.Product;
import com.syos.repository.DiscountRepository;
import com.syos.repository.ProductRepository;
import com.syos.service.DiscountAssignmentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks; // This is not strictly necessary for this setup, but good practice
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

	@Mock
	private Scanner scanner;
	@Mock
	private DiscountRepository discountRepository;
	@Mock
	private ProductRepository productRepository;

	private AssignDiscountCommand assignDiscountCommand;
	private DiscountAssignmentService discountAssignmentService;

	private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
	private final PrintStream originalOut = System.out;

	@BeforeEach
	void setUp() {
		System.setOut(new PrintStream(outContent));
		// Manually instantiate the service and command, injecting the mocks
		discountAssignmentService = new DiscountAssignmentService(scanner, discountRepository, productRepository);
		assignDiscountCommand = new AssignDiscountCommand(scanner, discountRepository, productRepository);
	}

	@AfterEach
	void restoreStreams() {
		System.setOut(originalOut);
	}

	@Test
	@DisplayName("Should successfully assign an existing discount with PERCENT type to a product")
	void shouldSuccessfullyAssignPercentDiscount() {
		// Arrange
		String productCode = "P123";
		int discountId = 1;
		String discountName = "Summer Sale";
		DiscountType discountType = DiscountType.PERCENT;
		double discountValue = 10.0;
		LocalDate startDate = LocalDate.of(2025, 6, 1);
		LocalDate endDate = LocalDate.of(2025, 6, 30);

		Product mockProduct = new Product(productCode, "Laptop", 1200.0);
		Discount mockDiscount = new Discount(discountId, discountName, discountType, discountValue, startDate, endDate);

		when(scanner.nextLine()).thenReturn(productCode).thenReturn(String.valueOf(discountId));

		when(productRepository.findByCode(productCode)).thenReturn(mockProduct);
		when(discountRepository.findById(discountId)).thenReturn(mockDiscount);
		doNothing().when(discountRepository).linkProductToDiscount(productCode, discountId);

		// Act
		assignDiscountCommand.execute();

		// Assert
		verify(scanner, times(2)).nextLine();
		verify(productRepository, times(1)).findByCode(productCode);
		verify(discountRepository, times(1)).findById(discountId);
		verify(discountRepository, times(1)).linkProductToDiscount(productCode, discountId);

		String output = outContent.toString();
		assertTrue(output.contains("=== Assign Existing Discount to Product ==="));
		assertTrue(output.contains("Enter product code:"));
		assertTrue(output.contains("Enter discount ID:"));
		assertTrue(output.contains(String.format("Selected Discount: ID %d | Name: '%s' | Type: %s", discountId,
				discountName, discountType.name())));
		assertTrue(output.contains(String.format("Discount ID %d assigned to product %s.", discountId, productCode)));
	}

	@Test
	@DisplayName("Should successfully assign an existing discount with AMOUNT type to a product")
	void shouldSuccessfullyAssignAmountDiscount() {
		// Arrange
		String productCode = "P456";
		int discountId = 2;
		String discountName = "Flat $5 Off";
		DiscountType discountType = DiscountType.AMOUNT;
		double discountValue = 5.0;
		LocalDate startDate = LocalDate.of(2025, 7, 1);
		LocalDate endDate = LocalDate.of(2025, 7, 31);

		Product mockProduct = new Product(productCode, "Mouse", 25.0);
		Discount mockDiscount = new Discount(discountId, discountName, discountType, discountValue, startDate, endDate);

		when(scanner.nextLine()).thenReturn(productCode).thenReturn(String.valueOf(discountId));

		when(productRepository.findByCode(productCode)).thenReturn(mockProduct);
		when(discountRepository.findById(discountId)).thenReturn(mockDiscount);
		doNothing().when(discountRepository).linkProductToDiscount(productCode, discountId);

		assignDiscountCommand.execute();

		verify(scanner, times(2)).nextLine();
		verify(productRepository, times(1)).findByCode(productCode);
		verify(discountRepository, times(1)).findById(discountId);
		verify(discountRepository, times(1)).linkProductToDiscount(productCode, discountId);

		String output = outContent.toString();
		assertTrue(output.contains("=== Assign Existing Discount to Product ==="));
		assertTrue(output.contains("Enter product code:"));
		assertTrue(output.contains("Enter discount ID:"));
		assertTrue(output.contains(String.format("Selected Discount: ID %d | Name: '%s' | Type: %s", discountId,
				discountName, discountType.name())));
		assertTrue(output.contains(String.format("Discount ID %d assigned to product %s.", discountId, productCode)));
	}

	@Test
	@DisplayName("Should handle empty product code input")
	void shouldHandleEmptyProductCode() {
		// Arrange
		when(scanner.nextLine()).thenReturn("");

		// Act
		assignDiscountCommand.execute();

		// Assert
		verify(scanner, times(1)).nextLine();
		verifyNoInteractions(productRepository);
		verifyNoInteractions(discountRepository);
		String output = outContent.toString();
		assertTrue(output.contains("Product code cannot be empty."));
		assertTrue(output.contains("=== Assign Existing Discount to Product ==="));
	}

	@Test
	@DisplayName("Should handle product code not found")
	void shouldHandleProductCodeNotFound() {
		// Arrange
		String productCode = "NONEXISTENT";
		when(scanner.nextLine()).thenReturn(productCode);
		when(productRepository.findByCode(productCode)).thenReturn(null);

		// Act
		assignDiscountCommand.execute();

		// Assert
		verify(scanner, times(1)).nextLine();
		verify(productRepository, times(1)).findByCode(productCode);
		verifyNoInteractions(discountRepository);
		String output = outContent.toString();
		assertTrue(output.contains("No such product: " + productCode));
		assertTrue(output.contains("=== Assign Existing Discount to Product ==="));
	}

	@Test
	@DisplayName("Should handle invalid discount ID format (non-numeric)")
	void shouldHandleInvalidDiscountIdFormat() {
		// Arrange
		String productCode = "P123";
		String invalidDiscountId = "abc";

		Product mockProduct = new Product(productCode, "Laptop", 1200.0);

		when(scanner.nextLine()).thenReturn(productCode).thenReturn(invalidDiscountId);

		when(productRepository.findByCode(productCode)).thenReturn(mockProduct);

		assignDiscountCommand.execute();

		verify(scanner, times(2)).nextLine();
		verify(productRepository, times(1)).findByCode(productCode);
		verifyNoInteractions(discountRepository);
		String output = outContent.toString();
		assertTrue(output.contains("Invalid discount ID format. Please enter a number."));
		assertTrue(output.contains("=== Assign Existing Discount to Product ==="));
	}

	@Test
	@DisplayName("Should handle non-positive discount ID")
	void shouldHandleNonPositiveDiscountId() {
		// Arrange
		String productCode = "P123";
		String nonPositiveDiscountId = "0";

		Product mockProduct = new Product(productCode, "Laptop", 1200.0);

		when(scanner.nextLine()).thenReturn(productCode).thenReturn(nonPositiveDiscountId);

		when(productRepository.findByCode(productCode)).thenReturn(mockProduct);

		// Act
		assignDiscountCommand.execute();

		// Assert
		verify(scanner, times(2)).nextLine();
		verify(productRepository, times(1)).findByCode(productCode);
		verifyNoInteractions(discountRepository);
		String output = outContent.toString();
		assertTrue(output.contains("Discount ID must be a positive number."));
		assertTrue(output.contains("=== Assign Existing Discount to Product ==="));
	}

	@Test
	@DisplayName("Should handle discount ID not found")
	void shouldHandleDiscountIdNotFound() {
		// Arrange
		String productCode = "P123";
		int nonExistentDiscountId = 999;

		Product mockProduct = new Product(productCode, "Laptop", 1200.0);

		when(scanner.nextLine()).thenReturn(productCode).thenReturn(String.valueOf(nonExistentDiscountId));

		when(productRepository.findByCode(productCode)).thenReturn(mockProduct);
		when(discountRepository.findById(nonExistentDiscountId)).thenReturn(null);

		// Act
		assignDiscountCommand.execute();

		// Assert
		verify(scanner, times(2)).nextLine();
		verify(productRepository, times(1)).findByCode(productCode);
		verify(discountRepository, times(1)).findById(nonExistentDiscountId);
		verify(discountRepository, never()).linkProductToDiscount(anyString(), anyInt());
		String output = outContent.toString();
		assertTrue(output.contains("No discount found with ID: " + nonExistentDiscountId));
		assertTrue(output.contains("Please create the discount first using 'Create new discount' option."));
		assertTrue(output.contains("=== Assign Existing Discount to Product ==="));
	}

	@Test
	@DisplayName("Should handle RuntimeException during discount assignment (e.g., linkProductToDiscount fails)")
	void shouldHandleLinkProductToDiscountException() {
		// Arrange
		String productCode = "P123";
		int discountId = 1;
		String errorMessage = "Product already has a conflicting discount.";
		DiscountType discountType = DiscountType.PERCENT;
		Product mockProduct = new Product(productCode, "Laptop", 1200.0);
		Discount mockDiscount = new Discount(discountId, "Test Discount", discountType, 5.0, LocalDate.now(),
				LocalDate.now().plusDays(7));

		when(scanner.nextLine()).thenReturn(productCode).thenReturn(String.valueOf(discountId));

		when(productRepository.findByCode(productCode)).thenReturn(mockProduct);
		when(discountRepository.findById(discountId)).thenReturn(mockDiscount);
		doThrow(new RuntimeException(errorMessage)).when(discountRepository).linkProductToDiscount(productCode,
				discountId);

		// Act
		assignDiscountCommand.execute();

		// Assert
		verify(scanner, times(2)).nextLine();
		verify(productRepository, times(1)).findByCode(productCode);
		verify(discountRepository, times(1)).findById(discountId);
		verify(discountRepository, times(1)).linkProductToDiscount(productCode, discountId);
		String output = outContent.toString();
		assertTrue(output.contains("Failed to assign discount: " + errorMessage));
		assertTrue(output.contains("=== Assign Existing Discount to Product ==="));
	}
}