package com.syos.service;

import com.syos.dto.BillReportDTO;
import com.syos.dto.BillItemReportDTO;
import com.syos.dto.ReportDTOMapper;
import com.syos.model.Bill;
import com.syos.model.BillItem;
import com.syos.repository.ReportRepository;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class ReportService {
	private final Scanner scanner = new Scanner(System.in);
	private final ReportRepository reportRepository = new ReportRepository();

	public void run() {
		while (true) {
			System.out.println("\n=== Report Menu ===");
			System.out.println("1) Daily Sales Report (Detailed)");
			System.out.println("2) Exit");
			System.out.print("Choose an option: ");
			String choice = scanner.nextLine();

			switch (choice) {
			case "1" -> generateDailySalesReport();
			case "2" -> {
				System.out.println("Exiting report menu.");
				return;
			}
			default -> System.out.println("Invalid option.");
			}
		}
	}

	private void generateDailySalesReport() {
		System.out.println("\n--- Daily Sales Report ---");
		LocalDate reportDate = null;
		while (reportDate == null) {
			System.out.print("Enter date for report (YYYY-MM-DD): ");
			String dateString = scanner.nextLine().trim();
			try {
				reportDate = LocalDate.parse(dateString);
			} catch (DateTimeParseException e) {
				System.out.println("Invalid date format. Please use YYYY-MM-DD.");
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

	private void displaySalesReport(LocalDate reportDate, List<BillReportDTO> billReportDTOs,
			double totalDailyRevenue) {
		System.out.println("\nSales Report for: " + reportDate.format(DateTimeFormatter.ISO_DATE));
		System.out.println("===================================================================================");

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
				System.out.println("  ---------------------------------------------------------------------------------");
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

		System.out.println("\n===================================================================================");
		System.out.printf("TOTAL REVENUE FOR %s: %.2f%n", reportDate.format(DateTimeFormatter.ISO_DATE),
				totalDailyRevenue);
		System.out.println("===================================================================================");
	}
}