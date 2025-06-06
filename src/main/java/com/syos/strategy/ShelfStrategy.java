package com.syos.strategy;

import java.util.Comparator;
import java.util.List;

import com.syos.model.StockBatch; // Keep for back-store operations
import com.syos.model.ShelfStock; // For shelf operations

public interface ShelfStrategy {
    // For selecting from back-store batches to move to shelf
    StockBatch selectBatchFromBackStore(List<StockBatch> batches);

    // For selecting from shelf batches to deduct from
    ShelfStock selectBatchFromShelf(List<ShelfStock> batches);

    // Comparator for StockBatch (used by back-store selection)
    Comparator<StockBatch> getStockBatchComparator();

    // Comparator for ShelfStock (used by shelf selection)
    Comparator<ShelfStock> getShelfStockComparator();
}