package com.syos.service;

import java.util.regex.Pattern;

import com.syos.dto.CustomerRegisterRequestDTO;
import com.syos.model.Customer;
import com.syos.repository.CustomerRepository;

public class CustomerRegistrationService extends RegistrationService<Customer> {

	private final CustomerRepository repo = new CustomerRepository();

	private static final Pattern EMAIL_REGEX = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

	@Override
	protected void validate(CustomerRegisterRequestDTO req) {
		if (!EMAIL_REGEX.matcher(req.getEmail()).matches()) {
			throw new IllegalArgumentException("Invalid email format");
		}
		if (req.getPassword().length() < 6) {
			throw new IllegalArgumentException("Password too short");
		}
	}

	@Override
	protected void checkUnique(CustomerRegisterRequestDTO req) {
		if (repo.existsByEmail(req.getEmail())) {
			throw new IllegalStateException("Email already registered");
		}
	}

	@Override
	protected Customer createUser(CustomerRegisterRequestDTO req) {
		return (Customer) com.syos.factory.UserFactory.createUser(req);
	}

	@Override
	protected void save(Customer user) {
		repo.save(user);
	}

}
