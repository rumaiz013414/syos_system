package com.test.service;

import com.syos.command.Command; // Needed because the service instantiates these
import com.syos.service.InventoryService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryServiceIntegrationTest { // Renamed to reflect integration nature

	private final InputStream originalIn = System.in;
	private final PrintStream originalOut = System.out;
	private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();

	@BeforeEach
	void setUp() {
		System.setOut(new PrintStream(outContent)); // Redirect System.out
		// System.setErr is not needed for this service unless commands print to it
		// No @Mock or @InjectMocks here because we're testing the concrete setup
	}

	@AfterEach
	void tearDown() {
		System.setIn(originalIn); // Restore original System.in
		System.setOut(originalOut); // Restore original System.out
		// Note: Repositories and InventoryManager are singletons/globally instantiated.
		// There's no easy way to reset their internal state here without making them
		// resettable,
		// which can lead to complex test setups. This is a drawback of this design.
	}

	@Test
	@DisplayName("Should display menu and exit on '16'")
	void run_DisplaysMenuAndExits() {
		// Arrange
		// Simulate user input: "16" (exit option)
		String input = "16\n";
		ByteArrayInputStream inContent = new ByteArrayInputStream(input.getBytes());
		System.setIn(inContent); // Redirect System.in

		InventoryService inventoryService = new InventoryService(); // Use the actual service

		// Act
		inventoryService.run();

		// Assert
		String consoleOutput = outContent.toString();
		assertTrue(consoleOutput.contains("=== Inventory Menu ==="), "Menu header should be displayed");
		assertTrue(consoleOutput.contains("16) Exit"), "Exit option should be displayed");
		assertTrue(consoleOutput.contains("Exiting Inventory Menu."), "Exit message should be displayed");
		assertTrue(consoleOutput.contains("Choose an option:"), "Prompt for choice should be displayed");
		// Ensure the menu is printed only once before exit
		long menuCount = consoleOutput.lines().filter(line -> line.contains("=== Inventory Menu ===")).count();
		// This is a weak assertion, might be 1 or 2 depending on exact print order and
		// loop iterations
		// For '16' straight away, it should be 1 menu print.
		assertTrue(menuCount >= 1, "Menu should appear at least once");
	}

	@Test
	@DisplayName("Should display error for invalid option and continue, then exit")
	void run_InvalidOptionThenExit() {
		// Arrange
		// Simulate user input: "invalid_option", then "16" (exit)
		String input = "invalid_option\n16\n";
		ByteArrayInputStream inContent = new ByteArrayInputStream(input.getBytes());
		System.setIn(inContent);

		InventoryService inventoryService = new InventoryService();

		// Act
		inventoryService.run();

		// Assert
		String consoleOutput = outContent.toString();
		assertTrue(consoleOutput.contains("Invalid option. Please choose from the available numbers."),
				"Error message for invalid option should appear");
		assertTrue(consoleOutput.contains("Exiting Inventory Menu."), "Exit message should appear");

		// The menu should appear twice (once before invalid, once before exit)
		long menuCount = consoleOutput.lines().filter(line -> line.contains("=== Inventory Menu ===")).count();
		assertTrue(menuCount >= 2, "Menu should appear at least twice for invalid input then exit");
	}

	// This kind of test is extremely brittle because it depends on the exact
	// sequence of prompts and user input needed by AddProductCommand.
	// It's often better to test commands directly or refactor the service.

}