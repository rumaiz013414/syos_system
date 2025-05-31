package com.syos.strategy;

import com.syos.model.StockBatch;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * A ShelfStrategy that:
 *
 * 1) Finds all “safe” batches (expiryDate > today + 7 days). Among those,
 *    picks the one with the earliest purchaseDate.
 * 2) If no “safe” batch exists, picks the batch with the earliest purchaseDate
 *    among all (i.e. everything is near‐expiry, so just do pure FIFO).
 */
public class ExpiryAwareFifoStrategy implements ShelfStrategy {

    @Override
    public StockBatch selectBatch(List<StockBatch> batches) {
        if (batches == null || batches.isEmpty()) {
            return null;
        }

        LocalDate cutoff = LocalDate.now().plusWeeks(1);
        StockBatch bestSafe = null;

        // 1) Scan for “safe” batches (expiryDate > cutoff), pick earliest purchaseDate
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

        // 2) If no safe batch, all are “near‐expiry”—pick earliest purchaseDate among all
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
