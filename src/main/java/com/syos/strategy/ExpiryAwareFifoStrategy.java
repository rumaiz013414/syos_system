package com.syos.strategy;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import com.syos.model.StockBatch;
import com.syos.util.CommonVariables;
import com.syos.model.ShelfStock;

public class ExpiryAwareFifoStrategy implements ShelfStrategy {

	// implementation for selecting from inventory batches
	@Override
	public StockBatch selectBatchFromBackStore(List<StockBatch> batches) {
		if (batches == null || batches.isEmpty()) {
			return null;
		}

		LocalDate cutoff = LocalDate.now().plusWeeks(CommonVariables.discountExpiryWeeks);

		// prioritize batches that are not expiring soon
		StockBatch bestSafe = batches.stream().filter(batch -> batch.getExpiryDate().isAfter(cutoff))
				.min(Comparator.comparing(StockBatch::getPurchaseDate).thenComparing(StockBatch::getExpiryDate))
				.orElse(null);

		if (bestSafe != null) {
			return bestSafe;
		}

		return batches.stream()
				.min(Comparator.comparing(StockBatch::getPurchaseDate).thenComparing(StockBatch::getExpiryDate))

				.orElse(null);
	}

	// implementation for selecting from shelf batches
	@Override
	public ShelfStock selectBatchFromShelf(List<ShelfStock> batches) {
		if (batches == null || batches.isEmpty()) {
			return null;
		}

		LocalDate cutoff = LocalDate.now().plusWeeks(CommonVariables.discountExpiryWeeks);

		// find best safe non-expiring soon batch based on earliest expiry date
		ShelfStock bestSafe = batches.stream().filter(batch -> batch.getExpiryDate().isAfter(cutoff))
				.min(Comparator.comparing(ShelfStock::getExpiryDate).thenComparing(ShelfStock::getBatchId))
				.orElse(null);

		if (bestSafe != null) {
			return bestSafe;
		}

		return batches.stream()
				.min(Comparator.comparing(ShelfStock::getExpiryDate).thenComparing(ShelfStock::getBatchId))
				.orElse(null);
	}

	@Override
	public Comparator<StockBatch> getStockBatchComparator() {
		return Comparator.comparing(StockBatch::getPurchaseDate).thenComparing(StockBatch::getExpiryDate);
	}

	@Override
	public Comparator<ShelfStock> getShelfStockComparator() {
		return Comparator.comparing(ShelfStock::getExpiryDate).thenComparing(ShelfStock::getBatchId);
	}
}