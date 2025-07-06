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

@ExtendWith(MockitoExtension.class)
class BillBuilderTest {

	@Mock
	private BillItem mockBillItem1;
	@Mock
	private BillItem mockBillItem2;

	private List<BillItem> validTestItems;
	private static final double DELTA = 0.001;

	@BeforeEach
	void setUp() {
		reset(mockBillItem1, mockBillItem2);
		when(mockBillItem1.getTotalPrice()).thenReturn(20.0);
		when(mockBillItem2.getTotalPrice()).thenReturn(15.0);
		validTestItems = Arrays.asList(mockBillItem1, mockBillItem2);
	}

	@Test
	@DisplayName("BillBuilder constructor should be successful with valid items and calculate totalAmount")
	void billBuilder_constructor_validItems_calculatesTotalAmount() {
		Bill.BillBuilder builder = new Bill.BillBuilder(1001, validTestItems);

		assertNotNull(builder);
		assertEquals(1001, getValue(builder, "serialNumber"));
		assertEquals(35.0, (Double) getValue(builder, "totalAmount"), DELTA);
		verify(mockBillItem1, times(1)).getTotalPrice();
		verify(mockBillItem2, times(1)).getTotalPrice();
	}

	@Test
	@DisplayName("withCashTendered should set cashTendered correctly when sufficient cash is provided")
	void withCashTendered_sufficientCash_setsValue() {
		Bill.BillBuilder builder = new Bill.BillBuilder(1004, validTestItems);
		Bill.BillBuilder resultBuilder = builder.withCashTendered(40.0);

		assertSame(builder, resultBuilder, "Should return the same builder instance for chaining");
		assertEquals(40.0, (Double) getValue(builder, "cashTendered"), DELTA);
	}

	@Test
	@DisplayName("withCashTendered should throw IllegalArgumentException if cash tendered is less than totalAmount")
	void withCashTendered_insufficientCash_throwsException() {
		Bill.BillBuilder builder = new Bill.BillBuilder(1005, validTestItems);
		IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
				() -> builder.withCashTendered(30.0));
		assertEquals("Cash tendered must cover total", thrown.getMessage());
		assertEquals(0.0, (Double) getValue(builder, "cashTendered"), DELTA);
	}

	@Test
	@DisplayName("withTransactionType should set the transaction type correctly")
	void withTransactionType_validType_setsValue() {
		Bill.BillBuilder builder = new Bill.BillBuilder(1006, validTestItems);
		Bill.BillBuilder resultBuilder = builder.withTransactionType("CASH");

		assertSame(builder, resultBuilder, "Should return the same builder instance for chaining");
		assertEquals("CASH", getValue(builder, "transactionType"));
	}

	@Test
	@DisplayName("withTransactionType should throw IllegalArgumentException for null transaction type")
	void withTransactionType_nullType_throwsException() {
		Bill.BillBuilder builder = new Bill.BillBuilder(1007, validTestItems);
		IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
				() -> builder.withTransactionType(null));
		assertEquals("Transaction type cannot be empty", thrown.getMessage());
		assertEquals("COUNTER", getValue(builder, "transactionType"));
	}

	@Test
	@DisplayName("withTransactionType should throw IllegalArgumentException for blank transaction type")
	void withTransactionType_blankType_throwsException() {
		Bill.BillBuilder builder = new Bill.BillBuilder(1008, validTestItems);
		IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
				() -> builder.withTransactionType("   "));
		assertEquals("Transaction type cannot be empty", thrown.getMessage());
		assertEquals("COUNTER", getValue(builder, "transactionType"));
	}

	@Test
	@DisplayName("build() should create a Bill object successfully when all conditions are met")
	void build_validBuilder_createsBill() {
		Bill.BillBuilder builder = new Bill.BillBuilder(1009, validTestItems);
		builder.withCashTendered(50.0);
		builder.withTransactionType("CASH");

		Bill bill = builder.build();

		assertNotNull(bill);
		assertEquals(1009, bill.getSerialNumber());
		assertEquals(35.0, bill.getTotalAmount(), DELTA);
		assertEquals(50.0, bill.getCashTendered(), DELTA);
		assertEquals(15.0, bill.getChangeReturned(), DELTA);
		assertEquals("CASH", bill.getTransactionType());
		assertNotNull(bill.getBillDate());
		assertFalse(bill.getItems().isEmpty());
		assertEquals(2, bill.getItems().size());
		assertTrue(bill.getItems().containsAll(validTestItems));
	}

	@Test
	@DisplayName("build() should throw IllegalStateException if cashTendered is zero (not set)")
	void build_cashTenderedZero_throwsException() {
		Bill.BillBuilder builder = new Bill.BillBuilder(1010, validTestItems);

		IllegalStateException thrown = assertThrows(IllegalStateException.class, builder::build);
		assertEquals("Must set cashTendered", thrown.getMessage());
	}

	private Object getValue(Object obj, String fieldName) {
		try {
			Field field = obj.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);
			return field.get(obj);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			throw new RuntimeException("Failed to access field '" + fieldName + "' for testing: " + e.getMessage(), e);
		}
	}
}
