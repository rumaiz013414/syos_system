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
import com.syos.command.RemoveCloseToExpiryStockCommand;
import com.syos.command.ViewStockCommand;
import com.syos.command.ViewExpiryStockCommand;
import com.syos.command.UnassignDiscountCommand;
import com.syos.command.UpdateProductCommand;
import com.syos.command.ViewAllInventoryStocksCommand;
import com.syos.command.ViewExpiringBatchesCommand;
import com.syos.command.DiscardExpiringBatchesCommand;
import com.syos.command.ViewAllProductsCommand;
import com.syos.command.ViewAllProductsWithDiscountsCommand;
import com.syos.command.ViewAllDiscountsCommand;

import com.syos.repository.DiscountRepository;
import com.syos.repository.ProductRepository;

import com.syos.singleton.InventoryManager;

import com.syos.strategy.ExpiryAwareFifoStrategy;
import com.syos.util.CommonVariables;

public class InventoryService {
	private final InventoryManager inventoryManager;
	private final Scanner scanner = new Scanner(System.in);
	private final Map<String, Command> commandMap = new HashMap<>();

	public InventoryService() {
		ProductRepository productRepository = new ProductRepository();
		DiscountRepository discountRepository = new DiscountRepository();

		this.inventoryManager = InventoryManager.getInstance(new ExpiryAwareFifoStrategy());

		inventoryManager.addObserver(new StockAlertService(CommonVariables.STOCK_ALERT_THRESHOLD));

		ProductService productService = new ProductService();

		commandMap.put("1", new AddProductCommand(productService, scanner, productRepository));
		commandMap.put("2", new ViewAllProductsCommand(productRepository, scanner));
		commandMap.put("3", new UpdateProductCommand(productService, scanner));
		commandMap.put("4", new ReceiveStockCommand(inventoryManager, scanner));
		commandMap.put("5", new MoveToShelfCommand(inventoryManager, scanner));
		commandMap.put("6", new ViewStockCommand(inventoryManager, scanner));
		commandMap.put("7", new ViewAllInventoryStocksCommand(inventoryManager, scanner));
		commandMap.put("8", new ViewExpiryStockCommand(inventoryManager, scanner));
		commandMap.put("9", new RemoveCloseToExpiryStockCommand(inventoryManager, scanner));
		commandMap.put("10", new ViewExpiringBatchesCommand(inventoryManager, scanner));
		commandMap.put("11", new DiscardExpiringBatchesCommand(inventoryManager, scanner));
		commandMap.put("12", new CreateDiscountCommand(scanner, discountRepository));
		commandMap.put("13", new AssignDiscountCommand(scanner, discountRepository, productRepository));
		commandMap.put("14", new ViewAllDiscountsCommand(discountRepository, scanner));
		commandMap.put("15", new ViewAllProductsWithDiscountsCommand(discountRepository, productRepository));
		commandMap.put("16", new UnassignDiscountCommand(scanner, discountRepository, productRepository));
	}

	public void run() {
		while (true) {
			System.out.println("\n=== Inventory Menu ===");
			System.out.println("1) Add new product");
			System.out.println("2) View all registered products");
			System.out.println("3) Update product details");
			System.out.println();
			System.out.println("4) Receive stocks to inventory");
			System.out.println("5) Move stocks from inventory to shelf");
			System.out.println();
			System.out.println("6) View all shelf stocks");
			System.out.println("7) View all inventory stocks");
			System.out.println("8) View close to expiry shelf stocks");
			System.out.println("9) Discard close to expiry stock from shelf");
			System.out.println("10) View all expiring inventory batches");
			System.out.println("11) Discard quantity from inventory batch");
			System.out.println();
			System.out.println("12) Create a new discount");
			System.out.println("13) Assign discount to products");
			System.out.println("14) View all discounts");
			System.out.println("15) View all products with discounts");
			System.out.println("16) Unassign discount from product");
			System.out.println();
			System.out.println("17) Exit");
			System.out.print("Choose an option: ");

			String choice = scanner.nextLine().trim();
			if ("17".equals(choice)) {
				System.out.println("Exiting Inventory Menu.");
				break;
			}

			Command command = commandMap.get(choice);
			if (command != null) {
				command.execute();
			} else {
				System.out.println("Invalid option. Please choose from the available numbers.");
			}
		}
	}
}