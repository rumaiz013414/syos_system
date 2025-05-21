package com.syos.observer;

// Observer for low‐stock alerts.

public interface StockObserver {
	void onStockLow(String productCode, int remaining);
}
