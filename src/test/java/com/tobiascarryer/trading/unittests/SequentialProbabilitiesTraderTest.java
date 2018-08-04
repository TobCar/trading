package com.tobiascarryer.trading.unittests;

import java.util.HashSet;
import java.util.Set;

import com.tobiascarryer.trading.bots.SequentialProbabilitiesTrader;
import com.tobiascarryer.trading.models.sequentialprobabilities.BinSequence;
import com.tobiascarryer.trading.models.sequentialprobabilities.PercentageChangeBin;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for SequentialProbabilitiesModelTest.
 */
public class SequentialProbabilitiesTraderTest extends TestCase {
	
	/**
     * Create the test case
     * @param testName name of the test case
     */
    public SequentialProbabilitiesTraderTest( String testName ) {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite( SequentialProbabilitiesTraderTest.class );
    }
    
    /**
     * Getting all latest bin sequences.
     */
    public void testGetSequences() {
    	int minLength = 2;
    	int maxLength = 4;
    	
    	PercentageChangeBin zero = new PercentageChangeBin(0);
    	PercentageChangeBin negOne = new PercentageChangeBin(-1);
    	PercentageChangeBin two = new PercentageChangeBin(2);
    	PercentageChangeBin negThree = new PercentageChangeBin(-3);
    	
    	PercentageChangeBin[] bins2 = {zero, negOne};
    	PercentageChangeBin[] bins3 = {zero, negOne, two};
    	PercentageChangeBin[] bins4 = {zero, negOne, two, negThree};
    	
    	Set<BinSequence> binSequencesLeftToAssert = new HashSet<>();
    	binSequencesLeftToAssert.add(new BinSequence(bins2));
    	binSequencesLeftToAssert.add(new BinSequence(bins3));
    	binSequencesLeftToAssert.add(new BinSequence(bins4));
    	
    	BinSequence[] sequences = SequentialProbabilitiesTrader.getSequences(bins4, minLength, maxLength);
    	
    	assertEquals(sequences.length, maxLength-minLength+1);
    	
    	for( BinSequence sequence: sequences ) {
    		assertTrue(binSequencesLeftToAssert.remove(sequence));
    	}
    	
    	assertTrue(binSequencesLeftToAssert.isEmpty());
    }

    /**
     * Keeping track of the latest bins. Smaller index = more recent.
     */
    public void testInsertLatestBin() {
    	PercentageChangeBin zero = new PercentageChangeBin(0);
    	PercentageChangeBin negOne = new PercentageChangeBin(-1);
    	PercentageChangeBin two = new PercentageChangeBin(2);
    	PercentageChangeBin negThree = new PercentageChangeBin(-3);
    	PercentageChangeBin[] bins = {zero, negOne, two, negThree};
    	PercentageChangeBin latestBin = new PercentageChangeBin(5);
    	PercentageChangeBin[] newBins = SequentialProbabilitiesTrader.insertLatestBin(latestBin, bins);
    	PercentageChangeBin[] newBinsExpected = {latestBin, zero, negOne, two};
    	for( int i = 0; i < newBinsExpected.length; i++ )
    		assertEquals(newBins[i], newBinsExpected[i]);
    }
}
