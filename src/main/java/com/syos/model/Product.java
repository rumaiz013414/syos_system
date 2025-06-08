package com.syos.model;

public class Product {

	private String code;
	private String name;
	private double price;

	public Product(String code, String name, double price) {
		super();
		this.code = code;
		this.name = name;
		this.price = price;

	}

	public String getCode() {
		return code;
	}

	public String getName() {
		return name;
	}

	public double getPrice() {
		return price;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setPrice(double price) {
		this.price = price;
	}

}
