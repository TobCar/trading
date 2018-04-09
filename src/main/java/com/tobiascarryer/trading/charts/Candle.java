package com.tobiascarryer.trading.charts;

import java.math.BigDecimal;

public class Candle {
	
	private BigDecimal high, low, open, close;
	
	public Candle( BigDecimal high, BigDecimal low, BigDecimal open, BigDecimal close ) {
		this.high = high;
		this.low = low;
		this.open = open;
		this.close = close;
	}
	
	public BigDecimal getHigh() {
		return this.high;
	}
	
	public BigDecimal getLow() {
		return this.low;
	}
	
	public BigDecimal getOpen() {
		return this.open;
	}
	
	public BigDecimal getClose() {
		return this.close;
	}
}
