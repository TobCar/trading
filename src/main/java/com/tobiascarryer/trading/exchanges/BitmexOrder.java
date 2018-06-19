package com.tobiascarryer.trading.exchanges;

import java.math.BigDecimal;

public class BitmexOrder {
	
	private String stopId, orderId;
	private BigDecimal orderQty;
	
	public BitmexOrder( String stopId, BigDecimal orderQty, String orderId ) {
		this.stopId = stopId;
		this.orderQty = orderQty;
		this.orderId = orderId;
	}
	
	public String getStopId() {
		return stopId;
	}
	
	public String getOrderId() {
		return orderId;
	}
	
	public BigDecimal getOrderQty() {
		return orderQty; 
	}
}
