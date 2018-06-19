package com.tobiascarryer.trading.bots;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.tobiascarryer.trading.ApiSecrets;
import com.tobiascarryer.trading.JsonReader;
import com.tobiascarryer.trading.charts.Candle;
import com.tobiascarryer.trading.charts.Candlesticks;
import com.tobiascarryer.trading.charts.CandlesticksObserver;
import com.tobiascarryer.trading.charts.ChildCandleUnifier;
import com.tobiascarryer.trading.charts.ChildCandleUnifierObserver;
import com.tobiascarryer.trading.charts.HeikinAshiCandlesticks;
import com.tobiascarryer.trading.charts.indicators.TrueRange;
import com.tobiascarryer.trading.exchanges.BitmexExchange;
import com.tobiascarryer.trading.exchanges.BitmexOrder;

import io.reactivex.disposables.Disposable;

public class HeikinAshiMomentumBotBitmex implements CandlesticksObserver, ChildCandleUnifierObserver {
	
	private TrueRange trueRangeIndicator;
	private Candle previousHeikinAshiCandle;
	
	private BitmexExchange bitmex;
	private boolean openLong = false;
	private boolean openShort = false;
	private boolean synced = false;
	private BitmexOrder lastOrder;
	
	private Disposable tickerSubscription;
	
	public static void main(String[] args) {
		int margin = 3;
		// Allows margin trades to be kept open for 7 days on x5 margin without moving the liquidation price too much.
		// It could be calculated dynamically for different order lengths and margins but this is faster and simpler.
		BigDecimal percentageToTrade = new BigDecimal("0.8");
		BitmexExchange bitmex = new BitmexExchange(ApiSecrets.bitmexTestnetKey, ApiSecrets.bitmexTestnetSecret, margin, true, new BigDecimal(4), percentageToTrade,  new BigDecimal("0.07"));
		
		HeikinAshiMomentumBotBitmex bot = new HeikinAshiMomentumBotBitmex(bitmex);
		bot.startTrading();
	}
	
	public HeikinAshiMomentumBotBitmex(BitmexExchange bitmex) {
		this.bitmex = bitmex;
	}
	
	
	public void startTrading() {
		try {
			int entryLimit = 3; // Only use current and previous, 3rd is kept just in case something is unaccounted for
			trueRangeIndicator = new TrueRange(entryLimit);
			Candlesticks candlesticks = new HeikinAshiCandlesticks(entryLimit);
			candlesticks.attachOnCandleObserver(trueRangeIndicator);
			candlesticks.attachOnCandleObserver(this);
			
			// Prepare to process historical data, the fewer format objects are created the more optimized
			TimeZone tz = TimeZone.getTimeZone("UTC");
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
			df.setTimeZone(tz);
			
			// Fetch historical data from Bitmex so the bot doesn't have to collect data before starting.
			JSONArray[] paginatedHistoricalData = paginateXBTHistoricalData(df, 5);
			
			ChildCandleUnifier unifier = null;
			
			for( int n = 0; n < paginatedHistoricalData.length; n++ ) {
				JSONArray historicalData = paginatedHistoricalData[n];
				
				// 4 hours to milliseconds = 1.44e7
				long timePerCandle = 14400000l;
				
				// Processing data starts with the first 1 hour historical candle
				// The bot starts running in the middle of a candle by offsetting the endTime
				// for the first candle to only have half the time as the other candles. This is
				// justifiable because the first candle will never be used in decision making.
				// It is only needed to calculate the following heikin ashi candles.
				Date timestampOfFirstCandle = df.parse(historicalData.getJSONObject(0).getString("timestamp"));
				long endTime = timestampOfFirstCandle.getTime() + (timePerCandle / 2l);

				unifier = new ChildCandleUnifier(timePerCandle, endTime, candlesticks);
				unifier.attachChildCandleUnifierObserver(this);
				
				// Process the historical data so there are heikin ashi candles for the bot to analyze
				for( int i = 0; i < historicalData.length(); i++ ) {
					JSONObject historicalEntry = historicalData.getJSONObject(i);
					long entryTimestamp = df.parse(historicalEntry.getString("timestamp")).getTime();
					unifier.processChildCandle(getCandleFromHistoricalData(historicalEntry), entryTimestamp);
				}
				synced = true;
			}
			
			// Start the bot
			// It is only designed to trade XBT/USD
			tickerSubscription = bitmex.startMonitoringTicker("XBT", "USD", unifier);
		} catch (JSONException | ParseException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onCandleAdded(Candle candleAdded) {
		previousHeikinAshiCandle = candleAdded;
	}
	
	@Override
	public void onPartialUnifiedCandle(BigDecimal high, BigDecimal low, BigDecimal open, BigDecimal close) {
		potentialTrade(high, low, open, close);
	}
	
	/**
	 * Executes a trade if there is an opportunity.
	 * @param high
	 * @param low
	 * @param open
	 * @param close
	 */
	public void potentialTrade(BigDecimal high, BigDecimal low, BigDecimal open, BigDecimal close) {
		if( previousHeikinAshiCandle != null && synced ) {
			// Raw ticker data is passed to the observer, convert it to heikin ashi for analysis
			Candle heikinAshi = HeikinAshiCandlesticks.convertCandleToHeikinAshi(new Candle(high, low, open, close), previousHeikinAshiCandle);
			high = heikinAshi.getHigh();
			low = heikinAshi.getLow();
			open = heikinAshi.getOpen();
			BigDecimal originalClose = close;
			close = heikinAshi.getClose();
			
			// It is assumed true range will have more than one value by the time the sync is done.
			BigDecimal rangeUp = previousHeikinAshiCandle.getClose().add(trueRangeIndicator.getTrueRanges().get(1));
			BigDecimal rangeDown = previousHeikinAshiCandle.getClose().subtract(trueRangeIndicator.getTrueRanges().get(1));
			
			// Upwards trend from HA candles is confirmed to continue
			boolean over = high.compareTo(rangeUp) == 1 && close.compareTo(open) == 1;
			
			// Downwards trend from HA candles is confirmed to continue
			boolean under = low.compareTo(rangeDown) == -1 && close.compareTo(open) == -1;
		
			// Cannot open two contradicting positions at the same time
			if( over != under ) {
				// Long condition
				if( over && !openLong ) {
					if( openShort ) {
						// Close short
						bitmex.closeShort(lastOrder.getOrderQty());
						
						// It is possible part of the short was never filled and left as a limit order
						bitmex.cancelOrder(lastOrder.getOrderId());
						
						// Remove the stop associated with the short
						bitmex.cancelOrder(lastOrder.getStopId());
						
						openShort = false;
					}
					
					// Open long
					lastOrder = bitmex.openLong(originalClose);
					openLong = true;
				}
				
				// Short condition
				if( under && !openShort ) {
					if( openLong ) {
						// Close long
						bitmex.closeLong(lastOrder.getOrderQty());
						
						// It is possible part of the long was never filled and left as a limit order
						bitmex.cancelOrder(lastOrder.getOrderId());
						
						// Remove the stop associated with the long
						bitmex.cancelOrder(lastOrder.getStopId());
						
						openLong = false;
					}
					
					// Open short
					lastOrder = bitmex.openShort(originalClose);
					openShort = true;
				}
			}
		}
	}
	
	public void stop() {
		// Close any open positions
		if( openShort )
			bitmex.closeShort(lastOrder.getOrderQty());
		if( openLong )
			bitmex.closeLong(lastOrder.getOrderQty());
		if( openShort || openLong ) {
			// It is possible part of the long was never filled and left as a limit order
			bitmex.cancelOrder(lastOrder.getOrderId());
			
			// Remove the stop associated with the long
			bitmex.cancelOrder(lastOrder.getStopId());
		}
			
		this.tickerSubscription.dispose();
	}
	
	public Candle getCandleFromHistoricalData(JSONObject historicalEntry) {
		return new Candle(historicalEntry.getBigDecimal("high"), historicalEntry.getBigDecimal("low"), historicalEntry.getBigDecimal("open"), historicalEntry.getBigDecimal("close"));
	}
	
	public JSONArray[] paginateXBTHistoricalData(DateFormat df, int numberOfPages) {
		JSONArray[] jsonPages = new JSONArray[numberOfPages];
		for( int page = 1; page <= numberOfPages; page++ ) {
			// ISO 8603 format (2018-numberOfPages-31T01:30:00.000Z)
			Calendar fourteenDaysAgo = Calendar.getInstance();
			fourteenDaysAgo.setTimeInMillis(System.currentTimeMillis());
			fourteenDaysAgo.set(Calendar.MINUTE, 0);
			fourteenDaysAgo.set(Calendar.SECOND, 0);
			fourteenDaysAgo.set(Calendar.MILLISECOND, 0);
			fourteenDaysAgo.add(Calendar.HOUR, -336*page); // Back 14 days * number of pages
			String startTime = df.format(fourteenDaysAgo.getTime());
			jsonPages[numberOfPages-page] = getXBTHistoricalData(startTime, 337);;
		}
		return jsonPages;
	}
	
	/**
	 * @param df, formatted to output dates in UTC
	 * @return Historical data sorted from oldest to newest in 1 hour candles. The most recent candle is incomplete
	 */
	public JSONArray getXBTHistoricalData(String startTime, int count) {
		// Encode reserved characters so startTime can be sent in a URL.
		// This practice is known as percent encoding.
		startTime = startTime.replaceAll(":", "%3A");
		
		String tradeHistoryUrl = "https://www.bitmex.com/api/v1/trade/bucketed?binSize=1h&partial=true&symbol=XBTUSD&columns=timestamp%2C%20open%2C%20high%2C%20low%2C%20close&count="+count+"&reverse=false&startTime="+startTime;
		
		try {
			return JsonReader.readJsonArrayFromUrl(tradeHistoryUrl);
		} catch ( IOException e) {
			e.printStackTrace();
			System.out.println("Irrecoverable. Shutting down.");
			System.exit(-1);
			return null;
		}
	}
}