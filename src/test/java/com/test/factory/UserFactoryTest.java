package com.test.factory;

import com.syos.dto.CustomerRegisterRequestDTO;
import com.syos.enums.UserType;
import com.syos.factory.UserFactory;
import com.syos.model.Customer;
import com.syos.model.User;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserFactoryTest {

	@Mock
	private CustomerRegisterRequestDTO mockRequestDTO;

	@Test
	@DisplayName("Should successfully create a Customer user when UserType is CUSTOMER")
	void createUser_customerType_returnsCustomer() {
		String firstName = "Jane";
		String lastName = "Doe";
		String email = "jane.doe@example.com";
		String password = "hashedPassword";

		when(mockRequestDTO.getUserType()).thenReturn(UserType.CUSTOMER);
		when(mockRequestDTO.getFirstName()).thenReturn(firstName);
		when(mockRequestDTO.getLastName()).thenReturn(lastName);
		when(mockRequestDTO.getEmail()).thenReturn(email);
		when(mockRequestDTO.getPassword()).thenReturn(password);

		User user = UserFactory.createUser(mockRequestDTO);

		assertNotNull(user, "Created user should not be null");
		assertTrue(user instanceof Customer, "Created user should be an instance of Customer");

		Customer customer = (Customer) user;
		assertEquals(firstName, customer.getFirstName(), "Customer's first name should match DTO");
		assertEquals(lastName, customer.getLastName(), "Customer's last name should match DTO");
		assertEquals(email.toLowerCase(), customer.getEmail(), "Customer's email should match DTO (lowercase)");
		assertEquals(password, customer.getPassword(), "Customer's password should match DTO");
		assertEquals(UserType.CUSTOMER, customer.getRole(), "Customer's role should be CUSTOMER");
		assertNotNull(customer.getId(), "Customer should have a generated UUID");

		verify(mockRequestDTO, times(1)).getUserType();
		verify(mockRequestDTO, times(1)).getFirstName();
		verify(mockRequestDTO, times(1)).getLastName();
		verify(mockRequestDTO, times(1)).getEmail();
		verify(mockRequestDTO, times(1)).getPassword();
	}

	@Test
	@DisplayName("Should throw IllegalArgumentException for unsupported user types")
	void createUser_unsupportedType_throwsException() {
		when(mockRequestDTO.getUserType()).thenReturn(UserType.ADMIN);

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> UserFactory.createUser(mockRequestDTO),
				"Should throw IllegalArgumentException for unsupported user type");
		assertEquals("Unsupported user type: " + UserType.ADMIN, exception.getMessage(),
				"Exception message should indicate unsupported type");

		verify(mockRequestDTO, times(1)).getUserType();
		verifyNoMoreInteractions(mockRequestDTO);
	}

	@Test
	@DisplayName("Should throw IllegalStateException if CustomerBuilder fields are missing (e.g., firstName)")
	void createUser_customerType_missingBuilderFields_throwsIllegalStateException() {
		when(mockRequestDTO.getUserType()).thenReturn(UserType.CUSTOMER);
		when(mockRequestDTO.getFirstName()).thenReturn(null);
		when(mockRequestDTO.getLastName()).thenReturn("Doe");
		when(mockRequestDTO.getEmail()).thenReturn("test@example.com");
		when(mockRequestDTO.getPassword()).thenReturn("pass");

		IllegalStateException exception = assertThrows(IllegalStateException.class,
				() -> UserFactory.createUser(mockRequestDTO),
				"Should rethrow IllegalStateException from CustomerBuilder for missing fields");
		assertEquals("All fields must be set", exception.getMessage(),
				"Exception message should indicate that all fields must be set");

		verify(mockRequestDTO, times(1)).getUserType();
		verify(mockRequestDTO, times(1)).getFirstName();
		verify(mockRequestDTO, times(1)).getLastName();
		verify(mockRequestDTO, times(1)).getEmail();
		verify(mockRequestDTO, times(1)).getPassword();
	}

	@Test
	@DisplayName("Should handle null CustomerRegisterRequestDTO gracefully (throws NullPointerException)")
	void createUser_nullRequestDTO_throwsNullPointerException() {
		assertThrows(NullPointerException.class, () -> UserFactory.createUser(null),
				"Should throw NullPointerException when request DTO is null");

		verifyNoInteractions(mockRequestDTO);
	}
}