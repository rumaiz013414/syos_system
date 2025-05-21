package com.syos.strategy;

import java.util.Comparator;
import java.util.List;

import com.syos.model.StockBatch;

// oldest purchase date first.
public class FifoExpiryStrategy implements ShelfStrategy {
	@Override
	public StockBatch selectBatch(List<StockBatch> batches) {
		return batches.stream().min(Comparator.comparing(StockBatch::getPurchaseDate)).orElse(null);
	}
}
