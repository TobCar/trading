package com.tobiascarryer.trading.models.sequentialprobabilities;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tobiascarryer.trading.HelperMethods;
import com.tobiascarryer.trading.models.BooleanMarkovChainLink;
import com.tobiascarryer.trading.models.ModelPrediction;

public class SequentialProbabilitiesTool {
	
	private static double percentageOfDataForTraining = 0.8;
	
	public static void main(String[] args) throws IOException {
		String fileNameBase = "daily_STOCK";
		String stocksToObserveFileName = "stocks-to-observe.txt";
		String binThresholdsFileNameAppend = "bin-thresholds.csv";
		String binsFileNameAppend = "bins.txt";
		String modelFileNameAppend = "model.txt";
		
		// Select the directory where the files described above are stored.
		File parentDirectory = HelperMethods.chooseDirectory();
		
		File stocksToObserveFile = new File(parentDirectory, stocksToObserveFileName);
		List<String> stockTickers = Files.readAllLines(stocksToObserveFile.toPath());
		
		for( String ticker: stockTickers ) {
			System.out.println("SequentialProbabilitiesTool: Creating model for "+ticker);
			String historicalDataFileName = createFileName(fileNameBase, ticker, ".csv");
			String binThresholdsFileName = createFileName(fileNameBase, ticker, binThresholdsFileNameAppend);
			String binsFileName = createFileName(fileNameBase, ticker, binsFileNameAppend);
			String modelFileName = createFileName(fileNameBase, ticker, modelFileNameAppend);
			
			SequentialProbabilitiesBinThresholds.writeBinThresholdsFile(SequentialProbabilitiesHyperparameters.numberOfBinIntervals, parentDirectory, historicalDataFileName, binThresholdsFileName);
			PercentageChangeBinFile.writeBinsFile(parentDirectory, historicalDataFileName, binThresholdsFileName, binsFileName);
			generateModel(parentDirectory, modelFileName, binsFileName);
		}
	}
	
	/**
	 * @param base The start of the file name where STOCK represents where a ticker should be inserted.
	 * @param stockTicker
	 * @param append
	 * @return String
	 */
	public static String createFileName(String base, String stockTicker, String append) {
		String modifiedBase = base.replace("STOCK", stockTicker);
		if( append.startsWith(".") )
			return modifiedBase + append;
		return modifiedBase + "-" + append;
	}

	public static void generateModel(File parentDirectory, String savedModelFileName, String binsFileName) throws IOException {
    	PercentageChangeBin[] bins = PercentageChangeBinFile.loadBinsFrom(parentDirectory, binsFileName);
    	Map<BinSequence, BooleanMarkovChainLink<BinSequence>> chainLinksInTraining = new HashMap<>();
    	
    	int minLength = SequentialProbabilitiesHyperparameters.minBinsInSequence;
    	int maxLength = SequentialProbabilitiesHyperparameters.maxBinsInSequence;
    	
    	for( int i = maxLength-1; i < bins.length * percentageOfDataForTraining; i++ ) {
    		PercentageChangeBin[] latestBins = new PercentageChangeBin[maxLength];
    		int binsAddedToLatestBins = 0;
    		for( int n = i-maxLength+1; n <= i; n++ ) {
    			latestBins[latestBins.length-binsAddedToLatestBins-1] = bins[n];
    			binsAddedToLatestBins++;
    		}
    		BinSequence[] sequences = BinSequence.getSequences(latestBins, minLength, maxLength);
    		for( BinSequence sequence: sequences ) {
    			Boolean isPositiveBin = bins[i+1].isPositiveBin();
    			if( isPositiveBin != null ) {
    				BooleanMarkovChainLink<BinSequence> defaultChainLink = new BooleanMarkovChainLink<BinSequence>(sequence);
    				BooleanMarkovChainLink<BinSequence> chainLink = chainLinksInTraining.getOrDefault(sequence, defaultChainLink);
    				chainLink.increaseOccurencesFor(isPositiveBin);
    				chainLinksInTraining.put(sequence, chainLink);
    			}
    		}
    	}
    	
    	// Test model
    	int startingIndex = (int) (bins.length * percentageOfDataForTraining) - SequentialProbabilitiesHyperparameters.maxBinsInSequence + 1;
    	int numberOfBinsToTestWith = bins.length - startingIndex;
    	if( numberOfBinsToTestWith > 0 ) {
    		PercentageChangeBin[] binsToTestWith = new PercentageChangeBin[numberOfBinsToTestWith];
    		for( int i = 0; i < binsToTestWith.length; i++ ) {
    			binsToTestWith[i] = bins[startingIndex + i];
    		}
    		testModel(chainLinksInTraining, binsToTestWith);
    	}
    	
    	BufferedWriter w = new BufferedWriter(new FileWriter(new File(parentDirectory, savedModelFileName)));
    	for( BooleanMarkovChainLink<BinSequence> chainLink: chainLinksInTraining.values() ) {
    		w.write(chainLink.toString()+"\n");
    	}
     	w.close();
    }
	
	private static ModelTestingResult testModel(Map<BinSequence, BooleanMarkovChainLink<BinSequence>> chainLinks, PercentageChangeBin[] binsToTestWith) {
		SequentialProbabilitiesModel model = new SequentialProbabilitiesModel(chainLinks);
    	double rightUpwardPredictions = 0;
    	double totalUpwardPredictions = 0;
    	double rightDownwardPredictions = 0;
    	double totalDownwardPredictions = 0;
    	
    	int maxBinSequenceLength = SequentialProbabilitiesHyperparameters.maxBinsInSequence;
    	for( int i = maxBinSequenceLength-1; i < binsToTestWith.length-1; i++ ) {
    		PercentageChangeBin[] latestBins = getLatestBins(i, binsToTestWith);
    		
    		ModelPrediction<Boolean> prediction = model.predictNext(latestBins);
    		if( prediction != null && prediction.getItem() != null ) {
    			Boolean isPositiveBin = binsToTestWith[i+1].isPositiveBin();
    			if( isPositiveBin != null && isPositiveBin == true ) {
    				if( isPositiveBin == prediction.getItem() )
    					rightUpwardPredictions += 1;
    				totalUpwardPredictions += 1;
    			} else if( isPositiveBin != null && isPositiveBin == false ) {
    				if( isPositiveBin == prediction.getItem() )
    					rightDownwardPredictions += 1;
    				totalDownwardPredictions += 1;
    			}
    		}
    	}
    	
    	double upwardsAccuracy = rightUpwardPredictions/totalUpwardPredictions;
    	double downwardsAccuracy = rightDownwardPredictions/totalDownwardPredictions;
    	
    	System.out.println("Accuracy (Upwards): "+upwardsAccuracy);
    	System.out.println("Accuracy (Downwards): "+downwardsAccuracy);
    	
    	if( upwardsAccuracy > SequentialProbabilitiesHyperparameters.minimumConfidence ) {
    		return ModelTestingResult.USE_UPWARDS;
    	} else if( downwardsAccuracy > SequentialProbabilitiesHyperparameters.minimumConfidence ) {
    		return ModelTestingResult.USE_DOWNWARDS;
    	} else {
    		return ModelTestingResult.DO_NOT_USE;
    	}
	}
	
	public static PercentageChangeBin[] getLatestBins(int latestBinIndex, PercentageChangeBin[] bins) {
		int maxLength = SequentialProbabilitiesHyperparameters.maxBinsInSequence;
		
		PercentageChangeBin[] latestBins = new PercentageChangeBin[maxLength];
		int binsAddedToLatestBins = 0;
		for( int n = latestBinIndex-maxLength+1; n <= latestBinIndex; n++ ) {
			latestBins[latestBins.length-1-binsAddedToLatestBins] = bins[n];
			binsAddedToLatestBins++;
		}
		
		return latestBins;
	}
	
	private enum ModelTestingResult {
		USE_UPWARDS, USE_DOWNWARDS, DO_NOT_USE
	}
}
