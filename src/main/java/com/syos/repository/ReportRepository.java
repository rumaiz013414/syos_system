package com.syos.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import com.syos.db.DatabaseManager;

public class ReportRepository {

    public double getTotalRevenue(LocalDate date) {
        String sql = "SELECT SUM(total_amount) FROM bills WHERE DATE(bill_date) = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, date);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getDouble(1);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error fetching total revenue", e);
        }
        return 0.0;
    }

}
