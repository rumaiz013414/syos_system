package com.test.builder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.syos.model.Bill;
import com.syos.model.BillItem;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// Using MockitoExtension to enable @Mock annotations and automatic mock management
@ExtendWith(MockitoExtension.class)
class BillBuilderTest {

    @Mock
    private BillItem mockBillItem1; // Mock BillItem to control getTotalPrice()
    @Mock
    private BillItem mockBillItem2; // Another mock BillItem

    private List<BillItem> validTestItems;
    private static final double DELTA = 0.001; // For comparing doubles

    @BeforeEach
    void setUp() {
        // Reset mocks for each test method
        reset(mockBillItem1, mockBillItem2);

        // Configure default behavior for mock BillItems
        when(mockBillItem1.getTotalPrice()).thenReturn(20.0);
        when(mockBillItem2.getTotalPrice()).thenReturn(15.0);
        validTestItems = Arrays.asList(mockBillItem1, mockBillItem2);
    }

    // --- BillBuilder Constructor Tests ---

    @Test
    @DisplayName("BillBuilder constructor should be successful with valid items and calculate totalAmount")
    void billBuilder_constructor_validItems_calculatesTotalAmount() {
        // Expected total: 20.0 (mockBillItem1) + 15.0 (mockBillItem2) = 35.0
        Bill.BillBuilder builder = new Bill.BillBuilder(1001, validTestItems);

        assertNotNull(builder);
        assertEquals(1001, getValue(builder, "serialNumber"));
        // Check initial totalAmount calculated in the constructor
        assertEquals(35.0, (Double) getValue(builder, "totalAmount"), DELTA);
        // Ensure getTotalPrice was called on each item
        verify(mockBillItem1, times(1)).getTotalPrice();
        verify(mockBillItem2, times(1)).getTotalPrice();
    }

  

    @Test
    @DisplayName("withCashTendered should set cashTendered correctly when sufficient cash is provided")
    void withCashTendered_sufficientCash_setsValue() {
        Bill.BillBuilder builder = new Bill.BillBuilder(1004, validTestItems); // totalAmount is 35.0
        Bill.BillBuilder resultBuilder = builder.withCashTendered(40.0);

        assertSame(builder, resultBuilder, "Should return the same builder instance for chaining");
        assertEquals(40.0, (Double) getValue(builder, "cashTendered"), DELTA);
    }

    @Test
    @DisplayName("withCashTendered should throw IllegalArgumentException if cash tendered is less than totalAmount")
    void withCashTendered_insufficientCash_throwsException() {
        Bill.BillBuilder builder = new Bill.BillBuilder(1005, validTestItems); // totalAmount is 35.0
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> builder.withCashTendered(30.0),
                "Should throw IllegalArgumentException for insufficient cash");
        assertEquals("Cash tendered must cover total", thrown.getMessage());
        // Verify cashTendered remains 0 (default)
        assertEquals(0.0, (Double) getValue(builder, "cashTendered"), DELTA);
    }

    // --- withTransactionType() Tests ---

    @Test
    @DisplayName("withTransactionType should set the transaction type correctly")
    void withTransactionType_validType_setsValue() {
        Bill.BillBuilder builder = new Bill.BillBuilder(1006, validTestItems);
        Bill.BillBuilder resultBuilder = builder.withTransactionType("CREDIT_CARD");

        assertSame(builder, resultBuilder, "Should return the same builder instance for chaining");
        assertEquals("CREDIT_CARD", getValue(builder, "transactionType"));
    }

    @Test
    @DisplayName("withTransactionType should throw IllegalArgumentException for null transaction type")
    void withTransactionType_nullType_throwsException() {
        Bill.BillBuilder builder = new Bill.BillBuilder(1007, validTestItems);
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> builder.withTransactionType(null),
                "Should throw IllegalArgumentException for null transaction type");
        assertEquals("Transaction type cannot be empty", thrown.getMessage());
        // Verify transactionType remains "COUNTER" (default)
        assertEquals("COUNTER", getValue(builder, "transactionType"));
    }

    @Test
    @DisplayName("withTransactionType should throw IllegalArgumentException for blank transaction type")
    void withTransactionType_blankType_throwsException() {
        Bill.BillBuilder builder = new Bill.BillBuilder(1008, validTestItems);
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> builder.withTransactionType("   "),
                "Should throw IllegalArgumentException for blank transaction type");
        assertEquals("Transaction type cannot be empty", thrown.getMessage());
        // Verify transactionType remains "COUNTER" (default)
        assertEquals("COUNTER", getValue(builder, "transactionType"));
    }

    // --- build() Tests ---

    @Test
    @DisplayName("build() should create a Bill object successfully when all conditions are met")
    void build_validBuilder_createsBill() {
        Bill.BillBuilder builder = new Bill.BillBuilder(1009, validTestItems); // totalAmount 35.0
        builder.withCashTendered(50.0); // Sufficient cash
        builder.withTransactionType("MOBILE_PAYMENT");

        Bill bill = builder.build();

        assertNotNull(bill, "Build method should return a non-null Bill object");
        assertEquals(1009, bill.getSerialNumber(), "Bill serial number should match builder");
        assertEquals(35.0, bill.getTotalAmount(), DELTA, "Bill total amount should match builder's calculated total");
        assertEquals(50.0, bill.getCashTendered(), DELTA, "Bill cash tendered should match builder");
        assertEquals(15.0, bill.getChangeReturned(), DELTA, "Bill change returned should be calculated correctly"); // 50.0 - 35.0
        assertEquals("MOBILE_PAYMENT", bill.getTransactionType(), "Bill transaction type should match builder");
        assertNotNull(bill.getBillDate(), "Bill date should be set");
        assertFalse(bill.getItems().isEmpty(), "Bill items should not be empty");
        assertEquals(2, bill.getItems().size());
        assertTrue(bill.getItems().containsAll(validTestItems), "Bill should contain the items from the builder");
    }

    @Test
    @DisplayName("build() should throw IllegalStateException if cashTendered is zero (not set)")
    void build_cashTenderedZero_throwsException() {
        Bill.BillBuilder builder = new Bill.BillBuilder(1010, validTestItems); // totalAmount 35.0
        // cashTendered is implicitly 0 if not set by withCashTendered()

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                builder::build, // Using method reference
                "Should throw IllegalStateException if cashTendered is zero");
        assertEquals("Must set cashTendered", thrown.getMessage());
    }

    // --- Helper for accessing private fields ---
    // This method is used to inspect the internal state of the BillBuilder (e.g., totalAmount, cashTendered)
    // before the build() method is called, as these fields are not exposed via public getters in the builder itself.
    private Object getValue(Object obj, String fieldName) {
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true); // Make the private field accessible
            return field.get(obj); // Get the value of the field
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // Rethrow as a runtime exception if field access fails, as it indicates a test setup issue
            throw new RuntimeException("Failed to access field '" + fieldName + "' for testing: " + e.getMessage(), e);
        }
    }
}