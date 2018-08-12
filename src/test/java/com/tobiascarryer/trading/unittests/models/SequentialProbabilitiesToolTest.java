package com.tobiascarryer.trading.unittests.models;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import com.tobiascarryer.trading.models.ModelTestingResult;
import com.tobiascarryer.trading.models.sequentialprobabilities.PercentageChangeBin;
import com.tobiascarryer.trading.models.sequentialprobabilities.SequentialProbabilitiesHyperparameters;
import com.tobiascarryer.trading.models.sequentialprobabilities.SequentialProbabilitiesTool;

public class SequentialProbabilitiesToolTest {

	@Test
	public void testGetLatestBins() {
		PercentageChangeBin zero = new PercentageChangeBin(0);
    	PercentageChangeBin negOne = new PercentageChangeBin(-1);
    	PercentageChangeBin two = new PercentageChangeBin(2);
    	PercentageChangeBin negThree = new PercentageChangeBin(-3);
    	PercentageChangeBin four = new PercentageChangeBin(4);
    	PercentageChangeBin negFive = new PercentageChangeBin(-5);
    	PercentageChangeBin six = new PercentageChangeBin(6);
    	PercentageChangeBin negSeven = new PercentageChangeBin(-7);
    	
    	PercentageChangeBin[] bins = {zero, negOne, two, negThree, four, negFive, six, negSeven};
    	PercentageChangeBin[] latestBins = SequentialProbabilitiesTool.getLatestBins(bins.length-1, bins);
    	
    	assertEquals(latestBins.length, SequentialProbabilitiesHyperparameters.maxBinsInSequence);
    	
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
}
