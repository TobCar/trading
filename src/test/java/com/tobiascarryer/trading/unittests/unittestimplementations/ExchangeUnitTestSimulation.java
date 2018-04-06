package com.tobiascarryer.trading.unittests.unittestimplementations;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import com.tobiascarryer.trading.exchanges.Exchange;
import com.tobiascarryer.trading.exchanges.exceptions.BookNotFoundException;
import com.tobiascarryer.trading.exchanges.exceptions.NotEnoughBalanceException;
import com.tobiascarryer.trading.exchanges.orderbook.OrderBookEntry;
import com.tobiascarryer.trading.exchanges.orderbook.OrderBookIterator;
import com.tobiascarryer.trading.exchanges.orders.Order;

/**
 * Intended to be used by tests that require an Exchange.
 */
public class ExchangeUnitTestSimulation implements Exchange {
	
	public static OrderBookEntry[] templateAsks = {new OrderBookEntry("1.000", "0.5"),
											 new OrderBookEntry("1.005", "0.75"),
											 new OrderBookEntry("1.010", "1"),
											 new OrderBookEntry("1.015", "1.25")};
	public static OrderBookEntry[] templateBids = {new OrderBookEntry("0.995", "0.5"),
			 								 new OrderBookEntry("0.990", "0.75"),
	 								 		 new OrderBookEntry("0.985", "1"),
	 								 		 new OrderBookEntry("0.980", "1.25")};
	private OrderBookEntry[] asks;
	private OrderBookEntry[] bids;
	private Map<String, BigDecimal> balances = new HashMap<String, BigDecimal>();
	
	/**
	 * @param asks
	 * @param bids
	 * @param balances, keys must be all uppercase
	 */
	public ExchangeUnitTestSimulation(OrderBookEntry[] asks, OrderBookEntry[] bids, Map<String, BigDecimal> balances) {
		this.asks = asks;
		this.bids = bids;
		this.balances = balances;
	}
	
	@Override
	public BigDecimal getPostTradingFee() {
		return new BigDecimal("0.995");
	}

	@Override
	public void startMonitoringBook(String[] majorCurrencies, String minorCurrency) {
		System.out.println("Unexpected method call: startMonitoringBook");
	}

	@Override
	public Boolean supports(String majorCurrency, String minorCurrency) {
		// Does not truly simulate checking a ticker since the minor currency would be supported as a "ticker" as well
		return balances.containsKey(majorCurrency.toUpperCase());
	}

	@Override
	public BigDecimal getLowestAsk(String majorCurrency, String minorCurrency) throws BookNotFoundException {
		return this.asks[0].getPrice();
	}

	@Override
	public BigDecimal getHighestBid(String majorCurrency, String minorCurrency) throws BookNotFoundException {
		return this.bids[0].getPrice();
	}

	@Override
	public OrderBookIterator getBidsIterator(String majorCurrency, String minorCurrency) throws BookNotFoundException {
		return OrderBookIterator.createIteratorByCloning(this.bids);
	}

	@Override
	public OrderBookIterator getAsksIterator(String majorCurrency, String minorCurrency) throws BookNotFoundException {
		return OrderBookIterator.createIteratorByCloning(this.asks);
	}

	@Override
	public void buy(Order order) throws NotEnoughBalanceException {
		// Simulate the order book. Assumes there are enough orders to match the order against.
		BigDecimal remaining = order.getMajorAmountToTrade();
		int i = 0;
		while( i < this.asks.length && remaining.compareTo(new BigDecimal("0")) == 1 ) {
			if( this.asks[i].getPrice().compareTo(order.getPrice()) == 1 )
				System.out.println("There are not enough orders to fill the buy order.");
			BigDecimal toBuy = this.asks[i].getMajorCurrencyAmount().min(remaining);
			this.balances.put(order.getMajorCurrency().toUpperCase(), this.balances.get(order.getMajorCurrency().toUpperCase()).add(toBuy));
			this.balances.put(order.getMinorCurrency().toUpperCase(), this.balances.get(order.getMinorCurrency().toUpperCase()).subtract(toBuy.multiply(this.asks[i].getPrice())));
			this.asks[i].setMajorCurrencyAmount(this.asks[i].getMajorCurrencyAmount().subtract(toBuy));
			remaining = remaining.subtract(toBuy);
		}
	}

	@Override
	public void sell(Order order) throws NotEnoughBalanceException {
		// Simulate the order book. Assumes there are enough orders to match the order against.
		BigDecimal remaining = order.getMajorAmountToTrade();
		int i = 0;
		while( i < this.bids.length && remaining.compareTo(new BigDecimal("0")) == 1 ) {
			if( this.bids[i].getPrice().compareTo(order.getPrice()) == -1 )
				System.out.println("There are not enough orders to fill the sell order.");
			BigDecimal toSell = this.bids[i].getMajorCurrencyAmount().min(remaining);
			this.balances.put(order.getMajorCurrency().toUpperCase(), this.balances.get(order.getMajorCurrency().toUpperCase()).subtract(toSell));
			this.balances.put(order.getMinorCurrency().toUpperCase(), this.balances.get(order.getMinorCurrency().toUpperCase()).add(toSell.multiply(this.bids[i].getPrice())));
			this.bids[i].setMajorCurrencyAmount(this.bids[i].getMajorCurrencyAmount().subtract(toSell));
			remaining = remaining.subtract(toSell);
		}
	}

	@Override
	public BigDecimal getBalance(String currency) {
		return this.balances.get(currency.toUpperCase());
	}

	@Override
	public Boolean canWithdraw(String currency) {
		return true;
	}

	@Override
	public String makeTicker(String majorCurrency, String minorCurrency) {
		return null;
	}

	@Override
	public int getQuantityDecimalPrecision(String majorCurrency, String minorCurrency) {
		return 4;
	}

	@Override
	public int getPriceDecimalPrecision(String majorCurrency, String minorCurrency) {
		return 3;
	}

	@Override
	public BigDecimal getMinimumMinorVolume(String majorCurrency, String minorCurrency) {
		return new BigDecimal("0");
	}

	@Override
	public BigDecimal getMinimumQuantity(String majorCurrency, String minorCurrency) {
		return new BigDecimal("0");
	}
}
