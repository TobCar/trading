package com.tobiascarryer.trading.simulations.exchanges;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiWebSocketClient;
import com.binance.api.client.BinanceApiAsyncRestClient;
import com.binance.api.client.domain.market.OrderBook;
import com.binance.api.client.domain.market.OrderBookEntry;
import com.tobiascarryer.trading.JsonReader;
import com.tobiascarryer.trading.exchanges.BasicExchange;
import com.tobiascarryer.trading.exchanges.exceptions.BookNotFoundException;
import com.tobiascarryer.trading.exchanges.exceptions.NotEnoughBalanceException;
import com.tobiascarryer.trading.exchanges.orderbook.OrderBookIterator;
import com.tobiascarryer.trading.exchanges.orderbook.OrderBooks;
import com.tobiascarryer.trading.exchanges.orders.Order;
import com.binance.api.client.domain.general.ExchangeInfo;
import com.binance.api.client.domain.general.FilterType;
import com.binance.api.client.domain.general.SymbolInfo;
import com.binance.api.client.domain.event.DepthEvent;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

public class SimulationBinanceExchange extends BasicExchange {
	
	private BinanceApiClientFactory clientFactory;
	private BinanceApiAsyncRestClient asyncClient;
	private BinanceApiWebSocketClient websocketClient;
	private boolean useBNB = false;
	
	private Map<String, Integer> quantityPrecision = new HashMap<String, Integer>();
	private Map<String, Integer> pricePrecision = new HashMap<String, Integer>();
	private Map<String, BigDecimal> minMinorVolume = new HashMap<String, BigDecimal>();
	private Map<String, BigDecimal> minQuantity = new HashMap<String, BigDecimal>();
	
	// Binance API's OrderBookEntry, different than the OrderBookEntry in the orderbook package
	private OrderBooks<List<OrderBookEntry>> asks = new OrderBooks<List<OrderBookEntry>>();
	private OrderBooks<List<OrderBookEntry>> bids = new OrderBooks<List<OrderBookEntry>>();
	
	public SimulationBinanceExchange(String key, String secret, boolean useBNB) {
		this.clientFactory = BinanceApiClientFactory.newInstance(key, secret);
		this.asyncClient = this.clientFactory.newAsyncRestClient();
		this.websocketClient = this.clientFactory.newWebSocketClient();
		this.useBNB = useBNB;
		
		this.asyncClient.getExchangeInfo((ExchangeInfo exchangeInfo) -> {
			for( SymbolInfo symbolInfo: exchangeInfo.getSymbols() ) {
				tickersSupported.add(symbolInfo.getSymbol());
				quantityPrecision.put(symbolInfo.getSymbol(), countDecimalPlaces(symbolInfo.getSymbolFilter(FilterType.LOT_SIZE).getStepSize()));
				pricePrecision.put(symbolInfo.getSymbol(), countDecimalPlaces(symbolInfo.getSymbolFilter(FilterType.PRICE_FILTER).getTickSize()));
				minMinorVolume.put(symbolInfo.getSymbol(), new BigDecimal(symbolInfo.getSymbolFilter(FilterType.MIN_NOTIONAL).getMinNotional()));
				minQuantity.put(symbolInfo.getSymbol(), new BigDecimal(symbolInfo.getSymbolFilter(FilterType.LOT_SIZE).getMinQty()));
			}
		});
		
		// Check what wallets are under maintenance every two seconds
		(new Timer()).schedule((new TimerTask() {
            public void run() {
				try {
					JSONArray assets = JsonReader.readJsonArrayFromUrl("https://www.binance.com/assetWithdraw/getAllAsset.html");
					for( int i=0; i < assets.length(); i++ ) {
	            		JSONObject asset = assets.getJSONObject(i);
	            		canWithdrawAsset.put(asset.getString("assetCode"), asset.getBoolean("enableWithdraw"));
	            	}
				} catch (JSONException | IOException e) {
					e.printStackTrace();
				}
            }
        }), 0, 2000);
		
		// Make up the balances for simulation purposes
		balances.put("ETH", new BigDecimal("2"));
		balances.put("LTC", new BigDecimal("3"));
		balances.put("BTC", new BigDecimal("0.5"));
	}
	
	@Override
	public void startMonitoringBook(final String[] majorCurrencies, final String minorCurrency) {
		for( String majorCurrency: majorCurrencies ) {
			String ticker = makeTicker(majorCurrency, minorCurrency);
			
			if( supports(majorCurrency, minorCurrency) ) {
				this.asyncClient.getOrderBook(ticker, 1000, (OrderBook orderBookResponse) -> {
					this.asks.put(majorCurrency, minorCurrency, orderBookResponse.getAsks());
					this.bids.put(majorCurrency, minorCurrency, orderBookResponse.getBids());
					
					final long snapshotUpdateId = orderBookResponse.getLastUpdateId();
					
					// Unknown why ticker has to be lower case for the websocket. It must be upper case
					// for the async client anyway.
					websocketClient.onDepthEvent(ticker.toLowerCase(), (DepthEvent depthEvent) -> {
						// Drop any event where u is <= lastUpdateId in the snapshot
						if( depthEvent.getFinalUpdateId() <= snapshotUpdateId )
							return; // Update is for data before the snapshot
	
						// Set up dictionary for what to update to make the updates take O(n)
						// time rather than O(n^2) if each entry would have to be search to check if
						// it was updated.
						HashMap<String, String> toUpdate = new HashMap<String, String>();
						Set<String> toRemove = new HashSet<String>();
						for( int i=0; i < depthEvent.getBids().size(); i++ ) {
							if( depthEvent.getBids().get(i).getQty().equals("0") )
								toRemove.add(depthEvent.getBids().get(i).getPrice());
							else
								toUpdate.put(depthEvent.getBids().get(i).getPrice(), depthEvent.getBids().get(i).getQty());
						}
						
						// Process the data sent to the websocket to keep the order book in sync.
						try {
							List<OrderBookEntry> asksToUpdate = this.asks.get(majorCurrency, minorCurrency);
							List<OrderBookEntry> bidsToUpdate = this.bids.get(majorCurrency, minorCurrency);
						
							// Update amounts if they changed or remove them if their quantity is zero
							for( Iterator<OrderBookEntry> iter = asksToUpdate.listIterator(); iter.hasNext(); ) {
								OrderBookEntry ask = iter.next();
								if( toUpdate.containsKey(ask.getPrice()) )
									ask.setQty(toUpdate.get(ask.getPrice()));
								else if( toRemove.contains(ask.getPrice()) )
									iter.remove();
							}
							for( Iterator<OrderBookEntry> iter = bidsToUpdate.listIterator(); iter.hasNext(); ) {
								OrderBookEntry bid = iter.next();
								if( toUpdate.containsKey(bid.getPrice()) )
									bid.setQty(toUpdate.get(bid.getPrice()));
								else if( toRemove.contains(bid.getPrice()) )
									iter.remove();
							}
							
							// Set the updated data in the OrderBooks data structure
							this.asks.put(majorCurrency, minorCurrency, asksToUpdate);
							this.bids.put(majorCurrency, minorCurrency, bidsToUpdate);
						} catch( BookNotFoundException e ) {
							e.printStackTrace(System.out);
							System.out.println("Book does not exist. Is it being set properly? Was the book fetched and set before trying to be kept in sync?");
						}
					});
				});
			}
		}
	}
	
	@Override
	public BigDecimal getPostTradingFee() {
		if( this.useBNB ) {
			return new BigDecimal("0.9995");
		} else {
			return new BigDecimal("0.999");
		}
	}
	
	@Override
	public BigDecimal getLowestAsk(String majorCurrency, String minorCurrency) throws BookNotFoundException {
		return new BigDecimal(this.asks.get(majorCurrency, minorCurrency).get(0).getPrice());
	}
	
	@Override
	public BigDecimal getHighestBid(String majorCurrency, String minorCurrency) throws BookNotFoundException {
		return new BigDecimal(this.bids.get(majorCurrency, minorCurrency).get(0).getPrice());
	}
	
	@Override
	public String makeTicker(String majorCurrency, String minorCurrency) {
		return majorCurrency.toUpperCase() + minorCurrency.toUpperCase();
	}
	
	@Override
	public OrderBookIterator getBidsIterator(String majorCurrency, String minorCurrency) throws BookNotFoundException {
		return OrderBookIterator.createBinanceIterator(this.bids.get(majorCurrency, minorCurrency));
	}

	@Override
	public OrderBookIterator getAsksIterator(String majorCurrency, String minorCurrency) throws BookNotFoundException {
		return OrderBookIterator.createBinanceIterator(this.asks.get(majorCurrency, minorCurrency));
	}
	
	@Override
	public void buy(Order order) throws NotEnoughBalanceException {
		try {
			System.out.println("Binance buying "+order.getMajorAmountToTrade()+" "+order.getMajorCurrency()+".");
			BigDecimal amountLeft = order.getMajorAmountToTrade();
			BigDecimal newMinorBalance = this.balances.get(order.getMinorCurrency());
			for( OrderBookEntry ask: asks.get(order.getMajorCurrency(), order.getMinorCurrency()) ) {
				BigDecimal amountToBuy = new BigDecimal(ask.getQty()).min(amountLeft);
				BigDecimal toSpend = amountToBuy.multiply(new BigDecimal(ask.getPrice()));
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
		System.out.println("Binance selling "+order.getMajorAmountToTrade()+" "+order.getMajorCurrency()+".");
		if( order.getMajorAmountToTrade().compareTo(this.balances.get(order.getMajorCurrency())) == 1 )
			throw new NotEnoughBalanceException();
		try {
			BigDecimal amountLeft = order.getMajorAmountToTrade();
			for( OrderBookEntry bid: bids.get(order.getMajorCurrency(), order.getMinorCurrency()) ) {
				BigDecimal toSell = (new BigDecimal(bid.getQty())).min(amountLeft);
				this.balances.put(order.getMinorCurrency(), this.balances.get(order.getMinorCurrency()).add(toSell.multiply(new BigDecimal(bid.getPrice()))));
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
		return this.quantityPrecision.get(this.makeTicker(majorCurrency, minorCurrency));
	}

	@Override
	public int getPriceDecimalPrecision(String majorCurrency, String minorCurrency) {
		return this.pricePrecision.get(this.makeTicker(majorCurrency, minorCurrency));
	}
	
	@Override
	public BigDecimal getMinimumMinorVolume(String majorCurrency, String minorCurrency) {
		return this.minMinorVolume.get(this.makeTicker(majorCurrency, minorCurrency));
	}
	
	@Override
	public BigDecimal getMinimumQuantity(String majorCurrency, String minorCurrency) {
		return this.minQuantity.get(this.makeTicker(majorCurrency, minorCurrency));
	}
}