package com.syos.dto;

import com.syos.model.Bill;
import com.syos.model.BillItem;
import com.syos.model.Product; // Import Product if your BillItem needs it
import com.syos.dto.BillReportDTO;
import com.syos.dto.BillItemReportDTO;

import java.util.List;
import java.util.stream.Collectors;

public class ReportDTOMapper {

	public static BillItemReportDTO toBillItemReportDTO(BillItem billItem) {
		// Ensure product is not null to prevent NullPointerException
		String productName = (billItem.getProduct() != null) ? billItem.getProduct().getName() : "Unknown Product";
		String productCode = (billItem.getProduct() != null) ? billItem.getProduct().getCode() : "N/A";
		double unitPrice = (billItem.getProduct() != null) ? billItem.getProduct().getPrice() : 0.0;
		double calculatedSubtotal = unitPrice * billItem.getQuantity();

		return new BillItemReportDTO(productName, productCode, billItem.getQuantity(), unitPrice, calculatedSubtotal,
				billItem.getDiscountAmount(), billItem.getTotalPrice() // This is the net price after discount for the
																		// item
		);
	}

	public static BillReportDTO toBillReportDTO(Bill bill, List<BillItemReportDTO> itemDTOs) {
		return new BillReportDTO(bill.getSerialNumber(), bill.getBillDate(), bill.getTotalAmount(),
				bill.getCashTendered(), bill.getChangeReturned(), bill.getTransactionType(), itemDTOs);
	}

	// Overload for when items are not yet converted to DTOs
	public static BillReportDTO toBillReportDTO(Bill bill) {
		return new BillReportDTO(bill.getSerialNumber(), bill.getBillDate(), bill.getTotalAmount(),
				bill.getCashTendered(), bill.getChangeReturned(), bill.getTransactionType(), null // Items will be set
																									// separately or
																									// loaded later
		);
	}
	

    
}