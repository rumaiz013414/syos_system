package com.syos.factory;

import com.syos.dto.CustomerRegisterRequestDTO;
import com.syos.enums.UserType;
import com.syos.model.Customer;
import com.syos.model.User;

public class UserFactory {
	public static User createUser(CustomerRegisterRequestDTO customerRegisterRequestDTO) {
		UserType type = customerRegisterRequestDTO.getUserType();
		switch (type) {
		case CUSTOMER:
			return new Customer.CustomerBuilder().firstName(customerRegisterRequestDTO.getFirstName())
					.lastName(customerRegisterRequestDTO.getLastName()).email(customerRegisterRequestDTO.getEmail())
					.password(customerRegisterRequestDTO.getPassword()).build();
		default:
			throw new IllegalArgumentException("Unsupported user type: " + type);
		}
	}
}
