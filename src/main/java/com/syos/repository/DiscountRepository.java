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
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, discountId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                DiscountType type = DiscountType.valueOf(rs.getString("type"));
                return new Discount(
                    rs.getInt("id"),
                    rs.getString("name"),
                    type,
                    rs.getDouble("value"),
                    rs.getDate("start_date").toLocalDate(),
                    rs.getDate("end_date").toLocalDate()
                );
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

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, productCode);
            ps.setDate(2, Date.valueOf(date));
            ps.setDate(3, Date.valueOf(date));

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                DiscountType type = DiscountType.valueOf(rs.getString("type"));
                result.add(new Discount(
                    rs.getInt("id"),
                    rs.getString("name"),
                    type,
                    rs.getDouble("value"),
                    rs.getDate("start_date").toLocalDate(),
                    rs.getDate("end_date").toLocalDate()
                ));
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

        try (Connection conn = DatabaseManager.getInstance().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, discountName);
            ps.setString(2, discountType.name());
            ps.setDouble(3, discountValue);
            ps.setDate(4, Date.valueOf(startDate));
            ps.setDate(5, Date.valueOf(endDate));

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
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

        try (Connection conn = DatabaseManager.getInstance().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, productCode);
            ps.setInt(2, discountId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error linking product " + productCode + " to discount " + discountId, e);
        }
    }
}