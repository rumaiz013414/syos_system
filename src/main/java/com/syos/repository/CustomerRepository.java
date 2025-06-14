package com.syos.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.syos.db.DatabaseManager;
import com.syos.model.Customer;
import com.syos.enums.UserType;

public class CustomerRepository {
	public void save(Customer customer) {
		String sql = """
				INSERT INTO users(email,password,first_name,last_name,user_type)
				VALUES (?,?,?,?,?)
				""";
		try (Connection conn = DatabaseManager.getInstance().getConnection();
				PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setString(1, customer.getEmail());
			ps.setString(2, customer.getPassword());
			ps.setString(3, customer.getFirstName());
			ps.setString(4, customer.getLastName());
			ps.setString(5, customer.getRole().name());
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException("Error saving customer", e);
		}
	}

	public Customer findByEmail(String email) {
		String sql = """
				SELECT email,password,first_name,last_name,user_type
				  FROM users
				 WHERE email = ?
				""";
		try (Connection conn = DatabaseManager.getInstance().getConnection();
				PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setString(1, email.toLowerCase());
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				String fetchedEmail = rs.getString("email");
				String hashedPassword = rs.getString("password");
				String firstName = rs.getString("first_name");
				String lastName = rs.getString("last_name");
				String userTypeString = rs.getString("user_type");

				UserType userType = UserType.valueOf(userTypeString.toUpperCase());

				return new Customer(firstName, lastName, fetchedEmail, hashedPassword, userType);
			}
		} catch (SQLException e) {
			throw new RuntimeException("Error finding customer", e);
		}
		return null;
	}

	public boolean existsByEmail(String email) {
		String sql = "SELECT 1 FROM users WHERE email = ?";
		try (Connection conn = DatabaseManager.getInstance().getConnection();
				PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, email.toLowerCase());
			ResultSet rs = ps.executeQuery();
			return rs.next();
		} catch (SQLException e) {
			throw new RuntimeException("Error checking customer existence", e);
		}
	}
}