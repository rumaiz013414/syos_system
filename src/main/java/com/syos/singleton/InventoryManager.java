package com.syos.singleton;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import com.syos.model.StockBatch;
import com.syos.observer.StockObserver;
import com.syos.repository.ShelfStockRepository;
import com.syos.repository.StockBatchRepository;
import com.syos.strategy.ShelfStrategy;

public class InventoryManager {
    private static InventoryManager instance;

    private final StockBatchRepository batchRepository; 
    private final ShelfStockRepository shelfRepository; 
    private final ShelfStrategy strategy;
    private final List<StockObserver> observers = new ArrayList<>();

    public InventoryManager(ShelfStrategy strategy, StockBatchRepository batchRepository, ShelfStockRepository shelfRepository) {
        this.strategy = strategy;
        this.batchRepository = batchRepository;
        this.shelfRepository = shelfRepository;
    }

    
    public static synchronized InventoryManager getInstance(ShelfStrategy strat) {
        if (instance == null) {
            instance = new InventoryManager(strat, new StockBatchRepository(), new ShelfStockRepository());
        }
        return instance;
    }

    public static synchronized void resetInstance() {
        instance = null;
    }

    public void addObserver(StockObserver obs) {
        observers.add(obs);
    }

    protected void notifyLow(String code, int remaining) {
        for (var o : observers) {
            o.onStockLow(code, remaining);
        }
    }

    public void receiveStock(String productCode, LocalDate purchaseDate, LocalDate expiryDate, int quantity) {
        if (productCode == null || productCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Product code cannot be empty.");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive.");
        }
        if (purchaseDate == null || expiryDate == null) {
            throw new IllegalArgumentException("Purchase date and expiry date cannot be null.");
        }
        if (expiryDate.isBefore(purchaseDate)) {
            throw new IllegalArgumentException("Expiry date cannot be before purchase date.");
        }

        batchRepository.createBatch(productCode, purchaseDate, expiryDate, quantity);
        System.out.printf("Received batch: %s qty=%d exp=%s%n", productCode, quantity, expiryDate);
    }

    public void moveToShelf(String productCode, int qtyToMove) {
        if (productCode == null || productCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Product code cannot be empty.");
        }
        if (qtyToMove <= 0) {
            throw new IllegalArgumentException("Quantity to move must be positive.");
        }

        int remainingToMove = qtyToMove;

        List<StockBatch> batches = batchRepository.findByProduct(productCode);

        if (batches == null || batches.isEmpty()) {
            throw new IllegalArgumentException("No stock batches found for product: " + productCode);
        }

        int totalAvailableInBatches = batches.stream().mapToInt(StockBatch::getQuantityRemaining).sum();
        if (totalAvailableInBatches < qtyToMove) {
            throw new IllegalArgumentException(String.format("Insufficient stock in back-store for %s. Available: %d, Requested: %d.", productCode, totalAvailableInBatches, qtyToMove));
        }

        while (remainingToMove > 0 && !batches.isEmpty()) {
            StockBatch chosenBatch = strategy.selectBatch(batches);
            if (chosenBatch == null) {
                throw new IllegalStateException("Shelf strategy returned null batch unexpectedly.");
            }

            int availableInBatch = chosenBatch.getQuantityRemaining();
            int usedFromBatch = Math.min(availableInBatch, remainingToMove);

            chosenBatch.setQuantityRemaining(availableInBatch - usedFromBatch);
            batchRepository.updateQuantity(chosenBatch.getId(), chosenBatch.getQuantityRemaining());

            shelfRepository.upsertQuantity(productCode, usedFromBatch); 
            System.out.printf("Moved %d units from batch %d to shelf for %s.%n", usedFromBatch, chosenBatch.getId(), productCode);

            remainingToMove -= usedFromBatch;

            if (chosenBatch.getQuantityRemaining() == 0) {
                batches.remove(chosenBatch);
            }
        }
        System.out.printf("Successfully moved %d units of %s to shelf.%n", qtyToMove, productCode);
    }

    public void deductFromShelf(String productCode, int qty) {
        if (productCode == null || productCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Product code cannot be empty.");
        }
        if (qty <= 0) {
            throw new IllegalArgumentException("Quantity to deduct must be positive.");
        }

        int currentShelfQuantity = shelfRepository.getQuantity(productCode);
        if (currentShelfQuantity < qty) {
            throw new IllegalArgumentException(String.format("Insufficient stock on shelf for %s. Available: %d, Requested: %d.", productCode, currentShelfQuantity, qty));
        }

        shelfRepository.deductQuantity(productCode, qty);
        int remain = shelfRepository.getQuantity(productCode); 
        System.out.printf("Deducted %d units of %s from shelf. Remaining: %d.%n", qty, productCode, remain);

        if (remain < 50) { 
            notifyLow(productCode, remain);
        }
    }

    public int getQuantityOnShelf(String productCode) {
        if (productCode == null || productCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Product code cannot be empty.");
        }
        return shelfRepository.getQuantity(productCode);
    }
    
    public List<StockBatch> getBatchesForProduct(String productCode) {
        return batchRepository.findByProduct(productCode);
    }

    public List<String> getAllProductCodes() {
        return shelfRepository.getAllProductCodes(); 
    }
    
    public List<String> getAllProductCodesWithExpiringBatches(int daysThreshold) {
        List<StockBatch> allExpiringBatches = batchRepository.findAllExpiringBatches(daysThreshold);
        List<String> productCodes = new ArrayList<>();
        for (StockBatch batch : allExpiringBatches) {
            if (!productCodes.contains(batch.getProductCode())) {
                productCodes.add(batch.getProductCode());
            }
        }
        return productCodes;
    }

    // get specific batches close to expiry for a given product
    public List<StockBatch> getExpiringBatchesForProduct(String productCode, int daysThreshold) {
        return batchRepository.findExpiringBatches(productCode, daysThreshold);
    }

    // remove quantity from shelf (used by RemoveExpiryStockCommand)
    public void removeQuantityFromShelf(String productCode, int quantityToRemove) {
        deductFromShelf(productCode, quantityToRemove);
    }

}