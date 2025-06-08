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
import com.syos.enums.DiscountType;
import com.syos.model.Discount;

public class DiscountRepository {

	public Discount findById(int discountId) {
		String sql = """
				    SELECT id, name, type, value, start_date, end_date
				    FROM discounts
				    WHERE id = ?
				""";
		try (Connection connection = DatabaseManager.getInstance().getConnection();
				PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

			preparedStatement.setInt(1, discountId);
			ResultSet resultSet = preparedStatement.executeQuery();

			if (resultSet.next()) {
				DiscountType type = DiscountType.valueOf(resultSet.getString("type"));
				return new Discount(resultSet.getInt("id"), resultSet.getString("name"), type,
						resultSet.getDouble("value"), resultSet.getDate("start_date").toLocalDate(),
						resultSet.getDate("end_date").toLocalDate());
			}
		} catch (SQLException e) {
			throw new RuntimeException("Error finding discount by ID: " + discountId, e);
		}
		return null;
	}

	public List<Discount> findActiveDiscounts(String productCode, LocalDate date) {
		String sql = """
				    SELECT d.id,
				           d.name,
				           d.type,
				           d.value,
				           d.start_date,
				           d.end_date
				    FROM discounts d
				    JOIN product_discounts pd ON pd.discount_id = d.id
				    WHERE pd.product_code = ?
				      AND d.start_date <= ?
				      AND d.end_date >= ?
				""";

		List<Discount> result = new ArrayList<>();

		try (Connection connection = DatabaseManager.getInstance().getConnection();
				PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

			preparedStatement.setString(1, productCode);
			preparedStatement.setDate(2, Date.valueOf(date));
			preparedStatement.setDate(3, Date.valueOf(date));

			ResultSet resultSet = preparedStatement.executeQuery();
			while (resultSet.next()) {
				DiscountType type = DiscountType.valueOf(resultSet.getString("type"));
				result.add(new Discount(resultSet.getInt("id"), resultSet.getString("name"), type,
						resultSet.getDouble("value"), resultSet.getDate("start_date").toLocalDate(),
						resultSet.getDate("end_date").toLocalDate()));
			}
		} catch (SQLException e) {
			throw new RuntimeException("Error loading active discounts for product " + productCode, e);
		}

		return result;
	}

	public int createDiscount(String discountName, DiscountType discountType, double discountValue, LocalDate startDate,
			LocalDate endDate) {
		String sql = """
				INSERT INTO discounts (name, type, value, start_date, end_date)
				VALUES (?, ?, ?, ?, ?)
				RETURNING id
				""";

		try (Connection connection = DatabaseManager.getInstance().getConnection();
				PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
			preparedStatement.setString(1, discountName);
			preparedStatement.setString(2, discountType.name());
			preparedStatement.setDouble(3, discountValue);
			preparedStatement.setDate(4, Date.valueOf(startDate));
			preparedStatement.setDate(5, Date.valueOf(endDate));

			ResultSet resultSet = preparedStatement.executeQuery();
			if (resultSet.next()) {
				return resultSet.getInt("id");
			} else {
				throw new RuntimeException("Failed to create discount (no ID returned).");
			}
		} catch (SQLException e) {
			throw new RuntimeException("Error inserting discount into database", e);
		}
	}

	public void linkProductToDiscount(String productCode, int discountId) {
		String sql = """
				INSERT INTO product_discounts (product_code, discount_id)
				VALUES (?, ?)
				""";

		try (Connection connection = DatabaseManager.getInstance().getConnection();
				PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
			preparedStatement.setString(1, productCode);
			preparedStatement.setInt(2, discountId);
			preparedStatement.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException("Error linking product " + productCode + " to discount " + discountId, e);
		}
	}

	public List<Discount> findAll() {
		String sql = """
				    SELECT id, name, type, value, start_date, end_date
				    FROM discounts
				    ORDER BY name ASC
				""";
		List<Discount> discounts = new ArrayList<>();
		try (Connection connection = DatabaseManager.getInstance().getConnection();
				PreparedStatement preparedStatement = connection.prepareStatement(sql);
				ResultSet resultSet = preparedStatement.executeQuery()) {

			while (resultSet.next()) {
				DiscountType type = DiscountType.valueOf(resultSet.getString("type"));
				discounts.add(new Discount(resultSet.getInt("id"), resultSet.getString("name"), type,
						resultSet.getDouble("value"), resultSet.getDate("start_date").toLocalDate(),
						resultSet.getDate("end_date").toLocalDate()));
			}
		} catch (SQLException e) {
			throw new RuntimeException("Error retrieving all discounts", e);
		}
		return discounts;
	}

	public List<Discount> findDiscountsByProductCode(String productCode, LocalDate asOfDate) {
		String sql = """
				    SELECT d.id, d.name, d.type, d.value, d.start_date, d.end_date
				    FROM discounts d
				    JOIN product_discounts pd ON d.id = pd.discount_id
				    WHERE pd.product_code = ?
				      AND d.start_date <= ?
				      AND d.end_date >= ?
				    ORDER BY d.name ASC
				""";
		List<Discount> discounts = new ArrayList<>();
		try (Connection connection = DatabaseManager.getInstance().getConnection();
				PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

			preparedStatement.setString(1, productCode);
			preparedStatement.setDate(2, Date.valueOf(asOfDate));
			preparedStatement.setDate(3, Date.valueOf(asOfDate));
			ResultSet resultSet = preparedStatement.executeQuery();

			while (resultSet.next()) {
				DiscountType type = DiscountType.valueOf(resultSet.getString("type"));
				discounts.add(new Discount(resultSet.getInt("id"), resultSet.getString("name"), type,
						resultSet.getDouble("value"), resultSet.getDate("start_date").toLocalDate(),
						resultSet.getDate("end_date").toLocalDate()));
			}
		} catch (SQLException e) {
			throw new RuntimeException("Error finding discounts for product " + productCode, e);
		}
		return discounts;
	}

	public boolean unassignDiscountFromProduct(String productCode, int discountId) {
		String sql = """
				DELETE FROM product_discounts
				WHERE product_code = ? AND discount_id = ?
				""";
		try (Connection connection = DatabaseManager.getInstance().getConnection();
				PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
			preparedStatement.setString(1, productCode);
			preparedStatement.setInt(2, discountId);
			int rowsAffected = preparedStatement.executeUpdate();
			return rowsAffected > 0;
		} catch (SQLException e) {
			throw new RuntimeException("Error unassigning discount " + discountId + " from product " + productCode, e);
		}
	}
}