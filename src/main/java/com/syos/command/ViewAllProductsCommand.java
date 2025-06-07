package com.syos.command;

import com.syos.model.Product;
import com.syos.repository.ProductRepository;
import java.util.List;
import java.util.Scanner;

public class ViewAllProductsCommand implements Command {
    private final ProductRepository productRepository;
    private final String NL = System.lineSeparator(); // Consistent newline for all outputs

    public ViewAllProductsCommand(ProductRepository productRepository, Scanner scanner) {
        this.productRepository = productRepository;
    }

    @Override
    public void execute() {
        // Use NL for consistent newline
        System.out.println(NL + "--- Viewing All Products ---");

        try {
            List<Product> products = productRepository.findAll();

            if (products.isEmpty()) {
                System.out.println("No products found in the system.");
                return;
            }

           
            System.out.println("----------------------------------------------------");
            System.out.printf("%-15s %-25s %-10s%n", "Product Code", "Product Name", "Price (LKR)");
            System.out.println("----------------------------------------------------");

           
            for (Product product : products) {
                System.out.printf("%-15s %-25s %-10.2f%n",
                                  product.getCode(),
                                  product.getName(),
                                  product.getPrice());
            }
           
            System.out.println("----------------------------------------------------");

        } catch (RuntimeException e) {
            System.out.println("Error retrieving products: " + e.getMessage());
        }
    }
}