package com.tobiascarryer.trading.bots;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import com.tobiascarryer.trading.charts.Candle;
import com.tobiascarryer.trading.models.ModelPrediction;
import com.tobiascarryer.trading.models.sequentialprobabilities.BinSequence;
import com.tobiascarryer.trading.models.sequentialprobabilities.PercentageChangeBin;
import com.tobiascarryer.trading.models.sequentialprobabilities.PercentageChangeBinFactory;
import com.tobiascarryer.trading.models.sequentialprobabilities.SequentialProbabilitiesHyperparameters;
import com.tobiascarryer.trading.models.sequentialprobabilities.SequentialProbabilitiesModel;
import com.tobiascarryer.trading.models.sequentialprobabilities.SequentialProbabilitiesPreCalculatedParameters;

public class SequentialProbabilitiesTrader {
	
	private SequentialProbabilitiesModel model;
	private PercentageChangeBinFactory factory;
	private Candle previousCandle;
	private PercentageChangeBin[] latestBins = new PercentageChangeBin[SequentialProbabilitiesHyperparameters.maxBinsInSequence];
	
	public SequentialProbabilitiesTrader(String precalculatedParametersFileName, String savedModelFileName) throws IOException {
		File precalculatedParametersFile = new File(precalculatedParametersFileName);
    	SequentialProbabilitiesPreCalculatedParameters precalculatedParameters = SequentialProbabilitiesPreCalculatedParameters.loadFrom(precalculatedParametersFile);
    	double[] posChangeThresholds = precalculatedParameters.posThresholds;
    	double[] negChangeThresholds = precalculatedParameters.negThresholds;
    	factory = new PercentageChangeBinFactory(posChangeThresholds, negChangeThresholds);
    	
    	model = new SequentialProbabilitiesModel(savedModelFileName);
	}
	
	/**
     * @param candle The latest candle.
     */
    public ModelPrediction<Boolean> processCandle(Candle candle) {
    	ModelPrediction<Boolean> prediction = new ModelPrediction<Boolean>(null, 0);
    	if( previousCandle != null ) {
    		prediction = processBin(factory.create(candle, previousCandle));
    	}
    	previousCandle = candle;
    	return prediction;
    }
    
    private ModelPrediction<Boolean> processBin(PercentageChangeBin bin) {
    	latestBins = insertLatestBin(bin, latestBins);
    	return model.predictNext(latestBins);
    }
    
    public static PercentageChangeBin[] insertLatestBin(PercentageChangeBin latestBin, PercentageChangeBin[] olderBins) {
    	for( int i = olderBins.length-1; i > 0; i-- ) {
    		olderBins[i] = olderBins[i-1];
    	}
    	olderBins[0] = latestBin;
    	return olderBins;
    }
}
