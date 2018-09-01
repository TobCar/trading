package com.tobiascarryer.trading.unittests;

import com.tobiascarryer.trading.bots.SequentialProbabilitiesTrader;
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
     * Keeping track of the latest bins. Smaller index = more recent.
     */
    public void testInsertLatestBin() {
    	PercentageChangeBin zero = new PercentageChangeBin(0, null);
    	PercentageChangeBin negOne = new PercentageChangeBin(-1, null);
    	PercentageChangeBin two = new PercentageChangeBin(2, null);
    	PercentageChangeBin negThree = new PercentageChangeBin(-3, null);
    	PercentageChangeBin[] bins = {zero, negOne, two, negThree};
    	PercentageChangeBin latestBin = new PercentageChangeBin(5, null);
    	PercentageChangeBin[] newBins = SequentialProbabilitiesTrader.insertLatestBin(latestBin, bins);
    	PercentageChangeBin[] newBinsExpected = {latestBin, zero, negOne, two};
    	for( int i = 0; i < newBinsExpected.length; i++ )
    		assertEquals(newBins[i], newBinsExpected[i]);
    }
}
