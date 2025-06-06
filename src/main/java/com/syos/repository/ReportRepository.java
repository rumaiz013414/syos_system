package com.syos.repository;

import com.syos.db.DatabaseManager;
import com.syos.model.Bill;
import com.syos.model.BillItem;
import com.syos.model.Product;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReportRepository {

    private final ProductRepository productRepository = new ProductRepository();

    public double getTotalRevenue(LocalDate date) {
        String sql = """
                SELECT SUM(total_amount)
                FROM bill
                WHERE DATE(bill_date) = ?
                """;
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching total revenue for date: " + date, e);
        }
        return 0.0;
    }

    public List<Bill> getBillsByDate(LocalDate date) {
        String sql = """
                SELECT id, serial_number, bill_date, total_amount, cash_tendered, change_returned, transaction_type
                FROM bill
                WHERE DATE(bill_date) = ?
                ORDER BY serial_number ASC
                """;
        List<Bill> bills = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Bill bill = new Bill(
                        rs.getInt("id"),
                        rs.getInt("serial_number"),
                        rs.getTimestamp("bill_date"),
                        rs.getDouble("total_amount"),
                        rs.getDouble("cash_tendered"),
                        rs.getDouble("change_returned"),
                        rs.getString("transaction_type")
                    );
                    bills.add(bill);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching bills for date: " + date, e);
        }
        return bills;
    }

    public List<BillItem> getBillItemsByBillId(int billId) {
        String sql = """
                SELECT id, bill_id, product_code, quantity, total_price, discount_amount
                FROM bill_item  -- CHANGED from 'bill_items' to 'bill_item'
                WHERE bill_id = ?
                ORDER BY id ASC
                """;
        List<BillItem> items = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, billId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String productCode = rs.getString("product_code");
                    Product product = productRepository.findByCode(productCode);

                    if (product == null) {
                        System.err.println("Warning: Product with code '" + productCode + "' not found for bill item ID " + rs.getInt("id") + ". Using placeholder.");
                        product = new Product(productCode, "[Product Not Found]", 0.0);
                    }

                    items.add(new BillItem(
                            rs.getInt("id"),
                            rs.getInt("bill_id"),
                            product,
                            rs.getInt("quantity"),
                            rs.getDouble("total_price"),
                            rs.getDouble("discount_amount")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching bill items for bill ID: " + billId, e);
        }
        return items;
    }
}