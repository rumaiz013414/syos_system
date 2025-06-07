package com.test.builder;

import com.syos.model.BillItem;
import com.syos.model.Product;
import com.syos.strategy.PricingStrategy; // Assuming PricingStrategy is in this package
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field; // Used for accessing private fields for verification

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// @ExtendWith(MockitoExtension.class) enables Mockito annotations like @Mock
// and handles automatic mock initialization and reset for each test method.
@ExtendWith(MockitoExtension.class)
class BillItemBuilderTest {

    @Mock
    private Product mockProduct; // Mock the Product dependency
    @Mock
    private PricingStrategy mockPricingStrategy; // Mock the PricingStrategy dependency

    private static final double DELTA = 0.001; // Tolerance for double comparisons

    @BeforeEach
    void setUp() {
        // We use @Mock and @ExtendWith(MockitoExtension.class) so mocks are
        // automatically initialized and reset before each test.
        // We set a default price for the mock product.
        when(mockProduct.getPrice()).thenReturn(100.0);
    }

    // --- Constructor Tests ---

    @Test
    @DisplayName("Constructor should calculate totalPrice and discountAmount correctly with no discount")
    void constructor_validInput_noDiscount() {
        int quantity = 2;
        double originalPrice = mockProduct.getPrice() * quantity; // 100.0 * 2 = 200.0

        // Mock the pricing strategy to return the original price (no discount)
        when(mockPricingStrategy.calculate(mockProduct, quantity)).thenReturn(originalPrice);

        BillItem.BillItemBuilder builder =
                new BillItem.BillItemBuilder(mockProduct, quantity, mockPricingStrategy);

        assertNotNull(builder, "Builder should be successfully created");
        assertEquals(mockProduct, getBuilderField(builder, "product"), "Product should be set correctly");
        assertEquals(quantity, (int) getBuilderField(builder, "quantity"), "Quantity should be set correctly");
        assertEquals(originalPrice, (double) getBuilderField(builder, "totalPrice"), DELTA, "Total price should be original price");
        assertEquals(0.0, (double) getBuilderField(builder, "discountAmount"), DELTA, "Discount should be zero");

        // Verify that the strategy was called with the correct arguments
        verify(mockPricingStrategy).calculate(mockProduct, quantity);
        // Verify product.getPrice() was called (once for originalPrice calculation, and internally by builder)
        verify(mockProduct, atLeastOnce()).getPrice();
    }

    @Test
    @DisplayName("Constructor should calculate totalPrice and discountAmount correctly with a discount")
    void constructor_validInput_withDiscount() {
        int quantity = 3;
        double originalPrice = mockProduct.getPrice() * quantity; // 100.0 * 3 = 300.0
        double discountedPrice = 250.0; // Simulate a 50.0 discount
        double expectedDiscount = originalPrice - discountedPrice; // 300.0 - 250.0 = 50.0

        // Mock the pricing strategy to return a discounted price
        when(mockPricingStrategy.calculate(mockProduct, quantity)).thenReturn(discountedPrice);

        BillItem.BillItemBuilder builder =
                new BillItem.BillItemBuilder(mockProduct, quantity, mockPricingStrategy);

        assertNotNull(builder, "Builder should be successfully created");
        assertEquals(discountedPrice, (double) getBuilderField(builder, "totalPrice"), DELTA, "Total price should be discounted price");
        assertEquals(expectedDiscount, (double) getBuilderField(builder, "discountAmount"), DELTA, "Discount should be calculated correctly");

        verify(mockPricingStrategy).calculate(mockProduct, quantity);
        verify(mockProduct, atLeastOnce()).getPrice();
    }


    // --- build() Method Test ---

    @Test
    @DisplayName("build() should create a BillItem object with values from the builder")
    void build_createsBillItemCorrectly() {
        int quantity = 1;
        double originalPrice = mockProduct.getPrice() * quantity; // 100.0
        double finalPrice = 95.0; // Simulate a 5.0 discount

        when(mockPricingStrategy.calculate(mockProduct, quantity)).thenReturn(finalPrice);

        BillItem.BillItemBuilder builder =
                new BillItem.BillItemBuilder(mockProduct, quantity, mockPricingStrategy);

        BillItem billItem = builder.build(); // Call the build method

        assertNotNull(billItem, "build() should return a non-null BillItem");
        // Verify fields in the resulting BillItem (id and billId should be 0 by default)
        assertEquals(0, billItem.getId(), "BillItem ID should be 0 by default from builder");
        assertEquals(0, billItem.getBillId(), "BillItem BillId should be 0 by default from builder");
        assertEquals(mockProduct, billItem.getProduct(), "Product should match builder's product");
        assertEquals(quantity, billItem.getQuantity(), "Quantity should match builder's quantity");
        assertEquals(finalPrice, billItem.getTotalPrice(), DELTA, "Total price should match builder's calculated total");
        assertEquals(originalPrice - finalPrice, billItem.getDiscountAmount(), DELTA, "Discount amount should match builder's calculated discount");

        verify(mockPricingStrategy).calculate(mockProduct, quantity);
        verify(mockProduct, atLeastOnce()).getPrice();
    }

    // --- Helper method to access private fields of BillItemBuilder for verification ---
    // This is useful because the builder's internal state (totalPrice, discountAmount)
    // is not directly exposed via public getters until `build()` is called.
    private Object getBuilderField(BillItem.BillItemBuilder builder, String fieldName) {
        try {
            Field field = builder.getClass().getDeclaredField(fieldName);
            field.setAccessible(true); // Allow access to private field
            return field.get(builder);  // Return the value of the field
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // Rethrow as a runtime exception if field access fails, indicating a test setup issue
            throw new RuntimeException("Failed to access field '" + fieldName + "' for testing: " + e.getMessage(), e);
        }
    }
}