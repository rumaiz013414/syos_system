package com.syos.service;

import com.syos.dto.BillItemReportDTO;
import com.syos.dto.BillReportDTO;
import com.syos.dto.ProductStockReportItemDTO;
import com.syos.dto.ReportDTOMapper;
import com.syos.model.Bill;
import com.syos.model.BillItem;
import com.syos.observer.StockObserver;
import com.syos.repository.ProductRepository;
import com.syos.repository.ReportRepository;
import com.syos.repository.ShelfStockRepository;
import com.syos.repository.StockBatchRepository;
import com.syos.util.CommonVariables;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class ReportService {
	private final Scanner scanner;
	private final ReportRepository reportRepository;

	String lineSeparatorHead = "===================================================================================";
	String lineSeparatorHead2 = "=====================================================================================================================================";
	String lineSeparator = "---------------------------------------------------------------";

	public ReportService(Scanner scanner, ProductRepository productRepository,
			ShelfStockRepository shelfStockRepository, StockBatchRepository stockBatchRepository) {
		this.scanner = scanner;
		this.reportRepository = new ReportRepository(productRepository, shelfStockRepository, stockBatchRepository);
	}

	public ReportService(Scanner scanner, ReportRepository reportRepository) {
		this.scanner = scanner;
		this.reportRepository = reportRepository;
	}

	public void run() {
		while (true) {
			System.out.println("\n=== Report Menu ===");
			System.out.println("1) Daily Sales Report (Detailed)");
			System.out.println("2) All Transactions Report");
			System.out.println("3) Product Stock Report (Combined Shelf & Inventory)");
			System.out.println("4) Shelf & Inventory Analysis Report (Reshelving & Low Stock)");
			System.out.println("5) Exit");
			System.out.print("Choose an option: ");
			String choice = scanner.nextLine();

			switch (choice) {
			case "1" -> generateDailySalesReport();
			case "2" -> generateAllTransactionsReport();
			case "3" -> generateProductStockReport();
			case "4" -> generateShelfAndInventoryAnalysisReport();
			case "5" -> {
				System.out.println("Exiting report menu.");
				return;
			}
			default -> System.out.println("Invalid option.");
			}
		}
	}

	public void generateDailySalesReport() {
		System.out.println("\n--- Daily Sales Report ---");
		LocalDate reportDate = null;
		while (reportDate == null) {
			System.out.print("Enter date for report (YYYY-MM-DD) or press Enter for today's report: ");
			String dateString = scanner.nextLine().trim();

			if (dateString.isEmpty()) {
				reportDate = LocalDate.now();
			} else {
				try {
					reportDate = LocalDate.parse(dateString);
				} catch (DateTimeParseException e) {
					System.out.println("Invalid date format. Please use YYYY-MM-DD.");
				}
			}
		}

		List<Bill> bills = reportRepository.getBillsByDate(reportDate);

		if (bills.isEmpty()) {
			System.out.println("No sales records found for " + reportDate.format(DateTimeFormatter.ISO_DATE));
			return;
		}

		List<BillReportDTO> billReportDTOs = new ArrayList<>();
		double totalDailyRevenue = 0.0;

		for (Bill bill : bills) {
			List<BillItem> billItems = reportRepository.getBillItemsByBillId(bill.getId());
			List<BillItemReportDTO> itemDTOs = billItems.stream().map(ReportDTOMapper::toBillItemReportDTO)
					.collect(Collectors.toList());
			BillReportDTO billDTO = ReportDTOMapper.toBillReportDTO(bill, itemDTOs);
			billReportDTOs.add(billDTO);
			totalDailyRevenue += billDTO.getTotalAmount();
		}
		displaySalesReport(reportDate, billReportDTOs, totalDailyRevenue);
	}

	public void generateAllTransactionsReport() {
		System.out.println("\n--- All Transactions Report ---");
		List<Bill> bills = reportRepository.getAllBills();

		if (bills.isEmpty()) {
			System.out.println("No sales records found.");
			return;
		}

		List<BillReportDTO> billReportDTOs = new ArrayList<>();
		double totalRevenue = 0.0;

		for (Bill bill : bills) {
			List<BillItem> billItems = reportRepository.getBillItemsByBillId(bill.getId());
			List<BillItemReportDTO> itemDTOs = billItems.stream().map(ReportDTOMapper::toBillItemReportDTO)
					.collect(Collectors.toList());
			BillReportDTO billDTO = ReportDTOMapper.toBillReportDTO(bill, itemDTOs);
			billReportDTOs.add(billDTO);
			totalRevenue += billDTO.getTotalAmount();
		}

		displaySalesReport(null, billReportDTOs, totalRevenue);
	}

	private void displaySalesReport(LocalDate reportDate, List<BillReportDTO> billReportDTOs,
			double totalDailyRevenue) {
		if (reportDate != null) {
			System.out.println("\nSales Report for: " + reportDate.format(DateTimeFormatter.ISO_DATE));
		} else {
			System.out.println("\nSales Report for: ALL TRANSACTIONS");
		}
		System.out.println(lineSeparatorHead);
		for (BillReportDTO billDTO : billReportDTOs) {
			System.out.printf(
					"Bill #%d - Date: %s - Type: %s%n", billDTO.getSerialNumber(), billDTO.getBillDate().toInstant()
							.atZone(java.time.ZoneId.systemDefault()).toLocalDate().format(DateTimeFormatter.ISO_DATE),
					billDTO.getTransactionType());
			System.out.printf("  Cash Tendered: %.2f | Change Returned: %.2f%n", billDTO.getCashTendered(),
					billDTO.getChangeReturned());

			List<BillItemReportDTO> itemDTOs = billDTO.getItems();
			if (itemDTOs != null && !itemDTOs.isEmpty()) {
				System.out.printf("  %-25s %-10s %-10s %-12s %-10s%n", "Item", "Qty", "Unit Price", "Subtotal",
						"Discount");
				System.out
						.println("  ---------------------------------------------------------------------------------");
				for (BillItemReportDTO itemDTO : itemDTOs) {
					System.out.printf("  %-25s %-10d %-10.2f %-12.2f %-10.2f%n", itemDTO.getProductName(),
							itemDTO.getQuantity(), itemDTO.getUnitPrice(), itemDTO.getCalculatedSubtotal(),
							itemDTO.getDiscountAmount());
				}
				System.out.printf("  %-50s Total for Bill #%d: %.2f%n", "", billDTO.getSerialNumber(),
						billDTO.getTotalAmount());
			} else {
				System.out.println("  No items found for this bill.");
			}
			System.out.println("-----------------------------------------------------------------------------------");
		}

		if (reportDate != null) {
			System.out.printf("Total revenue for %s: %.2f%n", reportDate.format(DateTimeFormatter.ISO_DATE),
					totalDailyRevenue);
		} else {
			System.out.printf("Total revenue for all transactions: %.2f%n", totalDailyRevenue);
		}
		System.out.println(lineSeparatorHead);
	}

	public void generateProductStockReport() {
		System.out.println("\n--- Product Stock Report (Combined Shelf & Inventory) ---");
		List<ProductStockReportItemDTO> reportItems = reportRepository.getProductStockReportData(0);

		if (reportItems.isEmpty()) {
			System.out.println("No product stock data found.");
			return;
		}

		displayProductStockReportTable(reportItems);
	}

	private void displayProductStockReportTable(List<ProductStockReportItemDTO> reportItems) {
		System.out.println("\nProduct Stock Report");
		System.out.println(lineSeparatorHead2);
		System.out.printf("%-15s %-30s %-10s %-10s %-22s %-22s %-10s%n", "Code", "Product Name", "Shelf Qty",
				"Inv. Qty", "Earliest Shelf Exp.", "Earliest Inv. Exp.", "Exp. Batches");
		System.out.println(lineSeparatorHead2);

		for (ProductStockReportItemDTO item : reportItems) {
			String earliestShelfExpiry = (item.getEarliestExpiryDateOnShelf() != null)
					? item.getEarliestExpiryDateOnShelf().format(DateTimeFormatter.ISO_DATE)
					: "N/A";
			String earliestInvExpiry = (item.getEarliestExpiryDateInInventory() != null)
					? item.getEarliestExpiryDateInInventory().format(DateTimeFormatter.ISO_DATE)
					: "N/A";

			System.out.printf("%-15s %-30s %-10d %-10d %-22s %-22s %-10d%n", item.getProductCode(),
					item.getProductName(), item.getTotalQuantityOnShelf(), item.getTotalQuantityInInventory(),
					earliestShelfExpiry, earliestInvExpiry, item.getNumberOfExpiringBatches());
		}
		System.out.println(lineSeparatorHead2);

		int totalProducts = reportItems.size();
		int totalShelfQuantity = reportItems.stream().mapToInt(ProductStockReportItemDTO::getTotalQuantityOnShelf)
				.sum();
		int totalInventoryQuantity = reportItems.stream()
				.mapToInt(ProductStockReportItemDTO::getTotalQuantityInInventory).sum();

		System.out.printf("Summary: %d Products | Total Shelf Quantity: %d | Total Inventory Quantity: %d%n",
				totalProducts, totalShelfQuantity, totalInventoryQuantity);
		System.out.println(lineSeparatorHead2);
	}

	private void generateShelfAndInventoryAnalysisReport() {
		System.out.println("--- Analysis as of " + LocalDate.now().format(DateTimeFormatter.ISO_DATE) + " ---");

		List<ProductStockReportItemDTO> allStockItems = reportRepository.getProductStockReportData(0);

		if (allStockItems.isEmpty()) {
			System.out.println("No product stock data found for analysis.");
			return;
		}

		List<ProductStockReportItemDTO> itemsOnShelf = allStockItems.stream()
				.filter(item -> item.getTotalQuantityOnShelf() > CommonVariables.MINIMUMQUANTITY)
				.collect(Collectors.toList());

		List<ProductStockReportItemDTO> reshelveCandidates = itemsOnShelf.stream()
				.filter(item -> item.getTotalQuantityOnShelf() > CommonVariables.STOCK_ALERT_THRESHOLD)
				.collect(Collectors.toList());

		List<ProductStockReportItemDTO> lowStockItems = allStockItems.stream()
				.filter(item -> (item.getTotalQuantityOnShelf()
						+ item.getTotalQuantityInInventory()) < CommonVariables.STOCK_ALERT_THRESHOLD)
				.collect(Collectors.toList());

		System.out.println("\n1. All Items Currently on Shelf");
		if (itemsOnShelf.isEmpty()) {
			System.out.println("No items currently on the shelf.");
		} else {
			System.out.println(lineSeparatorHead);
			System.out.printf("%-15s %-30s %-15s %-22s%n", "Code", "Product Name", "Qty on Shelf",
					"Earliest Shelf Exp.");
			System.out.println(lineSeparatorHead);

			int totalShelfQuantity = 0;
			for (ProductStockReportItemDTO item : itemsOnShelf) {
				String earliestShelfExpiry = (item.getEarliestExpiryDateOnShelf() != null)
						? item.getEarliestExpiryDateOnShelf().format(DateTimeFormatter.ISO_DATE)
						: "N/A";
				System.out.printf("%-15s %-30s %-15d %-22s%n", item.getProductCode(), item.getProductName(),
						item.getTotalQuantityOnShelf(), earliestShelfExpiry);
				totalShelfQuantity += item.getTotalQuantityOnShelf();
			}
			System.out.println(lineSeparatorHead);
			System.out.printf("Total unique products on shelf: %d | Total quantity on shelf: %d%n", itemsOnShelf.size(),
					totalShelfQuantity);
			System.out.println(lineSeparatorHead);
		}

		System.out.println("\n 2. Reshelving Products");
		if (reshelveCandidates.isEmpty()) {
			System.out.println("No items on shelf currently exceed the reshelving candidate threshold of "
					+ CommonVariables.STOCK_ALERT_THRESHOLD + ".");
		} else {
			System.out.println("The following items have a high quantity on the shelf");
			System.out.println(lineSeparatorHead);
			System.out.printf("%-15s %-30s %-15s %-22s%n", "Code", "Product Name", "Qty on Shelf",
					"Earliest Shelf Exp.");
			System.out.println(lineSeparatorHead);

			int totalReshelveCandidateQuantity = 0;
			for (ProductStockReportItemDTO item : reshelveCandidates) {
				String earliestShelfExpiry = (item.getEarliestExpiryDateOnShelf() != null)
						? item.getEarliestExpiryDateOnShelf().format(DateTimeFormatter.ISO_DATE)
						: "N/A";
				System.out.printf("%-15s %-30s %-15d %-22s%n", item.getProductCode(), item.getProductName(),
						item.getTotalQuantityOnShelf(), earliestShelfExpiry);
				totalReshelveCandidateQuantity += item.getTotalQuantityOnShelf();
			}
			System.out.println(lineSeparatorHead);
			System.out.printf("Total reshelving products: %d | Total quantity: %d%n", reshelveCandidates.size(),
					totalReshelveCandidateQuantity);
			System.out.println(lineSeparatorHead);
		}

		System.out.println("\n3. Low Stock Items");
		if (lowStockItems.isEmpty()) {
			System.out.println("No products currently in low stock.");
		} else {
			System.out.println("The following products have a total quantity (shelf + inventory) below "
					+ CommonVariables.STOCK_ALERT_THRESHOLD);
			System.out.println(lineSeparatorHead);
			System.out.printf("%-15s %-30s %-10s %-10s %-10s%n", "Code", "Product Name", "Shelf Qty", "Inv. Qty",
					"Total Qty");
			System.out.println(lineSeparatorHead);

			StockObserver stockAlertService = new StockAlertService(CommonVariables.STOCK_ALERT_THRESHOLD);
			int totalLowStockProducts = 0;
			for (ProductStockReportItemDTO item : lowStockItems) {
				int totalQuantity = item.getTotalQuantityOnShelf() + item.getTotalQuantityInInventory();
				System.out.printf("%-15s %-30s %-10d %-10d %-10d%n", item.getProductCode(), item.getProductName(),
						item.getTotalQuantityOnShelf(), item.getTotalQuantityInInventory(), totalQuantity);

				stockAlertService.onStockLow(item.getProductCode(), totalQuantity);
				totalLowStockProducts++;
			}
			System.out.println(lineSeparatorHead);
			System.out.printf("Total products in low stock: %d%n", totalLowStockProducts);
			System.out.println(lineSeparatorHead);
		}
	}
}