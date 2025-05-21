package com.syos.service;

import com.syos.observer.StockObserver;

public class StockAlertService implements StockObserver {
	private final int threshold;

	public StockAlertService(int threshold) {
		this.threshold = threshold;
	}

	@Override
	public void onStockLow(String productCode, int remaining) {
		System.out.printf("!LOW STOCK: %s remaining=%d (threshold=%d)%n", productCode, remaining, threshold);
	}
}
