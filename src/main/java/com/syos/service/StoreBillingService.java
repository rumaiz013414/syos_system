package com.syos.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import com.syos.factory.BillItemFactory;
import com.syos.model.Bill;
import com.syos.model.BillItem;
import com.syos.model.Product;
import com.syos.model.ShelfStock;
import com.syos.repository.BillingRepository;
import com.syos.repository.ProductRepository;
import com.syos.repository.ShelfStockRepository;
import com.syos.singleton.InventoryManager;
import com.syos.strategy.DiscountPricingStrategy;
import com.syos.strategy.ExpiryAwareFifoStrategy;
import com.syos.strategy.NoDiscountStrategy;
import com.syos.util.CommonVariables;

public class StoreBillingService {
	private final ProductRepository productReposiotry = new ProductRepository();
	private ShelfStockRepository shelfStockRepository = new ShelfStockRepository(productReposiotry);
	private final BillingRepository billRepository = new BillingRepository();
	private final BillItemFactory billItemFactory = new BillItemFactory(
			new DiscountPricingStrategy(new NoDiscountStrategy()));
	private final Scanner inputScanner = new Scanner(System.in);
	private final InventoryManager inventoryManager;

	public StoreBillingService() {
		inventoryManager = InventoryManager.getInstance(new ExpiryAwareFifoStrategy());
		inventoryManager.addObserver(new StockAlertService(CommonVariables.STOCK_ALERT_THRESHOLD));
	}

	String lineSeperator = "---------------------------------------------------------------------------";

	public void run() {
		while (true) {
			List<BillItem> billItems = new ArrayList<>();
			System.out.println("\n--- Start New Bill ---");
			System.out.println("Enter product details. Type 'done' to finish and proceed to payment.");

			while (true) {
				System.out.print("\nProduct Code (or 'done'): ");
				String productCode = inputScanner.nextLine().trim();
				if ("done".equalsIgnoreCase(productCode)) {
					break;
				}
				ShelfStock shelf = shelfStockRepository.findByCode(productCode);
				if (shelf == null) {
					System.out.println("Error: Product code not found in shelf. Please try again.");
					continue;
				}

				Product product = productReposiotry.findByCode(productCode);
				if (product == null) {
					System.out.println("Error: Product code not found. Please try again.");
					continue;
				}

				int availableStock = inventoryManager.getAvailableStock(product.getCode());
				if (availableStock == 0) {
					System.out.println("Product is currently out of stock. Please choose another item.");
					continue;
				}

				System.out.print("Enter Quantity: ");
				int quantity;
				try {
					quantity = Integer.parseInt(inputScanner.nextLine().trim());
					if (quantity <= 0) {
						System.out.println("Quantity must be a positive number.");
						continue;
					}
				} catch (NumberFormatException e) {
					System.out.println("Invalid quantity. Please enter a number.");
					continue;
				}

				if (quantity > availableStock) {
					System.out.printf("Insufficient stock for %s. Only %d available. Please enter a lower quantity.%n",
							product.getName(), availableStock);
					continue;
				}

				billItems.add(billItemFactory.create(product, quantity));
				System.out.printf("Added %d x %s to bill.%n", quantity, product.getName());
			}

			if (billItems.isEmpty()) {
				System.out.println("No items were added to the bill. Starting a new bill or exiting.");
				System.out.print("Process another bill? (yes/no): ");
				String choice = inputScanner.nextLine().trim().toLowerCase();
				if (!"yes".equals(choice)) {
					break;
				}
				continue;
			}

			double totalDue = billItems.stream().mapToDouble(BillItem::getTotalPrice).sum();
			System.out.printf("\n--- Order Summary ---%n");
			System.out.printf("Total amount due: %.2f%n", totalDue);
			System.out.print("Cash tendered: ");
			double cashTendered;
			try {
				cashTendered = Double.parseDouble(inputScanner.nextLine().trim());
				if (cashTendered < totalDue) {
					System.out.println("Cash tendered is less than total due. Please provide enough cash.");
					System.out.print("Do you want to cancel this bill? (yes/no): ");
					String cancelChoice = inputScanner.nextLine().trim().toLowerCase();
					if ("yes".equals(cancelChoice)) {
						System.out.println("Bill cancelled.");
						continue;
					} else {
						System.out.println("Please re-enter cash tendered correctly.");
						System.out.print("Cash tendered (retry): ");
						cashTendered = Double.parseDouble(inputScanner.nextLine().trim());
						if (cashTendered < totalDue) {
							System.out.println("Insufficient cash provided. Bill cancelled.");
							continue;
						}
					}
				}
			} catch (NumberFormatException e) {
				System.out.println("Invalid amount. Please enter a numeric value for cash tendered.");
				System.out.print("Do you want to cancel this bill? (yes/no): ");
				String cancelChoice = inputScanner.nextLine().trim().toLowerCase();
				if ("yes".equals(cancelChoice)) {
					System.out.println("Bill cancelled.");
				}
				continue;
			}

			int serialNumber = billRepository.nextSerial();
			Bill bill = new Bill.BillBuilder(serialNumber, billItems).withCashTendered(cashTendered).build();

			billRepository.save(bill);
			System.out.println("\nBill saved successfully!");

			for (BillItem item : billItems) {
				inventoryManager.deductFromShelf(item.getProduct().getCode(), item.getQuantity());
			}

			System.out.println("\n--- Final Bill #" + bill.getSerialNumber() + " ---");
			System.out.println("Date: " + bill.getBillDate());
			System.out.println(lineSeperator);
			System.out.printf("%-25s %-10s %-10s %-10s %-10s%n", "Item", "Qty", "Unit Price", "Subtotal", "Discount");
			System.out.println(lineSeperator);

			for (BillItem item : billItems) {
				String productName = item.getProduct().getName();
				int quantity = item.getQuantity();
				double unitPrice = item.getProduct().getPrice();
				double calculatedPrice = unitPrice * quantity;
				double totalPrice = item.getTotalPrice();
				double discountAmount = item.getDiscountAmount();

				if (discountAmount > 0) {
					System.out.printf("%-25s %-10d %-10.2f %-10.2f %-10.2f%n", productName, quantity, unitPrice,
							calculatedPrice, discountAmount);
					System.out.printf("%-25s %-10s %-10s %-10s %-10.2f (Net)%n", "", "", "", "", totalPrice);
				} else {
					System.out.printf("%-25s %-10d %-10.2f %-10.2f %-10s%n", productName, quantity, unitPrice,
							totalPrice, "-");
				}
			}
			System.out.println(lineSeperator);
			System.out.printf("%-50s Total: %.2f%n", "", bill.getTotalAmount());
			System.out.printf("%-50s Cash Tendered: %.2f%n", "", bill.getCashTendered());
			System.out.printf("%-50s Change Returned: %.2f%n", "", bill.getChangeReturned());
			System.out.println(lineSeperator);
			System.out.println("Sales Invoice");

			System.out.print("\nProcess another bill? (yes/no): ");
			String choice = inputScanner.nextLine().trim().toLowerCase();
			if (!"yes".equals(choice)) {
				break;
			}
		}
		System.out.println("Exiting billing");
	}
}