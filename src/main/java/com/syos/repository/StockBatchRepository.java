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

	public List<StockBatch> findByProduct(String code) {
		String sql = """
				SELECT id, product_code, purchase_date, expiry_date, quantity_remaining
				FROM stock_batches
				WHERE product_code = ? AND quantity_remaining > 0
				""";
		List<StockBatch> out = new ArrayList<>();
		try (Connection connection = DatabaseManager.getInstance().getConnection();
				PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

			preparedStatement.setString(1, code);
			ResultSet resultSet = preparedStatement.executeQuery();
			while (resultSet.next()) {
				out.add(new StockBatch(resultSet.getInt("id"), resultSet.getString("product_code"),
						resultSet.getDate("purchase_date").toLocalDate(),
						resultSet.getDate("expiry_date").toLocalDate(), resultSet.getInt("quantity_remaining")));
			}
		} catch (Exception e) {
			throw new RuntimeException("Error loading stock batches", e);
		}
		return out;
	}

	public List<StockBatch> findByProductAllBatches(String code) {
		String sql = """
				SELECT id, product_code, purchase_date, expiry_date, quantity_remaining
				FROM stock_batches
				WHERE product_code = ?
				""";
		List<StockBatch> out = new ArrayList<>();
		try (Connection connection = DatabaseManager.getInstance().getConnection();
				PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

			preparedStatement.setString(1, code);
			ResultSet resultSet = preparedStatement.executeQuery();
			while (resultSet.next()) {
				out.add(new StockBatch(resultSet.getInt("id"), resultSet.getString("product_code"),
						resultSet.getDate("purchase_date").toLocalDate(),
						resultSet.getDate("expiry_date").toLocalDate(), resultSet.getInt("quantity_remaining")));
			}
		} catch (Exception e) {
			throw new RuntimeException("Error loading all stock batches for product", e);
		}
		return out;
	}

	public void updateQuantity(int batchId, int newQty) {
		String sql = "UPDATE stock_batches SET quantity_remaining = ? WHERE id = ?";
		try (Connection connection = DatabaseManager.getInstance().getConnection();
				PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

			preparedStatement.setInt(1, newQty);
			preparedStatement.setInt(2, batchId);
			preparedStatement.executeUpdate();
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
		try (Connection connection = DatabaseManager.getInstance().getConnection();
				PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

			preparedStatement.setString(1, productCode);
			preparedStatement.setDate(2, Date.valueOf(purchaseDate));
			preparedStatement.setDate(3, Date.valueOf(expiryDate));
			preparedStatement.setInt(4, quantity);
			preparedStatement.setInt(5, quantity);
			preparedStatement.executeUpdate();
		} catch (Exception e) {
			throw new RuntimeException("Error inserting new batch", e);
		}
	}

	public List<String> getAllProductCodesWithBatches() {
		String sql = "SELECT DISTINCT product_code FROM stock_batches";
		List<String> productCodes = new ArrayList<>();
		try (Connection connection = DatabaseManager.getInstance().getConnection();
				PreparedStatement preparedStatement = connection.prepareStatement(sql);
				ResultSet resultSet = preparedStatement.executeQuery()) {

			while (resultSet.next()) {
				productCodes.add(resultSet.getString("product_code"));
			}
		} catch (SQLException e) {
			throw new RuntimeException("Error getting all product codes with batches", e);
		}
		return productCodes;
	}

	public List<StockBatch> findExpiringBatches(String productCode, int daysThreshold) {
		String sql = """
				SELECT id, product_code, purchase_date, expiry_date, quantity_remaining
				FROM stock_batches
				WHERE product_code = ? AND quantity_remaining > 0 AND expiry_date <= ?
				ORDER BY expiry_date ASC
				""";
		List<StockBatch> out = new ArrayList<>();
		try (Connection connection = DatabaseManager.getInstance().getConnection();
				PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

			preparedStatement.setString(1, productCode);
			preparedStatement.setDate(2, Date.valueOf(LocalDate.now().plusDays(daysThreshold)));
			ResultSet resultSet = preparedStatement.executeQuery();
			while (resultSet.next()) {
				out.add(new StockBatch(resultSet.getInt("id"), resultSet.getString("product_code"),
						resultSet.getDate("purchase_date").toLocalDate(),
						resultSet.getDate("expiry_date").toLocalDate(), resultSet.getInt("quantity_remaining")));
			}
		} catch (SQLException e) {
			throw new RuntimeException("Error loading expiring stock batches for product " + productCode, e);
		}
		return out;
	}

	public List<StockBatch> findAllExpiringBatches(int daysThreshold) {
		String sql = """
				SELECT id, product_code, purchase_date, expiry_date, quantity_remaining
				FROM stock_batches
				WHERE quantity_remaining > 0 AND expiry_date <= ?
				ORDER BY expiry_date ASC, product_code ASC
				""";
		List<StockBatch> out = new ArrayList<>();
		try (Connection connection = DatabaseManager.getInstance().getConnection();
				PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

			preparedStatement.setDate(1, Date.valueOf(LocalDate.now().plusDays(daysThreshold)));
			ResultSet resultSet = preparedStatement.executeQuery();
			while (resultSet.next()) {
				out.add(new StockBatch(resultSet.getInt("id"), resultSet.getString("product_code"),
						resultSet.getDate("purchase_date").toLocalDate(),
						resultSet.getDate("expiry_date").toLocalDate(), resultSet.getInt("quantity_remaining")));
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
		try (Connection connection = DatabaseManager.getInstance().getConnection();
				PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

			preparedStatement.setInt(1, batchId);
			ResultSet resultSet = preparedStatement.executeQuery();
			if (resultSet.next()) {
				return new StockBatch(resultSet.getInt("id"), resultSet.getString("product_code"),
						resultSet.getDate("purchase_date").toLocalDate(),
						resultSet.getDate("expiry_date").toLocalDate(), resultSet.getInt("quantity_remaining"));
			}
		} catch (SQLException e) {
			throw new RuntimeException("Error finding batch by ID", e);
		}
		return null;
	}

	public void setBatchQuantityToZero(int batchId) {
		String sql = "UPDATE stock_batches SET quantity_remaining = 0 WHERE id = ?";
		try (Connection connection = DatabaseManager.getInstance().getConnection();
				PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
			preparedStatement.setInt(1, batchId);
			preparedStatement.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException("Error setting batch quantity to zero for batch ID: " + batchId, e);
		}
	}
}