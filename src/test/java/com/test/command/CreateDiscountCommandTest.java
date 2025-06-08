package com.test.command;

import com.syos.command.CreateDiscountCommand;
import com.syos.enums.DiscountType;
import com.syos.repository.DiscountRepository;
import org.junit.jupiter.api.*;
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
        createDiscountCommand = new CreateDiscountCommand(scanner, discountRepository);
    }

    @AfterEach
    void restoreStreams() {
        System.setOut(originalOut);
    }

    @Test
    @DisplayName("Creates a PERCENT discount with valid input")
    void shouldSuccessfullyCreatePercentDiscount() {
        String name = "Black Friday";
        String typeInput = "PERCENT";
        String valueInput = "20.0";
        String startDateInput = "2025-11-20";
        String endDateInput = "2025-11-30";
        int expectedId = 100;

        when(scanner.nextLine()).thenReturn(name, typeInput, valueInput, startDateInput, endDateInput);
        when(discountRepository.createDiscount(eq(name), eq(DiscountType.PERCENT), eq(20.0),
                eq(LocalDate.parse(startDateInput)), eq(LocalDate.parse(endDateInput)))).thenReturn(expectedId);

        createDiscountCommand.execute();

        String output = outContent.toString();
        assertTrue(output.contains("Success: Discount 'Black Friday' (ID=100) created"));
    }

    @Test
    @DisplayName("Creates an AMOUNT discount with valid input")
    void shouldSuccessfullyCreateAmountDiscount() {
        String name = "Holiday Flat Rate";
        String typeInput = "AMOUNT";
        String valueInput = "5.0";
        String startDateInput = "2025-12-01";
        String endDateInput = "2025-12-25";
        int expectedId = 101;

        when(scanner.nextLine()).thenReturn(name, typeInput, valueInput, startDateInput, endDateInput);
        when(discountRepository.createDiscount(eq(name), eq(DiscountType.AMOUNT), eq(5.0),
                eq(LocalDate.parse(startDateInput)), eq(LocalDate.parse(endDateInput)))).thenReturn(expectedId);

        createDiscountCommand.execute();

        String output = outContent.toString();
        assertTrue(output.contains("Success: Discount 'Holiday Flat Rate' (ID=101) created"));
    }

    @ParameterizedTest
    @MethodSource("invalidInputTestCases")
    @DisplayName("Handles invalid inputs and succeeds after retries")
    void shouldHandleInvalidInputsAndEventuallySucceed(String[] scannerInputs, String expectedFinalName,
                                                       DiscountType expectedFinalType, double expectedFinalValue,
                                                       LocalDate expectedFinalStartDate, LocalDate expectedFinalEndDate,
                                                       String[] expectedErrorMessages) {
        when(scanner.nextLine()).thenReturn(scannerInputs[0],
                Stream.of(scannerInputs).skip(1).toArray(String[]::new));

        when(discountRepository.createDiscount(eq(expectedFinalName), eq(expectedFinalType), eq(expectedFinalValue),
                eq(expectedFinalStartDate), eq(expectedFinalEndDate))).thenReturn(1);

        createDiscountCommand.execute();

        String output = outContent.toString();
        for (String errorMessage : expectedErrorMessages) {
            assertTrue(output.contains(errorMessage));
        }
        assertTrue(output.contains("Success: Discount '" + expectedFinalName));
    }

    private static Stream<Arguments> invalidInputTestCases() {
        return Stream.of(
            Arguments.of(new String[]{"", "Valid", "PERCENT", "10", "2025-01-01", "2025-01-31"},
                    "Valid", DiscountType.PERCENT, 10.0,
                    LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31),
                    new String[]{"Error: Discount name cannot be empty."}),
            Arguments.of(new String[]{"Test", "INVALID", "AMOUNT", "5", "2025-01-01", "2025-01-31"},
                    "Test", DiscountType.AMOUNT, 5.0,
                    LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31),
                    new String[]{"Error: Invalid discount type. Use PERCENT or AMOUNT."})
        );
    }

    @Test
    @DisplayName("Handles repository exception during creation")
    void shouldHandleRepositoryRuntimeException() {
        String name = "ErrorTest";
        when(scanner.nextLine()).thenReturn(name, "PERCENT", "10", "2025-01-01", "2025-01-31");

        doThrow(new RuntimeException("Database down")).when(discountRepository)
                .createDiscount(anyString(), any(), anyDouble(), any(), any());

        createDiscountCommand.execute();

        String output = outContent.toString();
        assertTrue(output.contains("Error: An unexpected error occurred while creating the discount"));
        assertFalse(output.contains("Success:"));
    }


    @Test
    @DisplayName("Handles case-insensitive discount type input")
    void shouldHandleCaseInsensitiveType() {
        when(scanner.nextLine()).thenReturn("Lowercase Test", "percent", "15", "2025-05-01", "2025-05-31");
        when(discountRepository.createDiscount(eq("Lowercase Test"), eq(DiscountType.PERCENT), eq(15.0),
                any(), any())).thenReturn(999);

        createDiscountCommand.execute();

        String output = outContent.toString();
        assertTrue(output.contains("Success: Discount 'Lowercase Test' (ID=999) created"));
    }

    @Test
    @DisplayName("Trims whitespace from name and type input")
    void shouldTrimWhitespaceFromInputs() {
        when(scanner.nextLine()).thenReturn("   Trim Me  ", "  amount  ", "10", "2025-06-01", "2025-06-30");
        when(discountRepository.createDiscount(eq("Trim Me"), eq(DiscountType.AMOUNT), eq(10.0),
                any(), any())).thenReturn(888);

        createDiscountCommand.execute();

        String output = outContent.toString();
        assertTrue(output.contains("Success: Discount 'Trim Me' (ID=888) created"));
    }

    @Test
    @DisplayName("Handles multiple sequential invalid entries before success")
    void shouldHandleMultipleFailuresBeforeSuccess() {
        when(scanner.nextLine()).thenReturn(
                "", "   ", "New Year Deal",        // Name (2 invalid, 1 valid)
                "invalid", "wrong", "PERCENT",    // Type (2 invalid, 1 valid)
                "-1", "200", "25",                // Value (2 invalid, 1 valid)
                "2025/01/01", "2025-01-01",       // Start Date (1 invalid, 1 valid)
                "not-a-date", "2025-01-10"        // End Date (1 invalid, 1 valid)
        );

        when(discountRepository.createDiscount(eq("New Year Deal"), eq(DiscountType.PERCENT), eq(25.0),
                eq(LocalDate.of(2025, 1, 1)), eq(LocalDate.of(2025, 1, 10)))).thenReturn(777);

        createDiscountCommand.execute();

        String output = outContent.toString();
        assertTrue(output.contains("Success: Discount 'New Year Deal' (ID=777) created"));
    }
}
