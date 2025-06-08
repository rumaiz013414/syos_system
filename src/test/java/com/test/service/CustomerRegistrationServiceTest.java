package com.test.service; // Adjust package as per your test file location

import com.syos.dto.CustomerRegisterRequestDTO;
import com.syos.enums.UserType;
import com.syos.model.Customer;
import com.syos.repository.CustomerRepository;
import com.syos.service.CustomerRegistrationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mindrot.jbcrypt.BCrypt;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CustomerRegistrationServiceTest {

    @Mock
    private CustomerRepository customerRepository; // Mock the repository dependency

    @InjectMocks
    private CustomerRegistrationService customerRegistrationService; // Inject mocks into the service under test

    @BeforeEach
    void setUp() {
        // Initialize mocks before each test method
        MockitoAnnotations.openMocks(this);
        // Note: CustomerRegistrationService currently instantiates CustomerRepository internally.
        // For proper mocking, it's better to pass CustomerRepository via constructor or setter.
        // If CustomerRegistrationService has:
        // public CustomerRegistrationService(CustomerRepository customerRepository) { this.customerRepository = customerRepository; }
        // then @InjectMocks will work as intended. If not, the mock won't be used unless you adjust the service.
        // Assuming you'll refactor CustomerRegistrationService for proper dependency injection.
    }

    // --- Test Cases for Successful Registration (4 cases) ---

    @Test
    @DisplayName("Test 1: Successful registration with standard details")
    void testRegister_Success_Standard() throws Exception {
        // Arrange
        CustomerRegisterRequestDTO request = new CustomerRegisterRequestDTO(
                "John", "Doe", "john.doe@example.com", "Password123", UserType.CUSTOMER);

        // Mock repository behavior: email does not exist, save is successful
        when(customerRepository.existsByEmail(request.getEmail())).thenReturn(false);
        doNothing().when(customerRepository).save(any(Customer.class));

        // Act
        Customer registeredCustomer = customerRegistrationService.register(request);

        // Assert
        assertNotNull(registeredCustomer, "Registered customer should not be null");
        assertEquals("John", registeredCustomer.getFirstName(), "First name should match DTO");
        // This assertion will only pass if the bug in CustomerRegistrationService is fixed
        assertEquals("Doe", registeredCustomer.getLastName(), "Last name should match DTO after fix");
        assertEquals("john.doe@example.com", registeredCustomer.getEmail(), "Email should be lowercased and match DTO");
        // Verify password hashing by checking the raw password against the stored hashed one
        assertTrue(BCrypt.checkpw("Password123", registeredCustomer.getPassword()), "Password should be correctly hashed");
        assertEquals(UserType.CUSTOMER, registeredCustomer.getRole(), "User role should be CUSTOMER");

        // Verify repository interactions
        verify(customerRepository, times(1)).existsByEmail(request.getEmail());
        verify(customerRepository, times(1)).save(any(Customer.class));
    }

    @Test
    @DisplayName("Test 2: Successful registration with mixed-case email (should be lowercased)")
    void testRegister_Success_MixedCaseEmail() throws Exception {
        // Arrange
        CustomerRegisterRequestDTO request = new CustomerRegisterRequestDTO(
                "Jane", "Smith", "Jane.Smith@Example.com", "SecurePass456", UserType.CUSTOMER);

        // Mock repository behavior: check for lowercase email, then save
        when(customerRepository.existsByEmail("jane.smith@example.com")).thenReturn(false);
        doNothing().when(customerRepository).save(any(Customer.class));

        // Act
        Customer registeredCustomer = customerRegistrationService.register(request);

        // Assert
        assertNotNull(registeredCustomer);
        assertEquals("jane.smith@example.com", registeredCustomer.getEmail(), "Email should be lowercased in the stored customer");
        assertTrue(BCrypt.checkpw("SecurePass456", registeredCustomer.getPassword()), "Password should be correctly hashed");

        // Verify repository interactions
        verify(customerRepository, times(1)).existsByEmail("jane.smith@example.com");
        verify(customerRepository, times(1)).save(any(Customer.class));
    }

    @Test
    @DisplayName("Test 3: Successful registration with minimal name")
    void testRegister_Success_MinimalName() throws Exception {
        // Arrange
        CustomerRegisterRequestDTO request = new CustomerRegisterRequestDTO(
                "A", "B", "a.b@test.com", "MinPass1!", UserType.CUSTOMER);

        when(customerRepository.existsByEmail(request.getEmail())).thenReturn(false);
        doNothing().when(customerRepository).save(any(Customer.class));

        // Act
        Customer registeredCustomer = customerRegistrationService.register(request);

        // Assert
        assertNotNull(registeredCustomer);
        assertEquals("A", registeredCustomer.getFirstName(), "First name should match");
        assertEquals("B", registeredCustomer.getLastName(), "Last name should match after fix");
        assertEquals("a.b@test.com", registeredCustomer.getEmail(), "Email should match");
        assertTrue(BCrypt.checkpw("MinPass1!", registeredCustomer.getPassword()), "Password should be hashed");

        // Verify repository interactions
        verify(customerRepository, times(1)).existsByEmail(request.getEmail());
        verify(customerRepository, times(1)).save(any(Customer.class));
    }

    @Test
    @DisplayName("Test 4: Verify BCrypt password hashing logic")
    void testRegister_Success_PasswordHashing() throws Exception {
        // Arrange
        String rawPassword = "MySuperSecretPassword!";
        CustomerRegisterRequestDTO request = new CustomerRegisterRequestDTO(
                "Hash", "Test", "hash@test.com", rawPassword, UserType.CUSTOMER);

        when(customerRepository.existsByEmail(request.getEmail())).thenReturn(false);
        
        // Use doAnswer to intercept the save call and assert on the saved customer's password
        doAnswer(invocation -> {
            Customer savedCustomer = invocation.getArgument(0);
            assertNotNull(savedCustomer.getPassword(), "Hashed password should not be null");
            // Check if the hashed password starts with typical BCrypt prefixes
            assertTrue(savedCustomer.getPassword().startsWith("$2a$") ||
                       savedCustomer.getPassword().startsWith("$2b$") ||
                       savedCustomer.getPassword().startsWith("$2y$"), "Hashed password should be a valid BCrypt hash");
            // Verify that the raw password matches the hashed one
            assertTrue(BCrypt.checkpw(rawPassword, savedCustomer.getPassword()), "Raw password should match hashed password");
            return null; // Return null as the save method is void
        }).when(customerRepository).save(any(Customer.class));

        // Act
        customerRegistrationService.register(request);

        // Assert
        verify(customerRepository, times(1)).save(any(Customer.class)); // Ensure save was called
    }

    // --- Test Cases for Invalid Email (5 cases) ---

    @ParameterizedTest
    @NullAndEmptySource // Tests for null and empty strings
    @DisplayName("Test 5 & 6: Registration fails with null or empty email")
    @ValueSource(strings = { " ", "\t", "\n" }) // Tests for blank (whitespace-only) emails
    void testRegister_Fail_EmptyOrBlankEmail(String email) {
        // Arrange
        CustomerRegisterRequestDTO request = new CustomerRegisterRequestDTO(
                "Test", "User", email, "Password123", UserType.CUSTOMER);

        // Act & Assert
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () ->
                customerRegistrationService.register(request),
                "Should throw IllegalArgumentException for empty/blank email");

        assertEquals("Email cannot be empty.", thrown.getMessage(), "Exception message should match");
        verifyNoInteractions(customerRepository); // No repository calls should happen if validation fails early
    }

    // NOTE: The following 3 tests (7, 8, 9) currently *succeed* because your
    // CustomerRegistrationService does NOT include email format validation.
    // They will pass because the service proceeds to try and save them.
    // If you add email format validation in your service, these tests should
    // be updated to expect an IllegalArgumentException.

    @Test
    @DisplayName("Test 7: Registration succeeds with invalid email format (no @) - current service limitation")
    void testRegister_Success_InvalidEmailFormat_NoAtSign() throws Exception {
        // Arrange
        CustomerRegisterRequestDTO request = new CustomerRegisterRequestDTO(
                "Test", "User", "invalid.email", "Password123", UserType.CUSTOMER);

        when(customerRepository.existsByEmail(anyString())).thenReturn(false);
        doNothing().when(customerRepository).save(any(Customer.class));

        // Act & Assert (expects no exception as per current service behavior)
        assertDoesNotThrow(() -> customerRegistrationService.register(request));
        verify(customerRepository, times(1)).existsByEmail(anyString());
        verify(customerRepository, times(1)).save(any(Customer.class)); // It proceeds to save
    }

    @Test
    @DisplayName("Test 8: Registration succeeds with invalid email format (no domain) - current service limitation")
    void testRegister_Success_InvalidEmailFormat_NoDomain() throws Exception {
        // Arrange
        CustomerRegisterRequestDTO request = new CustomerRegisterRequestDTO(
                "Test", "User", "user@.com", "Password123", UserType.CUSTOMER);

        when(customerRepository.existsByEmail(anyString())).thenReturn(false);
        doNothing().when(customerRepository).save(any(Customer.class));

        // Act & Assert
        assertDoesNotThrow(() -> customerRegistrationService.register(request));
        verify(customerRepository, times(1)).existsByEmail(anyString());
        verify(customerRepository, times(1)).save(any(Customer.class));
    }

    @Test
    @DisplayName("Test 9: Registration succeeds with invalid email format (no user part) - current service limitation")
    void testRegister_Success_InvalidEmailFormat_NoUserPart() throws Exception {
        // Arrange
        CustomerRegisterRequestDTO request = new CustomerRegisterRequestDTO(
                "Test", "User", "@domain.com", "Password123", UserType.CUSTOMER);

        when(customerRepository.existsByEmail(anyString())).thenReturn(false);
        doNothing().when(customerRepository).save(any(Customer.class));

        // Act & Assert
        assertDoesNotThrow(() -> customerRegistrationService.register(request));
        verify(customerRepository, times(1)).existsByEmail(anyString());
        verify(customerRepository, times(1)).save(any(Customer.class));
    }

    // --- Test Cases for Invalid Password (5 cases) ---

    @ParameterizedTest
    @NullAndEmptySource // Tests for null and empty strings
    @DisplayName("Test 10 & 11: Registration fails with null or empty password")
    @ValueSource(strings = { " ", "\t", "\n" }) // Tests for blank (whitespace-only) passwords
    void testRegister_Fail_EmptyOrBlankPassword(String password) {
        // Arrange
        CustomerRegisterRequestDTO request = new CustomerRegisterRequestDTO(
                "Test", "User", "test@example.com", password, UserType.CUSTOMER);

        // Act & Assert
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () ->
                customerRegistrationService.register(request),
                "Should throw IllegalArgumentException for empty/blank password");

        assertEquals("Password cannot be empty.", thrown.getMessage(), "Exception message should match");
        verifyNoInteractions(customerRepository); // No repository calls should happen
    }

    // NOTE: The following 3 tests (12, 13, 14) currently *succeed* because your
    // CustomerRegistrationService does NOT include password complexity/length validation.
    // They will pass because the service proceeds to try and save them.
    // If you add password complexity/length validation, these tests should
    // be updated to expect an IllegalArgumentException.

    @Test
    @DisplayName("Test 12: Registration succeeds with very short password (no complexity check)")
    void testRegister_Success_ShortPassword() throws Exception {
        // Arrange
        CustomerRegisterRequestDTO request = new CustomerRegisterRequestDTO(
                "Short", "Pass", "short@test.com", "123", UserType.CUSTOMER);

        when(customerRepository.existsByEmail(request.getEmail())).thenReturn(false);
        doNothing().when(customerRepository).save(any(Customer.class));

        // Act & Assert
        assertDoesNotThrow(() -> customerRegistrationService.register(request));
        verify(customerRepository, times(1)).existsByEmail(anyString());
        verify(customerRepository, times(1)).save(any(Customer.class));
    }

    @Test
    @DisplayName("Test 13: Registration succeeds with password containing only spaces (hashes '   ')")
    void testRegister_Success_PasswordWithSpaces() throws Exception {
        // Arrange
        CustomerRegisterRequestDTO request = new CustomerRegisterRequestDTO(
                "Space", "Pass", "space@test.com", "   ", UserType.CUSTOMER);

        when(customerRepository.existsByEmail(request.getEmail())).thenReturn(false);
        doNothing().when(customerRepository).save(any(Customer.class));

        // Act & Assert
        assertDoesNotThrow(() -> customerRegistrationService.register(request));
        verify(customerRepository, times(1)).existsByEmail(anyString());
        verify(customerRepository, times(1)).save(any(Customer.class));
    }

    @Test
    @DisplayName("Test 14: Registration succeeds with password containing special characters")
    void testRegister_Success_SpecialCharPassword() throws Exception {
        // Arrange
        CustomerRegisterRequestDTO request = new CustomerRegisterRequestDTO(
                "Special", "Char", "special@test.com", "!@#$%^&*()", UserType.CUSTOMER);

        when(customerRepository.existsByEmail(request.getEmail())).thenReturn(false);
        doNothing().when(customerRepository).save(any(Customer.class));

        // Act & Assert
        assertDoesNotThrow(() -> customerRegistrationService.register(request));
        verify(customerRepository, times(1)).existsByEmail(anyString());
        verify(customerRepository, times(1)).save(any(Customer.class));
    }

    // --- Test Cases for Email Already Registered (3 cases) ---

    @Test
    @DisplayName("Test 15: Registration fails when email is already registered")
    void testRegister_Fail_EmailAlreadyRegistered() {
        // Arrange
        CustomerRegisterRequestDTO request = new CustomerRegisterRequestDTO(
                "Existing", "User", "existing@example.com", "Password123", UserType.CUSTOMER);

        // Mock repository behavior: email already exists
        when(customerRepository.existsByEmail(request.getEmail())).thenReturn(true);

        // Act & Assert
        Exception thrown = assertThrows(Exception.class, () ->
                customerRegistrationService.register(request),
                "Should throw an exception if email is already registered");

        assertEquals("Email 'existing@example.com' is already registered.", thrown.getMessage(), "Exception message should match");
        verify(customerRepository, times(1)).existsByEmail(request.getEmail());
        verify(customerRepository, never()).save(any(Customer.class)); // Save should not be called
    }

    @Test
    @DisplayName("Test 16: Registration fails when email already registered (case-insensitive check)")
    void testRegister_Fail_EmailAlreadyRegistered_CaseInsensitive() {
        // Arrange
        CustomerRegisterRequestDTO request = new CustomerRegisterRequestDTO(
                "Existing", "User", "Existing@Example.com", "Password123", UserType.CUSTOMER);

        // Mock repository behavior: email exists for the lowercased version
        when(customerRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // Act & Assert
        Exception thrown = assertThrows(Exception.class, () ->
                customerRegistrationService.register(request),
                "Should throw an exception for case-insensitive duplicate email");

        assertEquals("Email 'Existing@Example.com' is already registered.", thrown.getMessage(), "Exception message should match");
        // Verify that the service checked for the lowercased email
        verify(customerRepository, times(1)).existsByEmail("existing@example.com");
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    @DisplayName("Test 17: Verify no save call if email exists")
    void testRegister_Fail_EmailAlreadyRegistered_NoSave() {
        // Arrange
        CustomerRegisterRequestDTO request = new CustomerRegisterRequestDTO(
                "Another", "User", "another@test.com", "pass", UserType.CUSTOMER);

        when(customerRepository.existsByEmail(request.getEmail())).thenReturn(true);

        // Act & Assert
        assertThrows(Exception.class, () -> customerRegistrationService.register(request));

        verify(customerRepository, never()).save(any(Customer.class)); // Ensure save was NOT called
    }

    // --- Test Cases for Repository Errors (2 cases) ---

    @Test
    @DisplayName("Test 18: Registration fails if existsByEmail throws an exception")
    void testRegister_Fail_RepositoryExistsByEmailThrows() {
        // Arrange
        CustomerRegisterRequestDTO request = new CustomerRegisterRequestDTO(
                "DB", "Error", "db_error@example.com", "DbPass123", UserType.CUSTOMER);

        // Simulate a database error when checking email existence
        when(customerRepository.existsByEmail(request.getEmail()))
                .thenThrow(new RuntimeException("Database connection failed"));

        // Act & Assert
        RuntimeException thrown = assertThrows(RuntimeException.class, () ->
                customerRegistrationService.register(request),
                "Should rethrow the underlying repository exception");

        assertEquals("Database connection failed", thrown.getMessage(), "Exception message should match");
        verify(customerRepository, times(1)).existsByEmail(request.getEmail());
        verify(customerRepository, never()).save(any(Customer.class)); // Save should not be called
    }

    @Test
    @DisplayName("Test 19: Registration fails if save throws an exception")
    void testRegister_Fail_RepositorySaveThrows() {
        // Arrange
        CustomerRegisterRequestDTO request = new CustomerRegisterRequestDTO(
                "Save", "Fail", "save_fail@example.com", "SavePass123", UserType.CUSTOMER);

        when(customerRepository.existsByEmail(request.getEmail())).thenReturn(false);
        // Simulate a database error when saving the customer
        doThrow(new RuntimeException("Failed to insert into DB")).when(customerRepository).save(any(Customer.class));

        // Act & Assert
        RuntimeException thrown = assertThrows(RuntimeException.class, () ->
                customerRegistrationService.register(request),
                "Should rethrow the underlying repository exception");

        assertEquals("Failed to insert into DB", thrown.getMessage(), "Exception message should match");
        verify(customerRepository, times(1)).existsByEmail(request.getEmail());
        verify(customerRepository, times(1)).save(any(Customer.class)); // Save was attempted
    }

    // --- Test for Customer Constructor Bug ---

    // This test specifically targets the bug in CustomerRegistrationService where
    // request.getFirstName() was passed for both first and last names.
    // It will *fail* if the service is still buggy, and *pass* once the service is fixed.
    @Test
    @DisplayName("Test 20: Verify Customer constructor correctly uses firstName and lastName (Bug Fix Confirmation)")
    void testCustomerConstructorBugFixConfirmation() throws Exception {
        // Arrange
        CustomerRegisterRequestDTO request = new CustomerRegisterRequestDTO("First", "Last", "bug@test.com", "pass",
                UserType.CUSTOMER);

        when(customerRepository.existsByEmail(anyString())).thenReturn(false);
        doNothing().when(customerRepository).save(any(Customer.class));

        // Act
        Customer registeredCustomer = customerRegistrationService.register(request);

        // Assert
        assertNotNull(registeredCustomer, "Registered customer should not be null");
        assertEquals("First", registeredCustomer.getFirstName(), "First name should be 'First'");
        // This assertion will only pass IF you fix the bug in CustomerRegistrationService
        // by changing `new Customer(request.getFirstName(), request.getFirstName(), ...)` to
        // `new Customer(request.getFirstName(), request.getLastName(), ...)`
        assertEquals("Last", registeredCustomer.getLastName(), "Last name should be 'Last' after service fix");

        verify(customerRepository, times(1)).existsByEmail(request.getEmail());
        verify(customerRepository, times(1)).save(any(Customer.class));
    }
}