package com.syos;

import com.syos.service.BillingService;
import com.syos.service.InventoryService;

import java.util.Scanner;

/**
 * Main entry point: chooses between Billing and Inventory services.
 */
public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        BillingService billingService = new BillingService();
        InventoryService inventoryService = new InventoryService();

        while (true) {
            System.out.println("\n=== SYOS Main Menu ===");
            System.out.println("1) Billing");
            System.out.println("2) Inventory");
            System.out.println("3) Exit");
            System.out.println("\n");
            System.out.print("Select option: ");

            String choice = sc.nextLine().trim();
            switch (choice) {
                case "1" -> billingService.run();
                case "2" -> inventoryService.run();
                case "3" -> {
                    System.out.println("Goodbye!");
                    sc.close();
                    return;
                }
                default -> System.out.println("Invalid selection.");
            }
        }
    }
}
