package com.syos.command;

import com.syos.service.DiscountCreationService; 
import java.util.Scanner;

public class CreateDiscountCommand implements Command {
    private final DiscountCreationService discountCreationService;

    public CreateDiscountCommand(Scanner scanner) {
        this.discountCreationService = new DiscountCreationService(scanner);
    }

    @Override
    public void execute() {
        discountCreationService.createDiscount();
    }
}