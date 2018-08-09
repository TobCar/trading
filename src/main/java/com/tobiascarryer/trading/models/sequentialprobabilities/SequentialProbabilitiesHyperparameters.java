package com.tobiascarryer.trading.models.sequentialprobabilities;

public class SequentialProbabilitiesHyperparameters {
	public static int maxBinsInSequence = 5;
	public static int minBinsInSequence = 2;
	public static int numberOfBinIntervals = 4; // Must calculate bin thresholds again if this is changed
	public static double minimumConfidence = 0.75;
	public static double minimumTimesPatternsWereEncountered = 8;
}
