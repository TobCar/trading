package com.tobiascarryer.trading.models.sequentialprobabilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tobiascarryer.trading.models.BooleanMarkovChainLink;
import com.tobiascarryer.trading.models.ModelPrediction;

/**
 * Custom implementation of a Markov chain to find patterns based on the previous given number of data points.
 * Its predictions are binary, did the market move up or down.
 */
public class SequentialProbabilitiesModel {
	
	private Map<BinSequence, BooleanMarkovChainLink<BinSequence>> chainLinks;
	
    public SequentialProbabilitiesModel(File savedModelFile) throws IOException {
    	this.chainLinks = loadModelFrom(savedModelFile);
    }
    
    public SequentialProbabilitiesModel(Map<BinSequence, BooleanMarkovChainLink<BinSequence>> chainLinks) {
    	this.chainLinks = chainLinks;
    }
    
    public static Map<BinSequence, BooleanMarkovChainLink<BinSequence>> loadModelFrom(File file) throws IOException {
    	BufferedReader r = new BufferedReader(new FileReader(file));
    	Map<BinSequence, BooleanMarkovChainLink<BinSequence>> links = new HashMap<>(); 
    	String line = r.readLine();
    	while( line != null ) {
    		String[] split = line.split(":");
    		BinSequence sequence = BinSequence.parse(split[0]);
    		BooleanMarkovChainLink<BinSequence> newLink = new BooleanMarkovChainLink<BinSequence>(sequence);
    		newLink.setOccurencesFromString(split[1]);
    		links.put(sequence, newLink);
    		line = r.readLine();
    	}
    	r.close();
    	return links;
    }
    
    public ModelPrediction<Boolean> predictNext(PercentageChangeBin[] latestBins) {
    	int minLength = SequentialProbabilitiesHyperparameters.minBinsInSequence;
    	int maxLength = SequentialProbabilitiesHyperparameters.maxBinsInSequence;
    	BinSequence[] sequences = BinSequence.getSequences(latestBins, minLength, maxLength);
    	
    	List<ModelPrediction<Boolean>> upwardPredictions = new ArrayList<>();
    	List<ModelPrediction<Boolean>> downwardPredictions = new ArrayList<>();
    	List<Double> totalOccurencesForUpwardPredictions = new ArrayList<>();
    	List<Double> totalOccurencesForDownwardPredictions = new ArrayList<>();
    	
    	for( BinSequence sequence: sequences ) {
    		BooleanMarkovChainLink<BinSequence> chainLink = chainLinks.get(sequence);
    		if( chainLink != null ) {
	    		ModelPrediction<Boolean> prediction = chainLink.predictNextValue();
	    		if( prediction.getItem() == null ) {
	    			// Prediction was flat or not previously encountered
	    			upwardPredictions.add(new ModelPrediction<Boolean>(true, 0.5));
	    			totalOccurencesForUpwardPredictions.add(chainLink.getTotalOccurences());
	    			downwardPredictions.add(new ModelPrediction<Boolean>(false, 0.5));
	    			totalOccurencesForDownwardPredictions.add(chainLink.getTotalOccurences());
	    		} else if( prediction.getItem() ) {
	    			upwardPredictions.add(prediction);
	    			totalOccurencesForUpwardPredictions.add(chainLink.getTotalOccurences());
	    		} else if( !prediction.getItem() ) {
	    			downwardPredictions.add(prediction);
	    			totalOccurencesForDownwardPredictions.add(chainLink.getTotalOccurences());
	    		}
    		}
    	}
    	
    	double upwardProbability = getUnifiedProbability(upwardPredictions, totalOccurencesForUpwardPredictions);
    	double downwardProbability = getUnifiedProbability(downwardPredictions, totalOccurencesForDownwardPredictions);
    	
    	double sumOfTotalOccurencesForUpwardPredictions = sumOf(totalOccurencesForUpwardPredictions);
    	double sumOfTotalOccurencesForDownwardPredictions = sumOf(totalOccurencesForDownwardPredictions);
    	
    	boolean enoughOccurencesUpward = sumOfTotalOccurencesForUpwardPredictions > SequentialProbabilitiesHyperparameters.minimumTimesPatternsWereEncountered;
    	boolean enoughOccurencesDownward = sumOfTotalOccurencesForDownwardPredictions > SequentialProbabilitiesHyperparameters.minimumTimesPatternsWereEncountered;
    	
    	double minimumConfidence = SequentialProbabilitiesHyperparameters.minimumConfidence;
    	if( upwardProbability > downwardProbability && upwardProbability > minimumConfidence && enoughOccurencesUpward ) {
    		System.out.println("Predicting a rise. Confidence: "+upwardProbability+" Total times these patterns were encountered: "+sumOfTotalOccurencesForUpwardPredictions+".");
    		return new ModelPrediction<Boolean>(true, upwardProbability);
    	} else if( downwardProbability > upwardProbability && downwardProbability > minimumConfidence && enoughOccurencesDownward ) {
    		System.out.println("Predicting a drop. Confidence: "+downwardProbability+" Total times these patterns were encountered: "+sumOfTotalOccurencesForDownwardPredictions+".");
    		return new ModelPrediction<Boolean>(false, downwardProbability);
    	}
    	
    	return null;
    }
    
    public static double getUnifiedProbability(List<ModelPrediction<Boolean>> predictions, List<Double> totalOccurencesForPredictions) {
    	double totalOccurences = 0;
    	for( double occurence: totalOccurencesForPredictions ) {
    		totalOccurences += occurence;
    	}
    	double unifiedProbability = 0;
    	for( int i = 0; i < predictions.size(); i++ ) {
    		double weight = totalOccurencesForPredictions.get(i) / totalOccurences;
    		unifiedProbability += predictions.get(i).getProbability() * weight;
    	}
    	return unifiedProbability;
    }
    
    private static double sumOf(List<Double> toSum) {
    	double sum = 0;
    	for( Double d: toSum ) {
    		sum += d;
    	}
    	return sum;
    }
}
