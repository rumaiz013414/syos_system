package com.syos.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.syos.db.DatabaseManager;
import com.syos.model.Product;

public class ProductRepository {
	public Product findByCode(String code) {
		String sql = "SELECT code,name,price FROM product WHERE code = ?";
		try (Connection conn = DatabaseManager.getInstance().getConnection();
				PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setString(1, code);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				return new Product(rs.getString("code"), rs.getString("name"), rs.getDouble("price"));
			}
		} catch (Exception e) {
			throw new RuntimeException("Error loading product", e);
		}
		return null;
	}

}
