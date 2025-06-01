package com.syos.util;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

public class CommonInputs {
	private static final Scanner SC = new Scanner(System.in);

	private CommonInputs() {
	}

	public static void printHeader(String title) {
		System.out.println("\n=== " + title + " ===");
	}

	public static void printMenu(List<String> options) {
		for (int i = 0; i < options.size(); i++) {
			System.out.printf("%d) %s%n", i + 1, options.get(i));
		}
		System.out.print("Choose an option: ");
	}

	public static String promptNonEmptyString(String prompt) {
		System.out.print(prompt);
		String input = SC.nextLine().trim();
		if (input.isEmpty()) {
			System.out.println("Input cannot be empty.");
			return null;
		}
		return input;
	}

	public static Integer promptPositiveInt(String prompt) {
		System.out.print(prompt);
		String line = SC.nextLine().trim();
		int value;
		try {
			value = Integer.parseInt(line);
		} catch (NumberFormatException nfe) {
			System.out.println("Invalid number. Please enter a positive integer.");
			return null;
		}
		if (value <= 0) {
			System.out.println("Quantity must be greater than zero.");
			return null;
		}
		return value;
	}

	public static LocalDate promptDate(String prompt) {
		System.out.print(prompt);
		String line = SC.nextLine().trim();
		try {
			return LocalDate.parse(line);
		} catch (DateTimeException dte) {
			System.out.println("Invalid date format. Please use YYYY-MM-DD.");
			return null;
		}
	}

	public static void printSuccess(String message) {
		System.out.println("Success !!!  " + message);
	}

	public static void printError(String message) {
		System.out.println("Error !!! " + message);
	}

	public static <T> void printList(String header, List<T> items) {
		System.out.println("\n--- " + header + " ---");
		if (items.isEmpty()) {
			System.out.println("No entries found.");
			return;
		}
		for (T item : items) {
			System.out.println(item);
		}
	}

}
