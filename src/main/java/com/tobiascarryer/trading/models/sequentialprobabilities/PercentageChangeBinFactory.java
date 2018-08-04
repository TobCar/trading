package com.tobiascarryer.trading.models.sequentialprobabilities;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.tobiascarryer.trading.charts.Candle;

public class PercentageChangeBinFactory {
		
	private double[] intervalThresholdsPosChange;
	private double[] intervalThresholdsNegChange;
	
	public static void main(String[] args) {
		// Test printing the intervals
		double[] posChangeThresholds = {0.01, 0.02, 0.03, 0.04};
		double[] negChangeThresholds = {0.09, 0.17, 0.32, 0.5};
		new PercentageChangeBinFactory(posChangeThresholds, negChangeThresholds);
	}
	
	/**
	 * It is predicted calculating the interval so an equal number of bins exist for each interval is the best approach.
	 * 
	 * @param posChangeThresholds The thresholds denoting a new positive change bin.
	 * @param negChangeThresholds The thresholds denoting a new negative change bin.
	**/
	public PercentageChangeBinFactory(double[] posChangeThresholds, double[] negChangeThresholds) {
		this.intervalThresholdsPosChange = posChangeThresholds;
		this.intervalThresholdsNegChange = negChangeThresholds;
		printIntervals();
	}
	
	public PercentageChangeBin create(Candle candle, Candle previousCandle) {
		if( previousCandle == null )
			return null;
			
		double percentageChange = candle.getClose().divide(previousCandle.getClose(), RoundingMode.HALF_EVEN).subtract(new BigDecimal(1)).doubleValue();
		if( percentageChange > 0 ) {
			for( int i = 0; i < intervalThresholdsPosChange.length; i++ ) {
				if( percentageChange < intervalThresholdsPosChange[i] )
					return new PercentageChangeBin(i+1);
			}
			// Final interval is all encompassing
			return new PercentageChangeBin(intervalThresholdsPosChange.length+1);
		} else if( percentageChange < 0 ) {
			for( int i = 0; i < intervalThresholdsNegChange.length; i++ ) {
				if( percentageChange > intervalThresholdsNegChange[i] )
					return new PercentageChangeBin(-i-1);
			}
			// Final interval is all encompassing
			return new PercentageChangeBin(-intervalThresholdsNegChange.length-1);
		}
		
		return new PercentageChangeBin(0); // % change was exactly zero
	}
	
	public void printIntervals() {
		System.out.println("In each interval the second number is not included in the range. (The intervals are exclusive.)");
		System.out.println("Positive change intervals:");
		System.out.println("Just above 0% to "+intervalThresholdsPosChange[0]+"%");
		for( int i = 0; i < intervalThresholdsPosChange.length; i++ ) {
			if( i == intervalThresholdsPosChange.length-1 )
				System.out.println(intervalThresholdsPosChange[i]+"% and above");
			else
				System.out.println(intervalThresholdsPosChange[i]+"% to "+intervalThresholdsPosChange[i+1]+"%");
		}
		
		System.out.println("Negative change intervals:");
		System.out.println("Just below 0% to "+intervalThresholdsNegChange[0]+"%");
		for( int i = 0; i < intervalThresholdsNegChange.length; i++ ) {
			if( i == intervalThresholdsNegChange.length-1 )
				System.out.println(intervalThresholdsNegChange[i]+"% and below");
			else
				System.out.println(intervalThresholdsNegChange[i]+"% to "+intervalThresholdsNegChange[i+1]+"%");
		}
	}
}
