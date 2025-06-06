package com.syos.model;

import java.time.LocalDate;

import com.syos.enums.DiscountType;

public class Discount {
	private final int id;
	private final String name;
	private final DiscountType type;
	private final double value;
	private final LocalDate start;
	private final LocalDate end;

	public Discount(int id, String name, DiscountType type, double value, LocalDate start, LocalDate end) {
		this.id = id;
		this.name = name;
		this.type = type;
		this.value = value;
		this.start = start;
		this.end = end;
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public DiscountType getType() {
		return type;
	}

	public double getValue() {
		return value;
	}

	public LocalDate getStart() {
		return start;
	}

	public LocalDate getEnd() {
		return end;
	}

	public boolean isActiveOn(LocalDate date) {
		return (date.isEqual(start) || date.isAfter(start)) && (date.isEqual(end) || date.isBefore(end));
	}
}
