package com.tobiascarryer.trading.charts;

import java.util.ArrayList;
import java.util.List;

public abstract class Candlesticks {
	
	protected List<CandlesticksObserver> observers = new ArrayList<>();
	protected int maxCandles = Integer.MAX_VALUE;
	
	public Candlesticks(int maxCandles) {
		this.maxCandles = maxCandles;
	}
	
	public void attachOnCandleObserver(CandlesticksObserver observer) {
		this.observers.add(observer);
	}
	
	public abstract void addCandle(Candle candle);
	public abstract List<Candle> getCandles();
}
