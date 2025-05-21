package com.syos.strategy;

import java.util.Comparator;
import java.util.List;

import com.syos.model.StockBatch;

// closest expire date first.
public class ClosestExpiryStrategy implements ShelfStrategy {
    @Override
    public StockBatch selectBatch(List<StockBatch> batches) {
        return batches.stream()
                      .min(Comparator.comparing(StockBatch::getExpiryDate))
                      .orElse(null);
    }
}