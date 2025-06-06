package com.syos.command;

import java.util.Scanner;

import com.syos.service.DiscountAssignmentService;
import com.syos.repository.DiscountRepository;
import com.syos.repository.ProductRepository;

public class AssignDiscountCommand implements Command {
	private final DiscountAssignmentService discountAssignmentService;

	public AssignDiscountCommand(Scanner scanner, DiscountRepository discountRepository,
			ProductRepository productRepository) {
		this.discountAssignmentService = new DiscountAssignmentService(scanner, discountRepository, productRepository);
	}

	@Override
	public void execute() {
		discountAssignmentService.assignDiscountToProduct();
	}
}