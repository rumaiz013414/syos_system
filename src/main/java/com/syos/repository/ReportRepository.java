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
		try (Connection connection = DatabaseManager.getInstance().getConnection();
				PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

			preparedStatement.setDate(1, Date.valueOf(date));
			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				if (resultSet.next()) {
					return resultSet.getDouble(1);
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
		try (Connection connection = DatabaseManager.getInstance().getConnection();
				PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

			preparedStatement.setDate(1, Date.valueOf(date));
			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				while (resultSet.next()) {
					Bill bill = new Bill(resultSet.getInt("id"), resultSet.getInt("serial_number"),
							resultSet.getTimestamp("bill_date"), resultSet.getDouble("total_amount"),
							resultSet.getDouble("cash_tendered"), resultSet.getDouble("change_returned"),
							resultSet.getString("transaction_type"));
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
		try (Connection connection = DatabaseManager.getInstance().getConnection();
				PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

			preparedStatement.setInt(1, billId);
			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				while (resultSet.next()) {
					String productCode = resultSet.getString("product_code");
					Product product = productRepository.findByCode(productCode);

					if (product == null) {
						System.err.println("Warning: Product with code '" + productCode
								+ "' not found for bill item ID " + resultSet.getInt("id") + ". Using placeholder.");
						product = new Product(productCode, "[Product Not Found]", 0.0);
					}

					items.add(new BillItem(resultSet.getInt("id"), resultSet.getInt("bill_id"), product,
							resultSet.getInt("quantity"), resultSet.getDouble("total_price"),
							resultSet.getDouble("discount_amount")));
				}
			}
		} catch (SQLException e) {
			throw new RuntimeException("Error fetching bill items for bill ID: " + billId, e);
		}
		return items;
	}
}