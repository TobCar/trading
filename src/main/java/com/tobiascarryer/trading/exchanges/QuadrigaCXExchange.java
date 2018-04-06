package com.tobiascarryer.trading.exchanges;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map.Entry;
import java.util.TimerTask;

import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.account.Balance;
import org.knowm.xchange.dto.account.Wallet;
import org.knowm.xchange.dto.trade.LimitOrder;

import com.tobiascarryer.trading.exchanges.exceptions.BookNotFoundException;
import com.tobiascarryer.trading.exchanges.exceptions.NotEnoughBalanceException;
import com.tobiascarryer.trading.exchanges.orderbook.OrderBookIterator;
import com.tobiascarryer.trading.exchanges.orderbook.OrderBooks;
import com.tobiascarryer.trading.exchanges.orders.Order;

import java.util.Timer;

final class QuadrigaCXExchange extends BasicExchange {
	
	private final org.knowm.xchange.Exchange quadrigaBackend;
	
	private final OrderBooks<List<LimitOrder>> asks = new OrderBooks<List<LimitOrder>>();
	private final OrderBooks<List<LimitOrder>> bids = new OrderBooks<List<LimitOrder>>();
	
	private boolean useFiatFees = false;
	
	public QuadrigaCXExchange(String key, String secret, String clientId, boolean useFiatFees) {
		org.knowm.xchange.ExchangeSpecification exSpec = new org.knowm.xchange.quadrigacx.QuadrigaCxExchange().getDefaultExchangeSpecification();
		exSpec.setApiKey(key);
		exSpec.setSecretKey(secret);
		exSpec.setUserName(clientId);
		quadrigaBackend = org.knowm.xchange.ExchangeFactory.INSTANCE.createExchange(exSpec);
		
		// Keep the balances in sync by fetching them every one and a half minutes.
		new Thread() {
		    public void run() {
		    	while(true) {
		    		try {
						Thread.sleep((System.currentTimeMillis() % 90000l)-10l);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
			    	updateBalances();
		    	}
		    }
		}.start();
		
		this.useFiatFees = useFiatFees;
		
		// Hardcode the supported currencies and symbols, there is no API method to fetch them and
		// they are always available for withdrawals so there is no need for updates.
		// This has the added benefit of excluding old assets Quadriga had before like gold.
		this.tickersSupported.add(makeTicker("BTC", "USD"));
		this.tickersSupported.add(makeTicker("BTC", "CAD"));
		this.tickersSupported.add(makeTicker("ETH", "CAD"));
		this.tickersSupported.add(makeTicker("LTC", "CAD"));
		this.tickersSupported.add(makeTicker("BTG", "CAD"));
		this.tickersSupported.add(makeTicker("BCH", "CAD"));
		this.tickersSupported.add(makeTicker("ETH", "BTC"));
		this.tickersSupported.add(makeTicker("LTC", "BTC"));
		this.tickersSupported.add(makeTicker("BTG", "BTC"));
		this.tickersSupported.add(makeTicker("BCH", "BTC"));
		this.canWithdrawAsset.put("BTC", true);
		this.canWithdrawAsset.put("ETH", true);
		this.canWithdrawAsset.put("LTC", true);
		this.canWithdrawAsset.put("BCH", true);
		this.canWithdrawAsset.put("BTG", true);
	}
	
	private void updateBalances() {
		try {
			for( Entry<String, Wallet> wallet: quadrigaBackend.getAccountService().getAccountInfo().getWallets().entrySet() ) {
				for( Entry<Currency, Balance> balance: wallet.getValue().getBalances().entrySet() ) {
					balances.put(balance.getKey().getCurrencyCode(), balance.getValue().getTotal());
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void startMonitoringBook(String[] majorCurrencies, final String minorCurrency) {
		final String majorCurrency = majorCurrencies[0];
		if( majorCurrencies.length > 1 )
			System.out.println("QuadrigaCX can only monitor one trading pair at a time to comply with API request limits. Monitoring major currency "+majorCurrency+".");
		
		this.tickersSupported.add(makeTicker(majorCurrency, minorCurrency));
		final CurrencyPair pair = new CurrencyPair(majorCurrency, minorCurrency);
		
		// Keep order book in sync every fifteen seconds.
		(new Timer()).schedule((new TimerTask() {
            public void run() {
            	// Balances are updated every 90 seconds, skip if within 15 seconds to avoid going over the API limit.
            	long timeSinceBalanceUpdate = System.currentTimeMillis() % 90000l;
            	if( timeSinceBalanceUpdate < 75000l && timeSinceBalanceUpdate > 15000l ) {
					try {
						org.knowm.xchange.dto.marketdata.OrderBook orderbook = quadrigaBackend.getMarketDataService().getOrderBook(pair);
						asks.put(majorCurrency, minorCurrency, orderbook.getAsks());
	            		bids.put(majorCurrency, minorCurrency, orderbook.getBids());
					} catch (IOException e) {
						e.printStackTrace();
					}
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
		CurrencyPair pair = new CurrencyPair(order.getMajorCurrency(), order.getMinorCurrency());
		LimitOrder quadrigaOrder = new LimitOrder(org.knowm.xchange.dto.Order.OrderType.BID, order.getMajorAmountToTrade(), pair, "", null, order.getPrice());
		try {
			quadrigaBackend.getTradeService().placeLimitOrder(quadrigaOrder);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// Keep track of the balance internally for the few seconds until a proper update is received
		// update task is in the constructor
		this.balances.put(order.getMajorCurrency(), this.balances.get(order.getMajorCurrency()).add(order.getMajorAmountToTrade()));
		this.balances.put(order.getMinorCurrency(), this.balances.get(order.getMinorCurrency()).add(order.getMajorAmountToTrade().multiply(order.getPrice())));
	}

	@Override
	public void sell(Order order) throws NotEnoughBalanceException {
		CurrencyPair pair = new CurrencyPair(order.getMajorCurrency(), order.getMinorCurrency());
		LimitOrder quadrigaOrder = new LimitOrder(org.knowm.xchange.dto.Order.OrderType.ASK, order.getMajorAmountToTrade(), pair, "", null, order.getPrice());
		try {
			quadrigaBackend.getTradeService().placeLimitOrder(quadrigaOrder);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
