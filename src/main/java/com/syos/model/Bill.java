package com.syos.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.syos.util.CommonVariables;

public class Bill {
	private int id;
	private final int serialNumber;
	private final Date billDate;
	private List<BillItem> items;
	private final double totalAmount;
	private final double cashTendered;
	private final double changeReturned;
	private final String transactionType;

	private Bill(BillBuilder b) {
		this.serialNumber = b.serialNumber;
		this.billDate = new Date();
		this.items = List.copyOf(b.items);
		this.totalAmount = b.totalAmount;
		this.cashTendered = b.cashTendered;
		this.changeReturned = b.cashTendered - b.totalAmount;
		this.transactionType = b.transactionType;
	}

	public Bill(int id, int serialNumber, Date billDate, double totalAmount, double cashTendered, double changeReturned,
			String transactionType) {
		this.id = id;
		this.serialNumber = serialNumber;
		this.billDate = (billDate != null) ? new Date(billDate.getTime()) : null;
		this.totalAmount = totalAmount;
		this.cashTendered = cashTendered;
		this.changeReturned = changeReturned;
		this.transactionType = transactionType;
		this.items = new ArrayList<>();
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getSerialNumber() {
		return serialNumber;
	}

	public Date getBillDate() {
		return new Date(billDate.getTime());
	}

	public List<BillItem> getItems() {
		return items;
	}

	public void setItems(List<BillItem> items) {
		this.items = new ArrayList<>(items);
	}

	public double getTotalAmount() {
		return totalAmount;
	}

	public double getCashTendered() {
		return cashTendered;
	}

	public double getChangeReturned() {
		return changeReturned;
	}

	public String getTransactionType() {
		return transactionType;
	}

	public static class BillBuilder {
		private final int serialNumber;
		private final List<BillItem> items;
		private double totalAmount = 0;
		private double cashTendered = 0;
		private String transactionType = "COUNTER";

		public BillBuilder(int serialNumber, List<BillItem> items) {
			if (items == null || items.isEmpty()) {
				throw new IllegalArgumentException("At least one BillItem required");
			}
			this.serialNumber = serialNumber;
			this.items = items;
			this.items.forEach(item -> totalAmount += item.getTotalPrice());
		}

		public BillBuilder withCashTendered(double cash) {
			if (cash < totalAmount) {
				throw new IllegalArgumentException("Cash tendered must cover total");
			}
			this.cashTendered = cash;
			return this;
		}

		public BillBuilder withTransactionType(String transactionType) {
			if (transactionType == null || transactionType.isBlank()) {
				throw new IllegalArgumentException("Transaction type cannot be empty");
			}
			this.transactionType = transactionType;
			return this;
		}

		public Bill build() {
			if (cashTendered == CommonVariables.MINIMUMAMOUNT) {
				throw new IllegalStateException("Must set cashTendered");
			}
			return new Bill(this);
		}
	}
}
