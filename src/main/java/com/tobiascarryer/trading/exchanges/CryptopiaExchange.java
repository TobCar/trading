package com.tobiascarryer.trading.exchanges;

import com.dylanjsa.cryptopia.CryptopiaClient;
import com.dylanjsa.cryptopia.remote.data.MarketOrderGroup;
import com.dylanjsa.cryptopia.remote.data.TradeSubmission;
import com.dylanjsa.cryptopia.remote.data.enums.CurrencyStatus;
import com.dylanjsa.cryptopia.remote.data.enums.TradeType;
import com.tobiascarryer.trading.exchanges.exceptions.BookNotFoundException;
import com.tobiascarryer.trading.exchanges.exceptions.NotEnoughBalanceException;
import com.tobiascarryer.trading.exchanges.orderbook.OrderBookIterator;
import com.tobiascarryer.trading.exchanges.orderbook.OrderBooks;
import com.tobiascarryer.trading.exchanges.orders.Order;
import com.dylanjsa.cryptopia.remote.data.Balance;
import com.dylanjsa.cryptopia.remote.data.MarketOrder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.Timer;

final class CryptopiaExchange extends BasicExchange {
	
	private final CryptopiaClient client = new CryptopiaClient();
	
	private final OrderBooks<List<MarketOrder>> asks = new OrderBooks<List<MarketOrder>>();
	private final OrderBooks<List<MarketOrder>> bids = new OrderBooks<List<MarketOrder>>();
	
	public CryptopiaExchange(String key, String secret) {
		this.client.setKey(key);
		this.client.setSecretKey(secret);
		
		// Check what wallets are under maintenance every five seconds
		(new Timer()).schedule((new TimerTask() {
            public void run() {
            	boolean memoizeSupportsFunction = tickersSupported.isEmpty();
            	for( com.dylanjsa.cryptopia.remote.data.Currency currency: client.getCurrencies() ) {
            		String symbol = cleanSymbol(currency.getSymbol());
            		canWithdrawAsset.put(symbol, currency.getStatus() == CurrencyStatus.OK);
            		
	        		if( memoizeSupportsFunction ) {
	        			// Cryptopia lists all assets with the following minor currencies:
	        			// BTC, USDT, NZDT
	        			tickersSupported.add(makeTicker(symbol, "BTC"));
	        			tickersSupported.add(makeTicker(symbol, "USDT"));
	        			tickersSupported.add(makeTicker(symbol, "NZDT"));
	        		}
        				
        		}
            }
        }), 0, 5000);
		
		// Keep the balances in sync by fetching them every second.
		(new Timer()).schedule((new TimerTask() {
            public void run() {
            	for( Balance balance: client.getBalance("") ) {
            		String symbol = cleanSymbol(balance.getSymbol());
            		balances.put(symbol, balance.getAvailable());
            	}
            }
        }), 0, 1000);
	}
	
	public void startMonitoringBook(String[] majorCurrencies, final String minorCurrency) {
		final List<String> tickersToRequest = new ArrayList<>();
		for( String majorCurrency: majorCurrencies ) {
			if( supports(majorCurrency, minorCurrency) )
				tickersToRequest.add(makeTicker(majorCurrency, minorCurrency));
		}
		
		// Keep order book in sync every second
		(new Timer()).schedule((new TimerTask() {
            public void run() {
            	List<MarketOrderGroup> marketOrderGroups = client.getMarketOrderGroups(tickersToRequest);
            	for( MarketOrderGroup marketOrderGroup: marketOrderGroups ) {
            		// Format for the market name is "MAJOR_MINOR" including the quotes.
            		// Substring 1 gets MAJOR_MINOR" then splitting by _ gets the MAJOR on its own.
            		String majorCurrency = marketOrderGroup.getMarket().substring(1).split("_")[0];
            		asks.put(majorCurrency, minorCurrency, marketOrderGroup.getSell());
            		bids.put(majorCurrency, minorCurrency, marketOrderGroup.getBuy());
            	}
            }
        }), 0, 1000);
	}
	
	public BigDecimal getPostTradingFee() {
		// It is possible Cryptopia adds 0.2% to the amount sent to the exchange.
		// Further testing should be done if small amounts not being traded becomes an issue.
		return new BigDecimal("0.998");
	}
	
	@Override
	public String makeTicker(String majorCurrency, String minorCurrency) {
		return majorCurrency.toUpperCase() + "_" + minorCurrency.toUpperCase();
	}
	
	public BigDecimal getLowestAsk(String majorCurrency, String minorCurrency) throws BookNotFoundException {
		return this.asks.get(majorCurrency, minorCurrency).get(0).getPrice();
	}
	
	public BigDecimal getHighestBid(String majorCurrency, String minorCurrency) throws BookNotFoundException {
		return this.bids.get(majorCurrency, minorCurrency).get(0).getPrice();
	}
	
	@Override
	public OrderBookIterator getBidsIterator(String majorCurrency, String minorCurrency) throws BookNotFoundException {
		return OrderBookIterator.createCryptopiaIterator(this.bids.get(majorCurrency, minorCurrency));
	}

	@Override
	public OrderBookIterator getAsksIterator(String majorCurrency, String minorCurrency) throws BookNotFoundException {
		return OrderBookIterator.createCryptopiaIterator(this.asks.get(majorCurrency, minorCurrency));
	}
	
	@Override
	public BigDecimal getMinimumMinorVolume(String majorCurrency, String minorCurrency) {
		String upperCaseSymbol = minorCurrency.toUpperCase();
		if( upperCaseSymbol.equals("NZDT") || upperCaseSymbol.equals("USDT") )
			return new BigDecimal("1");
		else
			return new BigDecimal("0.0005");
	}
	
	@Override
	public BigDecimal getMinimumQuantity(String majorCurrency, String minorCurrency) {
		return new BigDecimal("0");
	}
	
	private String cleanSymbol(String symbol) {
		return symbol.toUpperCase().replace("\"", "");
	}

	///                                        ///
	///    Methods requiring authentication    ///
	///                                        ///
	@Override
	public void buy(Order order) throws NotEnoughBalanceException {
		TradeSubmission trade = new TradeSubmission();
		trade.setType(TradeType.BUY);
		trade.setMarket(makeTicker(order.getMajorCurrency(), order.getMinorCurrency()));
		trade.setAmount(order.getMajorAmountToTrade());
		trade.setRate(order.getPrice());
		client.submitTrade(trade);
	}

	@Override
	public void sell(Order order) throws NotEnoughBalanceException {
		TradeSubmission trade = new TradeSubmission();
		trade.setType(TradeType.SELL);
		trade.setMarket(makeTicker(order.getMajorCurrency(), order.getMinorCurrency()));
		trade.setAmount(order.getMajorAmountToTrade());
		trade.setRate(order.getPrice());
		client.submitTrade(trade);	
	}
}
