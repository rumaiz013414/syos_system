package com.syos.repository;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.syos.db.DatabaseManager;
import com.syos.model.StockBatch;

public class StockBatchRepository {

	// fetch all batches for a product that still have quantity.
	public List<StockBatch> findByProduct(String code) {
		String sql = """
				    SELECT id, product_code, purchase_date, expiry_date, quantity_remaining
				    FROM stock_batches
				    WHERE product_code = ? AND quantity_remaining > 0
				""";
		List<StockBatch> out = new ArrayList<>();
		try (Connection conn = DatabaseManager.getInstance().getConnection();
				PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setString(1, code);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				out.add(new StockBatch(rs.getInt("id"), rs.getString("product_code"),
						rs.getDate("purchase_date").toLocalDate(), rs.getDate("expiry_date").toLocalDate(),
						rs.getInt("quantity_remaining")));
			}
		} catch (Exception e) {
			throw new RuntimeException("Error loading stock batches", e);
		}
		return out;
	}

	// update the remaining quantity on a batch after moving to shelf.
	public void updateQuantity(int batchId, int newQty) {
		String sql = "UPDATE stock_batches SET quantity_remaining = ? WHERE id = ?";
		try (Connection conn = DatabaseManager.getInstance().getConnection();
				PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setInt(1, newQty);
			ps.setInt(2, batchId);
			ps.executeUpdate();
		} catch (Exception e) {
			throw new RuntimeException("Error updating batch qty", e);
		}
	}

	public void createBatch(String productCode, LocalDate purchaseDate, LocalDate expiryDate, int quantity) {
		String sql = """
				INSERT INTO stock_batches
				(product_code, purchase_date, expiry_date,
				quantity_received, quantity_remaining)
				VALUES (?,?,?,?,?)
				""";
		try (Connection conn = DatabaseManager.getInstance().getConnection();
				PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setString(1, productCode);
			ps.setDate(2, Date.valueOf(purchaseDate));
			ps.setDate(3, Date.valueOf(expiryDate));
			ps.setInt(4, quantity);
			ps.setInt(5, quantity);
			ps.executeUpdate();
		} catch (Exception e) {
			throw new RuntimeException("Error inserting new batch", e);
		}
	}

	public List<String> getAllProductCodesWithBatches() {
		String sql = "SELECT DISTINCT product_code FROM stock_batches";
		List<String> productCodes = new ArrayList<>();
		try (Connection conn = DatabaseManager.getInstance().getConnection();
				PreparedStatement ps = conn.prepareStatement(sql);
				ResultSet rs = ps.executeQuery()) {

			while (rs.next()) {
				productCodes.add(rs.getString("product_code"));
			}
		} catch (SQLException e) {
			throw new RuntimeException("Error getting all product codes with batches", e);
		}
		return productCodes;
	}

	// fetch batches that are close to expiry for a specific product
	public List<StockBatch> findExpiringBatches(String productCode, int daysThreshold) {
		String sql = """
				SELECT id, product_code, purchase_date, expiry_date, quantity_remaining
				FROM stock_batches
				WHERE product_code = ? AND quantity_remaining > 0 AND expiry_date <= ?
				ORDER BY expiry_date ASC
				""";
		List<StockBatch> out = new ArrayList<>();
		try (Connection conn = DatabaseManager.getInstance().getConnection();
				PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setString(1, productCode);
			ps.setDate(2, Date.valueOf(LocalDate.now().plusDays(daysThreshold)));
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				out.add(new StockBatch(rs.getInt("id"), rs.getString("product_code"),
						rs.getDate("purchase_date").toLocalDate(), rs.getDate("expiry_date").toLocalDate(),
						rs.getInt("quantity_remaining")));
			}
		} catch (SQLException e) {
			throw new RuntimeException("Error loading expiring stock batches for product " + productCode, e);
		}
		return out;
	}

	// fetch aLL batches that are close to expire
	public List<StockBatch> findAllExpiringBatches(int daysThreshold) {
		String sql = """
				SELECT id, product_code, purchase_date, expiry_date, quantity_remaining
				FROM stock_batches
				WHERE quantity_remaining > 0 AND expiry_date <= ?
				ORDER BY expiry_date ASC, product_code ASC
				""";
		List<StockBatch> out = new ArrayList<>();
		try (Connection conn = DatabaseManager.getInstance().getConnection();
				PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setDate(1, Date.valueOf(LocalDate.now().plusDays(daysThreshold)));
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				out.add(new StockBatch(rs.getInt("id"), rs.getString("product_code"),
						rs.getDate("purchase_date").toLocalDate(), rs.getDate("expiry_date").toLocalDate(),
						rs.getInt("quantity_remaining")));
			}
		} catch (SQLException e) {
			throw new RuntimeException("Error loading all expiring stock batches", e);
		}
		return out;
	}

	public StockBatch findById(int batchId) {
		String sql = """
				SELECT id, product_code, purchase_date, expiry_date, quantity_remaining
				FROM stock_batches
				WHERE id = ?
				""";
		try (Connection conn = DatabaseManager.getInstance().getConnection();
				PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setInt(1, batchId);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				return new StockBatch(rs.getInt("id"), rs.getString("product_code"),
						rs.getDate("purchase_date").toLocalDate(), rs.getDate("expiry_date").toLocalDate(),
						rs.getInt("quantity_remaining"));
			}
		} catch (SQLException e) {
			throw new RuntimeException("Error finding batch by ID", e);
		}
		return null; // Return null if not found
	}
}