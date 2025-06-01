package com.syos.service;

import com.syos.model.Product;
import com.syos.repository.ProductRepository;

public class ProductService {
	private final ProductRepository productRepository = new ProductRepository();

	public Product addProduct(String code, String name, double price) {
		if (code.length() > 10) {
			throw new IllegalArgumentException("Product code must be at most 10 characters");
		}
		if (name.length() > 100) {
			throw new IllegalArgumentException("Product name must be at most 100 characters");
		}

		if (productRepository.findByCode(code) != null) {
			throw new IllegalArgumentException("Product code already exists: " + code);
		}

		Product p = new Product(code, name, price);

		productRepository.add(p);

		return p;
	}
}
