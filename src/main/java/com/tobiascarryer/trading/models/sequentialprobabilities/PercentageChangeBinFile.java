package com.tobiascarryer.trading.models.sequentialprobabilities;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.input.ReversedLinesFileReader;

import com.tobiascarryer.trading.HelperMethods;
import com.tobiascarryer.trading.charts.Candle;
import com.tobiascarryer.trading.exchanges.AlphaVantageDataPoint;

public class PercentageChangeBinFile {
	
	final static String binsFileNameAppend = "-bins.txt";
	
	public static void main(String[] args) throws IOException {
		// Prefix common to all relevant files.
		String historicalDataName = "daily_SHOP.csv";
		String precalculatedParametersFileName = "daily_SHOP-precalc-params.csv";
		createBinFile(historicalDataName, precalculatedParametersFileName);
	}
	
	public static PercentageChangeBin[] loadBinsFrom(String fileName) throws IOException {
		File binsFile = new File(fileName);
		BufferedReader r = new BufferedReader(new FileReader(binsFile));
		String line = r.readLine();
		List<PercentageChangeBin> bins = new ArrayList<>();
		while( line != null ) {
			bins.add(new PercentageChangeBin(Integer.parseInt(line)));
			line = r.readLine();
		}
		r.close();
		
		PercentageChangeBin[] binsArray = new PercentageChangeBin[bins.size()];
		for( int i = 0; i < bins.size(); i++ ) {
			binsArray[i] = bins.get(i);
		}
		
		return binsArray;
	}
	
	public static void createBinFile(String historicalDataName, String precalculatedParametersFileName) throws IOException {
		File historicalDataFile = new File(historicalDataName);
		File precalculatedParametersFile = new File(precalculatedParametersFileName);
		SequentialProbabilitiesPreCalculatedParameters precalculatedParameters = SequentialProbabilitiesPreCalculatedParameters.loadFrom(precalculatedParametersFile);
		
		// Read file backwards to get oldest data first
		final ReversedLinesFileReader r = new ReversedLinesFileReader(historicalDataFile, Charset.forName("UTF-8"));
		String line = r.readLine(); // First line is column labels
		
		Candle previousCandle = null;
		double[] posThresholds = precalculatedParameters.posThresholds;
		double[] negThresholds = precalculatedParameters.negThresholds;
		PercentageChangeBinFactory binFactory = new PercentageChangeBinFactory(posThresholds, negThresholds);
		
		String fileName = HelperMethods.getFileNameWithoutExtension(historicalDataFile) + binsFileNameAppend;
		BufferedWriter w = new BufferedWriter(new FileWriter(fileName));
		
		// There are lines left to load, and current line is not the titles
		while( line != null && !line.startsWith("timestamp")) {
			AlphaVantageDataPoint dataPoint = AlphaVantageDataPoint.parseLine(line);
			PercentageChangeBin bin = binFactory.create(dataPoint.candle, previousCandle);
			if( bin != null )
				w.write(bin.toString()+"\n");
			previousCandle = dataPoint.candle;
			
			line = r.readLine();
		}
		r.close();
		w.close();
	}
}
