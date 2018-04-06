package com.tobiascarryer.trading.exchanges;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiWebSocketClient;
import com.binance.api.client.BinanceApiAsyncRestClient;
import com.binance.api.client.domain.market.OrderBook;
import com.binance.api.client.domain.market.OrderBookEntry;
import com.tobiascarryer.trading.JsonReader;
import com.tobiascarryer.trading.exchanges.exceptions.BookNotFoundException;
import com.tobiascarryer.trading.exchanges.exceptions.NotEnoughBalanceException;
import com.tobiascarryer.trading.exchanges.orderbook.OrderBookIterator;
import com.tobiascarryer.trading.exchanges.orderbook.OrderBooks;
import com.tobiascarryer.trading.exchanges.orders.Order;
import com.binance.api.client.domain.general.ExchangeInfo;
import com.binance.api.client.domain.general.FilterType;
import com.binance.api.client.domain.general.SymbolInfo;
import com.binance.api.client.domain.OrderSide;
import com.binance.api.client.domain.OrderType;
import com.binance.api.client.domain.TimeInForce;
import com.binance.api.client.domain.account.Account;
import com.binance.api.client.domain.account.AssetBalance;
import com.binance.api.client.domain.account.NewOrder;
import com.binance.api.client.domain.event.DepthEvent;
import com.binance.api.client.domain.event.ListenKey;
import com.binance.api.client.domain.event.UserDataUpdateEvent;
import com.binance.api.client.domain.event.UserDataUpdateEvent.UserDataUpdateEventType;

import java.io.Closeable;
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

final class BinanceExchange extends BasicExchange {
	
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
	
	// Account websocket variable, needs to be an object variable to be modifiable in an async method
	private long lastKeepAliveTime = 0l;
	
	public BinanceExchange(String key, String secret, boolean useBNB) {
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
		
		// Keep the balance in sync
		this.asyncClient.getAccount((Account account) -> {
			for( AssetBalance assetBalance: account.getBalances() ) {
				this.balances.put(assetBalance.getAsset(), new BigDecimal(assetBalance.getFree()));
			}
			
			// Use websocket to keep balances updated after the initial query.
			startUserDataStream();
		});
	}
	
	private void startUserDataStream() {
		this.asyncClient.startUserDataStream((ListenKey listenKey) -> {
			final long startTime = System.nanoTime();
			this.lastKeepAliveTime = startTime;
			Closeable ws = this.websocketClient.onUserDataUpdateEvent(listenKey.getListenKey(), (UserDataUpdateEvent update) -> {
				if( update.getEventType() == UserDataUpdateEventType.ACCOUNT_UPDATE ) {
					for( AssetBalance assetBalance: update.getAccountUpdateEvent().getBalances() ) {
						this.balances.put(assetBalance.getAsset(), new BigDecimal(assetBalance.getFree()));
					}
				}
				
				// If time passed > 30 minutes in nanoseconds
				if( (update.getEventTime() - this.lastKeepAliveTime) > 1800000000000l ) {
					this.asyncClient.keepAliveUserDataStream(listenKey.getListenKey(), (Void) -> {});
					this.lastKeepAliveTime = update.getEventTime();
				}
			});
			
			// Binance closes websocket connections after 24 hours. Wait until the connection has been alive for
			// 23.8 hours, then force a disconnect and start a new connection.
			try {
				Thread.sleep(85680000l);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			try {
				this.asyncClient.closeUserDataStream(listenKey.getListenKey(), (Void) -> {}); 
				ws.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			// Start a new connection, resetting the 24 hour counter in the process
			startUserDataStream();
		});
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
	
	private String makeQuantityForOrder(BigDecimal amount, String symbol) {
		int precision = this.quantityPrecision.get(symbol);
		return amount.setScale(precision).toPlainString();
	}
	
	private String makePriceForOrder(BigDecimal price, String symbol) {
		int precision = this.pricePrecision.get(symbol);
		return price.setScale(precision).toPlainString();
	}
	
	///                                        ///
	///    Methods requiring authentication    ///
	///                                        ///
	@Override
	public void buy(Order order) throws NotEnoughBalanceException {
		final String symbol = makeTicker(order.getMajorCurrency(), order.getMinorCurrency());
		final String quantity = makeQuantityForOrder(order.getMajorAmountToTrade(), symbol);
		final String price = makePriceForOrder(order.getPrice(), symbol);
		NewOrder binanceOrder = new NewOrder(symbol, OrderSide.BUY, OrderType.LIMIT, TimeInForce.GTC, quantity, price);
		this.asyncClient.newOrderTest(binanceOrder, (Void)->{});
	}

	@Override
	public void sell(Order order) throws NotEnoughBalanceException {
		// No need to wait until a sell order reaches the exchange to know if there is enough balance.
		if( order.getMajorAmountToTrade().compareTo(this.balances.get(order.getMajorCurrency())) == 1 )
			throw new NotEnoughBalanceException();
		final String symbol = makeTicker(order.getMajorCurrency(), order.getMinorCurrency());
		final String quantity = makeQuantityForOrder(order.getMajorAmountToTrade(), symbol);
		final String price = makePriceForOrder(order.getPrice(), symbol);
		NewOrder binanceOrder = new NewOrder(symbol, OrderSide.SELL, OrderType.LIMIT, TimeInForce.GTC, quantity, price);
		this.asyncClient.newOrderTest(binanceOrder, (Void)->{});
	}

	/* @Override
	public void withdraw(String asset, String address, BigDecimal amount) {
		// Could hardcode a map of addresses to address names if Binance begins to require an actual name.
		String addressName = "";
		System.out.println("AMOUNT TEST: " + amount.round(new MathContext(8, RoundingMode.FLOOR)).toPlainString());
		this.asyncClient.withdraw(asset, address, amount.round(new MathContext(8, RoundingMode.FLOOR)).toPlainString(), addressName, (Void callback) -> {
		});
	} */
}
