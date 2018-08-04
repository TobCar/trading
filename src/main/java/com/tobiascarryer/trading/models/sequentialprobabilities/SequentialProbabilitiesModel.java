package com.tobiascarryer.trading.models.sequentialprobabilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import com.tobiascarryer.trading.models.BooleanMarkovChainLink;

/**
 * Custom implementation of a Markov chain to find patterns based on the previous given number of data points.
 * Its predictions are binary, did the market move up or down.
 */
public class SequentialProbabilitiesModel {
	
	private Set<BooleanMarkovChainLink<BinSequence>> chainLinks;
	
	public static void main(String[] args) throws IOException {
		
	}
	
    public SequentialProbabilitiesModel(String savedModelFileName) throws IOException {
    	File savedModelFile = new File(savedModelFileName);
    	chainLinks = loadModelFrom(savedModelFile);
    }
    
    public static Set<BooleanMarkovChainLink<BinSequence>> loadModelFrom(File file) throws IOException {
    	BufferedReader r = new BufferedReader(new FileReader(file));
    	Set<BooleanMarkovChainLink<BinSequence>> links = new HashSet<>(); 
    	String line = r.readLine();
    	while( line != null ) {
    		String[] split = line.split(":");
    		BinSequence sequence = BinSequence.parse(split[0]);
    		BooleanMarkovChainLink<BinSequence> newLink = new BooleanMarkovChainLink<BinSequence>(sequence);
    		newLink.setOccurencesFromString(split[1]);
    		links.add(newLink);
    		line = r.readLine();
    	}
    	r.close();
    	return links;
    }
}
