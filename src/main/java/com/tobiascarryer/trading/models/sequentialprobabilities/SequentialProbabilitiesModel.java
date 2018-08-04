package com.tobiascarryer.trading.models.sequentialprobabilities;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import com.tobiascarryer.trading.charts.Candle;
import com.tobiascarryer.trading.models.ModelPrediction;

/**
 * Custom implementation of a Markov chain to find patterns based on the previous given number of data points.
 * Its predictions are binary, did the market move up or down.
 */
public class SequentialProbabilitiesModel {
	
	private PercentageChangeBinFactory factory;
	private Candle previousCandle;
	private PercentageChangeBin[] latestBins = new PercentageChangeBin[SequentialProbabilitiesHyperparameters.maxBinsInSequence];
    
	public static void main(String[] args) throws IOException {
		
	}
	
    public SequentialProbabilitiesModel(String precalculatedParametersFileName) throws IOException {
    	File precalculatedParametersFile = new File(precalculatedParametersFileName);
    	SequentialProbabilitiesPreCalculatedParameters precalculatedParameters = SequentialProbabilitiesPreCalculatedParameters.loadFrom(precalculatedParametersFile);
    	double[] posChangeThresholds = precalculatedParameters.posThresholds;
    	double[] negChangeThresholds = precalculatedParameters.negThresholds;
    	factory = new PercentageChangeBinFactory(posChangeThresholds, negChangeThresholds);
    }
    
    /**
     * @param candle The latest candle.
     */
    public ModelPrediction processCandle(Candle candle) {
    	if( previousCandle != null ) {
    		processBin(factory.create(candle, previousCandle));
    	}
    	previousCandle = candle;
    }
    
    private ModelPrediction processBin(PercentageChangeBin bin) {
    	latestBins = insertLatestBin(bin, latestBins);
    	int minLength = SequentialProbabilitiesHyperparameters.minBinsInSequence;
    	int maxLength = SequentialProbabilitiesHyperparameters.maxBinsInSequence;
    	BinSequence[] sequences = getSequences(latestBins, minLength, maxLength);
    }
    
    /**
     * @param latestBins Array of bins, smaller indices are more recent.
     * @param minLength
     * @param maxLength
     * @return String array. Each index is a sequence based on a different amount of previous bins.
     */
    public static BinSequence[] getSequences(PercentageChangeBin[] latestBins, int minLength, int maxLength) {
    	BinSequence[] sequences = new BinSequence[maxLength-minLength + 1];
    	for( int i = 0; i < sequences.length; i++ ) {
    		sequences[i] = new BinSequence(Arrays.copyOfRange(latestBins, 0, i + minLength));
    	}
    	return sequences;
    }
    
    public static PercentageChangeBin[] insertLatestBin(PercentageChangeBin latestBin, PercentageChangeBin[] olderBins) {
    	for( int i = olderBins.length-1; i > 0; i-- ) {
    		olderBins[i] = olderBins[i-1];
    	}
    	olderBins[0] = latestBin;
    	return olderBins;
    }
}
