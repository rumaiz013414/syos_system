package com.syos.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import com.syos.command.AddProductCommand;
import com.syos.command.AssignDiscountCommand;
import com.syos.command.Command;
import com.syos.command.CreateDiscountCommand;
import com.syos.command.MoveToShelfCommand;
import com.syos.command.ReceiveStockCommand;
import com.syos.repository.DiscountRepository;
import com.syos.repository.ProductRepository;
import com.syos.singleton.InventoryManager;
import com.syos.strategy.ExpiryAwareFifoStrategy;

public class InventoryService {
	private final InventoryManager inventoryManager;
	private final Scanner scanner = new Scanner(System.in);
	private final Map<String, Command> commandMap = new HashMap<>();

	public InventoryService() {
		this.inventoryManager = InventoryManager.getInstance(new ExpiryAwareFifoStrategy());
		inventoryManager.addObserver(new StockAlertService(50));
		ProductService productService = new ProductService();
		ProductRepository productRepository = new ProductRepository();
		DiscountRepository discountRepository = new DiscountRepository();

		commandMap.put("1", new ReceiveStockCommand(inventoryManager, scanner));
		commandMap.put("2", new MoveToShelfCommand(inventoryManager, scanner));
		commandMap.put("3", new AddProductCommand(productService, scanner, productRepository));
		commandMap.put("4", new CreateDiscountCommand(scanner, discountRepository));
		commandMap.put("5", new AssignDiscountCommand(scanner, discountRepository, productRepository));
	}

	public void run() {
		while (true) {
			System.out.println("\n=== Inventory Menu ===");
			System.out.println("1) Receive stock");
			System.out.println("2) Move to shelf");
			System.out.println("3) Add product");
			System.out.println("4) Create new discount");
			System.out.println("5) Assign discount to product");
			System.out.println("6) Exit");
			System.out.print("Choose an option: ");

			String choice = scanner.nextLine().trim();
			if ("6".equals(choice)) {
				System.out.println("Exiting Inventory Menu.");
				break;
			}

			Command cmd = commandMap.get(choice);
			if (cmd != null) {
				cmd.execute();
			} else {
				System.out.println("Invalid option. Please choose 1, 2, 3, 4, 5, or 6.");
			}
		}
	}
}