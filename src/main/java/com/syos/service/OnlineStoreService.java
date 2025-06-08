package com.syos.service;

import java.util.List;
import java.util.Scanner;

import com.syos.dto.CustomerRegisterRequestDTO;
import com.syos.enums.UserType;
import com.syos.model.Customer;
import com.syos.model.Product;
import com.syos.repository.CustomerRepository;
import com.syos.repository.ProductRepository;
import org.mindrot.jbcrypt.BCrypt;

public class OnlineStoreService {
	private final Scanner scanner = new Scanner(System.in);
	private final CustomerRegistrationService registrationService = new CustomerRegistrationService();
	private final CustomerRepository customerRepository = new CustomerRepository();
	private final ProductRepository productRepository = new ProductRepository();

	public void run() {
		System.out.println("=== Welcome to SYOS Online Store ===");

		Customer customer = null;

		while (customer == null) {
			System.out.println("1) Login");
			System.out.println("2) Register");
			System.out.println("3) Exit");
			System.out.print("Select an option: ");
			String choice = scanner.nextLine().trim();

			switch (choice) {
			case "1" -> customer = login();
			case "2" -> customer = register();
			case "3" -> {
				System.out.println("Exited!");
				return;
			}
			default -> System.out.println("Invalid option. Try again.");
			}
		}

		while (true) {
			System.out.println("\n=== Online Store Menu ===");
			System.out.println("1) Browse Products");
			System.out.println("2) Search Product by Code");
			System.out.println("3) Logout");
			System.out.print("Select an option: ");
			String choice = scanner.nextLine().trim();

			switch (choice) {
			case "1" -> browseProducts();
			case "2" -> searchProduct();
			case "3" -> {
				System.out.println("Goodbye, " + customer.getFirstName() + "!");
				return;
			}
			default -> System.out.println("Invalid option. Please choose 1, 2 or 3.");
			}
		}
	}

	private Customer register() {
		System.out.println("\n=== Customer Registration ===");
		System.out.print("First name: ");
		String firstName = scanner.nextLine().trim();
		System.out.print("Last name: ");
		String lastName = scanner.nextLine().trim();
		System.out.print("Email: ");
		String email = scanner.nextLine().trim();
		System.out.print("Password: ");
		String password = scanner.nextLine().trim();

		var request = new CustomerRegisterRequestDTO(firstName, lastName, email, password, UserType.CUSTOMER);
		try {
			Customer customer = registrationService.register(request);
			System.out.printf(" Registered: %s (%s)%n", customer.getFullName(), customer.getEmail());
			return customer;
		} catch (Exception e) {
			System.err.println(" Registration failed: " + e.getMessage());
			return null;
		}
	}

	private Customer login() {
		System.out.println("\n=== Customer Login ===");
		System.out.print("Email: ");
		String email = scanner.nextLine().trim();
		System.out.print("Password: ");
		String password = scanner.nextLine().trim();

		Customer customer = customerRepository.findByEmail(email);
		if (customer == null) {
			System.out.println(" Invalid email.");
			return null;
		}

		if (!BCrypt.checkpw(password, customer.getPassword())) {
			System.out.println(" Incorrect password.");
			return null;
		}

		System.out.printf(" Welcome back, %s!%n", customer.getFirstName());
		return customer;
	}

	private void browseProducts() {
		List<Product> products = productRepository.findAll();
		if (products == null || products.isEmpty()) {
			System.out.println(" No products available.");
			return;
		}
		System.out.println("\nAvailable Products:");
		for (Product product : products) {
			System.out.printf(" - %s (%s): %.2f%n", product.getName(), product.getCode(), product.getPrice());
		}
	}

	private void searchProduct() {
		System.out.print("Enter product code: ");
		String code = scanner.nextLine().trim();
		Product product = productRepository.findByCode(code);
		if (product == null) {
			System.out.println(" Product not found.");
		} else {
			System.out.printf(" Found: %s (%s) — %.2f%n", product.getName(), product.getCode(), product.getPrice());
		}
	}
}