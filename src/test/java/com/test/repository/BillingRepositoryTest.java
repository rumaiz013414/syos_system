package com.test.repository;

import com.syos.db.DatabaseManager;
import com.syos.model.Bill;
import com.syos.model.BillItem;
import com.syos.model.Product;
import com.syos.repository.BillingRepository;
// import com.syos.enums.TransactionType; // Not using enum in Bill constructor

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.MockedStatic;

import java.sql.*;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillingRepositoryTest {

    private BillingRepository billingRepository;

    @Mock
    private DatabaseManager mockDatabaseManager;
    @Mock
    private Connection mockConnection;
    @Mock
    private PreparedStatement mockPsBill;
    @Mock
    private PreparedStatement mockPsItem;
    @Mock
    private ResultSet mockRs;

    private MockedStatic<DatabaseManager> mockedDatabaseManagerStatic;

    @BeforeEach
    void setUp() throws SQLException {
        mockedDatabaseManagerStatic = mockStatic(DatabaseManager.class);
        when(DatabaseManager.getInstance()).thenReturn(mockDatabaseManager);
        when(mockDatabaseManager.getConnection()).thenReturn(mockConnection);

        billingRepository = new BillingRepository();

        // Common setup for prepared statements
        // We set up mockPsBill and mockPsItem to be returned sequentially for the `save` method.
        // For `nextSerial` tests, we will explicitly reset `mockConnection` and set up the `prepareStatement`
        // behavior specifically for that test.
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPsBill, mockPsItem);

        // Ensure auto-commit and commit/rollback are handled
        doNothing().when(mockConnection).setAutoCommit(anyBoolean());
        doNothing().when(mockConnection).commit();
        doNothing().when(mockConnection).rollback();

        // Ensure all resources in try-with-resources blocks are closed
        doNothing().when(mockConnection).close();
        doNothing().when(mockPsBill).close(); // <--- ADDED
        doNothing().when(mockPsItem).close(); // <--- ADDED (for save method, not strictly needed for nextSerial tests but good practice)
        doNothing().when(mockRs).close();     // <--- ADDED
    }

    @AfterEach
    void tearDown() {
        if (mockedDatabaseManagerStatic != null) {
            mockedDatabaseManagerStatic.close();
        }
    }

    @Test
    @DisplayName("Should successfully save a bill and its items, then commit transaction")
    void save_validBillWithItems_commitsTransaction() throws SQLException {
        // Arrange
        Bill bill = new Bill(0, 1, new Date(), 100.0, 100.0, 0.0, "POS");

        Product product = new Product("P001", "Item A", 10.0);
        BillItem item1 = new BillItem(0, 0, product, 5, 45.0, 5.0);
        BillItem item2 = new BillItem(0, 0, product, 2, 20.0, 0.0);
        bill.setItems(Arrays.asList(item1, item2));

        // Mock ResultSet for generated ID for the bill
        when(mockPsBill.executeQuery()).thenReturn(mockRs);
        when(mockRs.next()).thenReturn(true);
        when(mockRs.getInt(1)).thenReturn(123);

        // Mock executeBatch for bill items
        when(mockPsItem.executeBatch()).thenReturn(new int[]{1, 1});

        // Act
        billingRepository.save(bill);

        // Assert
        assertEquals(123, bill.getId(), "Bill ID should be updated with the generated ID");

        // Verify connection setup
        verify(mockConnection).setAutoCommit(false);

        // Verify bill PreparedStatement execution
        verify(mockConnection).prepareStatement(argThat(sql -> sql.contains("INSERT INTO bill")));
        verify(mockPsBill).setInt(1, bill.getSerialNumber());
        verify(mockPsBill).setTimestamp(2, any(Timestamp.class));
        verify(mockPsBill).setDouble(3, bill.getTotalAmount());
        verify(mockPsBill).setDouble(4, bill.getCashTendered());
        verify(mockPsBill).setDouble(5, bill.getChangeReturned());
        verify(mockPsBill).setString(6, bill.getTransactionType());
        verify(mockPsBill).executeQuery();

        // Verify bill item PreparedStatement execution (addBatch calls)
        verify(mockConnection).prepareStatement(argThat(sql -> sql.contains("INSERT INTO bill_item")));
        verify(mockPsItem, times(2)).addBatch();

        verify(mockPsItem, atLeastOnce()).setInt(1, 123);
        verify(mockPsItem, atLeastOnce()).setString(2, "P001");
        verify(mockPsItem, atLeastOnce()).setInt(3, 5);
        verify(mockPsItem, atLeastOnce()).setDouble(4, 45.0);
        verify(mockPsItem, atLeastOnce()).setDouble(5, 5.0);

        verify(mockPsItem, atLeastOnce()).setInt(3, 2);
        verify(mockPsItem, atLeastOnce()).setDouble(4, 20.0);
        verify(mockPsItem, atLeastOnce()).setDouble(5, 0.0);

        verify(mockPsItem).executeBatch();

        // Verify transaction commit and connection closing
        verify(mockConnection).commit();
        verify(mockConnection, never()).rollback();
        verify(mockConnection).close();
        verify(mockPsBill).close(); // Ensure prepared statement is closed
        verify(mockPsItem).close(); // Ensure prepared statement is closed
        verify(mockRs, times(1)).close(); // ResultSet from executeQuery for Bill ID
    }

    @Test
    @DisplayName("Should rollback transaction if bill saving fails")
    void save_billSaveFails_rollbacksTransaction() throws SQLException {
        // Arrange
        Bill bill = new Bill(0, 1, new Date(), 100.0, 100.0, 0.0, "POS");
        bill.setItems(Arrays.asList(new BillItem(0, 0, new Product("P001", "A", 1), 1, 1, 0)));

        when(mockPsBill.executeQuery()).thenThrow(new SQLException("Simulated bill save error"));

        // Act & Assert
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> billingRepository.save(bill),
                "Should throw RuntimeException when bill save fails");
        assertTrue(thrown.getMessage().contains("Error saving bill & items"), "Exception message should indicate save error");
        assertTrue(thrown.getCause() instanceof SQLException, "Cause should be SQLException");

        // Verify transaction rollback and connection closing
        verify(mockConnection).setAutoCommit(false);
        verify(mockConnection, never()).commit();
        verify(mockConnection).rollback();
        verify(mockConnection).close();
        verify(mockPsBill).close(); // PreparedStatement should still be closed
        verify(mockRs, never()).close(); // ResultSet was never created
    }

    @Test
    @DisplayName("Should rollback transaction if bill item saving fails")
    void save_itemSaveFails_rollbacksTransaction() throws SQLException {
        // Arrange
        Bill bill = new Bill(0, 1, new Date(), 100.0, 100.0, 0.0, "POS");
        bill.setItems(Arrays.asList(new BillItem(0, 0, new Product("P001", "A", 1), 1, 1, 0)));

        when(mockPsBill.executeQuery()).thenReturn(mockRs);
        when(mockRs.next()).thenReturn(true);
        when(mockRs.getInt(1)).thenReturn(123);

        when(mockPsItem.executeBatch()).thenThrow(new SQLException("Simulated item batch error"));

        // Act & Assert
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> billingRepository.save(bill),
                "Should throw RuntimeException when item saving fails");
        assertTrue(thrown.getMessage().contains("Error saving bill & items"), "Exception message should indicate save error");
        assertTrue(thrown.getCause() instanceof SQLException, "Cause should be SQLException");

        // Verify transaction rollback and connection closing
        verify(mockConnection).setAutoCommit(false);
        verify(mockPsBill).executeQuery();
        verify(mockPsItem, atLeastOnce()).addBatch();
        verify(mockConnection, never()).commit();
        verify(mockConnection).rollback();
        verify(mockConnection).close();
        verify(mockPsBill).close();
        verify(mockPsItem).close();
        verify(mockRs, times(1)).close(); // ResultSet for Bill ID was created and closed
    }

    @Test
    @DisplayName("Should return next serial number for the current date (existing bills scenario)")
    void nextSerial_existingBills_returnsMaxPlusOne() throws SQLException {
        // Arrange
        reset(mockConnection); // Reset to ensure only this specific prepareStatement is captured
        when(mockConnection.prepareStatement(contains("SELECT COALESCE(MAX(serial_number), 0) + 1 FROM bill WHERE DATE(bill_date) = CURRENT_DATE")))
            .thenReturn(mockPsBill);
        when(mockPsBill.executeQuery()).thenReturn(mockRs);
        when(mockRs.next()).thenReturn(true); // Simulate a row is returned
        when(mockRs.getInt(1)).thenReturn(5); // Simulate max_serial + 1 = 5

        // Act
        int nextSerialNumber = billingRepository.nextSerial();

        // Assert
        assertEquals(5, nextSerialNumber, "Next serial number should be max + 1");
        verify(mockConnection).prepareStatement(contains("SELECT COALESCE(MAX(serial_number), 0) + 1 FROM bill WHERE DATE(bill_date) = CURRENT_DATE"));
        verify(mockPsBill).executeQuery();
        verify(mockRs).next();
        verify(mockRs).getInt(1);
        verify(mockConnection).close();
        verify(mockPsBill).close(); // Ensure prepared statement is closed
        verify(mockRs).close();     // Ensure result set is closed
    }

    @Test
    @DisplayName("Should return 1 when no bills exist for the current date (no existing bills scenario)")
    void nextSerial_noExistingBills_returnsOne() throws SQLException {
        // Arrange
        reset(mockConnection); // Reset to ensure only this specific prepareStatement is captured
        when(mockConnection.prepareStatement(contains("SELECT COALESCE(MAX(serial_number), 0) + 1 FROM bill WHERE DATE(bill_date) = CURRENT_DATE")))
            .thenReturn(mockPsBill);
        when(mockPsBill.executeQuery()).thenReturn(mockRs);
        when(mockRs.next()).thenReturn(true); // COALESCE(MAX, 0) means it will always return a row
        when(mockRs.getInt(1)).thenReturn(1); // Simulate no bills, so max is 0, returning 1

        // Act
        int nextSerialNumber = billingRepository.nextSerial();

        // Assert
        assertEquals(1, nextSerialNumber, "Next serial number should be 1 when no bills exist for today");
        verify(mockConnection).close();
        verify(mockPsBill).close(); // Ensure prepared statement is closed
        verify(mockRs).close();     // Ensure result set is closed
    }

    @Test
    @DisplayName("Should throw RuntimeException if nextSerial query fails")
    void nextSerial_queryFails_throwsRuntimeException() throws SQLException {
        // Arrange
        reset(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPsBill);
        when(mockPsBill.executeQuery()).thenThrow(new SQLException("Simulated query error"));

        // Act & Assert
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> billingRepository.nextSerial(),
                "Should throw RuntimeException when nextSerial query fails");
        assertTrue(thrown.getMessage().contains("Error generating daily serial"), "Exception message should indicate serial error");
        assertTrue(thrown.getCause() instanceof SQLException, "Cause should be SQLException");
        verify(mockConnection).close();
        verify(mockPsBill).close(); // PreparedStatement should still be closed
        verify(mockRs, never()).close(); // ResultSet was never created if executeQuery threw an exception
    }
}