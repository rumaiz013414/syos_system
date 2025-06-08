package com.syos.service;

import com.syos.dto.CustomerRegisterRequestDTO;
import com.syos.model.Customer;
import com.syos.repository.CustomerRepository;
import org.mindrot.jbcrypt.BCrypt;

public class CustomerRegistrationService {

	private final CustomerRepository customerRepository;

	public CustomerRegistrationService() {
		this.customerRepository = new CustomerRepository();
	}

	public Customer register(CustomerRegisterRequestDTO request) throws Exception {
		if (request.getFirstName() == null || request.getFirstName().isEmpty()) {
			throw new IllegalArgumentException("First name cannot be empty.");
		}
		if (request.getLastName() == null || request.getLastName().isEmpty()) {
			throw new IllegalArgumentException("Last name cannot be empty.");
		}
		if (request.getEmail() == null || request.getEmail().isEmpty()) {
			throw new IllegalArgumentException("Email cannot be empty.");
		}
		if (request.getPassword() == null || request.getPassword().isEmpty()) {
			throw new IllegalArgumentException("Password cannot be empty.");
		}

		if (customerRepository.existsByEmail(request.getEmail())) {
			throw new Exception("Email '" + request.getEmail() + "' is already registered.");
		}
		String hashedPassword = BCrypt.hashpw(request.getPassword(), BCrypt.gensalt());
		Customer newCustomer = new Customer(request.getFirstName(), request.getLastName(),
				request.getEmail().toLowerCase(), hashedPassword, request.getUserType());

		customerRepository.save(newCustomer);
		return newCustomer;
	}
}