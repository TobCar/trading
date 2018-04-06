package com.tobiascarryer.trading.simulations.exchanges;

import com.dylanjsa.cryptopia.CryptopiaClient;
import com.dylanjsa.cryptopia.remote.data.MarketOrderGroup;
import com.dylanjsa.cryptopia.remote.data.enums.CurrencyStatus;
import com.tobiascarryer.trading.exchanges.BasicExchange;
import com.tobiascarryer.trading.exchanges.exceptions.BookNotFoundException;
import com.tobiascarryer.trading.exchanges.exceptions.NotEnoughBalanceException;
import com.tobiascarryer.trading.exchanges.orderbook.OrderBookIterator;
import com.tobiascarryer.trading.exchanges.orderbook.OrderBooks;
import com.tobiascarryer.trading.exchanges.orders.Order;
import com.dylanjsa.cryptopia.remote.data.MarketOrder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.Timer;

public class SimulationCryptopiaExchange extends BasicExchange {
	
	private final CryptopiaClient client = new CryptopiaClient();
	
	private final OrderBooks<List<MarketOrder>> asks = new OrderBooks<List<MarketOrder>>();
	private final OrderBooks<List<MarketOrder>> bids = new OrderBooks<List<MarketOrder>>();
	
	public SimulationCryptopiaExchange(String key, String secret) {
		this.client.setKey(key);
		this.client.setSecretKey(secret);
		
		// Check what wallets are under maintenance every five seconds
		(new Timer()).schedule((new TimerTask() {
            public void run() {
            	boolean memoizeSupportsFunction = tickersSupported.isEmpty();
            	
            	for( com.dylanjsa.cryptopia.remote.data.Currency currency: client.getCurrencies() ) {
            		String symbol = currency.getSymbol().toUpperCase().replace("\"", "");
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
		
		// Make up the balances for simulation purposes
		balances.put("ETH", new BigDecimal("2"));
		balances.put("LTC", new BigDecimal("3"));
		balances.put("BTC", new BigDecimal("0.5"));
	}
	
	public void startMonitoringBook(String[] majorCurrencies, final String minorCurrency) {
		final List<String> tickersToRequest = new ArrayList<>();
		for( String majorCurrency: majorCurrencies ) {
			if( supports(majorCurrency, minorCurrency) )
				tickersToRequest.add(makeTicker(majorCurrency, minorCurrency));
		}
		
		// Keep order book in sync every second
		if( !tickersToRequest.isEmpty() ) {
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
	public void buy(Order order) throws NotEnoughBalanceException {
		try {
			System.out.println("Cryptopia buying "+order.getMajorAmountToTrade()+" "+order.getMajorCurrency()+".");
			BigDecimal amountLeft = order.getMajorAmountToTrade();
			BigDecimal newMinorBalance = this.balances.get(order.getMinorCurrency());
			for( MarketOrder ask: asks.get(order.getMajorCurrency(), order.getMinorCurrency()) ) {
				BigDecimal amountToBuy = ask.getVolume().min(amountLeft);
				BigDecimal toSpend = amountToBuy.multiply(ask.getPrice());
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
		System.out.println("Cryptopia selling "+order.getMajorAmountToTrade()+" "+order.getMajorCurrency()+".");
		if( order.getMajorAmountToTrade().compareTo(this.balances.get(order.getMajorCurrency())) == 1 )
			throw new NotEnoughBalanceException();
		try {
			BigDecimal amountLeft = order.getMajorAmountToTrade();
			for( MarketOrder bid: bids.get(order.getMajorCurrency(), order.getMinorCurrency()) ) {
				BigDecimal toSell = bid.getVolume().min(amountLeft);
				this.balances.put(order.getMinorCurrency(), this.balances.get(order.getMinorCurrency()).add(toSell.multiply(bid.getPrice())));
				amountLeft = amountLeft.subtract(toSell);
				if( amountLeft.compareTo(new BigDecimal("0")) == 0 )
					break;
			}
			this.balances.put(order.getMajorCurrency(), this.balances.get(order.getMajorCurrency()).subtract(order.getMajorAmountToTrade()));
		} catch (BookNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public BigDecimal simulateWithdrawal(String asset, BigDecimal amount) {
		try {
			Thread.sleep(300000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		asset = asset.toUpperCase();
		BigDecimal balance = this.balances.get(asset);
		if( balance == null ) {
			System.out.println("Asset ("+asset+") has no balance.");
			System.exit(0);
		}
		if( balance.compareTo(amount) == 1 ) {
			System.out.println("Amount to withdraw is larger than balance.");
			System.exit(0);
		}
		this.balances.put(asset, balance.subtract(amount));
		return amount;
	}

	@Override
	public int getQuantityDecimalPrecision(String majorCurrency, String minorCurrency) {
		return 8;
	}

	@Override
	public int getPriceDecimalPrecision(String majorCurrency, String minorCurrency) {
		return 8;
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
}
