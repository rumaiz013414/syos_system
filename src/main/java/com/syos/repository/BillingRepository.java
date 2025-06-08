package com.syos.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import com.syos.db.DatabaseManager;
import com.syos.model.Bill;
import com.syos.model.BillItem;

public class BillingRepository {

	public void save(Bill bill) {
		String insertBill = """
				INSERT INTO bill
				  (serial_number, bill_date, total_amount, cash_tendered, change_returned, transaction_type)
				VALUES (?, ?, ?, ?, ?, ?)
				RETURNING id
				""";

		String insertItem = """
				INSERT INTO bill_item (bill_id, product_code, quantity, total_price, discount_amount)
				VALUES (?, ?, ?, ?, ?)
				""";

		try (Connection connection = DatabaseManager.getInstance().getConnection()) {
			connection.setAutoCommit(false);

			int generatedBillId;
			try (PreparedStatement preparedStatement = connection.prepareStatement(insertBill)) {
				preparedStatement.setInt(1, bill.getSerialNumber());
				preparedStatement.setTimestamp(2, new Timestamp(bill.getBillDate().getTime()));
				preparedStatement.setDouble(3, bill.getTotalAmount());
				preparedStatement.setDouble(4, bill.getCashTendered());
				preparedStatement.setDouble(5, bill.getChangeReturned());
				preparedStatement.setString(6, bill.getTransactionType());

				ResultSet rs = preparedStatement.executeQuery();
				if (!rs.next()) {
					throw new RuntimeException("Failed to retrieve generated bill ID.");
				}
				generatedBillId = rs.getInt(1);

				bill.setId(generatedBillId);
			}

			try (PreparedStatement preparedStatement = connection.prepareStatement(insertItem)) {
				for (BillItem item : bill.getItems()) {
					preparedStatement.setInt(1, generatedBillId);
					preparedStatement.setString(2, item.getProduct().getCode());
					preparedStatement.setInt(3, item.getQuantity());
					preparedStatement.setDouble(4, item.getTotalPrice());
					preparedStatement.setDouble(5, item.getDiscountAmount());
					preparedStatement.addBatch();
				}
				preparedStatement.executeBatch();
			}

			connection.commit();
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
		try (Connection connection = DatabaseManager.getInstance().getConnection();
				PreparedStatement preparedStatement = connection.prepareStatement(sql);
				ResultSet result = preparedStatement.executeQuery()) {
			if (result.next()) {
				return result.getInt(1);
			}
		} catch (SQLException e) {
			throw new RuntimeException("Error generating daily serial", e);
		}
		return 1;
	}
}
