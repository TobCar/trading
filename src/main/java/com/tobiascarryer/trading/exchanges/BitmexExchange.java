package com.tobiascarryer.trading.exchanges;

import info.bitrich.xchangestream.bitmex.BitmexStreamingExchange;
import info.bitrich.xchangestream.core.StreamingExchangeFactory;
import io.reactivex.disposables.Disposable;

import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.math.BigDecimal;
import java.math.RoundingMode;

import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;

import com.tobiascarryer.trading.HelperMethods;
import com.tobiascarryer.trading.charts.Candle;
import com.tobiascarryer.trading.charts.ChildCandleUnifier;

import org.knowm.xchange.bitmex.service.BitmexAccountService;
import org.knowm.xchange.bitmex.service.BitmexTradeServiceRaw;

/**
 * This class has only been tested on BTC/USD.
 */
public final class BitmexExchange {
	
	private final org.knowm.xchange.bitmex.BitmexExchange bitmexBackend;
	private BitmexStreamingExchange bitmexWebsocket;
	
	private String key, secret;
	private boolean useSandbox;
	
	private BigDecimal margin;
	private BigDecimal highestBid;
	private BigDecimal lowestAsk;
	private String symbol;
	private BigDecimal diffFromActualPriceToOrder;
	private BigDecimal percentageToTrade;
	private BigDecimal percentChangeToStop; // Percent difference from the entry to put a stop order
	
	public BitmexExchange(String key, String secret, int margin, boolean useSandbox, BigDecimal diffFromActualPriceToOrder, BigDecimal percentageToTrade, BigDecimal percentChangeToStop) {
		if( margin > 25 ) {
			System.out.println("Margin is greater than x25. That is too risky. Shutting down.");
			System.exit(0);
		}
		this.margin = new BigDecimal(margin);
		this.key = key;
		this.secret = secret;
		this.diffFromActualPriceToOrder = diffFromActualPriceToOrder;
		this.percentageToTrade = percentageToTrade;
		this.percentChangeToStop = percentChangeToStop;
		
		if( percentChangeToStop.multiply(this.margin).compareTo(new BigDecimal("0.99")) == 1 ) {
			System.out.println("Stop wouldn't do anything. Shutting down.");
			System.exit(0);
		}
		
		org.knowm.xchange.ExchangeSpecification exSpec = new org.knowm.xchange.bitmex.BitmexExchange().getDefaultExchangeSpecification();
		exSpec.setApiKey(key);
		exSpec.setSecretKey(secret);
		exSpec.setExchangeSpecificParametersItem("Use_Sandbox", useSandbox);
		bitmexBackend = (org.knowm.xchange.bitmex.BitmexExchange) org.knowm.xchange.ExchangeFactory.INSTANCE.createExchange(exSpec);
	}
	
	public Disposable startMonitoringTicker(String majorCurrency, String minorCurrency, ChildCandleUnifier unifier) {
		symbol = majorCurrency.toUpperCase()+minorCurrency.toUpperCase();
		
		// Connect to Bitmex websocket
		bitmexWebsocket = (BitmexStreamingExchange) StreamingExchangeFactory.INSTANCE.createExchange(createWebsocketSpecification(key, secret, useSandbox));
		bitmexWebsocket.connect().blockingAwait();
		
		// Subscribe to Bitmex ticker websocket
		//bitmexWebsocket.useCompressedMessages(true);
		return bitmexWebsocket.getStreamingMarketDataService()
		        .getTicker(new CurrencyPair(majorCurrency, minorCurrency))
		        .subscribe(ticker -> {
		        	Candle childCandle = new Candle(ticker.getAsk(), ticker.getBid(), ticker.getBid(), ticker.getBid());
		        	System.out.println("Socket " + ticker.getTimestamp().getTime());
		            unifier.processChildCandle(childCandle, ticker.getTimestamp().getTime());
		        }, throwable -> {
		        	System.out.println("Error in subscribing trades. ");
		        	throwable.printStackTrace();
		        });
	}
	
	public BigDecimal getAvailableMarginBalance() {
		try {
			return ((BitmexAccountService) bitmexBackend.getAccountService()).getBitmexMarginAccountStatus(new Currency("XBt")).getAvailableMargin();
		} catch (IOException e) {
			e.printStackTrace();
			return new BigDecimal("0");
		}
	}
	
	public BigDecimal getAmountToTrade() {
		return this.getAvailableMarginBalance().multiply(percentageToTrade).multiply(margin);
	}
	
	/**
	 * @param contractsPerMinor
	 * @param bitcoinAmount measured in satoshis, divide by 10^8 for the actual Bitcoin amount
	 * @return number of contracts for the amount
	 */
	public BigDecimal contractsFor(BigDecimal contractsPerMinor, BigDecimal bitcoinAmount) {
		if( symbol.contains("USD") )
			return HelperMethods.roundToNearestPointFive(bitcoinAmount.movePointLeft(8).multiply(contractsPerMinor)); // USD minor, quoted in BTC
		return bitcoinAmount.divide(contractsPerMinor, RoundingMode.DOWN).setScale(0, RoundingMode.DOWN); // BTC minor
	}
	
	private ExchangeSpecification createWebsocketSpecification(String key, String secret, boolean useSandbox) {
		ExchangeSpecification spec = new ExchangeSpecification(BitmexStreamingExchange.class.getName());
		spec.setApiKey(key);
		spec.setSecretKey(secret);
		spec.setShouldLoadRemoteMetaData(false);
		spec.setSslUri("https://www.bitmex.com/");
		spec.setHost("bitmex.com");
		spec.setPort(443);
		spec.setExchangeName("Bitmex");
		spec.setExchangeDescription("Bitmex is a bitcoin exchange");
		spec.setExchangeSpecificParametersItem("Use_Sandbox", useSandbox);
		return spec;
	}

	///                                        ///
	///    Methods requiring authentication    ///
	///                                        ///
	public BitmexOrder openLong(BigDecimal price) {
		BigDecimal orderQty = contractsFor(price, getAmountToTrade());
		try {
			String orderId = ((BitmexTradeServiceRaw) bitmexBackend.getTradeService()).placeLimitOrder(symbol, orderQty, price.add(diffFromActualPriceToOrder).setScale(0, RoundingMode.DOWN), "").getId();
			String stopId = ((BitmexTradeServiceRaw) bitmexBackend.getTradeService()).placeStopOrder(symbol, orderQty.negate(), price.multiply((new BigDecimal(1)).subtract(percentChangeToStop)).setScale(0, RoundingMode.DOWN), "Close").getId();
			return new BitmexOrder(stopId, orderQty, orderId);
		} catch( UndeclaredThrowableException e ) {
			e.printStackTrace();
			return null;
		}
	}

	public BitmexOrder openShort(BigDecimal price) {
		BigDecimal orderQty = contractsFor(price, getAmountToTrade()).negate();
		try {
			String orderId = ((BitmexTradeServiceRaw) bitmexBackend.getTradeService()).placeLimitOrder(symbol, orderQty, price.subtract(diffFromActualPriceToOrder).setScale(0, RoundingMode.UP), "").getId();
			String stopId = ((BitmexTradeServiceRaw) bitmexBackend.getTradeService()).placeStopOrder(symbol, orderQty.negate(), price.multiply((new BigDecimal(1)).add(percentChangeToStop)).setScale(0, RoundingMode.UP), "Close").getId();
			return new BitmexOrder(stopId, orderQty, orderId);
		} catch( UndeclaredThrowableException e ) {
			e.printStackTrace();
			return null;
		}
	}
	
	public void closeLong(BigDecimal orderQty) {
		try {
			((BitmexTradeServiceRaw) bitmexBackend.getTradeService()).placeLimitOrder(symbol, orderQty.negate(), highestBid.subtract(diffFromActualPriceToOrder), "Close");
		} catch( UndeclaredThrowableException e ) {
			e.printStackTrace();
		}
	}
	
	public void closeShort(BigDecimal orderQty) {
		try {
			((BitmexTradeServiceRaw) bitmexBackend.getTradeService()).placeLimitOrder(symbol, orderQty.negate(), lowestAsk.add(diffFromActualPriceToOrder), "Close");
		} catch( UndeclaredThrowableException e ) {
			e.printStackTrace();
		}
	}
	
	public void cancelOrder(String orderId) {
		((BitmexTradeServiceRaw) bitmexBackend.getTradeService()).cancelBitmexOrder(orderId);
	}
}
