//package com.syos.service;
//
//import com.syos.repository.ReportRepository;
//
//import java.time.LocalDate;
//import java.util.Scanner;
//
//public class ReportService {
//    private final Scanner scanner = new Scanner(System.in);
//    private final ReportRepository reportRepository = new ReportRepository();
//
//    public void run() {
//        while (true) {
//            System.out.println("\n=== Report Menu ===");
//            System.out.println("1) Sales Report");
//            System.out.println("2) Low Stock Report");
//            System.out.println("3) Exit");
//            System.out.print("Choose an option: ");
//            String choice = scanner.nextLine();
//
//            switch (choice) {
//                case "1" -> showSalesReport();
//                case "2" -> showLowStockReport();
//                case "3" -> {
//                    System.out.println("Exiting report menu.");
//                    return;
//                }
//                default -> System.out.println("Invalid option.");
//            }
//        }
//    }
//
////    private void showSalesReport() {
////        System.out.print("Enter start date (YYYY-MM-DD): ");
////        LocalDate start = LocalDate.parse(scanner.nextLine());
////        System.out.print("Enter end date (YYYY-MM-DD): ");
////        LocalDate end = LocalDate.parse(scanner.nextLine());
////
////        var result = reportRepository.getSalesReport(start, end);
////        System.out.printf("Total Sales: %.2f | Bills: %d%n", result.totalSales(), result.totalBills());
////    }
////
////    private void showLowStockReport() {
////        var items = reportRepository.getLowStockProducts(50); // threshold
////        System.out.println("Products low in stock:");
////        items.forEach(p -> System.out.printf(" - %s (%s): %d units%n", p.getName(), p.getCode(), p.getQuantity()));
////    }
//}
