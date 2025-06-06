package com.syos.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import com.syos.factory.BillItemFactory;
import com.syos.model.Bill;
import com.syos.model.BillItem;
import com.syos.model.Product;
import com.syos.repository.BillingRepository;
import com.syos.repository.ProductRepository;
import com.syos.singleton.InventoryManager;
import com.syos.strategy.DiscountPricingStrategy;
import com.syos.strategy.ExpiryAwareFifoStrategy;
import com.syos.strategy.NoDiscountStrategy;

public class StoreBillingService {
	private final ProductRepository productReposiotry = new ProductRepository();
	private final BillingRepository billRepository = new BillingRepository();
	private final BillItemFactory billItemFactory = new BillItemFactory(
			new DiscountPricingStrategy(new NoDiscountStrategy()));
	private final Scanner inputScanner = new Scanner(System.in);
	private final InventoryManager inventoryManager;
	private static final int STOCK_ALERT_THRESHOLD = 50;

	public StoreBillingService() {
		inventoryManager = InventoryManager.getInstance(new ExpiryAwareFifoStrategy());
		inventoryManager.addObserver(new StockAlertService(STOCK_ALERT_THRESHOLD));
	}

	public void run() {
		List<BillItem> billItems = new ArrayList<>();
		System.out.println("\n Type the word 'done' after entering the products to proceed to payment.");

		// Input loop
		while (true) {
			System.out.print("\n Enter the product code : ");
			String productCode = inputScanner.nextLine().trim();
			if ("done".equalsIgnoreCase(productCode)) {
				break;
			}

			Product product = productReposiotry.findByCode(productCode);
			if (product == null) {
				System.out.println(" Code not found.");
				continue;
			}

			System.out.print("\n Quantity: ");
			int quantity = Integer.parseInt(inputScanner.nextLine().trim());
			billItems.add(billItemFactory.create(product, quantity));
		}

		if (billItems.isEmpty()) {
			System.out.println("No items entered. Exiting.");
			return;
		}

		// Compute total
		double totalDue = billItems.stream().mapToDouble(BillItem::getTotalPrice).sum();
		System.out.printf("\n Total due: %.2f\n", totalDue);

		System.out.print("\n Cash tendered: ");
		double cashTendered = Double.parseDouble(inputScanner.nextLine().trim());

		// Build and persist bill
		int serialNumber = billRepository.nextSerial();
		Bill bill = new Bill.BillBuilder(serialNumber, billItems).withCashTendered(cashTendered).build();

		billRepository.save(bill);

		// Update stock
		for (BillItem item : billItems) {
			inventoryManager.deductFromShelf(item.getProduct().getCode(), item.getQuantity());
		}

		// Display bill summary
		System.out.println("\n Bill #" + bill.getSerialNumber() + " — " + bill.getBillDate());

		for (BillItem item : billItems) {
			String productName = item.getProduct().getName();
			int quantity = item.getQuantity();
			double unitPrice = item.getProduct().getPrice(); 
			double calculatedPrice = unitPrice * quantity;
			double totalPrice = item.getTotalPrice(); 
			double discountAmount = item.getDiscountAmount();

			if (discountAmount > 0) {
				double netTotal = totalPrice;
				System.out.printf("  %s x%d @ %.2f = %.2f (Discount: %.2f | Net: %.2f)%n", productName, quantity,
						unitPrice, calculatedPrice, discountAmount, netTotal);
			} else {
				System.out.printf("  %s x%d @ %.2f = %.2f%n", productName, quantity, unitPrice, totalPrice);
			}
		}

		System.out.printf("\n Total: %.2f | Cash tendered: %.2f | Change: %.2f%n", bill.getTotalAmount(),
				bill.getCashTendered(), bill.getChangeReturned());
	}
}