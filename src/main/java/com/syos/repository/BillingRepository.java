package com.syos.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import com.syos.db.DatabaseManager;
import com.syos.model.Bill;
import com.syos.model.BillItem;

public class BillingRepository {
	public int nextSerial() {
        String sql = "SELECT COALESCE(MAX(serial_number), 0) + 1 FROM bill";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             var stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) {
            throw new RuntimeException("Error generating serial", e);
        }
        return 1;
    }

    public void save(Bill bill) {
        String insertBill = """
            INSERT INTO bill (serial_number,bill_date,total_amount,cash_tendered,change_returned)
            VALUES (?,?,?,?,?)
            """;
        String insertItem = """
            INSERT INTO bill_item (serial_number,product_code,quantity,total_price)
            VALUES (?,?,?,?)
            """;

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pb = conn.prepareStatement(insertBill);
                 PreparedStatement pi = conn.prepareStatement(insertItem)) {

                // bill
                pb.setInt(1, bill.getSerialNumber());
                pb.setTimestamp(2, new Timestamp(bill.getBillDate().getTime()));
                pb.setDouble(3, bill.getTotalAmount());
                pb.setDouble(4, bill.getCashTendered());
                pb.setDouble(5, bill.getChangeReturned());
                pb.executeUpdate();

                // items
                for (BillItem bi : bill.getItems()) {
                    pi.setInt(1, bill.getSerialNumber());
                    pi.setString(2, bi.getProduct().getCode());
                    pi.setInt(3, bi.getQuantity());
                    pi.setDouble(4, bi.getTotalPrice());
                    pi.addBatch();
                }
                pi.executeBatch();
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to save bill", e);
        }
    }

}
