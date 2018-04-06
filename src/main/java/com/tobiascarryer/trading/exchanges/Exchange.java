package com.tobiascarryer.trading.exchanges;

import java.math.BigDecimal;

import com.tobiascarryer.trading.exchanges.exceptions.*;
import com.tobiascarryer.trading.exchanges.orderbook.OrderBookIterator;
import com.tobiascarryer.trading.exchanges.orders.Order;

public interface Exchange {
	
	/**
	 * @return The percentage of a trade kept after fees. (1% fee would return 0.99)
	 */
	public BigDecimal getPostTradingFee();
	
	/**
	 * @param majorCurrencies, array of major currency symbols whose order book should be monitored
	 * @param minorCurrency, minor currency to monitor in
	 */
	public void startMonitoringBook(String[] majorCurrencies, String minorCurrency);
	
	/**
	 * @param majorCurrency, symbol
	 * @param minorCurrency, symbol
	 * @return True if the majorCurrency can be bought/sold using minorCurrency.
	 */
	public Boolean supports(String majorCurrency, String minorCurrency);
	
	/**
	 * @param currency, symbol
	 * @return True if there is no maintenance preventing a withdrawal. null if the asset is not listed on the exchange.
	 */
	public Boolean canWithdraw(String asset);
	
	/**
	 * Assumes the order book is being monitored.
	 * @param majorCurrency, symbol of the currency to get the ask for
	 * @param minorCurrency, symbol
	 * @return The price of the lowest ask, quoted in the minor currency.
	 */
	public BigDecimal getLowestAsk(String majorCurrency, String minorCurrency) throws BookNotFoundException;
	
	/**
	 * Assumes the order book is being monitored.
	 * @param majorCurrency, symbol of the currency to get the bid for
	 * @param minorCurrency, symbol
	 * @return The price of the highest bid, quoted in the minor currency.
	 */
	public BigDecimal getHighestBid(String majorCurrency, String minorCurrency) throws BookNotFoundException;
	
	/**
	 * @return OrderBookIterator to iterate through the bids from highest to lowest, agnostic to the exchange the bids are from. 
	 */
	public OrderBookIterator getBidsIterator(String majorCurrency, String minorCurrency) throws BookNotFoundException;
	
	/**
	 * @return OrderBookIterator to iterate through the asks from lowest to highest, agnostic to the exchange the asks are from. 
	 */
	public OrderBookIterator getAsksIterator(String majorCurrency, String minorCurrency) throws BookNotFoundException;
	
	/**
	 * @param order
	 * @throws NotEnoughBalanceException if there is not enough minorCurrency to buy amount
	 */
	public void buy(Order order) throws NotEnoughBalanceException;
	
	/**
	 * @param order
	 * @throws NotEnoughBalanceException if the balance of the majorCurrency < amount
	 */
	public void sell(Order order) throws NotEnoughBalanceException;
	
	/**
	 * @param asset, symbol
	 * @return The balance available to trade with.
	 */
	public BigDecimal getBalance(String asset);
	
	/**
	 * @param majorCurrency, symbol
	 * @param minorCurrency, symbol
	 * @return The ticker if the major currency was tradeable using the minor currency
	 */
	public String makeTicker(String majorCurrency, String minorCurrency);
	
	/**
	 * @param asset, symbol
	 * @return The number of decimal places in the quantity.
	 */
	public int getQuantityDecimalPrecision(String majorCurrency, String minorCurrency);
	
	/**
	 * @param majorCurrency, symbol
	 * @param minorCurrency, symbol
	 * @return The number of decimal places in the price.
	 */
	public int getPriceDecimalPrecision(String majorCurrency, String minorCurrency);
	
	/**
	 * @param majorCurrency, symbol
	 * @param minorCurrency, symbol
	 * @return The minimum volume for a trade, measured in the minor currency
	 */
	public BigDecimal getMinimumMinorVolume(String majorCurrency, String minorCurrency);
	
	/**
	 * @param majorCurrency, symbol
	 * @param minorCurrency, symbol
	 * @return The minimum quantity of the major currency required for a trade.
	 */
	public BigDecimal getMinimumQuantity(String majorCurrency, String minorCurrency);
	
	/**
	 * Submit a withdrawal request in this exchange. Requires API Key and Secret.
	 * @param asset, symbol
	 * @param address, blockchain address to send the asset to
	 * @param amount, amount of the asset to send
	 */
	//public void withdraw(String asset, String address, BigDecimal amount);
}
