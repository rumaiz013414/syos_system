package com.syos.command;

import java.util.Scanner;

import com.syos.service.DiscountCreationService;
import com.syos.repository.DiscountRepository;

public class CreateDiscountCommand implements Command {
	private final DiscountCreationService discountCreationService;

	public CreateDiscountCommand(Scanner scanner, DiscountRepository discountRepository) {
		this.discountCreationService = new DiscountCreationService(scanner, discountRepository);
	}

	@Override
	public void execute() {
		discountCreationService.createDiscount();
	}
}