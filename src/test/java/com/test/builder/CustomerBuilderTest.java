package com.test.builder; // Adjust package to match your test structure

import com.syos.model.Customer; // Import your Customer class
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CustomerBuilderTest {

	@Test
	@DisplayName("Should successfully build a Customer object when all fields are provided")
	void build_allFieldsProvided_success() {
		// Arrange
		String expectedFirstName = "John";
		String expectedLastName = "Doe";
		String expectedEmail = "john.doe@example.com";
		String expectedPassword = "securePassword123";

		// Act
		Customer customer = new Customer.CustomerBuilder().firstName(expectedFirstName).lastName(expectedLastName)
				.email(expectedEmail).password(expectedPassword).build();

		// Assert
		assertNotNull(customer, "Customer object should not be null");
		assertEquals(expectedFirstName, customer.getFirstName(), "First name should match");
		assertEquals(expectedLastName, customer.getLastName(), "Last name should match");
		assertEquals(expectedEmail, customer.getEmail(), "Email should match");
		// Note: Password would ideally be hashed in a real Customer class.
		// For this test, we assume direct storage based on the builder.
		assertEquals(expectedPassword, customer.getPassword(), "Password should match");
	}

	@Test
	@DisplayName("Should throw IllegalStateException if first name is not provided")
	void build_missingFirstName_throwsException() {
		// Arrange
		Customer.CustomerBuilder builder = new Customer.CustomerBuilder().lastName("Doe").email("john.doe@example.com")
				.password("securePassword123");

		// Act & Assert
		IllegalStateException exception = assertThrows(IllegalStateException.class, builder::build,
				"Should throw IllegalStateException when first name is missing");
		assertEquals("All fields must be set", exception.getMessage(),
				"Exception message should indicate missing fields");
	}

	@Test
	@DisplayName("Should throw IllegalStateException if last name is not provided")
	void build_missingLastName_throwsException() {
		// Arrange
		Customer.CustomerBuilder builder = new Customer.CustomerBuilder().firstName("John")
				.email("john.doe@example.com").password("securePassword123");

		// Act & Assert
		IllegalStateException exception = assertThrows(IllegalStateException.class, builder::build,
				"Should throw IllegalStateException when last name is missing");
		assertEquals("All fields must be set", exception.getMessage(),
				"Exception message should indicate missing fields");
	}

	@Test
	@DisplayName("Should throw IllegalStateException if email is not provided")
	void build_missingEmail_throwsException() {
		// Arrange
		Customer.CustomerBuilder builder = new Customer.CustomerBuilder().firstName("John").lastName("Doe")
				.password("securePassword123");

		// Act & Assert
		IllegalStateException exception = assertThrows(IllegalStateException.class, builder::build,
				"Should throw IllegalStateException when email is missing");
		assertEquals("All fields must be set", exception.getMessage(),
				"Exception message should indicate missing fields");
	}

	@Test
	@DisplayName("Should throw IllegalStateException if password is not provided")
	void build_missingPassword_throwsException() {
		// Arrange
		Customer.CustomerBuilder builder = new Customer.CustomerBuilder().firstName("John").lastName("Doe")
				.email("john.doe@example.com");

		// Act & Assert
		IllegalStateException exception = assertThrows(IllegalStateException.class, builder::build,
				"Should throw IllegalStateException when password is missing");
		assertEquals("All fields must be set", exception.getMessage(),
				"Exception message should indicate missing fields");
	}

	@Test
	@DisplayName("Should throw IllegalStateException if no fields are provided")
	void build_noFieldsProvided_throwsException() {
		// Arrange
		Customer.CustomerBuilder builder = new Customer.CustomerBuilder();

		// Act & Assert
		IllegalStateException exception = assertThrows(IllegalStateException.class, builder::build,
				"Should throw IllegalStateException when no fields are provided");
		assertEquals("All fields must be set", exception.getMessage(),
				"Exception message should indicate missing fields");
	}
}