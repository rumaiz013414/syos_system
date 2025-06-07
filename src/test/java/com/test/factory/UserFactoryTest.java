package com.test.factory; // Adjust package to match your test structure

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

@ExtendWith(MockitoExtension.class) // Enable Mockito annotations
class UserFactoryTest {

    @Mock
    private CustomerRegisterRequestDTO mockRequestDTO; // Mock the input DTO

    @Test
    @DisplayName("Should successfully create a Customer user when UserType is CUSTOMER")
    void createUser_customerType_returnsCustomer() {
        // Arrange
        String firstName = "Jane";
        String lastName = "Doe";
        String email = "jane.doe@example.com";
        String password = "hashedPassword";

        // Configure the mock DTO to return specific values for a CUSTOMER request
        when(mockRequestDTO.getUserType()).thenReturn(UserType.CUSTOMER);
        when(mockRequestDTO.getFirstName()).thenReturn(firstName);
        when(mockRequestDTO.getLastName()).thenReturn(lastName);
        when(mockRequestDTO.getEmail()).thenReturn(email);
        when(mockRequestDTO.getPassword()).thenReturn(password);

        // Act
        User user = UserFactory.createUser(mockRequestDTO);

        // Assert
        assertNotNull(user, "Created user should not be null");
        assertTrue(user instanceof Customer, "Created user should be an instance of Customer");

        Customer customer = (Customer) user;
        assertEquals(firstName, customer.getFirstName(), "Customer's first name should match DTO");
        assertEquals(lastName, customer.getLastName(), "Customer's last name should match DTO");
        assertEquals(email.toLowerCase(), customer.getEmail(), "Customer's email should match DTO (lowercase)");
        assertEquals(password, customer.getPassword(), "Customer's password should match DTO");
        assertEquals(UserType.CUSTOMER, customer.getRole(), "Customer's role should be CUSTOMER");
        assertNotNull(customer.getId(), "Customer should have a generated UUID");

        // Verify that DTO getters were called as expected by the factory and builder
        verify(mockRequestDTO, times(1)).getUserType();
        verify(mockRequestDTO, times(1)).getFirstName();
        verify(mockRequestDTO, times(1)).getLastName();
        verify(mockRequestDTO, times(1)).getEmail();
        verify(mockRequestDTO, times(1)).getPassword();
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for unsupported user types")
    void createUser_unsupportedType_throwsException() {
        // Arrange
        // IMPORTANT: For this test to compile and run, your UserType enum MUST contain a value
        // other than CUSTOMER, like 'ADMIN' or 'EMPLOYEE', which is not handled in the factory's switch.
        // If your UserType enum currently only has CUSTOMER, this test will not compile.
        // If that's the case, you can temporarily add another enum value or comment this test out.
        when(mockRequestDTO.getUserType()).thenReturn(UserType.ADMIN); // Example: assuming ADMIN exists

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> UserFactory.createUser(mockRequestDTO),
                "Should throw IllegalArgumentException for unsupported user type");
        assertEquals("Unsupported user type: " + UserType.ADMIN, exception.getMessage(), // Adjust message if type changes
                "Exception message should indicate unsupported type");

        // Verify that only getUserType was called
        verify(mockRequestDTO, times(1)).getUserType();
        verifyNoMoreInteractions(mockRequestDTO); // No other getters should be called if type is unsupported
    }

    @Test
    @DisplayName("Should throw IllegalStateException if CustomerBuilder fields are missing (e.g., firstName)")
    void createUser_customerType_missingBuilderFields_throwsIllegalStateException() {
        // Arrange
        // Simulate a scenario where the DTO provides null for a required field,
        // which the CustomerBuilder's build() method will then validate.
        when(mockRequestDTO.getUserType()).thenReturn(UserType.CUSTOMER);
        when(mockRequestDTO.getFirstName()).thenReturn(null); // This is the missing field causing the exception
        when(mockRequestDTO.getLastName()).thenReturn("Doe");
        when(mockRequestDTO.getEmail()).thenReturn("test@example.com");
        when(mockRequestDTO.getPassword()).thenReturn("pass");

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> UserFactory.createUser(mockRequestDTO),
                "Should rethrow IllegalStateException from CustomerBuilder for missing fields");
        assertEquals("All fields must be set", exception.getMessage(),
                "Exception message should indicate that all fields must be set");

        // Verify DTO getters were called up to the point of builder validation
        verify(mockRequestDTO, times(1)).getUserType();
        verify(mockRequestDTO, times(1)).getFirstName();
        verify(mockRequestDTO, times(1)).getLastName();
        verify(mockRequestDTO, times(1)).getEmail();
        verify(mockRequestDTO, times(1)).getPassword();
    }

    @Test
    @DisplayName("Should handle null CustomerRegisterRequestDTO gracefully (throws NullPointerException)")
    void createUser_nullRequestDTO_throwsNullPointerException() {
        // Act & Assert
        assertThrows(NullPointerException.class,
                () -> UserFactory.createUser(null),
                "Should throw NullPointerException when request DTO is null");

        // Verify no interactions with the mock DTO if it's null
        verifyNoInteractions(mockRequestDTO);
    }
}