package com.tobiascarryer.trading.exchanges.orders;

import java.math.BigDecimal;

public class Order {
	
	private OrderType action;
	private BigDecimal amountToTrade, price;
	private String majorCurrency, minorCurrency;
	
	/**
	 * @param action
	 * @param amountToTrade, measured in majorCurrency
	 * @param majorCurrency, symbol
	 * @param minorCurrency, symbol
	 */
	public Order(OrderType action, BigDecimal amountToTrade, BigDecimal price, String majorCurrency, String minorCurrency) {
		this.action = action;
		this.amountToTrade = amountToTrade;
		this.price = price;
		this.majorCurrency = majorCurrency;
		this.minorCurrency = minorCurrency;
	}
	
	public OrderType getAction() {
		return this.action;
	}
	
	public BigDecimal getMajorAmountToTrade() {
		return this.amountToTrade;
	}

	public BigDecimal getPrice() {
		return this.price;
	}
	
	public String getMajorCurrency() {
		return this.majorCurrency;
	}
	
	public String getMinorCurrency() {
		return this.minorCurrency;
	}
	
	public void setMajorAmountToTrade(BigDecimal volume) {
		this.amountToTrade = volume;
	}
	
	public void setPrice(BigDecimal price) {
		this.price = price;
	}
}


