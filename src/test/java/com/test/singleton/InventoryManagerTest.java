package com.test.singleton; 

import com.syos.model.Product;
import com.syos.model.ShelfStock;
import com.syos.model.StockBatch;
import com.syos.observer.StockObserver;
import com.syos.repository.ProductRepository;
import com.syos.repository.ShelfStockRepository;
import com.syos.repository.StockBatchRepository;
import com.syos.singleton.InventoryManager;
import com.syos.strategy.ShelfStrategy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryManagerTest {

    // Dependencies that InventoryManager uses
    @Mock private StockBatchRepository mockBatchRepository;
    @Mock private ShelfStockRepository mockShelfRepository;
    @Mock private ProductRepository mockProductRepository; // Used indirectly by ShelfStockRepository and in getInstance
    @Mock private ShelfStrategy mockStrategy;
    @Mock private StockObserver mockStockObserver; // For testing observer pattern

    // For mocking static methods and constructors
    private MockedStatic<InventoryManager> mockedInventoryManagerStatic;
    private MockedConstruction<StockBatchRepository> mockedStockBatchRepoConstruction;
    private MockedConstruction<ShelfStockRepository> mockedShelfStockRepoConstruction;
    private MockedConstruction<ProductRepository> mockedProductRepoConstruction;

    // For capturing System.out
    private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();
    private final PrintStream originalSystemOut = System.out;

    @BeforeEach
    void setUp() {
        // Reset the singleton instance before each test to ensure isolation
        InventoryManager.resetInstance();

        // Redirect System.out
        System.setOut(new PrintStream(outputStreamCaptor));

        // Mock the static getInstance method to return a controlled instance
        // This MUST be done before any getInstance() calls in the test
        mockedInventoryManagerStatic = mockStatic(InventoryManager.class, CALLS_REAL_METHODS);
        mockedInventoryManagerStatic.when(() -> InventoryManager.getInstance(any(ShelfStrategy.class)))
                .thenAnswer(invocation -> {
                    // This allows us to inject our mocks when getInstance is called by the test setup
                    // We need to mock the 'new' calls inside getInstance
                    mockedProductRepoConstruction = mockConstruction(ProductRepository.class, (mock, context) -> mockProductRepository = mock);
                    mockedStockBatchRepoConstruction = mockConstruction(StockBatchRepository.class, (mock, context) -> mockBatchRepository = mock);
                    mockedShelfStockRepoConstruction = mockConstruction(ShelfStockRepository.class, (mock, context) -> mockShelfRepository = mock);

                    // Call the real constructor, but with our specific mocks
                    return new InventoryManager(mockStrategy, mockBatchRepository, mockShelfRepository, mockProductRepository);
                });
    }

    @AfterEach
    void tearDown() {
        // Restore System.out
        System.setOut(originalSystemOut);

        // Close all static and constructor mocks
        if (mockedInventoryManagerStatic != null) mockedInventoryManagerStatic.close();
        if (mockedProductRepoConstruction != null) mockedProductRepoConstruction.close();
        if (mockedStockBatchRepoConstruction != null) mockedStockBatchRepoConstruction.close();
        if (mockedShelfStockRepoConstruction != null) mockedShelfStockRepoConstruction.close();

        // Reset the singleton instance for good measure
        InventoryManager.resetInstance();
    }

    // Helper method to get the InventoryManager instance with our mocks
    private InventoryManager getManagerInstance() {
        // The first call to getInstance will trigger the mockedConstruction in @BeforeEach
        InventoryManager manager = InventoryManager.getInstance(mockStrategy);
        manager.addObserver(mockStockObserver); // Add our mock observer for notification tests
        return manager;
    }

    // --- Test Cases for InventoryManager methods ---

    // --- receiveStock() tests ---
    @Test
    @DisplayName("Should successfully receive new stock and log it")
    void receiveStock_Success() {
        // Given
        InventoryManager manager = getManagerInstance();
        String productCode = "PROD001";
        LocalDate purchaseDate = LocalDate.now();
        LocalDate expiryDate = LocalDate.now().plusMonths(6);
        int quantity = 100;

        // When
        manager.receiveStock(productCode, purchaseDate, expiryDate, quantity);

        // Then
        // Verify that batchRepository.createBatch was called with the correct arguments
        verify(mockBatchRepository, times(1)).createBatch(productCode, purchaseDate, expiryDate, quantity);
        // Verify console output
        assertTrue(outputStreamCaptor.toString().contains(
                String.format("Received batch: %s qty=%d exp=%s", productCode, quantity, expiryDate)
        ));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for invalid productCode in receiveStock")
    void receiveStock_InvalidProductCode() {
        InventoryManager manager = getManagerInstance();
        assertThrows(IllegalArgumentException.class, () ->
                manager.receiveStock("", LocalDate.now(), LocalDate.now().plusMonths(1), 10));
        assertTrue(outputStreamCaptor.toString().isEmpty()); // No output expected before exception
        verifyNoInteractions(mockBatchRepository); // No interaction with repo for invalid input
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for non-positive quantity in receiveStock")
    void receiveStock_NonPositiveQuantity() {
        InventoryManager manager = getManagerInstance();
        assertThrows(IllegalArgumentException.class, () ->
                manager.receiveStock("PROD001", LocalDate.now(), LocalDate.now().plusMonths(1), 0));
        assertThrows(IllegalArgumentException.class, () ->
                manager.receiveStock("PROD001", LocalDate.now(), LocalDate.now().plusMonths(1), -5));
        verifyNoInteractions(mockBatchRepository);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for null dates in receiveStock")
    void receiveStock_NullDates() {
        InventoryManager manager = getManagerInstance();
        assertThrows(IllegalArgumentException.class, () ->
                manager.receiveStock("PROD001", null, LocalDate.now().plusMonths(1), 10));
        assertThrows(IllegalArgumentException.class, () ->
                manager.receiveStock("PROD001", LocalDate.now(), null, 10));
        verifyNoInteractions(mockBatchRepository);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException if expiry date is before purchase date in receiveStock")
    void receiveStock_ExpiryBeforePurchaseDate() {
        InventoryManager manager = getManagerInstance();
        assertThrows(IllegalArgumentException.class, () ->
                manager.receiveStock("PROD001", LocalDate.now(), LocalDate.now().minusDays(1), 10));
        verifyNoInteractions(mockBatchRepository);
    }

    // --- moveToShelf() tests ---
    @Test
    @DisplayName("Should move stock from back-store to shelf using strategy")
    void moveToShelf_Success() {
        // Given
        InventoryManager manager = getManagerInstance();
        String productCode = "PROD002";
        int qtyToMove = 15;

        // Create mock batches in back-store
        StockBatch batch1 = new StockBatch(1, productCode, LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(1), 10);
        StockBatch batch2 = new StockBatch(2, productCode, LocalDate.now().minusMonths(2), LocalDate.now().plusMonths(2), 20);
        List<StockBatch> backStoreBatches = new ArrayList<>(Arrays.asList(batch1, batch2));

        when(mockBatchRepository.findByProduct(productCode)).thenReturn(backStoreBatches);
        // Simulate strategy choosing batches
        when(mockStrategy.selectBatchFromBackStore(anyList()))
                .thenReturn(batch1) // First selection
                .thenReturn(batch2); // Second selection if needed

        // When
        manager.moveToShelf(productCode, qtyToMove);

        // Then
        // Verify strategy was called
        verify(mockStrategy, atLeastOnce()).selectBatchFromBackStore(backStoreBatches);
        // Verify updateQuantity was called for both batches used
        verify(mockBatchRepository).updateQuantity(batch1.getId(), 0); // 10 from batch1 used
        verify(mockBatchRepository).updateQuantity(batch2.getId(), 15); // 5 from batch2 used (20 - 5 = 15 remaining)

        // Verify upsertBatchQuantityOnShelf was called for the moved quantities
        verify(mockShelfRepository).upsertBatchQuantityOnShelf(productCode, batch1.getId(), 10, batch1.getExpiryDate());
        verify(mockShelfRepository).upsertBatchQuantityOnShelf(productCode, batch2.getId(), 5, batch2.getExpiryDate());

        // Verify console output
        assertTrue(outputStreamCaptor.toString().contains("Moved 10 units from back-store batch 1 to shelf for PROD002."));
        assertTrue(outputStreamCaptor.toString().contains("Moved 5 units from back-store batch 2 to shelf for PROD002."));
        assertTrue(outputStreamCaptor.toString().contains("Successfully moved 15 units of PROD002 to shelf."));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException if no stock batches found in back-store")
    void moveToShelf_NoStockBatches() {
        InventoryManager manager = getManagerInstance();
        String productCode = "PROD003";
        when(mockBatchRepository.findByProduct(productCode)).thenReturn(Collections.emptyList());

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () ->
                manager.moveToShelf(productCode, 10));

        assertTrue(thrown.getMessage().contains("No stock batches found in back-store for product: " + productCode));
        verifyNoInteractions(mockStrategy); // Strategy shouldn't be called
        verifyNoInteractions(mockShelfRepository);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException if insufficient stock in back-store")
    void moveToShelf_InsufficientBackStoreStock() {
        InventoryManager manager = getManagerInstance();
        String productCode = "PROD004";
        StockBatch batch = new StockBatch(1, productCode, LocalDate.now(), LocalDate.now().plusMonths(1), 5); // Only 5 available
        when(mockBatchRepository.findByProduct(productCode)).thenReturn(Arrays.asList(batch));

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () ->
                manager.moveToShelf(productCode, 10)); // Request 10

        assertTrue(thrown.getMessage().contains(
                String.format("Insufficient stock in back-store for %s. Available: %d, Requested: %d.", productCode, 5, 10)));
        verifyNoInteractions(mockStrategy);
        verifyNoInteractions(mockShelfRepository);
    }

    // --- deductFromShelf() tests ---
    @Test
    @DisplayName("Should deduct stock from shelf using strategy and notify if low")
    void deductFromShelf_SuccessAndNotifyLow() {
        // Given
        InventoryManager manager = getManagerInstance();
        String productCode = "PROD005";
        int quantityToDeduct = 30; // Deduct 30
        int initialShelfQuantity = 70; // Start with 70
        int expectedRemaining = initialShelfQuantity - quantityToDeduct; // 40

        // Mock shelf batches (ensure they add up to initialShelfQuantity or more)
        ShelfStock shelfBatch1 = new ShelfStock(new Product(productCode, "P", 10.0), 40, 1, LocalDate.now().plusMonths(1));
        ShelfStock shelfBatch2 = new ShelfStock(new Product(productCode, "P", 10.0), 30, 2, LocalDate.now().plusMonths(2));
        List<ShelfStock> shelfBatches = new ArrayList<>(Arrays.asList(shelfBatch1, shelfBatch2));

        when(mockShelfRepository.getQuantity(productCode))
                .thenReturn(initialShelfQuantity) // Initial check
                .thenReturn(expectedRemaining); // After deduction

        when(mockShelfRepository.getBatchesOnShelf(productCode)).thenReturn(shelfBatches);
        when(mockStrategy.selectBatchFromShelf(anyList()))
                .thenReturn(shelfBatch1); // Assume strategy picks this first

        // When
        manager.deductFromShelf(productCode, quantityToDeduct);

        // Then
        // Verify deduction call
        verify(mockShelfRepository).deductQuantityFromBatchOnShelf(productCode, shelfBatch1.getBatchId(), 30);
        // ShelfBatch1 had 40, 30 were used, so 10 remain. It should NOT be removed.
        verify(mockShelfRepository, never()).removeBatchFromShelf(productCode, shelfBatch1.getBatchId());

        // Verify console output
        assertTrue(outputStreamCaptor.toString().contains("Deducted 30 units from shelf batch 1 for PROD005."));
        assertTrue(outputStreamCaptor.toString().contains("Total deducted 30 units of PROD005 from shelf. Remaining on shelf: 40."));

        // Verify low stock notification (40 < CommonVariables.STOCK_ALERT_THRESHOLD which is 50)
        verify(mockStockObserver, times(1)).onStockLow(productCode, expectedRemaining);
    }

    @Test
    @DisplayName("Should deduct stock and remove depleted batch from shelf")
    void deductFromShelf_RemoveDepletedBatch() {
        // Given
        InventoryManager manager = getManagerInstance();
        String productCode = "PROD006";
        int quantityToDeduct = 5;
        int initialShelfQuantity = 10; // 5 from batch1, 5 from batch2

        ShelfStock shelfBatch1 = new ShelfStock(new Product(productCode, "P", 10.0), 5, 1, LocalDate.now().plusMonths(1));
        ShelfStock shelfBatch2 = new ShelfStock(new Product(productCode, "P", 10.0), 5, 2, LocalDate.now().plusMonths(2));
        List<ShelfStock> shelfBatches = new ArrayList<>(Arrays.asList(shelfBatch1, shelfBatch2));

        when(mockShelfRepository.getQuantity(productCode))
                .thenReturn(initialShelfQuantity) // Initial check
                .thenReturn(initialShelfQuantity - quantityToDeduct); // After deduction

        when(mockShelfRepository.getBatchesOnShelf(productCode)).thenReturn(new ArrayList<>(shelfBatches)); // Return a copy for internal manipulation
        when(mockStrategy.selectBatchFromShelf(anyList())).thenReturn(shelfBatch1); // Strategy picks the first batch

      
        manager.deductFromShelf(productCode, quantityToDeduct);

    
        verify(mockShelfRepository).deductQuantityFromBatchOnShelf(productCode, shelfBatch1.getBatchId(), 5);
     
        verify(mockShelfRepository).removeBatchFromShelf(productCode, shelfBatch1.getBatchId());

        assertTrue(outputStreamCaptor.toString().contains("Deducted 5 units from shelf batch 1 for PROD006."));
        assertTrue(outputStreamCaptor.toString().contains("Total deducted 5 units of PROD006 from shelf. Remaining on shelf: 5."));

    }

    @Test
    @DisplayName("Should throw IllegalArgumentException if insufficient stock on shelf")
    void deductFromShelf_InsufficientShelfStock() {
        InventoryManager manager = getManagerInstance();
        String productCode = "PROD007";
        when(mockShelfRepository.getQuantity(productCode)).thenReturn(5); // Only 5 available

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () ->
                manager.deductFromShelf(productCode, 10)); // Request 10

        assertTrue(thrown.getMessage().contains(
                String.format("Insufficient stock on shelf for %s. Available: %d, Requested: %d.", productCode, 5, 10)));
        verifyNoInteractions(mockStrategy);
    }

    // --- removeEntireBatch() tests ---
    @Test
    @DisplayName("Should remove entire batch from back-store and shelf if present")
    void removeEntireBatch_FromBoth() {
        // Given
        InventoryManager manager = getManagerInstance();
        String productCode = "PROD008";
        int batchId = 101;
        StockBatch backStoreBatch = new StockBatch(batchId, productCode, LocalDate.now(), LocalDate.now().plusMonths(3), 50);
        ShelfStock shelfBatch = new ShelfStock(new Product(productCode, "P", 10.0), 20, batchId, LocalDate.now().plusMonths(3));

        when(mockBatchRepository.findById(batchId)).thenReturn(backStoreBatch);
        when(mockShelfRepository.getBatchesOnShelf(productCode)).thenReturn(Arrays.asList(shelfBatch));
        when(mockShelfRepository.getQuantity(productCode)).thenReturn(30); // Simulate other shelf stock remains

        // When
        manager.removeEntireBatch(batchId);

    
        verify(mockShelfRepository).removeBatchFromShelf(productCode, batchId);
        verify(mockBatchRepository).setBatchQuantityToZero(batchId);

        assertTrue(outputStreamCaptor.toString().contains("Removed 20 units of Batch ID 101 (PROD008) from shelf."));
        assertTrue(outputStreamCaptor.toString().contains("Set remaining quantity of Batch ID 101 (PROD008) to 0 in back-store (was 50)."));
        assertTrue(outputStreamCaptor.toString().contains("Operation completed for Batch ID 101 (PROD008)."));
  
    }

    @Test
    @DisplayName("Should remove entire batch only from back-store if not on shelf")
    void removeEntireBatch_OnlyBackStore() {
       
        InventoryManager manager = getManagerInstance();
        String productCode = "PROD009";
        int batchId = 102;
        StockBatch backStoreBatch = new StockBatch(batchId, productCode, LocalDate.now(), LocalDate.now().plusMonths(3), 50);

        when(mockBatchRepository.findById(batchId)).thenReturn(backStoreBatch);
        when(mockShelfRepository.getBatchesOnShelf(productCode)).thenReturn(Collections.emptyList());
        when(mockShelfRepository.getQuantity(productCode)).thenReturn(60); 

        // When
        manager.removeEntireBatch(batchId);

        // Then
        verify(mockShelfRepository, never()).removeBatchFromShelf(anyString(), anyInt()); 
        verify(mockBatchRepository).setBatchQuantityToZero(batchId);

        assertTrue(outputStreamCaptor.toString().contains("Batch ID 102 (PROD009) was not found on the shelf, only in back-store records."));
        assertTrue(outputStreamCaptor.toString().contains("Set remaining quantity of Batch ID 102 (PROD009) to 0 in back-store (was 50)."));
        assertTrue(outputStreamCaptor.toString().contains("Operation completed for Batch ID 102 (PROD009)."));
        verify(mockStockObserver, never()).onStockLow(anyString(), anyInt());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException if batch not found for removeEntireBatch")
    void removeEntireBatch_BatchNotFound() {
        InventoryManager manager = getManagerInstance();
        int batchId = 999;
        when(mockBatchRepository.findById(batchId)).thenReturn(null);

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () ->
                manager.removeEntireBatch(batchId));

        assertTrue(thrown.getMessage().contains("Batch with ID " + batchId + " not found in back-store records."));
        verifyNoInteractions(mockShelfRepository);
    }

   
    @Test
    @DisplayName("Should return sum of back-store and shelf quantities for available stock")
    void getAvailableStock_Success() {
        // Given
        InventoryManager manager = getManagerInstance();
        String productCode = "PROD010";
        StockBatch backStoreBatch1 = new StockBatch(1, productCode, LocalDate.now(), LocalDate.now().plusMonths(1), 15);
        StockBatch backStoreBatch2 = new StockBatch(2, productCode, LocalDate.now(), LocalDate.now().plusMonths(2), 25);

        when(mockBatchRepository.findByProduct(productCode)).thenReturn(Arrays.asList(backStoreBatch1, backStoreBatch2));
        when(mockShelfRepository.getQuantity(productCode)).thenReturn(50); 

        // When
        int availableStock = manager.getAvailableStock(productCode);

        // Then
        assertEquals(15 + 25 + 50, availableStock); 
        verify(mockBatchRepository).findByProduct(productCode);
        verify(mockShelfRepository).getQuantity(productCode);
    }

  
    @Test
    @DisplayName("Should successfully discard quantity from a batch")
    void discardBatchQuantity_Success() {
    
        InventoryManager manager = getManagerInstance();
        int batchId = 201;
        String productCode = "PROD011";
        int initialQuantity = 100;
        int quantityToDiscard = 20;
        StockBatch batch = new StockBatch(batchId, productCode, LocalDate.now(), LocalDate.now().plusMonths(1), initialQuantity);

        when(mockBatchRepository.findById(batchId)).thenReturn(batch);
        manager.discardBatchQuantity(batchId, quantityToDiscard);
        int expectedRemaining = initialQuantity - quantityToDiscard;
        verify(mockBatchRepository, times(1)).updateQuantity(batchId, expectedRemaining);
        assertTrue(outputStreamCaptor.toString().contains(
            String.format("Discarded %d units from batch ID %d. Remaining quantity: %d.", quantityToDiscard, batchId, expectedRemaining)
        ));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException if batch not found for discardBatchQuantity")
    void discardBatchQuantity_BatchNotFound() {
        InventoryManager manager = getManagerInstance();
        int batchId = 999;
        when(mockBatchRepository.findById(batchId)).thenReturn(null);

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () ->
            manager.discardBatchQuantity(batchId, 10)
        );
        assertTrue(thrown.getMessage().contains("Batch with ID " + batchId + " not found."));
        verify(mockBatchRepository, never()).updateQuantity(anyInt(), anyInt());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException if quantity to discard is non-positive")
    void discardBatchQuantity_NonPositiveQuantity() {
        InventoryManager manager = getManagerInstance();
        int batchId = 202;
        String productCode = "PROD012";
        StockBatch batch = new StockBatch(batchId, productCode, LocalDate.now(), LocalDate.now().plusMonths(1), 50);
        when(mockBatchRepository.findById(batchId)).thenReturn(batch);

        assertThrows(IllegalArgumentException.class, () ->
            manager.discardBatchQuantity(batchId, 0)
        );
        assertThrows(IllegalArgumentException.class, () ->
            manager.discardBatchQuantity(batchId, -5)
        );
        verify(mockBatchRepository, never()).updateQuantity(anyInt(), anyInt());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException if quantity to discard exceeds remaining")
    void discardBatchQuantity_ExceedsRemaining() {
        InventoryManager manager = getManagerInstance();
        int batchId = 203;
        String productCode = "PROD013";
        int initialQuantity = 10;
        StockBatch batch = new StockBatch(batchId, productCode, LocalDate.now(), LocalDate.now().plusMonths(1), initialQuantity);
        when(mockBatchRepository.findById(batchId)).thenReturn(batch);

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () ->
            manager.discardBatchQuantity(batchId, 15)
        );
        assertTrue(thrown.getMessage().contains(
            String.format("Cannot discard 15 units from batch %d. Only 10 remaining.", batchId)
        ));
        verify(mockBatchRepository, never()).updateQuantity(anyInt(), anyInt());
    }

 
    @Test
    @DisplayName("removeQuantityFromShelf should delegate to deductFromShelf")
    void removeQuantityFromShelf_DelegatesCorrectly() {
       
        InventoryManager manager = getManagerInstance();
        String productCode = "PROD014";
        int quantity = 5;
        when(mockShelfRepository.getQuantity(productCode)).thenReturn(10); 
        ShelfStock shelfBatch = new ShelfStock(new Product(productCode, "P", 10.0), 10, 1, LocalDate.now().plusMonths(1));
        when(mockShelfRepository.getBatchesOnShelf(productCode)).thenReturn(Arrays.asList(shelfBatch));
        when(mockStrategy.selectBatchFromShelf(anyList())).thenReturn(shelfBatch);

      
        manager.removeQuantityFromShelf(productCode, quantity);

        verify(mockShelfRepository, times(1)).deductQuantityFromBatchOnShelf(productCode, shelfBatch.getBatchId(), quantity);

    }
}