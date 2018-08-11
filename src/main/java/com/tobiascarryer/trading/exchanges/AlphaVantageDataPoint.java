package com.tobiascarryer.trading.exchanges;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.tobiascarryer.trading.charts.Candle;

public class AlphaVantageDataPoint {
	
	public Date timestamp;
	public Candle candle;
	public BigDecimal volume;
	
	public AlphaVantageDataPoint(Date timestamp, BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close, BigDecimal volume ) {
		this.timestamp = timestamp;
		this.candle = new Candle(high, low, open, close);
		this.volume = volume;
	}
	
	public static AlphaVantageDataPoint parseLine(String line) {
		String[] columnValues = line.split(",");
		
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		
		Date timestamp = null;
		try {
			timestamp = dateFormat.parse(columnValues[0]);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		BigDecimal open = new BigDecimal(columnValues[1]);
		BigDecimal high = new BigDecimal(columnValues[2]);
		BigDecimal low = new BigDecimal(columnValues[3]);
		BigDecimal close = new BigDecimal(columnValues[4]);
		
		BigDecimal volume = new BigDecimal(-1);
		if( 5 < columnValues.length )
			volume = new BigDecimal(columnValues[5]);
		
		return new AlphaVantageDataPoint(timestamp, open, high, low, close, volume);
	}
}
