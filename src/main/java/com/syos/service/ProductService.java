package com.syos.service;

import com.syos.model.Product;
import com.syos.repository.ProductRepository;
import com.syos.util.CommonVariables;

public class ProductService {
	private final ProductRepository productRepository = new ProductRepository();

	public Product addProduct(String code, String name, double price) {
		if (code.length() > CommonVariables.MAX_CODE_LENGTH) {
			throw new IllegalArgumentException("Product code must be at most 10 characters");
		}
		if (name.length() > CommonVariables.MAX_PRODUCT_NAME_LENGTH) {
			throw new IllegalArgumentException("Product name must be at most 100 characters");
		}

		if (productRepository.findByCode(code) != null) {
			throw new IllegalArgumentException("Product code already exists: " + code);
		}

		Product poduct = new Product(code, name, price);

		productRepository.add(poduct);

		return poduct;
	}
	 public Product updateProductName(String code, String newName) {
	        if (newName.length() > CommonVariables.MAX_PRODUCT_NAME_LENGTH) {
	            throw new IllegalArgumentException("Product name must be at most 100 characters");
	        }

	        Product existingProduct = productRepository.findByCode(code);
	        if (existingProduct == null) {
	            throw new IllegalArgumentException("Product with code " + code + " not found.");
	        }

	        // Update only the name
	        existingProduct.setName(newName);
	        productRepository.update(existingProduct); // The update method in repository should handle only name change now
	        return existingProduct;
	    }

	    public Product findProductByCode(String code) {
	        return productRepository.findByCode(code);
	    }
}
