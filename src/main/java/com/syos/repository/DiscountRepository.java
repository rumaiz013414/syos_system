package com.syos.repository;

import com.syos.db.DatabaseManager;
import com.syos.model.Discount;
import com.syos.enums.DiscountType;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DiscountRepository {

	public List<Discount> findActiveDiscounts(String productCode, LocalDate date) {
		String sql = """
				SELECT d.id,
				       d.name,
				       d.type,
				       d.value,
				       d.start,
				       d."end"
				FROM discounts d
				JOIN product_discounts pd ON pd.discount_id = d.id
				WHERE pd.product_code = ?
				  AND d.start <= ?
				  AND d."end"   >= ?
				""";

		List<Discount> result = new ArrayList<>();
		try (Connection conn = DatabaseManager.getInstance().getConnection();
				PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, productCode);
			ps.setDate(2, Date.valueOf(date));
			ps.setDate(3, Date.valueOf(date));
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				DiscountType type = DiscountType.valueOf(rs.getString("type"));
				result.add(new Discount(rs.getInt("id"), rs.getString("name"), type, rs.getDouble("value"),
						rs.getDate("start").toLocalDate(), rs.getDate("end").toLocalDate()));
			}
		} catch (SQLException e) {
			throw new RuntimeException("Error loading active discounts for product " + productCode, e);
		}
		return result;
	}
}
