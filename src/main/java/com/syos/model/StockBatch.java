package com.syos.model;

import java.time.LocalDate;

public class StockBatch {
	private final int id;
	private final String productCode;
	private final LocalDate purchaseDate;
	private final LocalDate expiryDate;
	private int quantityRemaining;

	public StockBatch(int id, String productCode, LocalDate purchaseDate, LocalDate expiryDate, int quantityRemaining) {
		this.id = id;
		this.productCode = productCode;
		this.purchaseDate = purchaseDate;
		this.expiryDate = expiryDate;
		this.quantityRemaining = quantityRemaining;
	}

	public int getId() {
		return id;
	}

	public String getProductCode() {
		return productCode;
	}

	public LocalDate getPurchaseDate() {
		return purchaseDate;
	}

	public LocalDate getExpiryDate() {
		return expiryDate;
	}

	public int getQuantityRemaining() {
		return quantityRemaining;
	}

	public void setQuantityRemaining(int quantityRemaining) {
		this.quantityRemaining = quantityRemaining;
	}
}
