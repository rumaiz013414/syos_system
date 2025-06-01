package com.syos.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import com.syos.command.AddProductCommand;
import com.syos.command.Command;
import com.syos.command.ReceiveStockCommand;
import com.syos.command.MoveToShelfCommand;
import com.syos.singleton.InventoryManager;
import com.syos.strategy.ExpiryAwareFifoStrategy;

public class InventoryService {
	private final InventoryManager inventoryManager;
	private final Scanner sc = new Scanner(System.in);
	private final Map<String, Command> commandMap = new HashMap<>();

	public InventoryService() {
		this.inventoryManager = InventoryManager.getInstance(new ExpiryAwareFifoStrategy());
		inventoryManager.addObserver(new StockAlertService(50));
		ProductService productService = new ProductService();

		commandMap.put("1", new ReceiveStockCommand(inventoryManager, sc));
		commandMap.put("2", new MoveToShelfCommand(inventoryManager, sc));
		commandMap.put("3", new AddProductCommand(productService, sc));
	}

	public void run() {
		while (true) {
			System.out.println("\n=== Inventory Menu ===");
			System.out.println("1) Receive stock");
			System.out.println("2) Move to shelf");
			System.out.println("3) Add product");
			System.out.println("4) Exit");
			System.out.print("Choose an option: ");

			String opt = sc.nextLine().trim();
			if ("4".equals(opt)) {
				System.out.println("Exiting Inventory Menu.");
				break;
			}

			Command cmd = commandMap.get(opt);
			if (cmd != null) {
				cmd.execute();
			} else {
				System.out.println("Invalid option. Please choose 1, 2, 3, or 4.");
			}
		}
	}
}
