package com.syos.command;

import com.syos.model.Discount;
import com.syos.repository.DiscountRepository;
import com.syos.enums.DiscountType;

import java.util.List;
import java.util.Scanner;

public class ViewAllDiscountsCommand implements Command {
	private final DiscountRepository discountRepository;

	public ViewAllDiscountsCommand(DiscountRepository discountRepository, Scanner scanner) {
		this.discountRepository = discountRepository;
	}

	String lineSeperator = "-----------------------------------------------------------------------------------------";

	@Override
	public void execute() {
		System.out.println(System.lineSeparator() + "--- All Available Discounts ---");
		List<Discount> discounts = discountRepository.findAll();

		if (discounts.isEmpty()) {
			System.out.println("No discounts have been created yet.");
			return;
		}

		System.out.printf("%-5s %-20s %-15s %-15s %-15s %-15s%n", "ID", "Name", "Type", "Value", "Start Date",
				"End Date");
		System.out.println(lineSeperator);

		for (Discount discount : discounts) {
			String typeDisplay = (discount.getType() == DiscountType.PERCENT) ? "Percentage" : "Fixed Amount";
			String valueDisplay = (discount.getType() == DiscountType.PERCENT)
					? String.format("%.2f%%", discount.getValue())
					: String.format("%.2f", discount.getValue());

			System.out.printf("%-5d %-20s %-15s %-15s %-15s %-15s%n", discount.getId(), discount.getName(), typeDisplay,
					valueDisplay, discount.getStart().toString(), discount.getEnd().toString());
		}
		System.out.println(lineSeperator);
	}
}