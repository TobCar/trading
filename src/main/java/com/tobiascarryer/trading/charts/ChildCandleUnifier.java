package com.tobiascarryer.trading.charts;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ChildCandleUnifier {
	private BigDecimal open;
	private BigDecimal close;
	private BigDecimal high = new BigDecimal(Float.MIN_VALUE);
	private BigDecimal low = new BigDecimal(Float.MAX_VALUE);
	private long endTime;
	private long timePerCandle;
	private Candlesticks candlesticks;
	protected List<ChildCandleUnifierObserver> observers = new ArrayList<>();
	
	/**
	 * @param timePerCandle, time in milliseconds for each candle
	 * @param candlesticks, object to push unified candles
	 */
	public ChildCandleUnifier( long timePerCandle, long endTime, Candlesticks candlesticks ) {
		this.timePerCandle = timePerCandle;
		this.endTime = endTime;
		this.candlesticks = candlesticks;
	}
	
	public void processChildCandle( Candle childCandle, long timestamp ) {
		if( timestamp >= endTime ) {
			if( open != null ) {
				candlesticks.addCandle(new Candle(high, low, open, close));
				System.out.println("ChildCandleUnifier: passing unified candle to candlesticks (closing price: "+close+")");
			}
			
			// Prepare for the next parent candle
			open = close;
			high = new BigDecimal(Float.MIN_VALUE);
			low = new BigDecimal(Float.MAX_VALUE);
			endTime = timestamp + timePerCandle;
		}
	
		// First candle processed
		if( open == null )
			open = childCandle.getOpen();
		
		high = childCandle.getHigh().max(childCandle.getHigh());
		low = childCandle.getLow().min(childCandle.getLow());
		close = childCandle.getClose();
		
		for( ChildCandleUnifierObserver observer: observers )
			observer.onPartialUnifiedCandle(high, low, open, close);
	}

	public void attachChildCandleUnifierObserver(ChildCandleUnifierObserver observer) {
		this.observers.add(observer);
	}
}
