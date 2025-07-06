package com.test.singleton;

import com.syos.model.Discount;
import com.syos.model.Product;
import com.syos.repository.DiscountRepository;
import com.syos.singleton.InventoryManager;
import com.syos.strategy.DiscountPricingStrategy;
import com.syos.strategy.PricingStrategy;
import com.syos.strategy.ShelfStrategy;
import com.syos.util.CommonVariables;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscountPricingStrategyTest {

	@Mock
	private PricingStrategy mockBasePriceStrategy;
	@Mock
	private DiscountRepository mockDiscountRepository;
	@Mock
	private InventoryManager mockInventoryManager;

	private MockedConstruction<DiscountRepository> mockedDiscountRepoConstruction;
	private MockedStatic<InventoryManager> mockedInventoryManagerStatic;
	private MockedStatic<LocalDate> mockedLocalDateStatic;

	private DiscountPricingStrategy discountStrategy;

	@BeforeEach
	void setUp() {
		mockedDiscountRepoConstruction = mockConstruction(DiscountRepository.class,
				(mock, context) -> mockDiscountRepository = mock);

		mockedInventoryManagerStatic = mockStatic(InventoryManager.class);
		mockedInventoryManagerStatic.when(() -> InventoryManager.getInstance(null)).thenReturn(mockInventoryManager);
		mockedInventoryManagerStatic.when(() -> InventoryManager.getInstance(any(ShelfStrategy.class)))
				.thenReturn(mockInventoryManager);

		mockedLocalDateStatic = mockStatic(LocalDate.class, CALLS_REAL_METHODS);
		mockedLocalDateStatic.when(LocalDate::now).thenReturn(LocalDate.of(2025, 6, 8));

		discountStrategy = new DiscountPricingStrategy(mockBasePriceStrategy);
	}

	@AfterEach
	void tearDown() {
		if (mockedDiscountRepoConstruction != null)
			mockedDiscountRepoConstruction.close();
		if (mockedInventoryManagerStatic != null)
			mockedInventoryManagerStatic.close();
		if (mockedLocalDateStatic != null)
			mockedLocalDateStatic.close();

		CommonVariables.MIN_TOTAL_PRICE = 0.0;
	}

	@Test
	@DisplayName("Should return base price if no active discounts are found")
	void calculate_NoActiveDiscounts() {
		Product product = new Product("P001", "Laptop", 1000.00);
		int quantity = 1;
		double basePrice = 1000.00;

		when(mockBasePriceStrategy.calculate(product, quantity)).thenReturn(basePrice);
		when(mockInventoryManager.getQuantityOnShelf(product.getCode())).thenReturn(50);
		when(mockDiscountRepository.findActiveDiscounts(product.getCode(), LocalDate.of(2025, 6, 8)))
				.thenReturn(Collections.emptyList());

		double finalPrice = discountStrategy.calculate(product, quantity);

		assertEquals(basePrice, finalPrice, 0.001);
		verify(mockBasePriceStrategy).calculate(product, quantity);
		verify(mockInventoryManager).getQuantityOnShelf(product.getCode());
		verify(mockDiscountRepository).findActiveDiscounts(product.getCode(), LocalDate.of(2025, 6, 8));
	}

	@Test
	@DisplayName("Should return base price if stock is below minimum quantity for discount")
	void calculate_BelowMinimumStock() {
		Product product = new Product("P002", "Mouse", 25.00);
		int quantity = 2;
		double basePrice = 50.00;

		when(mockBasePriceStrategy.calculate(product, quantity)).thenReturn(basePrice);
		when(mockInventoryManager.getQuantityOnShelf(product.getCode()))
				.thenReturn(CommonVariables.MINIMUMQUANTITY - 1);

		double finalPrice = discountStrategy.calculate(product, quantity);

		assertEquals(basePrice, finalPrice, 0.001);
		verify(mockBasePriceStrategy).calculate(product, quantity);
		verify(mockInventoryManager).getQuantityOnShelf(product.getCode());
		verifyNoInteractions(mockDiscountRepository);
	}

	@Test
	@DisplayName("Should apply percentage discount if it results in a lower price")
	void calculate_PercentageDiscount() {
		Product product = new Product("P003", "Keyboard", 100.00);
		int quantity = 1;
		double basePrice = 100.00;

		Discount percentDiscount = new Discount(1, "KEYBOARD_10", com.syos.enums.DiscountType.PERCENT, 10.0,
				LocalDate.of(2025, 6, 7), LocalDate.of(2025, 6, 9));

		when(mockBasePriceStrategy.calculate(product, quantity)).thenReturn(basePrice);
		when(mockInventoryManager.getQuantityOnShelf(product.getCode())).thenReturn(50);
		when(mockDiscountRepository.findActiveDiscounts(product.getCode(), LocalDate.of(2025, 6, 8)))
				.thenReturn(Arrays.asList(percentDiscount));

		double finalPrice = discountStrategy.calculate(product, quantity);

		double expectedPrice = basePrice
				* (CommonVariables.oneHundredPercent - (10.0 / CommonVariables.percentageDevisor));
		assertEquals(expectedPrice, finalPrice, 0.001);
		verify(mockBasePriceStrategy).calculate(product, quantity);
		verify(mockInventoryManager).getQuantityOnShelf(product.getCode());
		verify(mockDiscountRepository).findActiveDiscounts(product.getCode(), LocalDate.of(2025, 6, 8));
	}

	@Test
	@DisplayName("Should handle multiple percentage discounts and pick the best one")
	void calculate_MultiplePercentageDiscounts() {
		Product product = new Product("P004", "Monitor", 200.00);
		int quantity = 1;
		double basePrice = 200.00;

		Discount discount10Percent = new Discount(2, "MONITOR_10", com.syos.enums.DiscountType.PERCENT, 10.0,
				LocalDate.of(2025, 6, 7), LocalDate.of(2025, 6, 9));
		Discount discount5Percent = new Discount(3, "MONITOR_5", com.syos.enums.DiscountType.PERCENT, 5.0,
				LocalDate.of(2025, 6, 7), LocalDate.of(2025, 6, 9));

		when(mockBasePriceStrategy.calculate(product, quantity)).thenReturn(basePrice);
		when(mockInventoryManager.getQuantityOnShelf(product.getCode())).thenReturn(50);
		when(mockDiscountRepository.findActiveDiscounts(product.getCode(), LocalDate.of(2025, 6, 8)))
				.thenReturn(Arrays.asList(discount10Percent, discount5Percent));

		double finalPrice = discountStrategy.calculate(product, quantity);

		double expectedPrice = basePrice
				* (CommonVariables.oneHundredPercent - (10.0 / CommonVariables.percentageDevisor));
		assertEquals(expectedPrice, finalPrice, 0.001);
	}

	@Test
	@DisplayName("Should apply amount discount if it results in a lower price")
	void calculate_AmountDiscount() {
		Product product = new Product("P005", "Printer", 300.00);
		int quantity = 1;
		double basePrice = 300.00;

		Discount amountDiscount = new Discount(4, "PRINTER_FIXED", com.syos.enums.DiscountType.AMOUNT, 20.0,
				LocalDate.of(2025, 6, 7), LocalDate.of(2025, 6, 9));

		when(mockBasePriceStrategy.calculate(product, quantity)).thenReturn(basePrice);
		when(mockInventoryManager.getQuantityOnShelf(product.getCode())).thenReturn(50);
		when(mockDiscountRepository.findActiveDiscounts(product.getCode(), LocalDate.of(2025, 6, 8)))
				.thenReturn(Arrays.asList(amountDiscount));

		double finalPrice = discountStrategy.calculate(product, quantity);

		double expectedPrice = basePrice - 20.0;
		assertEquals(expectedPrice, finalPrice, 0.001);
	}

	@Test
	@DisplayName("Should pick the best discount between percentage and amount")
	void calculate_MixedDiscounts_BestSelected() {
		Product product = new Product("P006", "Webcam", 50.00);
		int quantity = 1;
		double basePrice = 50.00;

		Discount percentDiscount = new Discount(5, "WEBCAM_10", com.syos.enums.DiscountType.PERCENT, 10.0,
				LocalDate.of(2025, 6, 7), LocalDate.of(2025, 6, 9));
		Discount amountDiscount = new Discount(6, "WEBCAM_FIXED", com.syos.enums.DiscountType.AMOUNT, 6.0,
				LocalDate.of(2025, 6, 7), LocalDate.of(2025, 6, 9));

		when(mockBasePriceStrategy.calculate(product, quantity)).thenReturn(basePrice);
		when(mockInventoryManager.getQuantityOnShelf(product.getCode())).thenReturn(50);
		when(mockDiscountRepository.findActiveDiscounts(product.getCode(), LocalDate.of(2025, 6, 8)))
				.thenReturn(Arrays.asList(percentDiscount, amountDiscount));

		double finalPrice = discountStrategy.calculate(product, quantity);

		double expectedPrice = basePrice - 6.0;
		assertEquals(expectedPrice, finalPrice, 0.001);
	}

	@Test
	@DisplayName("Should not go below MIN_TOTAL_PRICE even with large discount")
	void calculate_BelowMinPrice() {
		Product product = new Product("P007", "Cheap Item", 5.00);
		int quantity = 1;
		double basePrice = 5.00;

		double originalMinTotalPrice = CommonVariables.MIN_TOTAL_PRICE;
		CommonVariables.MIN_TOTAL_PRICE = 1.0;

		Discount largeDiscount = new Discount(7, "CHEAP_ITEM_DISCOUNT", com.syos.enums.DiscountType.AMOUNT, 4.50,
				LocalDate.of(2025, 6, 7), LocalDate.of(2025, 6, 9));

		when(mockBasePriceStrategy.calculate(product, quantity)).thenReturn(basePrice);
		when(mockInventoryManager.getQuantityOnShelf(product.getCode())).thenReturn(50);
		when(mockDiscountRepository.findActiveDiscounts(product.getCode(), LocalDate.of(2025, 6, 8)))
				.thenReturn(Arrays.asList(largeDiscount));

		double finalPrice = discountStrategy.calculate(product, quantity);

		assertEquals(CommonVariables.MIN_TOTAL_PRICE, finalPrice, 0.001);

		CommonVariables.MIN_TOTAL_PRICE = originalMinTotalPrice;
	}

	@Test
	@DisplayName("Should apply discount correctly when quantity affects base price (e.g., if base strategy multiplies)")
	void calculate_DiscountWithQuantity() {
		Product product = new Product("P008", "Tablet", 250.00);
		int quantity = 2;
		double basePrice = 500.00;

		Discount percentDiscount = new Discount(8, "TABLET_15", com.syos.enums.DiscountType.PERCENT, 15.0,
				LocalDate.of(2025, 6, 7), LocalDate.of(2025, 6, 9));

		when(mockBasePriceStrategy.calculate(product, quantity)).thenReturn(basePrice);
		when(mockInventoryManager.getQuantityOnShelf(product.getCode())).thenReturn(50);
		when(mockDiscountRepository.findActiveDiscounts(product.getCode(), LocalDate.of(2025, 6, 8)))
				.thenReturn(Arrays.asList(percentDiscount));

		double finalPrice = discountStrategy.calculate(product, quantity);

		double expectedPrice = basePrice
				* (CommonVariables.oneHundredPercent - (15.0 / CommonVariables.percentageDevisor));
		assertEquals(expectedPrice, finalPrice, 0.001);
	}
}