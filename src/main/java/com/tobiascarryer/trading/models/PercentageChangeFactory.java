package com.tobiascarryer.trading.models;

import java.math.BigDecimal;

import com.tobiascarryer.trading.charts.Candle;

public class PercentageChangeFactory {
		
	private double[] intervalThresholdsPosChange;
	private double[] intervalThresholdsNegChange;
	
	public static void main(String[] args) {
		// Test printing the intervals
		new PercentageChangeFactory(0.01, 0.09, 5);
	}
	
	/**
	 * It is predicted calculating the interval so an equal number of bins exist for each interval is the best approach.
	 * 
	 * @param intervalSizePosChange Percentage change per bin, when the percentage change is positive
	 * @param intervalSizeNegChange Percentage change per bin, when the percentage change is negative
	 * @param numberOfIntervals Number of percentage change bins
	 */
	public PercentageChangeFactory(double intervalSizePosChange, double intervalSizeNegChange, int numberOfIntervals) {
		this.intervalThresholdsPosChange = createIntervalThresholds(intervalSizePosChange, numberOfIntervals);
		this.intervalThresholdsNegChange = createIntervalThresholds(intervalSizeNegChange, numberOfIntervals);
		printIntervals();
	}
	
	public PercentageChangeBin create(Candle candle, Candle previousCandle) {
		double percentageChange = candle.getClose().divide(previousCandle.getClose()).subtract(new BigDecimal(1)).doubleValue();
		if( percentageChange > 0 ) {
			for( int i = 0; i < intervalThresholdsPosChange.length; i++ ) {
				if( percentageChange < intervalThresholdsPosChange[i] )
					return new PercentageChangeBin(i+1);
			}
			// Final interval is all encompassing
			return new PercentageChangeBin(intervalThresholdsPosChange.length+1);
		} else if( percentageChange < 0 ) {
			for( int i = 0; i < intervalThresholdsNegChange.length; i++ ) {
				// Absolute value allows the logic for the positive change to be reused here.
				if( Math.abs(percentageChange) < intervalThresholdsNegChange[i] )
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
		System.out.println("Just below 0% to -"+intervalThresholdsNegChange[0]+"%");
		for( int i = 0; i < intervalThresholdsNegChange.length; i++ ) {
			if( i == intervalThresholdsNegChange.length-1 )
				System.out.println("-"+intervalThresholdsNegChange[i]+"% and below");
			else
				System.out.println("-"+intervalThresholdsNegChange[i]+"% to -"+intervalThresholdsNegChange[i+1]+"%");
		}
	}
	
	private double[] createIntervalThresholds(double intervalSize, int numberOfIntervals) {
		double[] thresholds = new double[numberOfIntervals-1];
		for( int i = 0; i < thresholds.length; i++ ) {
			thresholds[i] = Math.abs(intervalSize) * (i+1);
		}
		return thresholds;
	}
}
