package com.tobiascarryer.trading.unittests.models;

import static org.junit.Assert.*;

import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.tobiascarryer.trading.models.BooleanMarkovChainLink;
import com.tobiascarryer.trading.models.ModelTestingResult;
import com.tobiascarryer.trading.models.sequentialprobabilities.BinSequence;
import com.tobiascarryer.trading.models.sequentialprobabilities.PercentageChangeBin;
import com.tobiascarryer.trading.models.sequentialprobabilities.SequentialProbabilitiesOptions;
import com.tobiascarryer.trading.models.sequentialprobabilities.SequentialProbabilitiesTool;

public class SequentialProbabilitiesToolTest {

	@Test
	public void testGetLatestBins() {
		PercentageChangeBin zero = new PercentageChangeBin(0, Calendar.MONDAY);
    	PercentageChangeBin negOne = new PercentageChangeBin(-1, Calendar.MONDAY);
    	PercentageChangeBin two = new PercentageChangeBin(2, Calendar.MONDAY);
    	PercentageChangeBin negThree = new PercentageChangeBin(-3, Calendar.MONDAY);
    	PercentageChangeBin four = new PercentageChangeBin(4, Calendar.MONDAY);
    	PercentageChangeBin negFive = new PercentageChangeBin(-5, Calendar.MONDAY);
    	PercentageChangeBin six = new PercentageChangeBin(6, Calendar.MONDAY);
    	PercentageChangeBin negSeven = new PercentageChangeBin(-7, Calendar.MONDAY);
    	
    	PercentageChangeBin[] bins = {zero, negOne, two, negThree, four, negFive, six, negSeven};
    	PercentageChangeBin[] latestBins = SequentialProbabilitiesTool.getLatestBins(bins.length-1, bins);
    	
    	assertEquals(latestBins.length, SequentialProbabilitiesOptions.maxBinsInSequence);
    	
    	for( int i = bins.length-1, n = 0; i > latestBins.length; i--, n++ ) {
    		assertTrue(latestBins[n] == bins[i]);
    	}
	}
	
	@Test
	public void testJoinModelTestingResults() {
		Set<ModelTestingResult> results = new HashSet<>();
		results.add(ModelTestingResult.USE_UPWARDS);
		results.add(ModelTestingResult.USE_DOWNWARDS);
		String joinedResults = SequentialProbabilitiesTool.joinModelTestingResults(results);
		assertEquals("USE_UPWARDS|USE_DOWNWARDS", joinedResults);
	}
	
	@Test
	public void testJoinModelTestingResultsWithOnlyOneCase() {
		Set<ModelTestingResult> results = new HashSet<>();
		results.add(ModelTestingResult.DO_NOT_USE);
		String joinedResults = SequentialProbabilitiesTool.joinModelTestingResults(results);
		assertEquals("DO_NOT_USE", joinedResults);
	}
	
	@Test
	public void testParseJoinedModelTestingResults() {
		Set<ModelTestingResult> expectedResults = new HashSet<>();
		expectedResults.add(ModelTestingResult.USE_UPWARDS);
		expectedResults.add(ModelTestingResult.USE_DOWNWARDS);
		
		Set<ModelTestingResult> results = SequentialProbabilitiesTool.parseJoinedModelTestingResults("USE_UPWARDS|USE_DOWNWARDS");
		
		assertEquals(results.size(), expectedResults.size());
		for( ModelTestingResult expected: expectedResults )
			assertTrue(results.contains(expected));
	}
	
	@Test
	public void testParseJoinedModelTestingResultsWithOnlyOneCase() {
		Set<ModelTestingResult> expectedResults = new HashSet<>();
		expectedResults.add(ModelTestingResult.DO_NOT_USE);
		
		Set<ModelTestingResult> results = SequentialProbabilitiesTool.parseJoinedModelTestingResults("DO_NOT_USE");
		
		assertEquals(results.size(), expectedResults.size());
		assertTrue(results.contains(ModelTestingResult.DO_NOT_USE));
	}
	
	@Test
	public void testIncreaseOccurencesFor() {
		PercentageChangeBin zero = new PercentageChangeBin(0, null);
    	PercentageChangeBin negOne = new PercentageChangeBin(-1, null);
    	PercentageChangeBin two = new PercentageChangeBin(2, null);
    	PercentageChangeBin negThree = new PercentageChangeBin(-3, null);
    	PercentageChangeBin[] bins = {zero, negOne, two, negThree};
		BinSequence sequence = BinSequence.getSequences(bins, 4, 4)[0];
		Boolean isPositiveBin = true;
		Map<BinSequence, BooleanMarkovChainLink<BinSequence>> chainLinksInTraining = new HashMap<BinSequence, BooleanMarkovChainLink<BinSequence>>();
		SequentialProbabilitiesTool.increaseOccurencesFor(sequence, isPositiveBin, chainLinksInTraining);
		for( BooleanMarkovChainLink<BinSequence> chainLink: chainLinksInTraining.values() ) {
			assertEquals(1, (int) chainLink.getTrueOccurences());
			assertEquals(0, (int) chainLink.getFalseOccurences());
			assertEquals(1, (int) chainLink.getTotalOccurences());
		}
		isPositiveBin = false;
		SequentialProbabilitiesTool.increaseOccurencesFor(sequence, isPositiveBin, chainLinksInTraining);
		for( BooleanMarkovChainLink<BinSequence> chainLink: chainLinksInTraining.values() ) {
			assertEquals(1, (int) chainLink.getTrueOccurences());
			assertEquals(1, (int) chainLink.getFalseOccurences());
			assertEquals(2, (int) chainLink.getTotalOccurences());
		}
	}
}
