package com.syos.model;

import com.syos.enums.UserType;

public class Customer extends User {
	private final String firstName;
	private final String lastName;
	private final UserType role;

	private Customer(CustomerBuilder customerBuilder) {
		super(customerBuilder.email, customerBuilder.password);
		this.firstName = customerBuilder.firstName;
		this.lastName = customerBuilder.lastName;
		this.role = UserType.CUSTOMER;
	}

	public Customer(String email, String password, String firstName, String lastName) {
		super(email, password);
		this.firstName = firstName;
		this.lastName = lastName;
		this.role = UserType.CUSTOMER;
	}

	public Customer(String firstName, String lastName, String email, String password, UserType role) {
		super(email, password);
		this.firstName = firstName;
		this.lastName = lastName;
		this.role = role;
	}

	@Override
	public UserType getRole() {
		return role;
	}

	public String getFirstName() {
		return firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public String getFullName() {
		return firstName + " " + lastName;
	}

	public static class CustomerBuilder {
		private String firstName;
		private String lastName;
		private String email;
		private String password;

		public CustomerBuilder firstName(String firstName) {
			this.firstName = firstName;
			return this;
		}

		public CustomerBuilder lastName(String lastName) {
			this.lastName = lastName;
			return this;
		}

		public CustomerBuilder email(String email) {
			this.email = email;
			return this;
		}

		public CustomerBuilder password(String password) {
			this.password = password;
			return this;
		}

		public Customer build() {
			if (firstName == null || lastName == null || email == null || password == null) {
				throw new IllegalStateException("All fields must be set");
			}
			return new Customer(this);
		}
	}
}