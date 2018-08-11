package com.tobiascarryer.trading.models.sequentialprobabilities;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;

import com.tobiascarryer.trading.ApiSecrets;
import com.tobiascarryer.trading.HelperMethods;
import com.tobiascarryer.trading.models.BooleanMarkovChainLink;
import com.tobiascarryer.trading.models.ModelPrediction;
import com.tobiascarryer.trading.models.ModelTestingResult;

public class SequentialProbabilitiesTool {
	
	private static double percentageOfDataForTraining = 0.8;
	
	public static void main(String[] args) throws IOException {
		// Select the directory where the files described above are stored.
		File parentDirectory = HelperMethods.chooseDirectory();
		
		downloadAlphaVantageHistoricalDataForAllStocks(parentDirectory);
		createModelsAndDetermineWhatStocksToObserve(parentDirectory);
	}
	
	private static void downloadAlphaVantageHistoricalDataForAllStocks(File parentDirectory) throws IOException {
		List<String> allStocks = getAllStocks(parentDirectory);
		
		int apiCalls = 0;
		for( String ticker: allStocks) {
			try {
				// If there are problems with SSL certificates, follow this guide: https://www.alpha-vantage.community/post/getting-ssl-to-work-java-eclipse-windows-alphavantage-lets-encrypt-9783025
				URL url = new URL("https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol="+ticker+"&apikey="+ApiSecrets.alphaVantageKey+"&datatype=csv&outputsize=full");
				File historicalDataFile = new File(parentDirectory, SequentialProbabilitiesFileNames.historicalDataFileName(ticker));
				System.out.println("Downloading historical data for "+ticker);
				FileUtils.copyURLToFile(url, historicalDataFile);
			} catch (MalformedURLException e) {
				e.printStackTrace();
				System.out.println("MalformedURLException while downloading historical data for "+ticker);
			} catch (IOException e2) {
				e2.printStackTrace();
				System.out.println("IOException while downloading historical data for "+ticker);
			}
			apiCalls++;
			if( apiCalls == 4 ) {
				apiCalls = 0;
				System.out.println("Throttling API calls.");
				try {
					long oneMinute = 60000;
					Thread.sleep(oneMinute);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private static void createModelsAndDetermineWhatStocksToObserve(File parentDirectory) throws IOException {
		List<String> allStocks = getAllStocks(parentDirectory);
		Map<String, ModelTestingResult> stocksToObserve = new HashMap<>();
		
		for( String ticker: allStocks ) {
			ModelTestingResult testingResult = generateModelFromFilesIn(parentDirectory, ticker);
			if( testingResult != ModelTestingResult.DO_NOT_USE ) {
				stocksToObserve.put(ticker, testingResult);
			} else {
				// Delete useless model and data
				String historicalDataFileName = SequentialProbabilitiesFileNames.historicalDataFileName(ticker);
				File historicalDataFile = new File(parentDirectory, historicalDataFileName);
				historicalDataFile.delete();
				String binThresholdsFileName = SequentialProbabilitiesFileNames.binThresholdsFileName(ticker);
				File binThresholdsFile = new File(parentDirectory, binThresholdsFileName);
				binThresholdsFile.delete();
				String binsFileName = SequentialProbabilitiesFileNames.binsFileName(ticker);
				File binsFile = new File(parentDirectory, binsFileName);
				binsFile.delete();
				String modelFileName = SequentialProbabilitiesFileNames.savedModelFileName(ticker);
				File modelFile = new File(parentDirectory, modelFileName);
				modelFile.delete();
			}
		}
		
		writeStocksToObserve(stocksToObserve, parentDirectory);
	}
	
	private static List<String> getAllStocks(File parentDirectory) throws IOException {
		File allStocksFile = new File(parentDirectory, SequentialProbabilitiesFileNames.allStocksFileName);
		return Files.readAllLines(allStocksFile.toPath());
	}
	
	public static Map<String, ModelTestingResult> getStocksToObserve(File parentDirectory) {
		File stocksToObserveFile = new File(parentDirectory, SequentialProbabilitiesFileNames.stocksToObserveFileName);
		Map<String, ModelTestingResult> stocksToObserve = new HashMap<>();
		
		try {
			BufferedReader r = new BufferedReader(new FileReader(stocksToObserveFile));
			String line = r.readLine();
			while( line != null ) {
				String[] parts = line.split(",");
				stocksToObserve.put(parts[0], ModelTestingResult.valueOf(parts[1]));
				line = r.readLine();
			}
			r.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Could not write to "+stocksToObserveFile.getAbsolutePath());
		}
		
		return stocksToObserve;
	}
	
	private static void writeStocksToObserve(Map<String, ModelTestingResult> stocksToObserve, File parentDirectory) {
		File stocksToObserveFile = new File(parentDirectory, SequentialProbabilitiesFileNames.stocksToObserveFileName);
		try {
			BufferedWriter w = new BufferedWriter(new FileWriter(stocksToObserveFile));
			for( Entry<String, ModelTestingResult> entry: stocksToObserve.entrySet() ) {
				w.write(entry.getKey()+","+entry.getValue()+"\n");
			}
			w.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Could not write to "+stocksToObserveFile.getAbsolutePath());
		}
	}
	
	private static ModelTestingResult generateModelFromFilesIn(File parentDirectory, String ticker) throws IOException {
		System.out.println("SequentialProbabilitiesTool: Creating model for "+ticker);
		String historicalDataFileName = SequentialProbabilitiesFileNames.historicalDataFileName(ticker);
		String binThresholdsFileName = SequentialProbabilitiesFileNames.binThresholdsFileName(ticker);
		String binsFileName = SequentialProbabilitiesFileNames.binsFileName(ticker);
		String modelFileName = SequentialProbabilitiesFileNames.savedModelFileName(ticker);
		
		SequentialProbabilitiesBinThresholds.writeBinThresholdsFile(SequentialProbabilitiesHyperparameters.numberOfBinIntervals, parentDirectory, historicalDataFileName, binThresholdsFileName);
		PercentageChangeBinFile.writeBinsFile(parentDirectory, historicalDataFileName, binThresholdsFileName, binsFileName);
		return generateModel(parentDirectory, modelFileName, binsFileName);
	}

	public static ModelTestingResult generateModel(File parentDirectory, String savedModelFileName, String binsFileName) throws IOException {
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
    	ModelTestingResult testingResult = ModelTestingResult.DO_NOT_USE;
    	
    	int startingIndex = (int) (bins.length * percentageOfDataForTraining) - SequentialProbabilitiesHyperparameters.maxBinsInSequence + 1;
    	int numberOfBinsToTestWith = bins.length - startingIndex;
    	if( numberOfBinsToTestWith > 0 ) {
    		PercentageChangeBin[] binsToTestWith = new PercentageChangeBin[numberOfBinsToTestWith];
    		for( int i = 0; i < binsToTestWith.length; i++ ) {
    			binsToTestWith[i] = bins[startingIndex + i];
    		}
    		testingResult = testModel(chainLinksInTraining, binsToTestWith);
    	}
    	
    	BufferedWriter w = new BufferedWriter(new FileWriter(new File(parentDirectory, savedModelFileName)));
    	for( BooleanMarkovChainLink<BinSequence> chainLink: chainLinksInTraining.values() ) {
    		w.write(chainLink.toString()+"\n");
    	}
     	w.close();
     	
     	return testingResult;
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
}
