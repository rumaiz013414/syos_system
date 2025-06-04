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
import com.syos.command.ViewStockCommand;
import com.syos.command.ViewExpiryStockCommand;
import com.syos.command.RemoveExpiryStockCommand;
import com.syos.command.ViewExpiringBatchesCommand;
import com.syos.command.DiscardExpiringBatchesCommand;
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
		commandMap.put("6", new ViewStockCommand(inventoryManager, scanner));
		commandMap.put("7", new ViewExpiryStockCommand(inventoryManager, scanner));
		commandMap.put("8", new RemoveExpiryStockCommand(inventoryManager, scanner));
		commandMap.put("9", new ViewExpiringBatchesCommand(inventoryManager, scanner));
		commandMap.put("10", new DiscardExpiringBatchesCommand(inventoryManager, scanner));
	}

	public void run() {
		while (true) {
			System.out.println("\n=== Inventory Menu ===");
			System.out.println("1) Receive stock");
			System.out.println("2) Move to shelf");
			System.out.println("3) Add product");
			System.out.println("4) Create new discount");
			System.out.println("5) Assign discount to product");
			System.out.println("6) View all shelf stock");
			System.out.println("7) View close to expiry shelf stock");
			System.out.println("8) Remove close to expiry stock from shelf");
			System.out.println("9) View all expiring inventory batches");
			System.out.println("10) Discard quantity from inventory batch");
			System.out.println("11) Exit");
			System.out.print("Choose an option: ");

			String choice = scanner.nextLine().trim();
			if ("11".equals(choice)) {
				System.out.println("Exiting Inventory Menu.");
				break;
			}

			Command cmd = commandMap.get(choice);
			if (cmd != null) {
				cmd.execute();
			} else {
				System.out.println("Invalid option. Please choose from the available numbers.");

			}
		}
	}
}