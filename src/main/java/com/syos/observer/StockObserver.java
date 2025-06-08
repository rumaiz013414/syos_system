package com.syos.observer;

public interface StockObserver {
	void onStockLow(String productCode, int quantityRemaining);
}
