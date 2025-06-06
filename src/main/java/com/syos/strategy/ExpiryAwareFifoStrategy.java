package com.syos.strategy;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import com.syos.model.StockBatch;
import com.syos.model.ShelfStock;

public class ExpiryAwareFifoStrategy implements ShelfStrategy {

    // Implementation for selecting from back-store batches
    @Override
    public StockBatch selectBatchFromBackStore(List<StockBatch> batches) {
        if (batches == null || batches.isEmpty()) {
            return null;
        }

        LocalDate cutoff = LocalDate.now().plusWeeks(1);

        // Prioritize batches that are not expiring soon
        StockBatch bestSafe = batches.stream()
                .filter(batch -> batch.getExpiryDate().isAfter(cutoff))
                .min(Comparator
                        .comparing(StockBatch::getPurchaseDate) // FIFO for safe batches
                        .thenComparing(StockBatch::getExpiryDate)) // Secondary sort by expiry
                .orElse(null);

        if (bestSafe != null) {
            return bestSafe;
        }

        // If no safe batches, pick the oldest (earliest purchase date) regardless of expiry
        return batches.stream()
                .min(Comparator
                        .comparing(StockBatch::getPurchaseDate)
                        .thenComparing(StockBatch::getExpiryDate)) // Secondary sort by expiry
                .orElse(null);
    }

    // Implementation for selecting from shelf batches (uses ShelfStock)
    @Override
    public ShelfStock selectBatchFromShelf(List<ShelfStock> batches) {
        if (batches == null || batches.isEmpty()) {
            return null;
        }

        LocalDate cutoff = LocalDate.now().plusWeeks(1);

        // Find best safe (non-expiring soon) batch based on earliest expiry date
        ShelfStock bestSafe = batches.stream()
                .filter(batch -> batch.getExpiryDate().isAfter(cutoff))
                .min(Comparator
                        .comparing(ShelfStock::getExpiryDate)
                        .thenComparing(ShelfStock::getBatchId)) // Assuming smaller batchId means older/FIFO for shelf
                .orElse(null);

        if (bestSafe != null) {
            return bestSafe;
        }

        // If no "safe" batches, return the oldest batch (earliest expiry)
        return batches.stream()
                .min(Comparator
                        .comparing(ShelfStock::getExpiryDate)
                        .thenComparing(ShelfStock::getBatchId)) // Assuming smaller batchId means older/FIFO for shelf
                .orElse(null);
    }

    @Override
    public Comparator<StockBatch> getStockBatchComparator() {
        return Comparator
                .comparing(StockBatch::getPurchaseDate)
                .thenComparing(StockBatch::getExpiryDate);
    }

    @Override
    public Comparator<ShelfStock> getShelfStockComparator() {
        return Comparator
                .comparing(ShelfStock::getExpiryDate)
                .thenComparing(ShelfStock::getBatchId);
    }
}