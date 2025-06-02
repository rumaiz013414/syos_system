package com.syos.command;

import com.syos.service.DiscountAssignmentService; // Corrected import
import java.util.Scanner;

public class AssignDiscountCommand implements Command {
    private final DiscountAssignmentService discountAssignmentService;

    public AssignDiscountCommand(Scanner scanner) {
        this.discountAssignmentService = new DiscountAssignmentService(scanner);
    }

    @Override
    public void execute() {
        discountAssignmentService.assignDiscountToProduct();
    }
}