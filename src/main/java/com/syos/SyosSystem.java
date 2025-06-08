package com.syos;

import java.util.Scanner;

import com.syos.repository.ProductRepository;
import com.syos.repository.ShelfStockRepository;
import com.syos.repository.StockBatchRepository;
import com.syos.service.InventoryService;
import com.syos.service.OnlineStoreService;
import com.syos.service.ReportService;
import com.syos.service.StoreBillingService;
import com.syos.singleton.InventoryManager;
import com.syos.strategy.ExpiryAwareFifoStrategy;
import com.syos.strategy.ShelfStrategy;

public class SyosSystem {
	public static void main(String[] args) {

		ShelfStrategy strategy = new ExpiryAwareFifoStrategy();
		InventoryManager.getInstance(strategy);
		Scanner scanner = new Scanner(System.in);
		ProductRepository productRepository = new ProductRepository();
		ShelfStockRepository shelfStockRepository = new ShelfStockRepository(productRepository);
		StockBatchRepository stockBatchRepository = new StockBatchRepository();
		StoreBillingService billingService = new StoreBillingService();
		InventoryService inventoryService = new InventoryService();
		OnlineStoreService onlineStoreService = new OnlineStoreService();

		ReportService reportService = new ReportService(scanner, productRepository, shelfStockRepository,
				stockBatchRepository);

		while (true) {
			System.out.println("\n=== SYOS Main Menu ===");
			System.out.println(" 1) Store Billing");
			System.out.println(" 2) Online Store");
			System.out.println(" 3) Inventory");
			System.out.println(" 4) Reports");
			System.out.println(" 5) Exit");

			System.out.print("\n Select an option : ");
			String choice = scanner.nextLine().trim();

			switch (choice) {
			case "1" -> billingService.run();
			case "2" -> onlineStoreService.run();
			case "3" -> inventoryService.run();
			case "4" -> reportService.run();
			case "5" -> {
				System.out.println("Goodbye!");
				scanner.close();
				return;
			}
			default -> System.out.println("Invalid selection.");
			}
		}
	}
}