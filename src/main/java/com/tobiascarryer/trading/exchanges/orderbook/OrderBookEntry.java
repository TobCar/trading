package com.tobiascarryer.trading.exchanges.orderbook;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class OrderBookEntry {
	private BigDecimal price;
	private BigDecimal minorCurrencyAmount;
	private BigDecimal majorCurrencyAmount;
	
	public OrderBookEntry(BigDecimal price, BigDecimal minorCurrencyAmount) {
		this.setPrice(price);
		this.setMinorCurrencyAmount(minorCurrencyAmount);
	}
	
	public OrderBookEntry(String price, String minorCurrencyAmount) {
		this.setPrice(new BigDecimal(price));
		this.setMinorCurrencyAmount(new BigDecimal(minorCurrencyAmount));
	}
	
	public BigDecimal getMajorCurrencyAmount() {
		return this.majorCurrencyAmount;
	}

	public BigDecimal getMinorCurrencyAmount() {
		return this.minorCurrencyAmount;
	}

	public BigDecimal getPrice() {
		return this.price;
	}

	public void setMajorCurrencyAmount(BigDecimal newAmount) {
		this.majorCurrencyAmount = newAmount;
		this.minorCurrencyAmount = this.majorCurrencyAmount.multiply(this.price);
	}

	public void setMinorCurrencyAmount(BigDecimal newVolume) {
		this.minorCurrencyAmount = newVolume.setScale(8, RoundingMode.HALF_EVEN);
		this.majorCurrencyAmount = this.minorCurrencyAmount.divide(this.price, 8, RoundingMode.HALF_EVEN);
	}

	public void setPrice(BigDecimal newPrice) {
		this.price = newPrice.setScale(8, RoundingMode.HALF_EVEN);
	}
}
