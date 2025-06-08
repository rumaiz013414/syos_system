package com.test.singleton; // Make sure this package matches your test directory

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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscountPricingStrategyTest {

    @Mock private PricingStrategy mockBasePriceStrategy;
    @Mock private DiscountRepository mockDiscountRepository;
    @Mock private InventoryManager mockInventoryManager;

    // For mocking 'new' keyword and static methods
    private MockedConstruction<DiscountRepository> mockedDiscountRepoConstruction;
    private MockedStatic<InventoryManager> mockedInventoryManagerStatic;
    private MockedStatic<LocalDate> mockedLocalDateStatic;

    private DiscountPricingStrategy discountStrategy;

    @BeforeEach
    void setUp() {
        // Mock the construction of DiscountRepository within DiscountPricingStrategy
        mockedDiscountRepoConstruction = mockConstruction(DiscountRepository.class, (mock, context) -> mockDiscountRepository = mock);

        // Mock InventoryManager.getInstance() to return our mock instance
        mockedInventoryManagerStatic = mockStatic(InventoryManager.class);
        mockedInventoryManagerStatic.when(() -> InventoryManager.getInstance(null))
                .thenReturn(mockInventoryManager);
        mockedInventoryManagerStatic.when(() -> InventoryManager.getInstance(any(ShelfStrategy.class)))
                .thenReturn(mockInventoryManager); // Handle potential other calls to getInstance if they happen

        // Mock LocalDate.now() for consistent date testing
        mockedLocalDateStatic = mockStatic(LocalDate.class, CALLS_REAL_METHODS); // Call real methods initially
        mockedLocalDateStatic.when(LocalDate::now).thenReturn(LocalDate.of(2025, 6, 8)); // Fix current date

        // Initialize the strategy AFTER all internal dependencies are mocked
        discountStrategy = new DiscountPricingStrategy(mockBasePriceStrategy);
    }

    @AfterEach
    void tearDown() {
        // Close all mocks
        if (mockedDiscountRepoConstruction != null) mockedDiscountRepoConstruction.close();
        if (mockedInventoryManagerStatic != null) mockedInventoryManagerStatic.close();
        if (mockedLocalDateStatic != null) mockedLocalDateStatic.close();
        
        // Reset CommonVariables.MIN_TOTAL_PRICE if it was changed for a specific test
        // This is important if CommonVariables is mutable and shared across tests
        CommonVariables.MIN_TOTAL_PRICE = 0.0; // Assuming its default value is 0.0
    }

    // --- Test Cases for DiscountPricingStrategy methods ---

    @Test
    @DisplayName("Should return base price if no active discounts are found")
    void calculate_NoActiveDiscounts() {
        // Given
        Product product = new Product("P001", "Laptop", 1000.00);
        int quantity = 1;
        double basePrice = 1000.00;

        when(mockBasePriceStrategy.calculate(product, quantity)).thenReturn(basePrice);
        when(mockInventoryManager.getQuantityOnShelf(product.getCode())).thenReturn(50); // Sufficient stock (50 > 10)
        when(mockDiscountRepository.findActiveDiscounts(product.getCode(), LocalDate.of(2025, 6, 8))).thenReturn(Collections.emptyList());

        // When
        double finalPrice = discountStrategy.calculate(product, quantity);

        // Then
        assertEquals(basePrice, finalPrice, 0.001);
        verify(mockBasePriceStrategy).calculate(product, quantity);
        verify(mockInventoryManager).getQuantityOnShelf(product.getCode());
        verify(mockDiscountRepository).findActiveDiscounts(product.getCode(), LocalDate.of(2025, 6, 8));
    }

    @Test
    @DisplayName("Should return base price if stock is below minimum quantity for discount")
    void calculate_BelowMinimumStock() {
        // Given
        Product product = new Product("P002", "Mouse", 25.00);
        int quantity = 2;
        double basePrice = 50.00;

        when(mockBasePriceStrategy.calculate(product, quantity)).thenReturn(basePrice);
        // Simulate stock below threshold
        when(mockInventoryManager.getQuantityOnShelf(product.getCode())).thenReturn(CommonVariables.MINIMUMQUANTITY - 1); 
        
        // When
        double finalPrice = discountStrategy.calculate(product, quantity);

        // Then
        assertEquals(basePrice, finalPrice, 0.001);
        verify(mockBasePriceStrategy).calculate(product, quantity);
        verify(mockInventoryManager).getQuantityOnShelf(product.getCode());
        // Crucial: verify that discountRepository was NOT called because stock was too low
        verifyNoInteractions(mockDiscountRepository); 
    }

    @Test
    @DisplayName("Should apply percentage discount if it results in a lower price")
    void calculate_PercentageDiscount() {
        // Given
        Product product = new Product("P003", "Keyboard", 100.00);
        int quantity = 1;
        double basePrice = 100.00; // 100 * 1

        // Updated Discount constructor: new Discount(id, name, type, value, start, end)
        Discount percentDiscount = new Discount(1, "KEYBOARD_10", com.syos.enums.DiscountType.PERCENT, 10.0, LocalDate.of(2025, 6, 7), LocalDate.of(2025, 6, 9)); // 10%

        when(mockBasePriceStrategy.calculate(product, quantity)).thenReturn(basePrice);
        when(mockInventoryManager.getQuantityOnShelf(product.getCode())).thenReturn(50); // Sufficient stock
        // Mocking for product.getCode()
        when(mockDiscountRepository.findActiveDiscounts(product.getCode(), LocalDate.of(2025, 6, 8))).thenReturn(Arrays.asList(percentDiscount));

        // When
        double finalPrice = discountStrategy.calculate(product, quantity);

        // Then
        double expectedPrice = basePrice * (CommonVariables.oneHundredPercent - (10.0 / CommonVariables.percentageDevisor)); // 100 * 0.90 = 90.00
        assertEquals(expectedPrice, finalPrice, 0.001); // Use delta for double comparisons
        verify(mockBasePriceStrategy).calculate(product, quantity);
        verify(mockInventoryManager).getQuantityOnShelf(product.getCode());
        verify(mockDiscountRepository).findActiveDiscounts(product.getCode(), LocalDate.of(2025, 6, 8));
    }

    @Test
    @DisplayName("Should handle multiple percentage discounts and pick the best one")
    void calculate_MultiplePercentageDiscounts() {
        // Given
        Product product = new Product("P004", "Monitor", 200.00);
        int quantity = 1;
        double basePrice = 200.00;

        // Updated Discount constructor
        Discount discount10Percent = new Discount(2, "MONITOR_10", com.syos.enums.DiscountType.PERCENT, 10.0, LocalDate.of(2025, 6, 7), LocalDate.of(2025, 6, 9)); // 10% -> 180.00
        Discount discount5Percent = new Discount(3, "MONITOR_5", com.syos.enums.DiscountType.PERCENT, 5.0, LocalDate.of(2025, 6, 7), LocalDate.of(2025, 6, 9));   // 5% -> 190.00

        when(mockBasePriceStrategy.calculate(product, quantity)).thenReturn(basePrice);
        when(mockInventoryManager.getQuantityOnShelf(product.getCode())).thenReturn(50);
        when(mockDiscountRepository.findActiveDiscounts(product.getCode(), LocalDate.of(2025, 6, 8))).thenReturn(Arrays.asList(discount10Percent, discount5Percent));

        // When
        double finalPrice = discountStrategy.calculate(product, quantity);

        // Then
        double expectedPrice = basePrice * (CommonVariables.oneHundredPercent - (10.0 / CommonVariables.percentageDevisor)); // The 10% discount is better
        assertEquals(expectedPrice, finalPrice, 0.001);
    }

    @Test
    @DisplayName("Should apply amount discount if it results in a lower price")
    void calculate_AmountDiscount() {
        // Given
        Product product = new Product("P005", "Printer", 300.00);
        int quantity = 1;
        double basePrice = 300.00;

        // Updated Discount constructor
        Discount amountDiscount = new Discount(4, "PRINTER_FIXED", com.syos.enums.DiscountType.AMOUNT, 20.0, LocalDate.of(2025, 6, 7), LocalDate.of(2025, 6, 9)); // $20 off -> 280.00

        when(mockBasePriceStrategy.calculate(product, quantity)).thenReturn(basePrice);
        when(mockInventoryManager.getQuantityOnShelf(product.getCode())).thenReturn(50);
        when(mockDiscountRepository.findActiveDiscounts(product.getCode(), LocalDate.of(2025, 6, 8))).thenReturn(Arrays.asList(amountDiscount));

        // When
        double finalPrice = discountStrategy.calculate(product, quantity);

        // Then
        double expectedPrice = basePrice - 20.0; // 280.00
        assertEquals(expectedPrice, finalPrice, 0.001);
    }

    @Test
    @DisplayName("Should pick the best discount between percentage and amount")
    void calculate_MixedDiscounts_BestSelected() {
        // Given
        Product product = new Product("P006", "Webcam", 50.00);
        int quantity = 1;
        double basePrice = 50.00;

        // Updated Discount constructor
        Discount percentDiscount = new Discount(5, "WEBCAM_10", com.syos.enums.DiscountType.PERCENT, 10.0, LocalDate.of(2025, 6, 7), LocalDate.of(2025, 6, 9)); // 10% off -> 45.00
        Discount amountDiscount = new Discount(6, "WEBCAM_FIXED", com.syos.enums.DiscountType.AMOUNT, 6.0, LocalDate.of(2025, 6, 7), LocalDate.of(2025, 6, 9)); // $6 off -> 44.00

        when(mockBasePriceStrategy.calculate(product, quantity)).thenReturn(basePrice);
        when(mockInventoryManager.getQuantityOnShelf(product.getCode())).thenReturn(50);
        when(mockDiscountRepository.findActiveDiscounts(product.getCode(), LocalDate.of(2025, 6, 8))).thenReturn(Arrays.asList(percentDiscount, amountDiscount));

        // When
        double finalPrice = discountStrategy.calculate(product, quantity);

        // Then
        double expectedPrice = basePrice - 6.0; // Amount discount is better (44.00 vs 45.00)
        assertEquals(expectedPrice, finalPrice, 0.001);
    }
    
    @Test
    @DisplayName("Should not go below MIN_TOTAL_PRICE even with large discount")
    void calculate_BelowMinPrice() {
        // Given
        Product product = new Product("P007", "Cheap Item", 5.00);
        int quantity = 1;
        double basePrice = 5.00;

        // Set MIN_TOTAL_PRICE for this specific test
        double originalMinTotalPrice = CommonVariables.MIN_TOTAL_PRICE; // Store original
        CommonVariables.MIN_TOTAL_PRICE = 1.0; 

        // Updated Discount constructor
        Discount largeDiscount = new Discount(7, "CHEAP_ITEM_DISCOUNT", com.syos.enums.DiscountType.AMOUNT, 4.50, LocalDate.of(2025, 6, 7), LocalDate.of(2025, 6, 9)); // $4.50 off -> 0.50 (which is less than MIN_TOTAL_PRICE)

        when(mockBasePriceStrategy.calculate(product, quantity)).thenReturn(basePrice);
        when(mockInventoryManager.getQuantityOnShelf(product.getCode())).thenReturn(50);
        when(mockDiscountRepository.findActiveDiscounts(product.getCode(), LocalDate.of(2025, 6, 8))).thenReturn(Arrays.asList(largeDiscount));

        // When
        double finalPrice = discountStrategy.calculate(product, quantity);

        // Then
        assertEquals(CommonVariables.MIN_TOTAL_PRICE, finalPrice, 0.001); // Price should be floored at MIN_TOTAL_PRICE
        
        // Restore original MIN_TOTAL_PRICE after the test
        CommonVariables.MIN_TOTAL_PRICE = originalMinTotalPrice;
    }

    @Test
    @DisplayName("Should apply discount correctly when quantity affects base price (e.g., if base strategy multiplies)")
    void calculate_DiscountWithQuantity() {
        // Given
        Product product = new Product("P008", "Tablet", 250.00);
        int quantity = 2;
        double basePrice = 500.00; // 250 * 2

        // Updated Discount constructor
        Discount percentDiscount = new Discount(8, "TABLET_15", com.syos.enums.DiscountType.PERCENT, 15.0, LocalDate.of(2025, 6, 7), LocalDate.of(2025, 6, 9)); // 15% off

        when(mockBasePriceStrategy.calculate(product, quantity)).thenReturn(basePrice);
        when(mockInventoryManager.getQuantityOnShelf(product.getCode())).thenReturn(50);
        when(mockDiscountRepository.findActiveDiscounts(product.getCode(), LocalDate.of(2025, 6, 8))).thenReturn(Arrays.asList(percentDiscount));

        // When
        double finalPrice = discountStrategy.calculate(product, quantity);

        // Then
        double expectedPrice = basePrice * (CommonVariables.oneHundredPercent - (15.0 / CommonVariables.percentageDevisor)); // 500 * 0.85 = 425.00
        assertEquals(expectedPrice, finalPrice, 0.001);
    }
}