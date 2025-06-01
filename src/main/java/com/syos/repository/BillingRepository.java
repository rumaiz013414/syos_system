package com.syos.repository;

import com.syos.db.DatabaseManager;
import com.syos.model.Bill;
import com.syos.model.BillItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class BillingRepository {

    public void save(Bill bill) {
        String insertBill = """
            INSERT INTO bill
              (serial_number, bill_date, total_amount, cash_tendered, change_returned, transaction_type)
            VALUES (?, ?, ?, ?, ?, ?)
            RETURNING id
            """;

        String insertItem = """
            INSERT INTO bill_item (bill_id, product_code, quantity, total_price)
            VALUES (?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            conn.setAutoCommit(false);

            // 1) Insert into 'bill' and get generated id
            int generatedBillId;
            try (PreparedStatement psBill = conn.prepareStatement(insertBill)) {
                psBill.setInt(1, bill.getSerialNumber());
                psBill.setTimestamp(2, new Timestamp(bill.getBillDate().getTime()));
                psBill.setDouble(3, bill.getTotalAmount());
                psBill.setDouble(4, bill.getCashTendered());
                psBill.setDouble(5, bill.getChangeReturned());
                psBill.setString(6, bill.getTransactionType());

                ResultSet rs = psBill.executeQuery();
                if (!rs.next()) {
                    throw new RuntimeException("Failed to retrieve generated bill ID.");
                }
                generatedBillId = rs.getInt(1);

                // Set the in‐memory Bill.id so the object is up-to-date
                bill.setId(generatedBillId);
            }

            // Insert each BillItem linked by bill_id
            try (PreparedStatement psItem = conn.prepareStatement(insertItem)) {
                for (BillItem item : bill.getItems()) {
                    psItem.setInt(1, generatedBillId);
                    psItem.setString(2, item.getProduct().getCode());
                    psItem.setInt(3, item.getQuantity());
                    psItem.setDouble(4, item.getTotalPrice());
                    psItem.addBatch();
                }
                psItem.executeBatch();
            }

            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Error saving bill & items", e);
        }
    }

    public int nextSerial() {
        String sql = """
            SELECT COALESCE(MAX(serial_number), 0) + 1
              FROM bill
             WHERE DATE(bill_date) = CURRENT_DATE
            """;
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error generating daily serial", e);
        }
        return 1;
    }
}
