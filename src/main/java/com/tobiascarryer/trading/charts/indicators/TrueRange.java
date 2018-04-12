package com.tobiascarryer.trading.charts.indicators;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;

import com.tobiascarryer.trading.charts.Candle;
import com.tobiascarryer.trading.charts.Candlesticks;
import com.tobiascarryer.trading.charts.CandlesticksObserver;

public class TrueRange implements CandlesticksObserver {
	private LinkedList<BigDecimal> trs = new LinkedList<>();
	private Candle previousCandle = null;
	private int entryLimit = Integer.MAX_VALUE;
	
	public TrueRange(int entryLimit) {
		this.entryLimit = entryLimit;
	}
	
	public TrueRange(Candlesticks candlesticks, int entryLimit) {
		this.entryLimit = entryLimit;
		for( Candle candle: candlesticks.getCandles() ) {
			onCandleAdded(candle);
		}
	}
	
	@Override
	public void onCandleAdded(Candle candleAdded) {
		if( trs.size() == entryLimit )
			trs.removeLast();
		
		// tr = max(high - low, abs(high - previousClose), abs(low - previousClose))
		if( previousCandle == null ) {
			trs.addFirst(candleAdded.getHigh().subtract(candleAdded.getLow()));
		} else {
			BigDecimal method1 = candleAdded.getHigh().subtract(candleAdded.getLow());
			BigDecimal method2 = candleAdded.getHigh().subtract(previousCandle.getClose()).abs();
			BigDecimal method3 = candleAdded.getLow().subtract(previousCandle.getClose()).abs();
			trs.addFirst(method1.max(method2).max(method3));
		}
		
		previousCandle = candleAdded;
	}
	
	/**
	 * @return the true range for each candle added
	 */
	public List<BigDecimal> getTrueRanges() {
		return this.trs;
	}
}
