package com.syos.model;

import java.time.LocalDate;

public class ShelfStock {

	private Product product;
	private int batchId;
	private int quantity;
	private LocalDate expiryDate;

	public ShelfStock(Product product, int quantity, int batchId, LocalDate expiryDate) {
		this.product = product;
		this.quantity = quantity;
		this.batchId = batchId;
		this.expiryDate = expiryDate;
	}

	public Product getProduct() {
		return product;
	}

	public void setProduct(Product product) {
		this.product = product;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public int getBatchId() {
		return batchId;
	}

	public void setBatchId(int batchId) {
		this.batchId = batchId;
	}

	public LocalDate getExpiryDate() {
		return expiryDate;
	}

	public void setExpiryDate(LocalDate expiryDate) {
		this.expiryDate = expiryDate;
	}

}