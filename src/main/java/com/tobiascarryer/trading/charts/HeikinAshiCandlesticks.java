package com.tobiascarryer.trading.charts;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;

public class HeikinAshiCandlesticks extends Candlesticks {

	private Candle previousHeikinAshiCandle = null;
	
	public HeikinAshiCandlesticks(int maxCandles) {
		super(maxCandles);
	}

	private List<Candle> candles = new LinkedList<Candle>();
	
	@Override
	public void addCandle(Candle candle) {
		if( this.candles.size() == maxCandles )
			this.candles.remove(this.candles.size()-1);
		Candle heikinAshiCandle = convertCandleToHeikinAshi(candle, previousHeikinAshiCandle);
		this.candles.add(0, heikinAshiCandle);
		previousHeikinAshiCandle = heikinAshiCandle;
		
		for( int i = 0; i < observers.size(); i++ )
			observers.get(i).onCandleAdded(candle);
	}

	@Override
	public List<Candle> getCandles() {
		return this.candles;
	}
	
	public static Candle convertCandleToHeikinAshi( Candle candleToConvert, Candle previousHeikinAshiCandle ) {
		BigDecimal heikinAshiClose = candleToConvert.getOpen().add(candleToConvert.getClose()).add(candleToConvert.getHigh()).add(candleToConvert.getLow()).divide(new BigDecimal(4));
		BigDecimal heikinAshiOpen;
		BigDecimal heikinAshiHigh;
		BigDecimal heikinAshiLow;
		
		if( previousHeikinAshiCandle == null ) {
			// First candle to calculate, doesn't use previous HA values
			heikinAshiOpen = candleToConvert.getOpen().add(candleToConvert.getClose()).divide(new BigDecimal(2));
			heikinAshiHigh = candleToConvert.getHigh();
			heikinAshiLow = candleToConvert.getLow();
		} else {
			heikinAshiOpen = previousHeikinAshiCandle.getOpen().add(previousHeikinAshiCandle.getClose()).divide(new BigDecimal(2));
			heikinAshiHigh = candleToConvert.getHigh().max(heikinAshiOpen).max(heikinAshiClose);
			heikinAshiLow = candleToConvert.getLow().min(heikinAshiOpen).min(heikinAshiClose);
		}
		
		return new Candle(heikinAshiHigh, heikinAshiLow, heikinAshiOpen, heikinAshiClose);
	}
}
