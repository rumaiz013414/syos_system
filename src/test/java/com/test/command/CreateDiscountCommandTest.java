package com.test.command;

import com.syos.command.CreateDiscountCommand;
import com.syos.enums.DiscountType;
import com.syos.repository.DiscountRepository;
import com.syos.service.DiscountCreationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDate;
import java.util.Scanner;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateDiscountCommandTest {

	@Mock
	private Scanner scanner;
	@Mock
	private DiscountRepository discountRepository;

	private CreateDiscountCommand createDiscountCommand;
	private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
	private final PrintStream originalOut = System.out;

	@BeforeEach
	void setUp() {
		System.setOut(new PrintStream(outContent));
		new DiscountCreationService(scanner, discountRepository);
		createDiscountCommand = new CreateDiscountCommand(scanner, discountRepository);
	}

	@AfterEach
	void restoreStreams() {
		System.setOut(originalOut);
	}

	// --- Success Scenarios ---

	@Test
	@DisplayName("Should successfully create a PERCENT discount with valid inputs")
	void shouldSuccessfullyCreatePercentDiscount() {
		// Arrange
		String name = "Black Friday";
		String typeInput = "PERCENT";
		String valueInput = "20.0";
		String startDateInput = "2025-11-20";
		String endDateInput = "2025-11-30";
		int expectedId = 100;

		when(scanner.nextLine()).thenReturn(name).thenReturn(typeInput).thenReturn(valueInput)
				.thenReturn(startDateInput).thenReturn(endDateInput);

		when(discountRepository.createDiscount(eq(name), eq(DiscountType.PERCENT), eq(20.0),
				eq(LocalDate.parse(startDateInput)), eq(LocalDate.parse(endDateInput)))).thenReturn(expectedId);

		// Act
		createDiscountCommand.execute();

		// Assert
		verify(scanner, times(5)).nextLine(); // Name, Type, Value, StartDate, EndDate
		verify(discountRepository, times(1)).createDiscount(anyString(), any(DiscountType.class), anyDouble(),
				any(LocalDate.class), any(LocalDate.class));

		String output = outContent.toString();
		assertTrue(output.contains("=== Create New Discount ==="));
		assertTrue(output.contains(String.format("Success: Discount '%s' (ID=%d) created from %s to %s.", name,
				expectedId, startDateInput, endDateInput)));
	}

	@Test
	@DisplayName("Should successfully create an AMOUNT discount with valid inputs")
	void shouldSuccessfullyCreateAmountDiscount() {
		// Arrange
		String name = "Holiday Flat Rate";
		String typeInput = "AMOUNT";
		String valueInput = "5.0";
		String startDateInput = "2025-12-01";
		String endDateInput = "2025-12-25";
		int expectedId = 101;

		when(scanner.nextLine()).thenReturn(name).thenReturn(typeInput).thenReturn(valueInput)
				.thenReturn(startDateInput).thenReturn(endDateInput);

		when(discountRepository.createDiscount(eq(name), eq(DiscountType.AMOUNT), eq(5.0),
				eq(LocalDate.parse(startDateInput)), eq(LocalDate.parse(endDateInput)))).thenReturn(expectedId);

		// Act
		createDiscountCommand.execute();

		// Assert
		verify(scanner, times(5)).nextLine();
		verify(discountRepository, times(1)).createDiscount(anyString(), any(DiscountType.class), anyDouble(),
				any(LocalDate.class), any(LocalDate.class));

		String output = outContent.toString();
		assertTrue(output.contains("=== Create New Discount ==="));
		assertTrue(output.contains(String.format("Success: Discount '%s' (ID=%d) created from %s to %s.", name,
				expectedId, startDateInput, endDateInput)));
	}

	// --- Failure Scenarios (Validations) ---

	@ParameterizedTest
	@MethodSource("invalidInputTestCases")
	@DisplayName("Should handle invalid inputs and re-prompt until valid data is entered")
	void shouldHandleInvalidInputsAndEventuallySucceed(String[] scannerInputs, // All inputs for scanner.nextLine() in
																				// sequence
			String expectedFinalName, DiscountType expectedFinalType, double expectedFinalValue,
			LocalDate expectedFinalStartDate, LocalDate expectedFinalEndDate, String[] expectedErrorMessages) {

		// Arrange
		// Chain all scanner inputs
		when(scanner.nextLine()).thenReturn(scannerInputs[0], Stream.of(scannerInputs).skip(1).toArray(String[]::new));

		// Mock repository for successful creation with final valid inputs
		when(discountRepository.createDiscount(eq(expectedFinalName), eq(expectedFinalType), eq(expectedFinalValue),
				eq(expectedFinalStartDate), eq(expectedFinalEndDate))).thenReturn(1); // Return a dummy ID for success

		// Act
		createDiscountCommand.execute();

		// Assert
		// Verify createDiscount was called exactly once with the final, valid inputs
		verify(discountRepository, times(1)).createDiscount(expectedFinalName, expectedFinalType, expectedFinalValue,
				expectedFinalStartDate, expectedFinalEndDate);

		// Verify error messages appeared for invalid inputs
		String output = outContent.toString();
		for (String errorMessage : expectedErrorMessages) {
			if (errorMessage != null) {
				assertTrue(output.contains(errorMessage), "Output should contain error: " + errorMessage);
			}
		}
		// Verify success message
		assertTrue(output.contains(String.format("Success: Discount '%s'", expectedFinalName)));
	}

	// Method to provide arguments for the parameterized test
	private static Stream<Arguments> invalidInputTestCases() {
		LocalDate today = LocalDate.now();
		LocalDate yesterday = today.minusDays(1);
		LocalDate tomorrow = today.plusDays(1);

		return Stream.of(
				// 1. Invalid Name -> Valid Name
				Arguments.of(new String[] { "", "Valid Name", "PERCENT", "10.0", "2025-01-01", "2025-01-31" },
						"Valid Name", DiscountType.PERCENT, 10.0, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31),
						new String[] { "Error: Discount name cannot be empty." }),

				// 2. Invalid Type -> Valid Type
				Arguments.of(
						new String[] { "Valid Name", "INVALID_TYPE", "PERCENT", "10.0", "2025-01-01", "2025-01-31" },
						"Valid Name", DiscountType.PERCENT, 10.0, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31),
						new String[] { "Error: Invalid discount type. Use PERCENT or AMOUNT." }),

				// 3. Invalid Value Format -> Valid Value
				Arguments.of(new String[] { "Valid Name", "PERCENT", "abc", "10.0", "2025-01-01", "2025-01-31" },
						"Valid Name", DiscountType.PERCENT, 10.0, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31),
						new String[] { "Error: Invalid number format for discount value." }),

				// 4. Negative Value -> Valid Value
				Arguments.of(new String[] { "Valid Name", "AMOUNT", "-5.0", "5.0", "2025-01-01", "2025-01-31" },
						"Valid Name", DiscountType.AMOUNT, 5.0, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31),
						new String[] { "Error: Discount value must be non-negative." }),

				// 5. Percentage > 100 -> Valid Percentage
				Arguments.of(new String[] { "Valid Name", "PERCENT", "101.0", "50.0", "2025-01-01", "2025-01-31" },
						"Valid Name", DiscountType.PERCENT, 50.0, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31),
						new String[] { "Error: Percentage cannot exceed 100." }),

				// 6. Invalid Start Date Format -> Valid Start Date
				Arguments.of(new String[] { "Valid Name", "PERCENT", "10.0", "2025/01/01", "2025-01-01", "2025-01-31" },
						"Valid Name", DiscountType.PERCENT, 10.0, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31),
						new String[] { "Error: Invalid start date format. Please use YYYY-MM-DD." }),

				// 7. Invalid End Date Format -> Valid End Date
				Arguments.of(new String[] { "Valid Name", "PERCENT", "10.0", "2025-01-01", "01-01-2025", "2025-01-31" },
						"Valid Name", DiscountType.PERCENT, 10.0, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31),
						new String[] { "Error: Invalid end date format. Please use YYYY-MM-DD." }),

				// 8. End Date before Start Date -> Valid End Date
				Arguments.of(new String[] { "Valid Name", "PERCENT", "10.0", "2025-01-31", "2025-01-01", "2025-02-01" },
						"Valid Name", DiscountType.PERCENT, 10.0, LocalDate.of(2025, 1, 31), LocalDate.of(2025, 2, 1),
						new String[] { "Error: End date cannot be before start date." }),

				// 9. All Invalid Inputs (chained) -> All Valid Inputs
				Arguments.of(new String[] { "", "All Valid", // Name
						"INVALID", "PERCENT", // Type
						"-10.0", "5.0", // Value
						"bad-date", "2025-01-01", // Start Date
						"another-bad", "2025-01-31" // End Date
				}, "All Valid", DiscountType.PERCENT, 5.0, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31),
						new String[] { "Error: Discount name cannot be empty.",
								"Error: Invalid discount type. Use PERCENT or AMOUNT.",
								"Error: Discount value must be non-negative.",
								"Error: Invalid start date format. Please use YYYY-MM-DD.",
								"Error: Invalid end date format. Please use YYYY-MM-DD." }));
	}

	// --- Exception Handling Scenario ---

	@Test
	@DisplayName("Should handle RuntimeException from DiscountRepository.createDiscount")
	void shouldHandleRepositoryRuntimeException() {
		// Arrange
		String name = "Repo Error Test";
		String typeInput = "PERCENT";
		String valueInput = "10.0";
		String startDateInput = "2025-01-01";
		String endDateInput = "2025-01-31";
		String errorMessage = "Database connection failed during insert.";

		when(scanner.nextLine()).thenReturn(name).thenReturn(typeInput).thenReturn(valueInput)
				.thenReturn(startDateInput).thenReturn(endDateInput);

		// Simulate repository throwing an exception during creation
		doThrow(new RuntimeException(errorMessage)).when(discountRepository).createDiscount(anyString(),
				any(DiscountType.class), anyDouble(), any(LocalDate.class), any(LocalDate.class));

		// Act
		createDiscountCommand.execute();

		// Assert
		verify(scanner, times(5)).nextLine();
		verify(discountRepository, times(1)).createDiscount(anyString(), any(DiscountType.class), anyDouble(),
				any(LocalDate.class), any(LocalDate.class));

		String output = outContent.toString();
		assertTrue(output.contains("Error: An unexpected error occurred while creating the discount: " + errorMessage));
		assertFalse(output.contains("Success:")); // Ensure no success message
	}

	@Test
	@DisplayName("Should handle repository returning -1 (failure to get ID)")
	void shouldHandleRepositoryReturnsNegativeOne() {
		// Arrange
		String name = "Repo Returns -1";
		String typeInput = "PERCENT";
		String valueInput = "10.0";
		String startDateInput = "2025-01-01";
		String endDateInput = "2025-01-31";

		when(scanner.nextLine()).thenReturn(name).thenReturn(typeInput).thenReturn(valueInput)
				.thenReturn(startDateInput).thenReturn(endDateInput);

		// Simulate repository returning -1 (e.g., if it failed to get a generated ID)
		when(discountRepository.createDiscount(anyString(), any(DiscountType.class), anyDouble(), any(LocalDate.class),
				any(LocalDate.class))).thenReturn(-1);

		// Act
		createDiscountCommand.execute();

		// Assert
		verify(scanner, times(5)).nextLine();
		verify(discountRepository, times(1)).createDiscount(anyString(), any(DiscountType.class), anyDouble(),
				any(LocalDate.class), any(LocalDate.class));

		String output = outContent.toString();
		assertTrue(output.contains("Error: Failed to create discount (repository returned -1)."));
		assertFalse(output.contains("Success:"));
	}
}