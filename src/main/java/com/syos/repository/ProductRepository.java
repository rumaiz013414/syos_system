package com.syos.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.syos.db.DatabaseManager;
import com.syos.model.Product;

public class ProductRepository {

	// Find one product by its code
	public Product findByCode(String code) {
		String sql = "SELECT code, name, price FROM product WHERE code = ?";
		try (Connection conn = DatabaseManager.getInstance().getConnection();
				PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setString(1, code);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				return new Product(rs.getString("code"), rs.getString("name"), rs.getDouble("price"));
			}
		} catch (Exception e) {
			throw new RuntimeException("Error loading product by code", e);
		}
		return null;
	}

	// Load all products from the product table
	public List<Product> findAll() {
		String sql = "SELECT code, name, price FROM product";
		List<Product> products = new ArrayList<>();
		try (Connection conn = DatabaseManager.getInstance().getConnection();
				PreparedStatement ps = conn.prepareStatement(sql)) {

			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				products.add(new Product(rs.getString("code"), rs.getString("name"), rs.getDouble("price")));
			}
		} catch (Exception e) {
			throw new RuntimeException("Error loading all products", e);
		}
		return products;
	}

	public void add(Product product) {
		if (findByCode(product.getCode()) != null) {
			throw new RuntimeException("Product code already exists: " + product.getCode());
		}

		String sql = "INSERT INTO product(code, name, price) VALUES (?, ?, ?)";
		try (Connection conn = DatabaseManager.getInstance().getConnection();
				PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, product.getCode());
			ps.setString(2, product.getName());
			ps.setDouble(3, product.getPrice());
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException("Error adding new product", e);
		}
	}

	public void clear() {
		// TODO Auto-generated method stub
		
	}
}
