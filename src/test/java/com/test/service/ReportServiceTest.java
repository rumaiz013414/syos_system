package com.test.service;

import com.syos.dto.BillItemReportDTO;
import com.syos.dto.BillReportDTO;
import com.syos.dto.ProductStockReportItemDTO;
import com.syos.dto.ReportDTOMapper;
import com.syos.model.Bill;
import com.syos.model.BillItem;
import com.syos.model.Product;
import com.syos.repository.ReportRepository; // Only ReportRepository needs to be mocked for ReportService tests
import com.syos.service.ReportService;
import com.syos.util.CommonVariables;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito; // Import Mockito for spy
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

	@Mock
	private Scanner scanner;

	// We only need to mock ReportRepository directly for ReportService unit tests
	// because ReportService will now accept this mock in its constructor.
	@Mock
	private ReportRepository reportRepository;

	// ReportService instance will be initialized manually in setUp()
	private ReportService reportService;

	private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
	private final PrintStream originalOut = System.out;
	private final InputStream originalIn = System.in;

	@BeforeEach
	void setUp() {
		System.setOut(new PrintStream(outContent));

		CommonVariables.STOCK_ALERT_THRESHOLD = 50;
		CommonVariables.MINIMUMQUANTITY = 1;

		// Initialize ReportService using the new test-friendly constructor
		// This ensures ReportService uses the mocked 'reportRepository'
		this.reportService = new ReportService(scanner, reportRepository);
	}

	@AfterEach
	void restoreStreams() {
		System.setOut(originalOut);
		System.setIn(originalIn);
	}

	@Test
	@DisplayName("Should generate daily sales report for today with data")
	void generateDailySalesReport_todayWithData() {
		// Arrange
		LocalDate today = LocalDate.now();
		String expectedDateString = today.format(DateTimeFormatter.ISO_DATE);

		when(scanner.nextLine()).thenReturn("");

		Date todayAsDate = Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant());

		Bill bill1 = new Bill(1, 1001, todayAsDate, 50.0, 60.0, 10.0, "CASH");
		Bill bill2 = new Bill(2, 1002, todayAsDate, 75.0, 75.0, 0.0, "CARD");
		List<Bill> mockBills = Arrays.asList(bill1, bill2);

		Product productA = new Product("PA001", "Product A", 10.0);
		Product productB = new Product("PB002", "Product B", 25.0);

		// Mock the behavior of reportRepository methods
		when(reportRepository.getBillsByDate(today)).thenReturn(mockBills);
		when(reportRepository.getBillItemsByBillId(bill1.getId()))
				.thenReturn(Arrays.asList(new BillItem(1, bill1.getId(), productA, 2, 20.0, 0.0),
						new BillItem(2, bill1.getId(), productB, 1, 25.0, 0.0)));
		when(reportRepository.getBillItemsByBillId(bill2.getId()))
				.thenReturn(Arrays.asList(new BillItem(3, bill2.getId(), productA, 3, 30.0, 0.0),
						new BillItem(4, bill2.getId(), productB, 1, 25.0, 0.0),
						new BillItem(5, bill2.getId(), productB, 1, 20.0, 5.0)));

		// Act
		reportService.generateDailySalesReport();

		// Assert
		String output = outContent.toString();
		assertTrue(output.contains("Daily Sales Report"));
		assertTrue(output.contains("Sales Report for: " + expectedDateString));
		assertTrue(output.contains("Bill #1001"));
		assertTrue(output.contains("Bill #1002"));
		assertTrue(output.contains("Total for Bill #1001: 50.00"));
		assertTrue(output.contains("Total for Bill #1002: 75.00"));
		assertTrue(output.contains("Total revenue for " + expectedDateString + ": 125.00"));

		verify(reportRepository).getBillsByDate(today);
		verify(reportRepository, times(2)).getBillItemsByBillId(anyInt());
	}

	@Test
	@DisplayName("Should show message when no daily sales records found")
	void generateDailySalesReport_noData() {
		// Arrange
		LocalDate today = LocalDate.now();
		String expectedDateString = today.format(DateTimeFormatter.ISO_DATE);
		when(scanner.nextLine()).thenReturn("");
		when(reportRepository.getBillsByDate(today)).thenReturn(Collections.emptyList());

		// Act
		reportService.generateDailySalesReport();

		// Assert
		String output = outContent.toString();
		assertTrue(output.contains("No sales records found for " + expectedDateString));
		verify(reportRepository).getBillsByDate(today);
		verify(reportRepository, never()).getBillItemsByBillId(anyInt()); // No bill items fetched if no bills
	}

	@Test
	@DisplayName("Should handle invalid date format for daily sales report")
	void generateDailySalesReport_invalidDateFormat() {
		// Arrange
		when(scanner.nextLine()).thenReturn("2024/01/01", ""); // Invalid date, then empty string (for today)

		// Mock reportRepository to return empty for any date (as no data scenario)
		when(reportRepository.getBillsByDate(any(LocalDate.class))).thenReturn(Collections.emptyList());

		// Act
		reportService.generateDailySalesReport();

		// Assert
		String output = outContent.toString();
		assertTrue(output.contains("Invalid date format. Please use YYYY-MM-DD."));
		assertTrue(output.contains("No sales records found for " + LocalDate.now().format(DateTimeFormatter.ISO_DATE)));

		verify(reportRepository).getBillsByDate(LocalDate.now()); // Verify call for today's date after invalid input
	}

	@Test
	@DisplayName("Should generate product stock report with data")
	void generateProductStockReport_withData() {
		// Arrange
		List<ProductStockReportItemDTO> mockReportItems = Arrays.asList(
				new ProductStockReportItemDTO("P001", "Item A", 10.0, 50, 100, LocalDate.now().plusMonths(3),
						LocalDate.now().plusMonths(6), 0),
				new ProductStockReportItemDTO("P002", "Item B", 20.0, 5, 10, LocalDate.now().plusMonths(1),
						LocalDate.now().plusMonths(2), 1));
		when(reportRepository.getProductStockReportData(0)).thenReturn(mockReportItems);

		// Act
		reportService.generateProductStockReport();

		// Assert
		String output = outContent.toString();
		assertTrue(output.contains("Product Stock Report"));
		assertTrue(output.contains("Item A"));
		assertTrue(output.contains("50"));
		assertTrue(output.contains("100"));
		assertTrue(output.contains("Item B"));
		assertTrue(output.contains("5"));
		assertTrue(output.contains("10"));
		assertTrue(output.contains("Summary: 2 Products | Total Shelf Quantity: 55 | Total Inventory Quantity: 110"));
		verify(reportRepository).getProductStockReportData(0);
	}

	@Test
	@DisplayName("Should show message when no product stock data found")
	void generateProductStockReport_noData() {
		// Arrange
		when(reportRepository.getProductStockReportData(0)).thenReturn(Collections.emptyList());

		// Act
		reportService.generateProductStockReport();

		// Assert
		String output = outContent.toString();
		assertTrue(output.contains("No product stock data found."));
		verify(reportRepository).getProductStockReportData(0);
	}

	@Test
	@DisplayName("Should select option 4 and generate Shelf & Inventory Analysis Report")
	void run_selectShelfAndInventoryAnalysisReport() {
		// Arrange
		when(scanner.nextLine()).thenReturn("4", "5"); // Select option 4, then 5 to exit menu

		ProductStockReportItemDTO item1 = new ProductStockReportItemDTO("P001", "Prod A", 10.0, 60, 20,
				LocalDate.now().plusMonths(1), LocalDate.now().plusMonths(3), 0);
		ProductStockReportItemDTO item2 = new ProductStockReportItemDTO("P002", "Prod B", 20.0, 5, 10,
				LocalDate.now().plusDays(5), LocalDate.now().plusMonths(1), 1);

		List<ProductStockReportItemDTO> allStockItems = Arrays.asList(item1, item2);
		when(reportRepository.getProductStockReportData(0)).thenReturn(allStockItems);

		// Act
		reportService.run();

		// Assert
		String output = outContent.toString();

		assertTrue(output.contains("=== Report Menu ==="));
		assertTrue(output.contains("4) Shelf & Inventory Analysis Report (Reshelving & Low Stock)"));

		assertTrue(
				output.contains("--- Analysis as of " + LocalDate.now().format(DateTimeFormatter.ISO_DATE) + " ---"));
		assertTrue(output.contains("1. All Items Currently on Shelf"));
		assertTrue(output.contains("Prod A") && output.contains("60"));
		assertTrue(output.contains("Prod B") && output.contains("5"));
		assertTrue(output.contains("2. Reshelving Products"));
		assertTrue(output.contains("Prod A") && output.contains("60")); // Item1 is > 50 (STOCK_ALERT_THRESHOLD)
		assertTrue(output.contains("3. Low Stock Items"));
		assertTrue(output.contains("Prod B") && output.contains("15")); // Item2 (5+10=15) is < 50

		assertTrue(output.contains("Exiting report menu."));

		verify(reportRepository).getProductStockReportData(0);
		verify(scanner, times(2)).nextLine(); // Once for '4', once for '5'
	}

	@Test
	@DisplayName("Should handle invalid menu option in run()")
	void run_invalidOption() {
		// Arrange
		when(scanner.nextLine()).thenReturn("invalid", "5"); // Invalid option, then 5 to exit

		// Act
		reportService.run();

		// Assert
		String output = outContent.toString();
		assertTrue(output.contains("Invalid option."));
		assertTrue(output.contains("Exiting report menu."));
		verify(scanner, times(2)).nextLine();
		// Verify no calls to ReportRepository methods for report generation if invalid
		// option selected
		verify(reportRepository, never()).getBillsByDate(any());
		verify(reportRepository, never()).getAllBills();
		verify(reportRepository, never()).getProductStockReportData(anyInt());
	}

	@Test
	@DisplayName("Should generate All Transactions Report with data")
	void generateAllTransactionsReport_withData() {
		// Arrange
		Date todayAsDate = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
		Bill bill1 = new Bill(1, 1001, todayAsDate, 50.0, 60.0, 10.0, "CASH");
		Bill bill2 = new Bill(2, 1002, todayAsDate, 75.0, 75.0, 0.0, "CARD");
		List<Bill> mockBills = Arrays.asList(bill1, bill2);

		Product productA = new Product("PA001", "Product A", 10.0);
		Product productB = new Product("PB002", "Product B", 25.0);

		when(reportRepository.getAllBills()).thenReturn(mockBills);
		when(reportRepository.getBillItemsByBillId(bill1.getId()))
				.thenReturn(Arrays.asList(new BillItem(1, bill1.getId(), productA, 2, 20.0, 0.0),
						new BillItem(2, bill1.getId(), productB, 1, 25.0, 0.0)));
		when(reportRepository.getBillItemsByBillId(bill2.getId()))
				.thenReturn(Arrays.asList(new BillItem(3, bill2.getId(), productA, 3, 30.0, 0.0),
						new BillItem(4, bill2.getId(), productB, 1, 25.0, 0.0),
						new BillItem(5, bill2.getId(), productB, 1, 20.0, 5.0)));

		// Act
		reportService.generateAllTransactionsReport();

		// Assert
		String output = outContent.toString();
		assertTrue(output.contains("--- All Transactions Report ---"));
		assertTrue(output.contains("Sales Report for: ALL TRANSACTIONS"));
		assertTrue(output.contains("Bill #1001"));
		assertTrue(output.contains("Bill #1002"));
		assertTrue(output.contains("Total for Bill #1001: 50.00"));
		assertTrue(output.contains("Total for Bill #1002: 75.00"));
		assertTrue(output.contains("Total revenue for all transactions: 125.00"));

		verify(reportRepository).getAllBills();
		verify(reportRepository, times(2)).getBillItemsByBillId(anyInt());
	}

	@Test
	@DisplayName("Should show message when no All Transactions records found")
	void generateAllTransactionsReport_noData() {
		// Arrange
		when(reportRepository.getAllBills()).thenReturn(Collections.emptyList());

		// Act
		reportService.generateAllTransactionsReport();

		// Assert
		String output = outContent.toString();
		assertTrue(output.contains("No sales records found."));
		verify(reportRepository).getAllBills();
		verify(reportRepository, never()).getBillItemsByBillId(anyInt()); // No bill items fetched if no bills
	}
}