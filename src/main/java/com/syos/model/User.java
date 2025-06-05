package com.syos.model;

import java.util.UUID;

import com.syos.enums.UserType;

public abstract class User {
	private final UUID id;
	private final String email;
	private final String password;

	protected User(String email, String password) {
		this.id = UUID.randomUUID();
		this.email = email.toLowerCase();
		this.password = password;
	}

	public UUID getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public String getPassword() {
		return password;
	}

	public abstract UserType getRole();
}
