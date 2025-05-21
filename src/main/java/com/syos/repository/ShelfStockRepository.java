package com.syos.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.syos.db.DatabaseManager;

public class ShelfStockRepository {

    // get current shelf quantity returns 0 if none.
    public int getQuantity(String productCode) {
        String sql = "SELECT quantity_on_shelf FROM shelf_stock WHERE product_code = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, productCode);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return 0;
    }

    // increase or insert shelf quantity. 
    public void upsertQuantity(String productCode, int qty) {
        String sql = """
            INSERT INTO shelf_stock(product_code,quantity_on_shelf)
            VALUES(?,?)
            ON CONFLICT(product_code) DO UPDATE
              SET quantity_on_shelf = shelf_stock.quantity_on_shelf + EXCLUDED.quantity_on_shelf
            """;
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, productCode);
            ps.setInt(2, qty);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // deduct shelf stock on purchase. 
    public void deductQuantity(String productCode, int qty) {
        String sql = """
            UPDATE shelf_stock
            SET quantity_on_shelf = quantity_on_shelf - ?
            WHERE product_code = ?
            """;
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, qty);
            ps.setString(2, productCode);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}