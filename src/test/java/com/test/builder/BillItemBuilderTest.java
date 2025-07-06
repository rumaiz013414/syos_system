package com.test.builder;

import com.syos.model.BillItem;
import com.syos.model.Product;
import com.syos.strategy.PricingStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillItemBuilderTest {

    @Mock
    private Product mockProduct;
    @Mock
    private PricingStrategy mockPricingStrategy;

    private static final double DELTA = 0.001;

    @BeforeEach
    void setUp() {
        when(mockProduct.getPrice()).thenReturn(100.0);
    }

    @Test
    @DisplayName("Constructor should calculate totalPrice and discountAmount correctly with no discount")
    void constructor_validInput_noDiscount() {
        int quantity = 2;
        double originalPrice = mockProduct.getPrice() * quantity;

        when(mockPricingStrategy.calculate(mockProduct, quantity)).thenReturn(originalPrice);

        BillItem.BillItemBuilder builder =
                new BillItem.BillItemBuilder(mockProduct, quantity, mockPricingStrategy);

        assertNotNull(builder, "Builder should be successfully created");
        assertEquals(mockProduct, getBuilderField(builder, "product"), "Product should be set correctly");
        assertEquals(quantity, (int) getBuilderField(builder, "quantity"), "Quantity should be set correctly");
        assertEquals(originalPrice, (double) getBuilderField(builder, "totalPrice"), DELTA, "Total price should be original price");
        assertEquals(0.0, (double) getBuilderField(builder, "discountAmount"), DELTA, "Discount should be zero");

        verify(mockPricingStrategy).calculate(mockProduct, quantity);
        verify(mockProduct, atLeastOnce()).getPrice();
    }

    @Test
    @DisplayName("Constructor should calculate totalPrice and discountAmount correctly with a discount")
    void constructor_validInput_withDiscount() {
        int quantity = 3;
        double originalPrice = mockProduct.getPrice() * quantity;
        double discountedPrice = 250.0;
        double expectedDiscount = originalPrice - discountedPrice;

        when(mockPricingStrategy.calculate(mockProduct, quantity)).thenReturn(discountedPrice);

        BillItem.BillItemBuilder builder =
                new BillItem.BillItemBuilder(mockProduct, quantity, mockPricingStrategy);

        assertNotNull(builder, "Builder should be successfully created");
        assertEquals(discountedPrice, (double) getBuilderField(builder, "totalPrice"), DELTA, "Total price should be discounted price");
        assertEquals(expectedDiscount, (double) getBuilderField(builder, "discountAmount"), DELTA, "Discount should be calculated correctly");

        verify(mockPricingStrategy).calculate(mockProduct, quantity);
        verify(mockProduct, atLeastOnce()).getPrice();
    }


    @Test
    @DisplayName("build() should create a BillItem object with values from the builder")
    void build_createsBillItemCorrectly() {
        int quantity = 1;
        double originalPrice = mockProduct.getPrice() * quantity;
        double finalPrice = 95.0;

        when(mockPricingStrategy.calculate(mockProduct, quantity)).thenReturn(finalPrice);

        BillItem.BillItemBuilder builder =
                new BillItem.BillItemBuilder(mockProduct, quantity, mockPricingStrategy);

        BillItem billItem = builder.build();

        assertNotNull(billItem, "build() should return a non-null BillItem");
        assertEquals(0, billItem.getId(), "BillItem ID should be 0 by default from builder");
        assertEquals(0, billItem.getBillId(), "BillItem BillId should be 0 by default from builder");
        assertEquals(mockProduct, billItem.getProduct(), "Product should match builder's product");
        assertEquals(quantity, billItem.getQuantity(), "Quantity should match builder's quantity");
        assertEquals(finalPrice, billItem.getTotalPrice(), DELTA, "Total price should match builder's calculated total");
        assertEquals(originalPrice - finalPrice, billItem.getDiscountAmount(), DELTA, "Discount amount should match builder's calculated discount");

        verify(mockPricingStrategy).calculate(mockProduct, quantity);
        verify(mockProduct, atLeastOnce()).getPrice();
    }

    private Object getBuilderField(BillItem.BillItemBuilder builder, String fieldName) {
        try {
            Field field = builder.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(builder);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to access field '" + fieldName + "' for testing: " + e.getMessage(), e);
        }
    }
}