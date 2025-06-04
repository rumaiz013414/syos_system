package com.syos.strategy;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import com.syos.model.StockBatch;


public class ExpiryAwareFifoStrategy implements ShelfStrategy {

    @Override
    public StockBatch selectBatch(List<StockBatch> batches) {
        if (batches == null || batches.isEmpty()) {
            return null;
        }

        LocalDate cutoff = LocalDate.now().plusWeeks(1);
        StockBatch bestSafe = null;

        for (StockBatch batch : batches) {
            if (batch.getExpiryDate().isAfter(cutoff)) {
                if (bestSafe == null
                    || batch.getPurchaseDate().isBefore(bestSafe.getPurchaseDate())) {
                    bestSafe = batch;
                }
            }
        }

        if (bestSafe != null) {
            return bestSafe;
        }


        StockBatch oldest = null;
        for (StockBatch batch : batches) {
            if (oldest == null
                || batch.getPurchaseDate().isBefore(oldest.getPurchaseDate())) {
                oldest = batch;
            }
        }
        return oldest;
    }

	@Override
	public Comparator<StockBatch> getComparator() {
		// TODO Auto-generated method stub
		return null;
	}
}
