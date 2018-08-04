package com.tobiascarryer.trading.models.sequentialprobabilities;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.input.ReversedLinesFileReader;

import com.tobiascarryer.trading.HelperMethods;
import com.tobiascarryer.trading.charts.Candle;
import com.tobiascarryer.trading.exchanges.AlphaVantageDataPoint;

public final class SequentialProbabilitiesPreCalculatedParameters {
	
	static String savedParametersFileNameAppend = "-precalc-params.csv";
	
	double[] posThresholds;
	double[] negThresholds;
	
	public SequentialProbabilitiesPreCalculatedParameters(double[] posThresholds, double[] negThresholds) {
		this.posThresholds = posThresholds;
		this.negThresholds = negThresholds;
	}
	
	public static void main(String[] args) throws IOException {
		calculateParameters(SequentialProbabilitiesHyperparameters.numberOfBinIntervals);
	}
	
	public static SequentialProbabilitiesPreCalculatedParameters loadFrom(File precalculatedParametersFile) throws IOException {
		BufferedReader r = new BufferedReader(new FileReader(precalculatedParametersFile));
		
		double[] posChanges = convertToDoubleArray(r.readLine().split(","));
		double[] negChanges = convertToDoubleArray(r.readLine().split(","));
		
		r.close();
		
		return new SequentialProbabilitiesPreCalculatedParameters(posChanges, negChanges);
	}
	
	private static void calculateParameters(int numberOfBinIntervals) throws IOException {
		File historicalDataFile = HelperMethods.chooseFile();
		
		// Read file backwards to get oldest data first
		final ReversedLinesFileReader r = new ReversedLinesFileReader(historicalDataFile, Charset.forName("UTF-8"));
		String line = r.readLine(); // First line is column labels
		
		Candle previousCandle = null;
		List<Double> posChanges = new ArrayList<>();
		List<Double> negChanges = new ArrayList<>();

		// There are lines left to load, and current line is not the titles
		while( line != null && !line.startsWith("timestamp")) {
			AlphaVantageDataPoint dataPoint = AlphaVantageDataPoint.parseLine(line);
			
			if( previousCandle != null ) {
				BigDecimal percentageChange = dataPoint.candle.getClose().divide(previousCandle.getClose(), RoundingMode.HALF_EVEN).subtract(new BigDecimal(1));
				double percentageChangeDouble = percentageChange.doubleValue();
				int comparison = percentageChange.compareTo(new BigDecimal(0));
				if( comparison == 1 ) {
					posChanges.add(percentageChangeDouble);
				} else if( comparison == -1 ){
					negChanges.add(percentageChangeDouble);
				}
			}
			
			previousCandle = dataPoint.candle;
			line = r.readLine();
		}
		r.close();
		
		Collections.sort(posChanges);
		Collections.sort(negChanges);
		Collections.reverse(negChanges);
		
		String fileName = precalculatedParametersFileNameFromHistoricalDataFileName(historicalDataFile);
		BufferedWriter w = new BufferedWriter(new FileWriter(fileName));
		w.write(doubleArrayToWriteableString(createThresholds(posChanges, numberOfBinIntervals))+"\n");
		w.write(doubleArrayToWriteableString(createThresholds(negChanges, numberOfBinIntervals))+"\n");
		w.close();
	}
	
	private static String doubleArrayToWriteableString(double[] array) {
		String toWrite = "";
		for( int i = 0; i < array.length; i++ ) {
			toWrite += array[i];
			if( i != array.length-1 )
				toWrite += ",";
		}
		return toWrite;
	}
	
	private static double[] createThresholds(List<Double> percentageChanges, int numberOfBinIntervals) {
		int index = percentageChanges.size() / numberOfBinIntervals;
		double[] thresholds = new double[numberOfBinIntervals-1];
		for( int i = 1; i <= thresholds.length; i++ ) {
			thresholds[i-1] = percentageChanges.get(index * i);
		}
		return thresholds;
	}
	
	private static String precalculatedParametersFileNameFromHistoricalDataFileName(File historicalDataFile) {
		return HelperMethods.getFileNameWithoutExtension(historicalDataFile) + savedParametersFileNameAppend;
	}
	
	private static double[] convertToDoubleArray(String[] stringArray) {
		double[] doubleArray = new double[stringArray.length];
		for( int i = 0; i < doubleArray.length; i++ ) {
			doubleArray[i] = Double.valueOf(stringArray[i]);
		}
		return doubleArray;
	}
}
