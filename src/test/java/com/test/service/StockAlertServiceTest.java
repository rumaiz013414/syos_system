package com.test.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.syos.service.StockAlertService;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class StockAlertServiceTest {

	private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();
	private final PrintStream originalSystemOut = System.out; // Stores the original System.out

	@BeforeEach
	void setUp() {
		// Redirect System.out to our ByteArrayOutputStream
		System.setOut(new PrintStream(outputStreamCaptor));
	}

	@AfterEach
	void tearDown() {
		// Restore System.out to its original stream after each test
		System.setOut(originalSystemOut);
	}

	@Test
	@DisplayName("Should print low stock alert message correctly")
	void shouldPrintLowStockAlertCorrectly() {
		// Given
		int threshold = 5;
		String productCode = "PROD001";
		int remaining = 3;

		// Create an instance of the service with the given threshold
		StockAlertService stockAlertService = new StockAlertService(threshold);

		// When
		// Call the method to be tested
		stockAlertService.onStockLow(productCode, remaining);

		// Then
		// Construct the expected output string
		String expectedOutput = String.format("!LOW STOCK: %s remaining=%d (threshold=%d)%n", productCode, remaining,
				threshold);

		// Verify that the captured output contains the expected message
		// We use .contains() because there might be other system outputs or line
		// endings variations
		assertTrue(outputStreamCaptor.toString().contains(expectedOutput.trim()));
	}

	@Test
	@DisplayName("Should print low stock alert with different values")
	void shouldPrintLowStockAlertWithDifferentValues() {
		// Given
		int threshold = 10;
		String productCode = "ITEMX-A23";
		int remaining = 7;

		StockAlertService stockAlertService = new StockAlertService(threshold);

		// When
		stockAlertService.onStockLow(productCode, remaining);

		// Then
		String expectedOutput = String.format("!LOW STOCK: %s remaining=%d (threshold=%d)%n", productCode, remaining,
				threshold);
		assertTrue(outputStreamCaptor.toString().contains(expectedOutput.trim()));
	}
}