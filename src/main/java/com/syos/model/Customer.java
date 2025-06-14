package com.syos.model;

import com.syos.enums.UserType;

public class Customer extends User {
	private final String firstName;
	private final String lastName;
	private final UserType role;

	private Customer(CustomerBuilder b) {
		super(b.email, b.password);
		this.firstName = b.firstName;
		this.lastName = b.lastName;
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

		public CustomerBuilder firstName(String fn) {
			this.firstName = fn;
			return this;
		}

		public CustomerBuilder lastName(String ln) {
			this.lastName = ln;
			return this;
		}

		public CustomerBuilder email(String e) {
			this.email = e;
			return this;
		}

		public CustomerBuilder password(String pw) {
			this.password = pw;
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