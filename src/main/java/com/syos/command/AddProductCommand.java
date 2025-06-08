package com.syos.command;

import java.util.Scanner;

import com.syos.model.Product;
import com.syos.repository.ProductRepository;
import com.syos.service.ProductService;
import com.syos.util.CommonVariables;

public class AddProductCommand implements Command {
	private final ProductService productService;
	private final Scanner scanner;
	private final ProductRepository productRepository;

	public AddProductCommand(ProductService productService, Scanner scanner, ProductRepository productRepository) {
		this.productService = productService;
		this.scanner = scanner;
		this.productRepository = productRepository;
	}

	@Override
	public void execute() {
		System.out.println("\n=== Add New Product ===");

		String code;
		while (true) {
			System.out.print("Enter product code (e.g., P123, 3-10 chars): ");
			code = scanner.nextLine().trim();
			if (code.isEmpty()) {
				System.out.println("Error: Product code cannot be empty.");
			} else if (code.length() < CommonVariables.MIN_CODE_LENGTH || code.length() > CommonVariables.MAX_CODE_LENGTH) {
				System.out.printf("Error: Product code must be between %d and %d characters long.%n", CommonVariables.MIN_CODE_LENGTH,
						CommonVariables.MAX_CODE_LENGTH);
			} else if (!code.matches("^[a-zA-Z0-9]+$")) {
				System.out.println("Error: Product code can only contain letters and numbers.");
			}

			else if (productRepository.findByCode(code) != null) {
				System.out.println("Error: Product code already exists. Please choose a different one.");
			} else {
				break;
			}
		}

		String name;
		while (true) {
			System.out.print("Enter product name (2-50 chars): ");
			name = scanner.nextLine().trim();
			if (name.isEmpty()) {
				System.out.println("Error: Product name cannot be empty.");
			} else if (name.length() < CommonVariables.MIN_NAME_LENGTH || name.length() > CommonVariables.MAX_NAME_LENGTH) {
				System.out.printf("Error: Product name must be between %d and %d characters long.%n", CommonVariables.MIN_NAME_LENGTH,
						CommonVariables.MAX_NAME_LENGTH);
			} else {
				break;
			}
		}

		double price;
		while (true) {
			System.out.print("Enter product price (e.g., 99.99): ");
			String priceInput = scanner.nextLine().trim();

			if (!priceInput.matches("^\\d*\\.?\\d+$")) {
				System.out.println("Error: Invalid price format. Please enter a number (e.g., 12.34).");
				continue;
			}

			try {
				price = Double.parseDouble(priceInput);
				if (price < CommonVariables.MIN_PRICE) {
					System.out.printf("Error: Price cannot be negative. Minimum price is %.2f.%n", CommonVariables.MIN_PRICE);
				} else {
					break;
				}
			} catch (NumberFormatException e) {
				System.out.println("Error: Invalid price format. Please enter a number (e.g., 12.34).");
			}
		}

		try {
			Product created = productService.addProduct(code, name, price);
			System.out.printf("Success: Product added! Details: %s | %s | %.2f%n", created.getCode(), created.getName(),
					created.getPrice());
		} catch (IllegalArgumentException e) {
			System.out.println("Failed to add product: " + e.getMessage());
		} catch (IllegalStateException e) {
			System.out.println("Operation failed: " + e.getMessage());
		} catch (RuntimeException e) {
			System.out.println("An unexpected error occurred while adding the product: " + e.getMessage());
			e.printStackTrace();
		}
	}
}