package com.test.command;

import com.syos.command.ViewExpiringBatchesCommand;
import com.syos.model.StockBatch;
import com.syos.singleton.InventoryManager;

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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ViewExpiringBatchesCommandTest {

    @Mock
    private InventoryManager inventoryManager;
    @Mock
    private Scanner scanner;

    private ViewExpiringBatchesCommand viewExpiringBatchesCommand;

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final String NL = System.lineSeparator(); // Use system-dependent newline

    // Define the separator line used in the command for consistency (74 hyphens)
    private final String TABLE_SEPARATOR = "--------------------------------------------------------------------------";

    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(outContent));
        viewExpiringBatchesCommand = new ViewExpiringBatchesCommand(inventoryManager, scanner);
    }

    @AfterEach
    void restoreStreams() {
        System.setOut(originalOut);
    }

    @Test
    @DisplayName("Should display error message for non-numeric input")
    void testExecute_invalidInput_notANumber() {
        // Arrange
        when(scanner.nextLine()).thenReturn("abc");

        // Act
        viewExpiringBatchesCommand.execute();

        // Assert
        String expectedOutput = NL + "--- View Expiring Stock Batches (Back-Store) ---" + NL +
                                "Enter expiry threshold in days (e.g., 30 for batches expiring in next 30 days): " +
                                "Invalid input. Please enter a number for days threshold." + NL;
        assertEquals(expectedOutput, outContent.toString());
        verifyNoInteractions(inventoryManager); // Should not call inventory manager
    }

    @Test
    @DisplayName("Should display error message for negative days threshold")
    void testExecute_invalidInput_negativeNumber() {
        // Arrange
        when(scanner.nextLine()).thenReturn("-5");

        // Act
        viewExpiringBatchesCommand.execute();

        // Assert
        String expectedOutput = NL + "--- View Expiring Stock Batches (Back-Store) ---" + NL +
                                "Enter expiry threshold in days (e.g., 30 for batches expiring in next 30 days): " +
                                "Expiry threshold must be a non-negative number." + NL;
        assertEquals(expectedOutput, outContent.toString());
        verifyNoInteractions(inventoryManager); // Should not call inventory manager
    }

    @Test
    @DisplayName("Should display message when no expiring batches are found")
    void testExecute_noExpiringBatchesFound() {
        // Arrange
        int daysThreshold = 30;
        when(scanner.nextLine()).thenReturn(String.valueOf(daysThreshold));
        when(inventoryManager.getAllExpiringBatches(daysThreshold)).thenReturn(Collections.emptyList());

        // Act
        viewExpiringBatchesCommand.execute();

        // Assert
        verify(inventoryManager, times(1)).getAllExpiringBatches(daysThreshold);
        String expectedOutput = NL + "--- View Expiring Stock Batches (Back-Store) ---" + NL +
                                "Enter expiry threshold in days (e.g., 30 for batches expiring in next 30 days): " +
                                String.format("No stock batches found expiring within %d days in the back-store.%n", daysThreshold);
        assertEquals(expectedOutput, outContent.toString());
    }

    @Test
    @DisplayName("Should display a single expiring batch correctly")
    void testExecute_expiringBatchesExist_singleBatch() {
        // Arrange
        int daysThreshold = 7;
        when(scanner.nextLine()).thenReturn(String.valueOf(daysThreshold));

        // Note: Current date is June 6, 2025. A batch expiring within 7 days would be up to June 13, 2025.
        StockBatch batch = new StockBatch(101, "PROD001", LocalDate.of(2025, 5, 1), LocalDate.of(2025, 6, 10), 50); // Expiring within 7 days
        List<StockBatch> expiringBatches = Collections.singletonList(batch);
        when(inventoryManager.getAllExpiringBatches(daysThreshold)).thenReturn(expiringBatches);

        // Act
        viewExpiringBatchesCommand.execute();

        // Assert
        verify(inventoryManager, times(1)).getAllExpiringBatches(daysThreshold);

        StringBuilder expectedOutput = new StringBuilder();
        expectedOutput.append(NL).append("--- View Expiring Stock Batches (Back-Store) ---").append(NL);
        expectedOutput.append("Enter expiry threshold in days (e.g., 30 for batches expiring in next 30 days): ");
        expectedOutput.append(String.format("%n--- Stock Batches Expiring in Next %d Days ---%n", daysThreshold));
        expectedOutput.append(TABLE_SEPARATOR).append(NL);
        expectedOutput.append(String.format("%-10s %-15s %-15s %-15s %-15s%n", "Batch ID", "Product Code", "Expiry Date", "Purch Date", "Remaining Qty"));
        expectedOutput.append(TABLE_SEPARATOR).append(NL);
        expectedOutput.append(String.format("%-10d %-15s %-15s %-15s %-15d%n",
                batch.getId(), batch.getProductCode(), batch.getExpiryDate().toString(),
                batch.getPurchaseDate().toString(), batch.getQuantityRemaining()));
        expectedOutput.append(TABLE_SEPARATOR).append(NL);

        assertEquals(expectedOutput.toString(), outContent.toString());
    }

    @Test
    @DisplayName("Should display multiple expiring batches correctly")
    void testExecute_expiringBatchesExist_multipleBatches() {
        // Arrange
        int daysThreshold = 90;
        when(scanner.nextLine()).thenReturn(String.valueOf(daysThreshold));

        // Note: Current date is June 6, 2025.
        StockBatch batch1 = new StockBatch(201, "PROD002", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 7, 15), 100); // Expiring within 90 days
        StockBatch batch2 = new StockBatch(202, "PROD003", LocalDate.of(2025, 2, 10), LocalDate.of(2025, 9, 1), 75); // Expiring within 90 days
        StockBatch batch3 = new StockBatch(203, "PROD001", LocalDate.of(2025, 3, 5), LocalDate.of(2025, 6, 20), 20); // Expiring within 90 days (and also very soon)
        List<StockBatch> expiringBatches = Arrays.asList(batch1, batch2, batch3);
        when(inventoryManager.getAllExpiringBatches(daysThreshold)).thenReturn(expiringBatches);

        // Act
        viewExpiringBatchesCommand.execute();

        // Assert
        verify(inventoryManager, times(1)).getAllExpiringBatches(daysThreshold);

        StringBuilder expectedOutput = new StringBuilder();
        expectedOutput.append(NL).append("--- View Expiring Stock Batches (Back-Store) ---").append(NL);
        expectedOutput.append("Enter expiry threshold in days (e.g., 30 for batches expiring in next 30 days): ");
        expectedOutput.append(String.format("%n--- Stock Batches Expiring in Next %d Days ---%n", daysThreshold));
        expectedOutput.append(TABLE_SEPARATOR).append(NL);
        expectedOutput.append(String.format("%-10s %-15s %-15s %-15s %-15s%n", "Batch ID", "Product Code", "Expiry Date", "Purch Date", "Remaining Qty"));
        expectedOutput.append(TABLE_SEPARATOR).append(NL);
        for (StockBatch batch : expiringBatches) {
            expectedOutput.append(String.format("%-10d %-15s %-15s %-15s %-15d%n",
                    batch.getId(), batch.getProductCode(), batch.getExpiryDate().toString(),
                    batch.getPurchaseDate().toString(), batch.getQuantityRemaining()));
        }
        expectedOutput.append(TABLE_SEPARATOR).append(NL);

        assertEquals(expectedOutput.toString(), outContent.toString());
    }
}