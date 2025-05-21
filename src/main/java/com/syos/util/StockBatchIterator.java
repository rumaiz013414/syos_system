package com.syos.util;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;

import com.syos.model.StockBatch;

public class StockBatchIterator implements Iterator<StockBatch> {
	private final PriorityQueue<StockBatch> queue;

	/**
	 * @param batches    the list of batches to iterate (won't be modified)
	 * @param comparator defines the iteration order
	 */
	public StockBatchIterator(List<StockBatch> batches, Comparator<StockBatch> comparator) {
		this.queue = new PriorityQueue<>(comparator);
		this.queue.addAll(batches);
	}

	@Override
	public boolean hasNext() {
		return !queue.isEmpty();
	}

	@Override
	public StockBatch next() {
		return queue.poll();
	}
}