package com.test.service; // Your test file location

import com.syos.dto.CustomerRegisterRequestDTO;
import com.syos.enums.UserType;
import com.syos.model.Customer;
import com.syos.model.Product;
import com.syos.repository.CustomerRepository;
import com.syos.repository.ProductRepository;
import com.syos.service.OnlineStoreService;
import com.syos.service.CustomerRegistrationService; // Import the registration service

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Scanner; // Need to explicitly import Scanner for the test setup

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

class OnlineStoreServiceIntegrationTest {

    private final InputStream originalSystemIn = System.in;
    private final PrintStream originalSystemOut = System.out;
    private final PrintStream originalSystemErr = System.err;

    private ByteArrayOutputStream capturedOutput;
    private ByteArrayOutputStream capturedError;
    private Scanner testScanner; // Declare a Scanner for the test input

    // These will be the ACTUAL repository instances used by the service under test
    private CustomerRepository customerRepository;
    private ProductRepository productRepository;
    private CustomerRegistrationService customerRegistrationService; // The actual instance

    @BeforeEach
    void setUp() {
        // Capture System.out and System.err
        capturedOutput = new ByteArrayOutputStream();
        capturedError = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capturedOutput));
        System.setErr(new PrintStream(capturedError));

        // Initialize actual repository instances for the test
        customerRepository = new CustomerRepository(); // Your dummy in-memory one
        productRepository = new ProductRepository();   // Your dummy in-memory one

        // Initialize CustomerRegistrationService, injecting the SAME customerRepository
        customerRegistrationService = new CustomerRegistrationService();

        // Clear repositories before each test to ensure a clean state
        customerRepository.clear();
        productRepository.clear(); // If your dummy ProductRepo can be cleared, clear it.
                                  // If it pre-populates, you might need to re-add data
                                  // or ensure its pre-population is done once globally or in a static block.
    }

    @AfterEach
    void tearDown() {
        // Restore original System.in, System.out, and System.err
        System.setIn(originalSystemIn);
        System.setOut(originalSystemOut);
        System.setErr(originalSystemErr);
        if (testScanner != null) {
            testScanner.close(); // Close the scanner created in the test
        }
    }

    private void provideInput(String data) {
        ByteArrayInputStream testIn = new ByteArrayInputStream(data.getBytes());
        System.setIn(testIn);
        testScanner = new Scanner(System.in); // Create a new scanner for the redirected input
    }

    private String getOutput() {
        return capturedOutput.toString();
    }

    private String getErrorOutput() {
        return capturedError.toString();
    }

    // --- Test Cases for Login/Registration Menu ---

    @Test
    @DisplayName("Should display welcome message and main menu on run")
    void run_DisplaysWelcomeAndMenu() {
        // Arrange
        provideInput("3\n"); // Exit

        // Pass all the dependencies to the OnlineStoreService constructor
        OnlineStoreService service = new OnlineStoreService();

        // Act
        service.run();

        // Assert
        String output = getOutput();
        assertTrue(output.contains("=== Welcome to SYOS Online Store ==="));
        assertTrue(output.contains("1) Login"));
        assertTrue(output.contains("2) Register"));
        assertTrue(output.contains("3) Exit"));
        assertTrue(output.contains("Select an option:"));
        assertTrue(output.contains("Exited!"));
    }

    @Test
    @DisplayName("Should display error for invalid option in login/register menu")
    void run_InvalidOption_LoginRegisterMenu() {
        // Arrange
        provideInput("4\n3\n"); // Invalid option, then Exit

        OnlineStoreService service = new OnlineStoreService();

        // Act
        service.run();

        // Assert
        String output = getOutput();
        assertTrue(output.contains("Invalid option. Try again."));
        assertTrue(output.contains("Exited!"));
    }

    // --- Test Cases for Registration Flow ---

    @Test
    @DisplayName("Should successfully register a new customer")
    void register_Success() {
        // Arrange
        String input = "2\n" + // Choose Register
                       "Test\n" + // First name
                       "User\n" + // Last name
                       "test.user@example.com\n" + // Email
                       "Password123\n" + // Password
                       "3\n"; // Logout after successful registration

        provideInput(input);
        OnlineStoreService service = new OnlineStoreService();

        // Act
        service.run();

        // Assert
        String output = getOutput();
        assertTrue(output.contains("=== Customer Registration ==="));
        assertTrue(output.contains("Registered: Test User (test.user@example.com)"));
        assertTrue(output.contains("Welcome back, Test!")); // Login after registration
        assertTrue(output.contains("Goodbye, Test!"));
        assertFalse(getErrorOutput().contains("Registration failed:")); // No error should be printed
    }

    @Test
    @DisplayName("Should fail registration if email already exists")
    void register_EmailAlreadyExists() throws Exception {
        // Arrange
        // Pre-register a customer using the SAME customerRepository instance
        Customer existingCustomer = new Customer("Existing", "User", "existing@example.com", BCrypt.hashpw("ExistingPass", BCrypt.gensalt()), UserType.CUSTOMER);
        customerRepository.save(existingCustomer);

        String input = "2\n" + // Choose Register
                       "Another\n" +
                       "User\n" +
                       "existing@example.com\n" + // Duplicate email
                       "NewPassword\n" +
                       "3\n"; // Exit

        provideInput(input);
        // Inject the *same* instances into the OnlineStoreService
        OnlineStoreService service = new OnlineStoreService();

        // Act
        service.run();

        // Assert
        String output = getOutput();
        String errorOutput = getErrorOutput();
        assertTrue(output.contains("=== Customer Registration ==="));
        assertTrue(errorOutput.contains("Registration failed: Email 'existing@example.com' is already registered."));
        assertFalse(output.contains("Registered:")); // Should not show success message
        assertFalse(output.contains("Welcome back,")); // Should not log in
        assertTrue(output.contains("Select an option:")); // Should return to login/register menu
        assertTrue(output.contains("Exited!")); // Should exit eventually
    }

    @Test
    @DisplayName("Should fail registration with empty email")
    void register_EmptyEmail() {
        // Arrange
        String input = "2\n" + // Choose Register
                       "First\n" +
                       "Last\n" +
                       "\n" + // Empty email - This will now be caught by OnlineStoreService's new validation
                       "Password123\n" +
                       "3\n"; // Exit

        provideInput(input);
        OnlineStoreService service = new OnlineStoreService();

        // Act
        service.run();

        // Assert
        String output = getOutput();
        String errorOutput = getErrorOutput();
        // Updated assertion for OnlineStoreService's new direct validation
        assertTrue(errorOutput.contains("Registration failed: All fields are required."));
        // The more specific "Email cannot be empty" from CustomerRegistrationService might not be hit first now
        assertFalse(output.contains("Registered:"));
        assertTrue(output.contains("Exited!"));
    }

    @Test
    @DisplayName("Should fail registration with empty first name")
    void register_EmptyFirstName() {
        // Arrange
        String input = "2\n" + // Choose Register
                "\n" + // Empty first name
                "Last\n" +
                "email@example.com\n" +
                "Password123\n" +
                "3\n"; // Exit

        provideInput(input);
        OnlineStoreService service = new OnlineStoreService();

        // Act
        service.run();

        // Assert
        String output = getOutput();
        String errorOutput = getErrorOutput();
        assertTrue(errorOutput.contains("Registration failed: All fields are required."));
        assertFalse(output.contains("Registered:"));
        assertTrue(output.contains("Exited!"));
    }

    @Test
    @DisplayName("Should fail registration with empty password")
    void register_EmptyPassword() {
        // Arrange
        String input = "2\n" + // Choose Register
                "First\n" +
                "Last\n" +
                "email@example.com\n" +
                "\n" + // Empty password
                "3\n"; // Exit

        provideInput(input);
        OnlineStoreService service = new OnlineStoreService();

        // Act
        service.run();

        // Assert
        String output = getOutput();
        String errorOutput = getErrorOutput();
        assertTrue(errorOutput.contains("Registration failed: All fields are required."));
        assertFalse(output.contains("Registered:"));
        assertTrue(output.contains("Exited!"));
    }


    // --- Test Cases for Login Flow ---

    @Test
    @DisplayName("Should successfully login an existing customer")
    void login_Success() throws Exception {
        // Arrange
        Customer existingCustomer = new Customer("Login", "User", "testuser2343@gmail.com", BCrypt.hashpw("LoginPass", BCrypt.gensalt()), UserType.CUSTOMER);
        customerRepository.save(existingCustomer);

        String input = "1\n" + // Choose Login
                       "login.user@example.com\n" + // Email
                       "LoginPass\n" + // Password
                       "3\n"; // Logout after successful login

        provideInput(input);
        OnlineStoreService service = new OnlineStoreService();

        // Act
        service.run();

        // Assert
        String output = getOutput();
        assertTrue(output.contains("=== Customer Login ==="));
        assertTrue(output.contains("Welcome back, Login!"));
        assertTrue(output.contains("Goodbye, Login!"));
        assertFalse(output.contains("Invalid email."));
        assertFalse(output.contains("Incorrect password."));
        assertFalse(output.contains("Email and password cannot be empty."));
    }

    @Test
    @DisplayName("Should fail login with invalid email")
    void login_InvalidEmail() {
        // Arrange
        provideInput("1\n" + // Choose Login
                     "nonexistent@example.com\n" + // Non-existent email
                     "SomePassword\n" +
                     "3\n"); // Exit

        OnlineStoreService service = new OnlineStoreService();

        // Act
        service.run();

        // Assert
        String output = getOutput();
        assertTrue(output.contains("Invalid email."));
        assertFalse(output.contains("Welcome back,")); // Should not log in
        assertTrue(output.contains("Exited!")); // Should eventually exit
    }

    @Test
    @DisplayName("Should fail login with incorrect password")
    void login_IncorrectPassword() throws Exception {
        // Arrange
        Customer existingCustomer = new Customer("Login", "User", "correct@example.com", BCrypt.hashpw("CorrectPass", BCrypt.gensalt()), UserType.CUSTOMER);
        customerRepository.save(existingCustomer);

        provideInput("1\n" + // Choose Login
                     "correct@example.com\n" + // Correct email
                     "WrongPass\n" + // Incorrect password
                     "3\n"); // Exit

        OnlineStoreService service = new OnlineStoreService();

        // Act
        service.run();

        // Assert
        String output = getOutput();
        assertTrue(output.contains("Incorrect password."));
        assertFalse(output.contains("Welcome back,"));
        assertTrue(output.contains("Exited!"));
        assertFalse(output.contains("Email and password cannot be empty."));
    }

    @Test
    @DisplayName("Should fail login with empty email")
    void login_EmptyEmail() {
        // Arrange
        provideInput("1\n" + // Choose Login
                "\n" + // Empty email
                "password\n" +
                "3\n"); // Exit

        OnlineStoreService service = new OnlineStoreService();

        // Act
        service.run();

        // Assert
        String output = getOutput();
        assertTrue(output.contains("Email and password cannot be empty."));
        assertFalse(output.contains("Invalid email.")); // This won't be hit first
        assertFalse(output.contains("Welcome back,"));
        assertTrue(output.contains("Exited!"));
    }

    @Test
    @DisplayName("Should fail login with empty password")
    void login_EmptyPassword() {
        // Arrange
        provideInput("1\n" + // Choose Login
                "some@email.com\n" +
                "\n" + // Empty password
                "3\n"); // Exit

        OnlineStoreService service = new OnlineStoreService();

        // Act
        service.run();

        // Assert
        String output = getOutput();
        assertTrue(output.contains("Email and password cannot be empty."));
        assertFalse(output.contains("Incorrect password.")); // This won't be hit first
        assertFalse(output.contains("Welcome back,"));
        assertTrue(output.contains("Exited!"));
    }

    // --- Test Cases for Post-Login Menu ---

    @Test
    @DisplayName("Should browse products after login")
    void postLogin_BrowseProducts() throws Exception {
        // Arrange - Register and Login first
        Customer existingCustomer = new Customer("Browser", "Test", "browser@example.com", BCrypt.hashpw("Pass123", BCrypt.gensalt()), UserType.CUSTOMER);
        customerRepository.save(existingCustomer);

        // Input sequence: Login -> Browse -> Logout
        String input = "1\n" + // Login
                       "browser@example.com\n" +
                       "Pass123\n" +
                       "1\n" + // Browse Products
                       "3\n"; // Logout

        provideInput(input);
        OnlineStoreService service = new OnlineStoreService();

        // Act
        service.run();

        // Assert
        String output = getOutput();
        assertTrue(output.contains("Welcome back, Browser!"));
        assertTrue(output.contains("Available Products:"));
        assertTrue(output.contains("Gaming Laptop (LAPTOP001): 1500.00")); // From dummy ProductRepository
        assertTrue(output.contains("Wireless Mouse (MOUSE001): 25.50"));
        assertTrue(output.contains("Mechanical Keyboard (KEYBD001): 75.99"));
        assertTrue(output.contains("Goodbye, Browser!"));
    }

    @Test
    @DisplayName("Should search for product by code after login")
    void postLogin_SearchProduct_Found() throws Exception {
        // Arrange - Register and Login first
        Customer existingCustomer = new Customer("Searcher", "Test", "searcher@example.com", BCrypt.hashpw("Pass123", BCrypt.gensalt()), UserType.CUSTOMER);
        customerRepository.save(existingCustomer);

        // Input sequence: Login -> Search ("LAPTOP001") -> Logout
        String input = "1\n" + // Login
                       "searcher@example.com\n" +
                       "Pass123\n" +
                       "2\n" + // Search Product
                       "LAPTOP001\n" + // Product Code
                       "3\n"; // Logout

        provideInput(input);
        OnlineStoreService service = new OnlineStoreService();

        // Act
        service.run();

        // Assert
        String output = getOutput();
        assertTrue(output.contains("Welcome back, Searcher!"));
        assertTrue(output.contains("Enter product code:"));
        assertTrue(output.contains("Found: Gaming Laptop (LAPTOP001) — 1500.00"));
        assertTrue(output.contains("Goodbye, Searcher!"));
    }

    @Test
    @DisplayName("Should search for product by code after login - Not Found")
    void postLogin_SearchProduct_NotFound() throws Exception {
        // Arrange - Register and Login first
        Customer existingCustomer = new Customer("Searcher", "Test", "searcher2@example.com", BCrypt.hashpw("Pass123", BCrypt.gensalt()), UserType.CUSTOMER);
        customerRepository.save(existingCustomer);

        // Input sequence: Login -> Search ("NONEXISTENT") -> Logout
        String input = "1\n" + // Login
                       "searcher2@example.com\n" +
                       "Pass123\n" +
                       "2\n" + // Search Product
                       "NONEXISTENT\n" + // Non-existent Product Code
                       "3\n"; // Logout

        provideInput(input);
        OnlineStoreService service = new OnlineStoreService();

        // Act
        service.run();

        // Assert
        String output = getOutput();
        assertTrue(output.contains("Welcome back, Searcher!"));
        assertTrue(output.contains("Enter product code:"));
        assertTrue(output.contains("Product not found."));
        assertTrue(output.contains("Goodbye, Searcher!"));
    }

    @Test
    @DisplayName("Should handle invalid option in post-login menu")
    void postLogin_InvalidOption() throws Exception {
        // Arrange - Register and Login first
        Customer existingCustomer = new Customer("Invalid", "Option", "invalid@example.com", BCrypt.hashpw("Pass123", BCrypt.gensalt()), UserType.CUSTOMER);
        customerRepository.save(existingCustomer);

        // Input sequence: Login -> Invalid Option -> Logout
        String input = "1\n" + // Login
                       "invalid@example.com\n" +
                       "Pass123\n" +
                       "4\n" + // Invalid option
                       "3\n"; // Logout

        provideInput(input);
        OnlineStoreService service = new OnlineStoreService();

        // Act
        service.run();

        // Assert
        String output = getOutput();
        assertTrue(output.contains("Welcome back, Invalid!"));
        assertTrue(output.contains("Invalid option. Please choose 1, 2 or 3."));
        assertTrue(output.contains("Goodbye, Invalid!"));
    }

    @Test
    @DisplayName("Should fail search for product by code after login - Empty Code")
    void postLogin_SearchProduct_EmptyCode() throws Exception {
        // Arrange - Register and Login first
        Customer existingCustomer = new Customer("EmptyCodeSearch", "Test", "emptysearch@example.com", BCrypt.hashpw("Pass123", BCrypt.gensalt()), UserType.CUSTOMER);
        customerRepository.save(existingCustomer);

        // Input sequence: Login -> Search ("") -> Logout
        String input = "1\n" + // Login
                "emptysearch@example.com\n" +
                "Pass123\n" +
                "2\n" + // Search Product
                "\n" + // Empty Product Code
                "3\n"; // Logout

        provideInput(input);
        OnlineStoreService service = new OnlineStoreService();

        // Act
        service.run();

        // Assert
        String output = getOutput();
        assertTrue(output.contains("Welcome back, EmptyCodeSearch!"));
        assertTrue(output.contains("Enter product code:"));
        assertTrue(output.contains("Product code cannot be empty."));
        assertTrue(output.contains("Goodbye, EmptyCodeSearch!"));
    }
}