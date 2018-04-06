package com.tobiascarryer.trading.exchanges;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class BasicExchange implements Exchange {

	protected Map<String, BigDecimal> balances = new HashMap<String, BigDecimal>();
	protected Map<String, Boolean> canWithdrawAsset = new HashMap<String, Boolean>();
	protected final Set<String> tickersSupported = new HashSet<String>();
	
	@Override
	public BigDecimal getBalance(String asset) {
		return this.balances.get(asset.toUpperCase());
	}

	@Override
	public Boolean canWithdraw(String asset) {
		// Null if currency is not supported
		return this.canWithdrawAsset.get(asset.toUpperCase());
	}
	
	@Override
	public Boolean supports(String majorCurrency, String minorCurrency) {
		return this.tickersSupported.contains(makeTicker(majorCurrency, minorCurrency)); 
	}
	
	@Override
	public int getQuantityDecimalPrecision(String majorCurrency, String minorCurrency) {
		return 8;
	}

	@Override
	public int getPriceDecimalPrecision(String majorCurrency, String minorCurrency) {
		return 8;
	}
	
	/**
	 * @param priceOrQuantity, String intended to be used when setting priceDecimalPrecision or quantityDecimalPrecision
	 * @return the number of numbers after the decimal excluding trailing zeroes. For example: 0.00100000 = 3
	 */
	protected int countDecimalPlaces(String priceOrQuantity) {
		// Remove zeroes, remove numbers before decimal, return number of numbers after the decimal point.
		return priceOrQuantity.trim().replaceAll("^.*\\.", "").length();
	}
}
