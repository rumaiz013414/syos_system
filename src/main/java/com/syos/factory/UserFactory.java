package com.syos.factory;

import com.syos.dto.CustomerRegisterRequestDTO;
import com.syos.enums.UserType;
import com.syos.model.Customer;
import com.syos.model.User;

public class UserFactory {
	public static User createUser(CustomerRegisterRequestDTO req) {
        UserType type = UserType.valueOf(req.getUserType());
        switch (type) {
            case CUSTOMER:
                return new Customer.CustomerBuilder()
                    .firstName(req.getFirstName())
                    .lastName(req.getLastName())
                    .email(req.getEmail())
                    .password(req.getPassword())
                    .build();
            default:
                throw new IllegalArgumentException("Unsupported user type: " + type);
        }
    }
}
