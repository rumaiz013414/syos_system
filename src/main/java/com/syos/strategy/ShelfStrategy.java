package com.syos.strategy;

import java.util.List;

import com.syos.model.StockBatch;

public interface ShelfStrategy {
	StockBatch selectBatch(List<StockBatch> batches);
}
