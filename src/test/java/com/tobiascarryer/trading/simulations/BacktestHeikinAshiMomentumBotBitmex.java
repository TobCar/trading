package com.tobiascarryer.trading.simulations;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
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

public class BacktestHeikinAshiMomentumBotBitmex implements CandlesticksObserver, ChildCandleUnifierObserver {
	
	// Backtesting variables
	private BigDecimal lastTradePrice;
	private long lastTradeTime;
	private BigDecimal balance = new BigDecimal(1);
	private int subCandlesProcessed = 0;
	private int candlesToSkip = 2;
	private long startTime;
	private long timeBetweenCandles = 3600000l;
	private static BigDecimal postTakerFee = new BigDecimal("0.99925");
	private static BigDecimal averageFundingRate = new BigDecimal(0.001); // Not a calculated number
	private static BigDecimal percentOfBalanceToTrade = new BigDecimal(0.8);
	private static long janFirst2015 = 1420070400000l; // Earliest available date in historical data
	private static long timeBetweenFundings = 28800000l; // Eight hours
	private static int margin = 3;
	private int profitableTrades = 0;
	private int numberOfTrades = 0;
	
	private TrueRange trueRangeIndicator;
	private Candle previousHeikinAshiCandle;
	
	private boolean openLong = false;
	private boolean openShort = false;
	private boolean synced = false;
	
	public static void main(String[] args) {
		BacktestHeikinAshiMomentumBotBitmex bot = new BacktestHeikinAshiMomentumBotBitmex();
		bot.startTrading();
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
			
			// 4 hours to milliseconds = 1.44e7
			long timePerCandle = 14400000l;
			
			ChildCandleUnifier unifier = null;
			
			// Historical data from the backtest's perspective
			JSONArray[] paginatedHistoricalData = paginateXBTHistoricalData(df, 5);
			
			for( int n = 0; n < paginatedHistoricalData.length; n++ ) {
				JSONArray historicalData = paginatedHistoricalData[n];
				
				// Processing data starts with the first 1 hour historical candle
				// The bot starts running in the middle of a candle by offsetting the endTime
				// for the first candle to only have half the time as the other candles. This is
				// justifiable because the first candle will never be used in decision making.
				// It is only needed to calculate the following heikin ashi candles.
				Date timestampOfFirstCandle = df.parse(historicalData.getJSONObject(0).getString("timestamp"));
				long endTime = timestampOfFirstCandle.getTime() + (timePerCandle / 2l);
				
				if( unifier == null ) {
					unifier = new ChildCandleUnifier(timePerCandle, endTime, candlesticks);
					unifier.attachChildCandleUnifierObserver(this);
				}
				
				// Process the historical data so there are heikin ashi candles for the bot to analyze
				for( int i = 0; i < historicalData.length(); i++ ) {
					JSONObject historicalEntry = historicalData.getJSONObject(i);
					long entryTimestamp = df.parse(historicalEntry.getString("timestamp")).getTime();
					unifier.processChildCandle(getCandleFromHistoricalData(historicalEntry), entryTimestamp);
				}
			}
			synced = true;
			
			// Backtest
			JSONArray[] paginatedHistoricalBacktestData = getXBTBacktestData(df);
			BigDecimal closePrice = null;
			
			long lastTime = 0;
			for( int n = 0; n < paginatedHistoricalBacktestData.length; n++ ) {
				JSONArray historicalData = paginatedHistoricalBacktestData[n];
				for( int i = 0; i < historicalData.length(); i++ ) {
					JSONObject historicalEntry = historicalData.getJSONObject(i);
					long entryTimestamp = df.parse(historicalEntry.getString("timestamp")).getTime();
					
					// Skip repeat entries
					if( lastTime > entryTimestamp ) {
						continue;
					}
					lastTime = entryTimestamp;
					
					unifier.processChildCandle(getCandleFromHistoricalData(historicalEntry), entryTimestamp);
					closePrice = historicalEntry.getBigDecimal("close");
				}
			}
			
			// Output backtest results
			System.out.println("Final balance: "+balance+" XBT ("+balance.multiply(closePrice)+" CAD)");
			System.out.println("Percent profitable: "+profitableTrades/(double)numberOfTrades);
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
		// Simulate the timestamp
		long timestamp = startTime + timeBetweenCandles * subCandlesProcessed;
		subCandlesProcessed++;
		
		if( candlesToSkip > 0 ) {
			candlesToSkip--;
		} else {
			candlesToSkip = 2;
			
			if( previousHeikinAshiCandle != null && synced ) {
				// Raw ticker data is passed to the observer, convert it to heikin ashi for analysis
				Candle heikinAshi = HeikinAshiCandlesticks.convertCandleToHeikinAshi(new Candle(high, low, open, close), previousHeikinAshiCandle);
				high = heikinAshi.getHigh();
				low = heikinAshi.getLow();
				open = heikinAshi.getOpen();
				BigDecimal originalClose = close;
				close = heikinAshi.getClose();
				
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
							closePosition(originalClose, timestamp, false);
							
							openShort = false;
						}
						
						// Open long
						System.out.println("Entering long at "+originalClose+".");
						lastTradeTime = timestamp;
						lastTradePrice = originalClose;
						openLong = true;
					}
					
					// Short condition
					if( under && !openShort ) {
						if( openLong ) {
							closePosition(originalClose, timestamp, true);
							
							openLong = false;
						}
						
						// Open short
						System.out.println("Entering short at "+originalClose+".");
						lastTradeTime = timestamp;
						lastTradePrice = originalClose;
						openShort = true;
					}
				}
			}
		}
	}
	
	public void closePosition(BigDecimal closePrice, long currentTime, boolean isLong) {
		// Taker fee on the entry
		BigDecimal tradingWith = balance.multiply(percentOfBalanceToTrade).multiply(postTakerFee);
		BigDecimal notTradingWith = balance.multiply(new BigDecimal(1).subtract(percentOfBalanceToTrade));
		
		// Price change, the trade itself
		BigDecimal priceChange = closePrice.subtract(lastTradePrice).setScale(8).divide(lastTradePrice, RoundingMode.HALF_EVEN);
		BigDecimal roi = isLong ? priceChange : priceChange.negate();
		roi = roi.multiply(new BigDecimal(margin));
		roi = roi.add(new BigDecimal(1));
		
		// Margin funding fees
		BigDecimal percentFundingToPay = new BigDecimal(0);
		for( long i = janFirst2015; i <= currentTime; i += timeBetweenFundings ) {
			if( lastTradeTime < i && currentTime >= i ) {
				percentFundingToPay = percentFundingToPay.add(averageFundingRate);
			}
		}
		
		percentFundingToPay = percentFundingToPay.multiply(new BigDecimal(margin));
		BigDecimal fundingPayed = tradingWith.multiply(percentFundingToPay);
		
		// Final balance after taker fee and other fees
		BigDecimal newBalance = tradingWith.multiply(roi).multiply(postTakerFee).subtract(fundingPayed).add(notTradingWith).setScale(8, RoundingMode.HALF_EVEN);
		BigDecimal net = newBalance.subtract(balance);
		
		numberOfTrades++;
		if( net.compareTo(new BigDecimal("0")) == 1 )
				profitableTrades++;
		
		System.out.println("Exiting at "+closePrice+". "+(currentTime-lastTradeTime)/60000+" minutes passed.");
		System.out.println("XBT Net: "+ net);
		System.out.println("USD Net: "+ net.multiply(closePrice));
		
		balance = newBalance;
	}
	
	public Candle getCandleFromHistoricalData(JSONObject historicalEntry) {
		return new Candle(historicalEntry.getBigDecimal("high"), historicalEntry.getBigDecimal("low"), historicalEntry.getBigDecimal("open"), historicalEntry.getBigDecimal("close"));
	}
	
	public JSONArray[] paginateXBTHistoricalData(DateFormat df, int numberOfPages) {
		JSONArray[] jsonPages = new JSONArray[numberOfPages];
		for( int page = 1; page <= numberOfPages; page++ ) {
			// ISO 8603 format
			Calendar fourteenDaysAgo = Calendar.getInstance();
			fourteenDaysAgo.setTimeInMillis(System.currentTimeMillis());
			fourteenDaysAgo.set(Calendar.MINUTE, 0);
			fourteenDaysAgo.set(Calendar.SECOND, 0);
			fourteenDaysAgo.set(Calendar.MILLISECOND, 0);
			fourteenDaysAgo.add(Calendar.HOUR, -336*page - 336); // Back 14 days starting from 14 days ago 
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
	
	public JSONArray[] getXBTBacktestData(DateFormat df) {
		int numberOfPages = 41;
		JSONArray[] jsonPages = new JSONArray[numberOfPages];
		for( int page = 0; page < numberOfPages; page++ ) {
			// ISO 8603 format
			Calendar timeAgo = Calendar.getInstance();
			timeAgo.setTimeInMillis(System.currentTimeMillis());
			timeAgo.set(Calendar.SECOND, 0);
			timeAgo.set(Calendar.MILLISECOND, 0);
			timeAgo.add(Calendar.MINUTE, -20160 + 500*page); // 14 days ago 
			String startTime = df.format(timeAgo.getTime());
			jsonPages[page] = getXBTHistoricalData(startTime, 500);
		}
		return jsonPages;
	}
}