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
	
	public static Candle parseLine(String line) {
		String[] values = line.split(",");
		BigDecimal open = new BigDecimal(values[0]);
		BigDecimal high = new BigDecimal(values[1]);
		BigDecimal low = new BigDecimal(values[2]);
		BigDecimal close = new BigDecimal(values[3]);
		return new Candle(high, low, open, close);
	}
	
	@Override
	public String toString() {
		return open + "," + high + "," + low + "," + close;
	}
}
