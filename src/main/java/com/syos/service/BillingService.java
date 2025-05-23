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
import com.syos.strategy.ClosestExpiryStrategy;
import com.syos.strategy.NoDiscountStrategy;

public class BillingService {
	private final ProductRepository productRepository = new ProductRepository();
	private final BillingRepository billingRepository = new BillingRepository();
	private final BillItemFactory itemFactory = new BillItemFactory(new NoDiscountStrategy());
	private final Scanner sc = new Scanner(System.in);
	private final InventoryManager inventoryManager;
	private static int stockThreshhold = 50;
	// alert if stock is below 50
	public BillingService() {
		inventoryManager = InventoryManager.getInstance(new ClosestExpiryStrategy());
		inventoryManager.addObserver(new StockAlertService(stockThreshhold));
	}

	public void run() {
		List<BillItem> items = new ArrayList<>();

		// Item entry loop
		while (true) {
			System.out.print("Input product code (enter 'done' to proceed to payment): ");
			String code = sc.nextLine().trim();
			if ("done".equalsIgnoreCase(code))
				break;

			Product p = productRepository.findByCode(code);
			if (p == null) {
				System.out.println("Code not found.");
				continue;
			}

			System.out.print("Quantity: ");
			int qty = Integer.parseInt(sc.nextLine().trim());
			items.add(itemFactory.create(p, qty));
		}

		if (items.isEmpty()) {
			System.out.println("No items entered. Exiting.");
			return;
		}

		// compute totals and accept cash
		double total = items.stream().mapToDouble(BillItem::getTotalPrice).sum();
		System.out.printf("Total due: %.2f\n", total);

		System.out.print("Cash tendered: ");
		double cash = Double.parseDouble(sc.nextLine().trim());

		// build and persist bill
		int serial = billingRepository.nextSerial();
		Bill bill = new Bill.BillBuilder(serial, items).withCashTendered(cash).build();

		billingRepository.save(bill);

		// deduct from shelf stock
		for (BillItem item : items) {
			inventoryManager.deductFromShelf(item.getProduct().getCode(), item.getQuantity());
		}

		// display summary
		System.out.println("\nBill #" + bill.getSerialNumber() + " — " + bill.getBillDate());
		items.forEach(i -> System.out.printf("  %s x%d = %.2f%n", i.getProduct().getName(), i.getQuantity(),
				i.getTotalPrice()));
		System.out.printf("Total: %.2f | Cash tendered: %.2f | Change: %.2f%n", bill.getTotalAmount(),
				bill.getCashTendered(), bill.getChangeReturned());
	}
}
