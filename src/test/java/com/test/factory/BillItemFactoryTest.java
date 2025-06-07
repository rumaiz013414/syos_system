package com.test.factory; // Adjust package to match your test structure

import com.syos.factory.BillItemFactory;
import com.syos.model.BillItem;
import com.syos.model.Product;
import com.syos.strategy.PricingStrategy; // Assuming this is an interface or abstract class

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // Enables Mockito annotations for mock injection
class BillItemFactoryTest {

	@Mock
	private PricingStrategy mockPricingStrategy; // Mock the PricingStrategy dependency
	@Mock
	private Product mockProduct; // Mock the Product dependency (less critical, but good practice for isolation)

	private BillItemFactory billItemFactory;

	@BeforeEach
	void setUp() {
		// Initialize the factory with the mocked PricingStrategy before each test
		billItemFactory = new BillItemFactory(mockPricingStrategy);
	}

	@Test
	@DisplayName("Should create BillItem with zero discount if strategy returns original price")
	void create_noDiscount_returnsZeroDiscount() {
		// Arrange
		double productUnitPrice = 10.0;
		int quantity = 5;
		double calculatedTotalPrice = 50.0; // No discount from strategy
		double originalPrice = productUnitPrice * quantity; // 50.0
		double expectedDiscount = 0.0;

		when(mockProduct.getPrice()).thenReturn(productUnitPrice);
		when(mockPricingStrategy.calculate(mockProduct, quantity)).thenReturn(calculatedTotalPrice);

		// Act
		BillItem billItem = billItemFactory.create(mockProduct, quantity);

		// Assert
		assertNotNull(billItem);
		assertEquals(expectedDiscount, billItem.getDiscountAmount(),
				"Discount should be 0 when total price equals original price");
		assertEquals(calculatedTotalPrice, billItem.getTotalPrice());

		verify(mockPricingStrategy, times(1)).calculate(mockProduct, quantity);
		verify(mockProduct, times(1)).getPrice();
	}

	@Test
	@DisplayName("Should throw IllegalArgumentException if product is null")
	void create_nullProduct_throwsIllegalArgumentException() {
		// Arrange
		int quantity = 5;

		// Act & Assert
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> billItemFactory.create(null, quantity),
				"Should throw IllegalArgumentException when product is null");
		assertEquals("Product cannot be null", exception.getMessage());

		// Verify no interactions with mocks as the exception should occur immediately
		verifyNoInteractions(mockPricingStrategy);
		verifyNoInteractions(mockProduct);
	}

	@Test
	@DisplayName("Should throw IllegalArgumentException if quantity is zero")
	void create_zeroQuantity_throwsIllegalArgumentException() {
		// Arrange
		int quantity = 0;

		// Act & Assert
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> billItemFactory.create(mockProduct, quantity),
				"Should throw IllegalArgumentException when quantity is zero");
		assertEquals("Quantity must be > 0", exception.getMessage());

		// Verify no interactions with strategy or product (as quantity check is first)
		verifyNoInteractions(mockPricingStrategy);
		verifyNoInteractions(mockProduct); // getPrice() not called if quantity invalid
	}

	@Test
	@DisplayName("Should throw IllegalArgumentException if quantity is negative")
	void create_negativeQuantity_throwsIllegalArgumentException() {
		// Arrange
		int quantity = -1;

		// Act & Assert
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> billItemFactory.create(mockProduct, quantity),
				"Should throw IllegalArgumentException when quantity is negative");
		assertEquals("Quantity must be > 0", exception.getMessage());

		// Verify no interactions with strategy or product
		verifyNoInteractions(mockPricingStrategy);
		verifyNoInteractions(mockProduct);
	}

	// Additional test for the BillItem constructor (not the builder path)
	@Test
	@DisplayName("Should correctly instantiate BillItem using its direct constructor")
	void constructor_directInstantiation_success() {
		// Arrange
		Product testProduct = new Product("P001", "Apple", 1.50);
		int id = 1;
		int billId = 101;
		int quantity = 2;
		double totalPrice = 3.00;
		double discountAmount = 0.50;

		// Act
		BillItem billItem = new BillItem(id, billId, testProduct, quantity, totalPrice, discountAmount);

		// Assert
		assertNotNull(billItem, "BillItem should not be null");
		assertEquals(id, billItem.getId(), "ID should match");
		assertEquals(billId, billItem.getBillId(), "Bill ID should match");
		assertEquals(testProduct, billItem.getProduct(), "Product should match");
		assertEquals(quantity, billItem.getQuantity(), "Quantity should match");
		assertEquals(totalPrice, billItem.getTotalPrice(), "Total price should match");
		assertEquals(discountAmount, billItem.getDiscountAmount(), "Discount amount should match");
	}
}