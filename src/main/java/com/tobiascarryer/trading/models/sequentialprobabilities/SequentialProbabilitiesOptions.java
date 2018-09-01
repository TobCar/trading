package com.tobiascarryer.trading.models.sequentialprobabilities;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class SequentialProbabilitiesOptions {
	public static int maxBinsInSequence = 5;
	public static int minBinsInSequence = 2;
	public static int numberOfBinIntervals = 4; // Must calculate bin thresholds again if this is changed
	public static double minimumConfidence = 0.75;
	public static double minimumTimesPatternsWereEncountered = 8;
	public static String exchangeTimeZone = "America/New_York";
	
	public static boolean isARecentDate(Date date) {
		Calendar.Builder recencyThresholdBuilder = new Calendar.Builder();
		recencyThresholdBuilder.setDate(2014, 1, 1);
		recencyThresholdBuilder.setTimeZone(TimeZone.getTimeZone(exchangeTimeZone));
		Date recencyThreshold = recencyThresholdBuilder.build().getTime();
		return recencyThreshold.compareTo(date) <= 0; // date >= recencyThreshold
	}
}
