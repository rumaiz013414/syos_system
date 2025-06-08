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
		try (Connection connection = DatabaseManager.getInstance().getConnection();
				PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

			preparedStatement.setString(1, customer.getEmail());
			preparedStatement.setString(2, customer.getPassword());
			preparedStatement.setString(3, customer.getFirstName());
			preparedStatement.setString(4, customer.getLastName());
			preparedStatement.setString(5, customer.getRole().name());
			preparedStatement.executeUpdate();
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
		try (Connection connection = DatabaseManager.getInstance().getConnection();
				PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

			preparedStatement.setString(1, email.toLowerCase());
			ResultSet resultSet = preparedStatement.executeQuery();
			if (resultSet.next()) {
				String fetchedEmail = resultSet.getString("email");
				String hashedPassword = resultSet.getString("password");
				String firstName = resultSet.getString("first_name");
				String lastName = resultSet.getString("last_name");
				String userTypeString = resultSet.getString("user_type");

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
		try (Connection connection = DatabaseManager.getInstance().getConnection();
				PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
			preparedStatement.setString(1, email.toLowerCase());
			ResultSet resultSet = preparedStatement.executeQuery();
			return resultSet.next();
		} catch (SQLException e) {
			throw new RuntimeException("Error checking customer existence", e);
		}
	}

	public void clear() {
		// TODO Auto-generated method stub

	}
}