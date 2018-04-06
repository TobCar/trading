package com.tobiascarryer.trading.simulations.exchanges;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.TimerTask;

import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.trade.LimitOrder;

import com.tobiascarryer.trading.exchanges.BasicExchange;
import com.tobiascarryer.trading.exchanges.exceptions.BookNotFoundException;
import com.tobiascarryer.trading.exchanges.exceptions.NotEnoughBalanceException;
import com.tobiascarryer.trading.exchanges.orderbook.OrderBookIterator;
import com.tobiascarryer.trading.exchanges.orderbook.OrderBooks;
import com.tobiascarryer.trading.exchanges.orders.Order;

import java.util.Timer;

public class SimulationQuadrigaCXExchange extends BasicExchange {
	
	private final org.knowm.xchange.Exchange quadrigaBackend;
	
	private final OrderBooks<List<LimitOrder>> asks = new OrderBooks<List<LimitOrder>>();
	private final OrderBooks<List<LimitOrder>> bids = new OrderBooks<List<LimitOrder>>();
	
	private boolean useFiatFees = false;
	
	public SimulationQuadrigaCXExchange(String key, String secret, String clientId, boolean useFiatFees) {
		org.knowm.xchange.ExchangeSpecification exSpec = new org.knowm.xchange.quadrigacx.QuadrigaCxExchange().getDefaultExchangeSpecification();
		quadrigaBackend = org.knowm.xchange.ExchangeFactory.INSTANCE.createExchange(exSpec);
		
		this.useFiatFees = useFiatFees;
		
		// Hardcode the withdrawable, there is no API method to fetch them and
		// they are always available for withdrawals so there is no need for updates.
		// This has the added benefit of excluding old assets Quadriga had before like gold.
		this.canWithdrawAsset.put("BTC", true);
		this.canWithdrawAsset.put("ETH", true);
		this.canWithdrawAsset.put("LTC", true);
		this.canWithdrawAsset.put("BCH", true);
		this.canWithdrawAsset.put("BTG", true);
		
		// Make up the balances for simulation purposes
		balances.put("ETH", new BigDecimal("2"));
		balances.put("LTC", new BigDecimal("3"));
		balances.put("BTC", new BigDecimal("0.5"));
	}
	
	public void startMonitoringBook(String[] majorCurrencies, final String minorCurrency) {
		final String majorCurrency = majorCurrencies[0];
		if( majorCurrencies.length > 1 )
			System.out.println("QuadrigaCX can only monitor one trading pair at a time to comply with API request limits. Monitoring major currency "+majorCurrency+".");
			
		this.tickersSupported.add(makeTicker(majorCurrency, minorCurrency));
		final CurrencyPair pair = new CurrencyPair(majorCurrency, minorCurrency);
			
		// Keep order book in sync every two and a half seconds.
		(new Timer()).schedule((new TimerTask() {
            public void run() {
				try {
					org.knowm.xchange.dto.marketdata.OrderBook orderbook = quadrigaBackend.getMarketDataService().getOrderBook(pair);
					asks.put(majorCurrency, minorCurrency, orderbook.getAsks());
            		bids.put(majorCurrency, minorCurrency, orderbook.getBids());
				} catch (IOException e) {
					e.printStackTrace();
				}	
            }
        }), 0, 5000);
	}
	
	public BigDecimal getPostTradingFee() {
		if( useFiatFees ) {
			return new BigDecimal("0.998");
		} else {
			return new BigDecimal("0.995");
		}
	}
	
	@Override
	public String makeTicker(String majorCurrency, String minorCurrency) {
		return minorCurrency.toLowerCase() + "_" + majorCurrency.toLowerCase();
	}
	
	public BigDecimal getLowestAsk(String majorCurrency, String minorCurrency) throws BookNotFoundException {
		return this.asks.get(majorCurrency, minorCurrency).get(0).getLimitPrice();
	}
	
	public BigDecimal getHighestBid(String majorCurrency, String minorCurrency) throws BookNotFoundException {
		return this.bids.get(majorCurrency, minorCurrency).get(0).getLimitPrice();
	}
	
	@Override
	public OrderBookIterator getBidsIterator(String majorCurrency, String minorCurrency) throws BookNotFoundException {
		return OrderBookIterator.createXChangeIterator(this.bids.get(majorCurrency, minorCurrency));
	}

	@Override
	public OrderBookIterator getAsksIterator(String majorCurrency, String minorCurrency) throws BookNotFoundException {
		return OrderBookIterator.createXChangeIterator(this.asks.get(majorCurrency, minorCurrency));
	}
	
	@Override
	public BigDecimal getMinimumMinorVolume(String majorCurrency, String minorCurrency) {
		switch( minorCurrency.toUpperCase() ) {
		case "CAD": return new BigDecimal("10");
		case "USD": return new BigDecimal("10");
		default: return new BigDecimal("0.000001");
		}
	}
	
	@Override
	public BigDecimal getMinimumQuantity(String majorCurrency, String minorCurrency) {
		switch( minorCurrency.toUpperCase() ) {
		case "CAD": return new BigDecimal("0.005");
		case "USD": return new BigDecimal("0.005");
		default: return new BigDecimal("0.000001");
		}
	}

	///                                        ///
	///    Methods requiring authentication    ///
	///                                        ///
	@Override
	public void buy(Order order) throws NotEnoughBalanceException {
		try {
			System.out.println("Quadriga buying "+order.getMajorAmountToTrade()+" "+order.getMajorCurrency()+".");
			BigDecimal amountLeft = order.getMajorAmountToTrade();
			BigDecimal newMinorBalance = this.balances.get(order.getMinorCurrency());
			for( LimitOrder ask: asks.get(order.getMajorCurrency(), order.getMinorCurrency()) ) {
				BigDecimal amountToBuy = ask.getRemainingAmount().min(amountLeft);
				BigDecimal toSpend = amountToBuy.multiply(ask.getLimitPrice());
				if( toSpend.compareTo(newMinorBalance) == 1 )
					throw new NotEnoughBalanceException();
				newMinorBalance = newMinorBalance.subtract(toSpend);
				amountLeft = amountLeft.subtract(amountToBuy);
				if( amountLeft.compareTo(new BigDecimal("0")) == 0 )
					break;
			}
			this.balances.put(order.getMinorCurrency(), newMinorBalance);
			this.balances.put(order.getMajorCurrency(), this.balances.get(order.getMajorCurrency()).add(order.getMajorAmountToTrade()));
		} catch (BookNotFoundException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void sell(Order order) throws NotEnoughBalanceException {
		System.out.println("Quadriga selling "+order.getMajorAmountToTrade()+" "+order.getMajorCurrency()+".");
		if( order.getMajorAmountToTrade().compareTo(this.balances.get(order.getMajorCurrency())) == 1 )
			throw new NotEnoughBalanceException();
		try {
			BigDecimal amountLeft = order.getMajorAmountToTrade();
			for( LimitOrder bid: bids.get(order.getMajorCurrency(), order.getMinorCurrency()) ) {
				BigDecimal toSell = bid.getRemainingAmount().min(amountLeft);
				this.balances.put(order.getMinorCurrency(), this.balances.get(order.getMinorCurrency()).add(toSell.multiply(bid.getLimitPrice())));
				amountLeft = amountLeft.subtract(toSell);
				if( amountLeft.compareTo(new BigDecimal("0")) == 0 )
					break;
			}
			this.balances.put(order.getMajorCurrency(), this.balances.get(order.getMajorCurrency()).subtract(order.getMajorAmountToTrade()));
		} catch (BookNotFoundException e) {
			e.printStackTrace();
		}
	}
}
