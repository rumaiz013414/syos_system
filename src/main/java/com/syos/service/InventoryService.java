package com.syos.service;

import java.time.LocalDate;
import java.util.Scanner;

import com.syos.singleton.InventoryManager;

import com.syos.strategy.FifoExpiryStrategy;

public class InventoryService {
    private final InventoryManager inventoryManager;
    private final Scanner sc = new Scanner(System.in);

    public InventoryService() {
        this.inventoryManager = InventoryManager.getInstance(new FifoExpiryStrategy());
        inventoryManager.addObserver(new StockAlertService(50));
    }

    public void run() {
        while (true) {
            System.out.println("\n=== Inventory Menu ===");
            System.out.println("1) Receive stock");
            System.out.println("2) Move to shelf");
            System.out.println("3) Exit");
            System.out.println("\n");
            System.out.print("Choose an option: ");

            String opt = sc.nextLine().trim();
            if ("3".equals(opt)) break;

            System.out.print("Product code: ");
            String code = sc.nextLine().trim();

            System.out.print("Quantity: ");
            int qty = Integer.parseInt(sc.nextLine().trim());

            switch (opt) {
                case "1" -> {
                    System.out.print("Purchase date (YYYY-MM-DD): ");
                    LocalDate pd = LocalDate.parse(sc.nextLine().trim());
                    System.out.print("Expiry date  (YYYY-MM-DD): ");
                    LocalDate ed = LocalDate.parse(sc.nextLine().trim());
                    inventoryManager.receiveStock(code, pd, ed, qty);
                }
                case "2" -> {
                    inventoryManager.moveToShelf(code, qty);
                }
                default -> System.out.println("Invalid option.");
            }
        }
    }
}
