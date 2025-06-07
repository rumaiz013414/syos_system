package com.test.command;

import com.syos.command.ViewAllDiscountsCommand;
import com.syos.model.Discount;
import com.syos.repository.DiscountRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ViewAllDiscountsCommandTest {

    @Mock
    private DiscountRepository discountRepository;
    @Mock
    private Scanner scanner;

    private ViewAllDiscountsCommand viewAllDiscountsCommand;

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final String NL = System.lineSeparator(); // Use system-dependent newline

    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(outContent));
        viewAllDiscountsCommand = new ViewAllDiscountsCommand(discountRepository, scanner);
    }

    @AfterEach
    void restoreStreams() {
        System.setOut(originalOut);
    }

    @Test
    @DisplayName("Should display message when no discounts have been created")
    void shouldDisplayNoDiscountsMessageWhenRepositoryIsEmpty() {
        // Arrange
        when(discountRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        viewAllDiscountsCommand.execute();

        // Assert
        verify(discountRepository, times(1)).findAll();
        // Updated expected output to reflect the consistent initial newline
        String expectedOutput = NL + "--- All Available Discounts ---" + NL +
                                "No discounts have been created yet." + NL;
        assertEquals(expectedOutput, outContent.toString());
    }

    @Test
    @DisplayName("Should display all discounts correctly when multiple discounts exist")
    void shouldDisplayAllDiscountsWhenDiscountsExist() {
        // Arrange
        LocalDate startDate1 = LocalDate.of(2025, 1, 1);
        LocalDate endDate1 = LocalDate.of(2025, 1, 31);
        LocalDate startDate2 = LocalDate.of(2025, 6, 1);
        LocalDate endDate2 = LocalDate.of(2025, 6, 15);
        LocalDate startDate3 = LocalDate.of(2024, 12, 1);
        LocalDate endDate3 = LocalDate.of(2025, 1, 15);

        Discount discount1 = new Discount(1, "Winter Sale", DiscountType.PERCENT, 10.5, startDate1, endDate1);
        Discount discount2 = new Discount(2, "Summer Deal", DiscountType.AMOUNT, 25.0, startDate2, endDate2);
        Discount discount3 = new Discount(3, "Clearance", DiscountType.PERCENT, 50.0, startDate3, endDate3);

        List<Discount> discounts = Arrays.asList(discount1, discount2, discount3);
        when(discountRepository.findAll()).thenReturn(discounts);

        // Act
        viewAllDiscountsCommand.execute();

        // Assert
        verify(discountRepository, times(1)).findAll();

        StringBuilder expectedOutput = new StringBuilder();
        expectedOutput.append(NL).append("--- All Available Discounts ---").append(NL);
        expectedOutput.append(String.format("%-5s %-20s %-15s %-15s %-15s %-15s", "ID", "Name", "Type", "Value", "Start Date", "End Date")).append(NL);
        // Updated separator line
        expectedOutput.append("-----------------------------------------------------------------------------------------").append(NL);

        // Format and append each discount line
        expectedOutput.append(String.format("%-5d %-20s %-15s %-15s %-15s %-15s",
                discount1.getId(), discount1.getName(), "Percentage", String.format("%.2f%%", discount1.getValue()),
                discount1.getStart().toString(), discount1.getEnd().toString())).append(NL);

        expectedOutput.append(String.format("%-5d %-20s %-15s %-15s %-15s %-15s",
                discount2.getId(), discount2.getName(), "Fixed Amount", String.format("%.2f", discount2.getValue()),
                discount2.getStart().toString(), discount2.getEnd().toString())).append(NL);

        expectedOutput.append(String.format("%-5d %-20s %-15s %-15s %-15s %-15s",
                discount3.getId(), discount3.getName(), "Percentage", String.format("%.2f%%", discount3.getValue()),
                discount3.getStart().toString(), discount3.getEnd().toString())).append(NL);
        
        // Updated separator line
        expectedOutput.append("-----------------------------------------------------------------------------------------").append(NL);

        assertEquals(expectedOutput.toString(), outContent.toString());
    }

    @Test
    @DisplayName("Should correctly format a single percentage discount")
    void shouldHandleSinglePercentageDiscountCorrectly() {
        // Arrange
        LocalDate startDate = LocalDate.of(2025, 7, 1);
        LocalDate endDate = LocalDate.of(2025, 7, 7);
        Discount singleDiscount = new Discount(5, "Weekend Deal", DiscountType.PERCENT, 7.75, startDate, endDate);
        List<Discount> discounts = Collections.singletonList(singleDiscount);
        when(discountRepository.findAll()).thenReturn(discounts);

        // Act
        viewAllDiscountsCommand.execute();

        // Assert
        verify(discountRepository, times(1)).findAll();
        StringBuilder expectedOutput = new StringBuilder();
        expectedOutput.append(NL).append("--- All Available Discounts ---").append(NL);
        expectedOutput.append(String.format("%-5s %-20s %-15s %-15s %-15s %-15s", "ID", "Name", "Type", "Value", "Start Date", "End Date")).append(NL);
        // Updated separator line
        expectedOutput.append("-----------------------------------------------------------------------------------------").append(NL);
        expectedOutput.append(String.format("%-5d %-20s %-15s %-15s %-15s %-15s",
                singleDiscount.getId(), singleDiscount.getName(), "Percentage", String.format("%.2f%%", singleDiscount.getValue()),
                singleDiscount.getStart().toString(), singleDiscount.getEnd().toString())).append(NL);
        // Updated separator line
        expectedOutput.append("-----------------------------------------------------------------------------------------").append(NL);
        
        assertEquals(expectedOutput.toString(), outContent.toString());
    }

    @Test
    @DisplayName("Should correctly format a single fixed amount discount")
    void shouldHandleSingleFixedAmountDiscountCorrectly() {
        // Arrange
        LocalDate startDate = LocalDate.of(2025, 8, 10);
        LocalDate endDate = LocalDate.of(2025, 8, 20);
        Discount singleDiscount = new Discount(6, "Back-to-School", DiscountType.AMOUNT, 12.34, startDate, endDate);
        List<Discount> discounts = Collections.singletonList(singleDiscount);
        when(discountRepository.findAll()).thenReturn(discounts);

        // Act
        viewAllDiscountsCommand.execute();

        // Assert
        verify(discountRepository, times(1)).findAll();
        StringBuilder expectedOutput = new StringBuilder();
        expectedOutput.append(NL).append("--- All Available Discounts ---").append(NL);
        expectedOutput.append(String.format("%-5s %-20s %-15s %-15s %-15s %-15s", "ID", "Name", "Type", "Value", "Start Date", "End Date")).append(NL);
        // Updated separator line
        expectedOutput.append("-----------------------------------------------------------------------------------------").append(NL);
        expectedOutput.append(String.format("%-5d %-20s %-15s %-15s %-15s %-15s",
                singleDiscount.getId(), singleDiscount.getName(), "Fixed Amount", String.format("%.2f", singleDiscount.getValue()),
                singleDiscount.getStart().toString(), singleDiscount.getEnd().toString())).append(NL);
        // Updated separator line
        expectedOutput.append("-----------------------------------------------------------------------------------------").append(NL);
        
        assertEquals(expectedOutput.toString(), outContent.toString());
    }
}