package com.tobiascarryer.trading.unittests.models;

import static org.junit.Assert.*;
import org.junit.Test;

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
}
