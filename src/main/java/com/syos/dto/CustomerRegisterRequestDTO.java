package com.syos.dto;

public class CustomerRegisterRequestDTO {
	private final String firstName;
	private final String lastName;
	private final String email;
	private final String password;
	private final String userType;

	public CustomerRegisterRequestDTO(String firstName, String lastName, String email, String password,
			String userType) {
		super();
		this.firstName = firstName;
		this.lastName = lastName;
		this.email = email;
		this.password = password;
		this.userType = userType;
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

	public String getUserType() {
		return userType;
	}

}
