package com.syos.dto;

import com.syos.enums.UserType;

public class CustomerRegisterRequestDTO {
	private final String firstName;
	private final String lastName;
	private final String email;
	private final String password;
	private final UserType role;

	public CustomerRegisterRequestDTO(String firstName, String lastName, String email, String password,
			UserType customer) {
		super();
		this.firstName = firstName;
		this.lastName = lastName;
		this.email = email;
		this.password = password;
		this.role = customer;
	}

	public String getFirstName() {
		return firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public String getEmail() {
		return email;
	}

	public String getPassword() {
		return password;
	}

	public UserType getUserType() {
		return role;
	}

}
