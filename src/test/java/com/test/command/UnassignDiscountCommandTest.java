package com.test.command;

import com.syos.command.UnassignDiscountCommand;
import com.syos.model.Discount;
import com.syos.model.Product;
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
		System.setOut(new PrintStream(outContent));
		unassignDiscountCommand = new UnassignDiscountCommand(scanner, discountRepository, productRepository);
	}

	@AfterEach
	void restoreStreams() {
		System.setOut(originalOut);
	}

	@Test
	@DisplayName("Should successfully unassign an active discount from a product")
	void shouldSuccessfullyUnassignDiscount() {
		String productCode = "PROD001";
		Product product = new Product(productCode, "Laptop", 1200.0);
		int discountIdToUnassign = 1;
		Discount discountToUnassign = new Discount(discountIdToUnassign, "Summer Sale", DiscountType.PERCENT, 15.0,
				LocalDate.now().minusDays(5), LocalDate.now().plusDays(5));
		Discount otherActiveDiscount = new Discount(2, "Student Discount", DiscountType.AMOUNT, 50.0,
				LocalDate.now().minusDays(10), LocalDate.now().plusDays(10));
		List<Discount> activeDiscounts = Arrays.asList(discountToUnassign, otherActiveDiscount);

		when(scanner.nextLine()).thenReturn(productCode).thenReturn(String.valueOf(discountIdToUnassign));

		when(productRepository.findByCode(productCode)).thenReturn(product);
		when(discountRepository.findDiscountsByProductCode(productCode, LocalDate.now())).thenReturn(activeDiscounts);
		when(discountRepository.findById(discountIdToUnassign)).thenReturn(discountToUnassign);
		when(discountRepository.unassignDiscountFromProduct(productCode, discountIdToUnassign)).thenReturn(true);

		unassignDiscountCommand.execute();

		verify(productRepository, times(1)).findByCode(productCode);
		verify(discountRepository, times(1)).findDiscountsByProductCode(productCode, LocalDate.now());
		verify(discountRepository, times(1)).findById(discountIdToUnassign);
		verify(discountRepository, times(1)).unassignDiscountFromProduct(productCode, discountIdToUnassign);

		String output = outContent.toString();
		assertTrue(output.contains("--- Unassign Discount from Product ---"));
		assertTrue(output.contains("Enter Product Code:"));
		assertTrue(output.contains("Active discounts for " + product.getName() + " (" + productCode + "):"));
		assertTrue(
				output.contains(String.format("%-5d %-20s", discountToUnassign.getId(), discountToUnassign.getName())));
		assertTrue(output.contains("Enter Discount ID to unassign:"));
		assertTrue(output.contains("Discount '" + discountToUnassign.getName() + "' (ID: " + discountIdToUnassign
				+ ") successfully unassigned from product '" + product.getName() + "' (" + productCode + ")."));
	}

	@Test
	@DisplayName("Should display error if product is not found")
	void shouldDisplayErrorIfProductNotFound() {
		String productCode = "NONEXISTENT_PROD";
		when(scanner.nextLine()).thenReturn(productCode);
		when(productRepository.findByCode(productCode)).thenReturn(null);

		unassignDiscountCommand.execute();

		verify(productRepository, times(1)).findByCode(productCode);
		verify(discountRepository, never()).findDiscountsByProductCode(anyString(), any(LocalDate.class));
		verify(scanner, times(1)).nextLine();

		String output = outContent.toString();
		assertTrue(output.contains("Error: Product with code '" + productCode + "' not found."));
	}

	@Test
	@DisplayName("Should handle product code with leading/trailing spaces correctly")
	void shouldHandleProductCodeWithSpaces() {
		String productCodeWithSpaces = " PROD002 ";
		String expectedProductCode = "PROD002";
		Product product = new Product(expectedProductCode, "Keyboard", 75.0);
		int discountId = 1;
		Discount discount = new Discount(discountId, "Black Friday", DiscountType.PERCENT, 20.0,
				LocalDate.now().minusDays(1), LocalDate.now().plusDays(1));
		List<Discount> activeDiscounts = Collections.singletonList(discount);

		when(scanner.nextLine()).thenReturn(productCodeWithSpaces).thenReturn(String.valueOf(discountId));

		when(productRepository.findByCode(expectedProductCode)).thenReturn(product);
		when(discountRepository.findDiscountsByProductCode(expectedProductCode, LocalDate.now()))
				.thenReturn(activeDiscounts);
		when(discountRepository.findById(discountId)).thenReturn(discount);
		when(discountRepository.unassignDiscountFromProduct(expectedProductCode, discountId)).thenReturn(true);

		unassignDiscountCommand.execute();

		verify(productRepository, times(1)).findByCode(expectedProductCode);
		verify(discountRepository, times(1)).unassignDiscountFromProduct(expectedProductCode, discountId);
		assertTrue(outContent.toString().contains("successfully unassigned"));
	}

	@Test
	@DisplayName("Should inform user if product has no active discounts")
	void shouldInformIfNoActiveDiscounts() {
		String productCode = "PROD003";
		Product product = new Product(productCode, "Mouse", 25.0);

		when(scanner.nextLine()).thenReturn(productCode);
		when(productRepository.findByCode(productCode)).thenReturn(product);
		when(discountRepository.findDiscountsByProductCode(productCode, LocalDate.now()))
				.thenReturn(Collections.emptyList());

		unassignDiscountCommand.execute();

		verify(productRepository, times(1)).findByCode(productCode);
		verify(discountRepository, times(1)).findDiscountsByProductCode(productCode, LocalDate.now());
		verify(discountRepository, never()).findById(anyInt());
		verify(scanner, times(1)).nextLine();

		String output = outContent.toString();
		assertTrue(output.contains("Product '" + product.getName() + "' (" + productCode
				+ ") currently has no active discounts to unassign."));
	}

	@Test
	@DisplayName("Should display error for invalid (non-numeric) Discount ID input")
	void shouldDisplayErrorForInvalidDiscountIdInput() {
		String productCode = "PROD004";
		Product product = new Product(productCode, "Monitor", 300.0);
		List<Discount> activeDiscounts = Collections.singletonList(new Discount(1, "Test Discount",
				DiscountType.PERCENT, 10.0, LocalDate.now().minusDays(1), LocalDate.now().plusDays(1)));

		when(scanner.nextLine()).thenReturn(productCode).thenReturn("invalid_id");

		when(productRepository.findByCode(productCode)).thenReturn(product);
		when(discountRepository.findDiscountsByProductCode(productCode, LocalDate.now())).thenReturn(activeDiscounts);

		unassignDiscountCommand.execute();

		verify(productRepository, times(1)).findByCode(productCode);
		verify(discountRepository, times(1)).findDiscountsByProductCode(productCode, LocalDate.now());
		verify(discountRepository, never()).findById(anyInt());
		verify(discountRepository, never()).unassignDiscountFromProduct(anyString(), anyInt());

		String output = outContent.toString();
		assertTrue(output.contains("Invalid Discount ID. Please enter a number."));
	}

	@Test
	@DisplayName("Should handle discount ID with leading/trailing spaces correctly")
	void shouldHandleDiscountIdWithSpaces() {
		String productCode = "PROD005";
		Product product = new Product(productCode, "Webcam", 50.0);
		String discountIdWithSpaces = " 123 ";
		int expectedDiscountId = 123;
		Discount discount = new Discount(expectedDiscountId, "Flash Sale", DiscountType.AMOUNT, 5.0,
				LocalDate.now().minusDays(1), LocalDate.now().plusDays(1));
		List<Discount> activeDiscounts = Collections.singletonList(discount);

		when(scanner.nextLine()).thenReturn(productCode).thenReturn(discountIdWithSpaces);

		when(productRepository.findByCode(productCode)).thenReturn(product);
		when(discountRepository.findDiscountsByProductCode(productCode, LocalDate.now())).thenReturn(activeDiscounts);
		when(discountRepository.findById(expectedDiscountId)).thenReturn(discount);
		when(discountRepository.unassignDiscountFromProduct(productCode, expectedDiscountId)).thenReturn(true);

		unassignDiscountCommand.execute();

		verify(discountRepository, times(1)).findById(expectedDiscountId);
		verify(discountRepository, times(1)).unassignDiscountFromProduct(productCode, expectedDiscountId);
		assertTrue(outContent.toString().contains("successfully unassigned"));
	}

	@Test
	@DisplayName("Should display error if discount with given ID is not found")
	void shouldDisplayErrorIfDiscountNotFound() {
		String productCode = "PROD006";
		Product product = new Product(productCode, "Headphones", 100.0);
		int nonExistentDiscountId = 999;
		List<Discount> activeDiscounts = Collections.singletonList(new Discount(1, "Existing Discount",
				DiscountType.PERCENT, 10.0, LocalDate.now().minusDays(1), LocalDate.now().plusDays(1)));

		when(scanner.nextLine()).thenReturn(productCode).thenReturn(String.valueOf(nonExistentDiscountId));

		when(productRepository.findByCode(productCode)).thenReturn(product);
		when(discountRepository.findDiscountsByProductCode(productCode, LocalDate.now())).thenReturn(activeDiscounts);
		when(discountRepository.findById(nonExistentDiscountId)).thenReturn(null);

		unassignDiscountCommand.execute();

		verify(productRepository, times(1)).findByCode(productCode);
		verify(discountRepository, times(1)).findDiscountsByProductCode(productCode, LocalDate.now());
		verify(discountRepository, times(1)).findById(nonExistentDiscountId);
		verify(discountRepository, never()).unassignDiscountFromProduct(anyString(), anyInt());

		String output = outContent.toString();
		assertTrue(output.contains("Error: Discount with ID " + nonExistentDiscountId + " not found."));
	}

	@Test
	@DisplayName("Should display error if discount ID is not assigned to the product")
	void shouldDisplayErrorIfDiscountNotAssignedToProduct() {
		String productCode = "PROD007";
		Product product = new Product(productCode, "Charger", 20.0);
		int discountIdNotAssigned = 3;
		Discount foundDiscount = new Discount(discountIdNotAssigned, "Another Store Discount", DiscountType.AMOUNT, 2.0,
				LocalDate.now().minusDays(5), LocalDate.now().plusDays(5));

		List<Discount> activeDiscounts = Collections.singletonList(new Discount(1, "Existing Discount",
				DiscountType.PERCENT, 10.0, LocalDate.now().minusDays(1), LocalDate.now().plusDays(1)));

		when(scanner.nextLine()).thenReturn(productCode).thenReturn(String.valueOf(discountIdNotAssigned));

		when(productRepository.findByCode(productCode)).thenReturn(product);
		when(discountRepository.findDiscountsByProductCode(productCode, LocalDate.now())).thenReturn(activeDiscounts);
		when(discountRepository.findById(discountIdNotAssigned)).thenReturn(foundDiscount);

		unassignDiscountCommand.execute();

		verify(productRepository, times(1)).findByCode(productCode);
		verify(discountRepository, times(1)).findDiscountsByProductCode(productCode, LocalDate.now());
		verify(discountRepository, times(1)).findById(discountIdNotAssigned);
		verify(discountRepository, never()).unassignDiscountFromProduct(anyString(), anyInt());

		String output = outContent.toString();
		assertTrue(output.contains("Error: Discount ID " + discountIdNotAssigned
				+ " is not currently assigned to product '" + productCode + "'."));
	}

	@Test
	@DisplayName("Should display failure message if unassignDiscountFromProduct returns false")
	void shouldDisplayFailureIfRepositoryUnassignFails() {
		String productCode = "PROD008";
		Product product = new Product(productCode, "USB Drive", 15.0);
		int discountId = 1;
		Discount discountToUnassign = new Discount(discountId, "Winter Sale", DiscountType.AMOUNT, 3.0,
				LocalDate.now().minusDays(5), LocalDate.now().plusDays(5));
		List<Discount> activeDiscounts = Collections.singletonList(discountToUnassign);

		when(scanner.nextLine()).thenReturn(productCode).thenReturn(String.valueOf(discountId));

		when(productRepository.findByCode(productCode)).thenReturn(product);
		when(discountRepository.findDiscountsByProductCode(productCode, LocalDate.now())).thenReturn(activeDiscounts);
		when(discountRepository.findById(discountId)).thenReturn(discountToUnassign);
		when(discountRepository.unassignDiscountFromProduct(productCode, discountId)).thenReturn(false);

		unassignDiscountCommand.execute();

		verify(productRepository, times(1)).findByCode(productCode);
		verify(discountRepository, times(1)).findDiscountsByProductCode(productCode, LocalDate.now());
		verify(discountRepository, times(1)).findById(discountId);
		verify(discountRepository, times(1)).unassignDiscountFromProduct(productCode, discountId);

		String output = outContent.toString();
		assertTrue(output.contains("Failed to unassign discount. This might happen if the assignment didn't exist."));
	}
}