package com.tobiascarryer.trading.unittests;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.TimeZone;

import com.tobiascarryer.trading.exchanges.AlphaVantageDataPoint;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for AlphaVantageDataPoint.
 */
public class AlphaVantageDataPointTest extends TestCase {
	
	/**
     * Create the test case
     * @param testName name of the test case
     */
    public AlphaVantageDataPointTest( String testName ) {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite( AlphaVantageDataPointTest.class );
    }

    /**
     * This is critical for parsing historical data.
     */
    public void testParsingLine() {
    	String line = "2018-08-02,134.5800,144.2100,132.6300,143.9200,2724368";
		AlphaVantageDataPoint dataPoint = AlphaVantageDataPoint.parseLine(line);
		
		// PDT may change depending on where the computer running this test is located
		assertTrue("Thu Aug 02 00:00:00 PDT 2018".equals(dataPoint.timestamp.toString()));
		assertTrue((new BigDecimal("134.5800")).equals((dataPoint.candle.getOpen())));
		assertTrue((new BigDecimal("144.2100")).equals(dataPoint.candle.getHigh()));
		assertTrue((new BigDecimal("132.6300")).equals(dataPoint.candle.getLow()));
		assertTrue((new BigDecimal("143.9200")).equals(dataPoint.candle.getClose()));
		assertTrue((new BigDecimal("2724368")).equals(dataPoint.volume));
    }
}
