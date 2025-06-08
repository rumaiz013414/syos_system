package com.syos.strategy;

import java.util.Comparator;
import java.util.List;

import com.syos.model.StockBatch; // Keep for back-store operations
import com.syos.model.ShelfStock; // For shelf operations

public interface ShelfStrategy {
	// for selecting from inventory batches to move to shelf
	StockBatch selectBatchFromBackStore(List<StockBatch> batches);

	// for selecting from shelf batches to deduct from
	ShelfStock selectBatchFromShelf(List<ShelfStock> batches);

	// comparator for StockBatch used by inventory selection
	Comparator<StockBatch> getStockBatchComparator();

	// comparator for ShelfStock used by shelf selection
	Comparator<ShelfStock> getShelfStockComparator();
}