package com.syos.command;

import java.util.Scanner;

import com.syos.model.Product;
import com.syos.service.ProductService;

public class AddProductCommand implements Command {
    private final ProductService productService;
    private final Scanner scanner;

    public AddProductCommand(ProductService productService, Scanner scanner) {
        this.productService = productService;
        this.scanner = scanner;
    }

    @Override
    public void execute() {
        System.out.println("\n=== Add New Product ===");

        System.out.print("Enter product code: ");
        String code = scanner.nextLine().trim();
        if (code.isEmpty()) {
            System.out.println("Product code cannot be empty.");
            return;
        }

        System.out.print("Enter product name: ");
        String name = scanner.nextLine().trim();
        if (name.isEmpty()) {
            System.out.println("Product name cannot be empty.");
            return;
        }

        System.out.print("Enter product price: ");
        String priceInput = scanner.nextLine().trim();
        double price;
        try {
            price = Double.parseDouble(priceInput);
            if (price < 0) {
                System.out.println("Price must be non-negative.");
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid price format.");
            return;
        }

        try {
            Product created = productService.addProduct(code, name, price);
            System.out.printf("Product added: %s | %s | %.2f%n",
                               created.getCode(),
                               created.getName(),
                               created.getPrice());
        } catch (RuntimeException e) {
            System.out.println("Failed to add product: " + e.getMessage());
        }
    }
}
