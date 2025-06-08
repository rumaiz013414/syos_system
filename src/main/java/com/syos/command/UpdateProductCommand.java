package com.syos.command;

import com.syos.service.ProductService;
import com.syos.model.Product;

import java.util.Scanner;

public class UpdateProductCommand implements Command {
    private final ProductService productService;
    public final Scanner scanner;

    public UpdateProductCommand(ProductService productService, Scanner scanner) {
        this.productService = productService;
        this.scanner = scanner;
    }

    @Override
    public void execute() {
        System.out.println("\n--- Update Product Name ---");
        System.out.print("Enter product code to update: ");
        String productCode = scanner.nextLine().trim();

        Product existingProduct = productService.findProductByCode(productCode);
        if (existingProduct == null) {
            System.out.println("Product with code '" + productCode + "' not found.");
            return;
        }

        System.out.println("Current Product Details:");
        System.out.println("Code: " + existingProduct.getCode());
        System.out.println("Name: " + existingProduct.getName());
        System.out.println("Price: " + String.format("%.2f", existingProduct.getPrice())); // Display price, but not editable

        System.out.print("Enter new product name: ");
        String newName = scanner.nextLine().trim();

        // Basic validation: name cannot be empty
        if (newName.isEmpty()) {
            System.out.println("Product name cannot be empty. No changes made.");
            return;
        }

        try {
            productService.updateProductName(productCode, newName);
            System.out.println("Product name for '" + productCode + "' updated successfully to '" + newName + "'!");
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        } catch (RuntimeException e) {
            System.out.println("An unexpected error occurred while updating the product name: " + e.getMessage());
        }
    }
}
