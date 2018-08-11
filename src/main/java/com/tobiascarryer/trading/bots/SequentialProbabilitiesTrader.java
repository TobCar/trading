package com.tobiascarryer.trading.bots;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.TimeZone;

import org.apache.commons.io.input.ReversedLinesFileReader;

import com.tobiascarryer.trading.ApiSecrets;
import com.tobiascarryer.trading.HelperMethods;
import com.tobiascarryer.trading.charts.Candle;
import com.tobiascarryer.trading.exchanges.AlphaVantageDataPoint;
import com.tobiascarryer.trading.models.ModelPrediction;
import com.tobiascarryer.trading.models.sequentialprobabilities.PercentageChangeBin;
import com.tobiascarryer.trading.models.sequentialprobabilities.PercentageChangeBinFactory;
import com.tobiascarryer.trading.models.sequentialprobabilities.SequentialProbabilitiesFileNames;
import com.tobiascarryer.trading.models.sequentialprobabilities.SequentialProbabilitiesHyperparameters;
import com.tobiascarryer.trading.models.sequentialprobabilities.SequentialProbabilitiesModel;
import com.tobiascarryer.trading.models.sequentialprobabilities.SequentialProbabilitiesTool;
import com.tobiascarryer.trading.models.ModelTestingResult;

public class SequentialProbabilitiesTrader {
	
	private SequentialProbabilitiesModel model;
	private PercentageChangeBinFactory factory;
	private Candle previousCandle;
	private PercentageChangeBin[] latestBins = new PercentageChangeBin[SequentialProbabilitiesHyperparameters.maxBinsInSequence];
	private String tickerToObserve;
	private ModelTestingResult useInDirection;
	
	public static void main(String[] args) throws IOException {
		File parentDirectory = HelperMethods.chooseDirectory();
		Map<String, ModelTestingResult> stocksToObserve = SequentialProbabilitiesTool.getStocksToObserve(parentDirectory);
		
		int apiCalls = 0;
		for( Entry<String, ModelTestingResult> entry: stocksToObserve.entrySet() ) {
			String ticker = entry.getKey();
			ModelTestingResult useInDirection = entry.getValue();
			
			System.out.println("Trader is analyzing "+ticker);
			
			String binThresholdsFileName = SequentialProbabilitiesFileNames.binThresholdsFileName(ticker);
			File binThresholdsFile = new File(parentDirectory, binThresholdsFileName);
			File savedModelFile = new File(parentDirectory, SequentialProbabilitiesFileNames.savedModelFileName(ticker));
			
			SequentialProbabilitiesTrader trader = new SequentialProbabilitiesTrader(binThresholdsFile, savedModelFile, ticker, useInDirection);
			
			File latestBinsFile = new File(parentDirectory, SequentialProbabilitiesFileNames.latestBinsFileName(ticker));
			File previousCandleFile = new File(parentDirectory, SequentialProbabilitiesFileNames.previousCandleFileName(ticker));
			if( latestBinsFile.exists() && previousCandleFile.exists() ) {
				trader.loadLatestBinsFrom(latestBinsFile);
				trader.loadPreviousCandleFrom(previousCandleFile);
			} else {
				File binsFile = new File(parentDirectory, SequentialProbabilitiesFileNames.binsFileName(ticker));
				File historicalDataFile = new File(parentDirectory, SequentialProbabilitiesFileNames.historicalDataFileName(ticker));
				trader.latestBins = getLatestBinsFromBinsFile(binsFile, trader.latestBins.length);
				trader.previousCandle = getLastCandleFromHistoricalData(historicalDataFile);
			}
			
			trader.processLatestDailyCandleFromAlphaVantage(parentDirectory);
			trader.saveState(latestBinsFile, previousCandleFile);
			
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
	
	public SequentialProbabilitiesTrader(File binThresholdsFile, File savedModelFile, String ticker, ModelTestingResult useInDirection) throws IOException {
	    this.factory = PercentageChangeBinFactory.loadFrom(binThresholdsFile);
	    this.model = new SequentialProbabilitiesModel(savedModelFile);
	    this.tickerToObserve = ticker;
	    this.useInDirection = useInDirection;
	}
	
	public void processLatestDailyCandleFromAlphaVantage(File parentDirectory) throws IOException {
		try {
			URL alphaVantageDailyDataURL = new URL("https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol="+tickerToObserve+"&apikey="+ApiSecrets.alphaVantageKey+"&datatype=csv");
			
			URLConnection connection = alphaVantageDailyDataURL.openConnection();
			Scanner scanner = new Scanner(connection.getInputStream());
			scanner.next(); // Skip csv file titles
			AlphaVantageDataPoint dataPoint = AlphaVantageDataPoint.parseLine(scanner.next());
			scanner.close();
			handlePrediction(processCandle(dataPoint.candle), parentDirectory);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}
	
	public void handlePrediction(ModelPrediction<Boolean> prediction, File parentDirectory) throws IOException {
		if( prediction != null && prediction.getItem() != null ) {
			if( prediction.getItem() && useInDirection == ModelTestingResult.USE_UPWARDS ) {
				System.out.println("Predicting a rise in "+tickerToObserve);
			} else if( !prediction.getItem() && useInDirection == ModelTestingResult.USE_DOWNWARDS ){
				System.out.println("Pridicting a drop in "+tickerToObserve);
			}
		
			File traderLogsFile = new File(parentDirectory, SequentialProbabilitiesFileNames.traderLogsFileName);
			logPredictionIn(traderLogsFile, prediction);
		}
	}
	
	public void logPredictionIn(File traderLogsFile, ModelPrediction<Boolean> prediction) throws IOException {
		FileWriter w = new FileWriter(traderLogsFile, true); // The true will append the new data
		if( !traderLogsFile.exists() )
			w.write("Ticker,Direction,TimeWhenPredicted,PriceWhenPredicted\n");
		SimpleDateFormat torontoDF = new SimpleDateFormat("dd.MM.yyyy/kk:mmz");
		torontoDF.setTimeZone(TimeZone.getTimeZone("EDT"));
		String currentDate = torontoDF.format(new Date());
	    w.write(tickerToObserve+","+useInDirection+","+currentDate+","+previousCandle.getClose()+"\n");
	    w.close();
	}
	
	public void saveState(File latestBinsFile, File previousCandleFile) throws IOException {
		saveLatestBinsIn(latestBinsFile);
		savePreviousCandleIn(previousCandleFile);
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
    
    private static PercentageChangeBin[] getLatestBinsFromBinsFile(File binsFile, int numberOfBins) throws IOException {
    	// Read file backwards to get newest bins first
		ReversedLinesFileReader r = new ReversedLinesFileReader(binsFile, Charset.forName("UTF-8"));
		
		PercentageChangeBin[] binsFromFile = new PercentageChangeBin[SequentialProbabilitiesHyperparameters.maxBinsInSequence];
		
		String line = r.readLine();
		for( int i = 0; i < binsFromFile.length; i++ ) {
			binsFromFile[i] = new PercentageChangeBin(Integer.valueOf(line));
			line = r.readLine();
		}
		
		r.close();
		
		return binsFromFile;
    }
    
    private static Candle getLastCandleFromHistoricalData(File historicalDataFile) throws IOException {
    	BufferedReader r = new BufferedReader(new FileReader(historicalDataFile));
    	r.readLine(); // First line is the column titles
    	AlphaVantageDataPoint dataPoint = AlphaVantageDataPoint.parseLine(r.readLine());
    	r.close();
    	return dataPoint.candle;
    }
    
    private void saveLatestBinsIn(File latestBinsFile) throws IOException {
    	BufferedWriter w = new BufferedWriter(new FileWriter(latestBinsFile));
    	try {
	    	for( int i = 0; i < latestBins.length; i++ ) {
				w.write(latestBins[i] + "\n");
	    	}
    	} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Problem saving latest bins for "+tickerToObserve);
		}
    	w.close();
    }
    
    private void savePreviousCandleIn(File previousCandleFile) throws IOException {
    	BufferedWriter w = new BufferedWriter(new FileWriter(previousCandleFile));
    	try {
	    	w.write(previousCandle.toString());
    	} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Problem saving last candle for "+tickerToObserve);
		}
    	w.close();
    }
    
    private void loadLatestBinsFrom(File latestBinsFile) {
		try {
			BufferedReader r = new BufferedReader(new FileReader(latestBinsFile));
	    	for( int i = 0; i < latestBins.length; i++ ) {
				latestBins[i] = new PercentageChangeBin(Integer.valueOf(r.readLine()));
	    	}
	    	r.close();
    	} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e2) {
			e2.printStackTrace();
			System.out.println("Problem loading latest bins for "+tickerToObserve);
		}
    }
    
    private void loadPreviousCandleFrom(File previousCandleFile) {
		try {
			BufferedReader r = new BufferedReader(new FileReader(previousCandleFile));
			previousCandle = Candle.parseLine(r.readLine());
	    	r.close();
    	} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e2) {
			e2.printStackTrace();
			System.out.println("Problem loading latest bins for "+tickerToObserve);
		}
    }
}
