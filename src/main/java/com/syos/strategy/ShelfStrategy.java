package com.syos.strategy;

import java.util.Comparator;
import java.util.List;

import com.syos.model.StockBatch;

public interface ShelfStrategy {
	StockBatch selectBatch(List<StockBatch> batches);
	
    /**
     * @return a Comparator that orders StockBatch instances
     *         according to this strategy’s policy.
     */
    Comparator<StockBatch> getComparator();
    
}
