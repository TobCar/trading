package com.tobiascarryer.trading.unittests;

import org.junit.BeforeClass;
import org.junit.Assert;
import org.junit.Test;

import com.tobiascarryer.trading.exchanges.Exchange;
import com.tobiascarryer.trading.exchanges.ExchangeFactory;
import com.tobiascarryer.trading.exchanges.exceptions.BookNotFoundException;

import java.math.BigDecimal;

/**
 * Unit tests for QuadrigaExchange.
 */
public class QuadrigaCXExchangeTest {
	
	static Exchange quadriga = ExchangeFactory.newFactory().newQuadrigaExchange("KEY", "SECRET", "CLIENTID", false);
    
    @BeforeClass
    public static void init() {
    	String[] majorCurrencies = {"ETH", "LTC"};
    	quadriga.startMonitoringBook(majorCurrencies, "BTC");
    	
    	// Let data arrive.
    	try {
			Thread.sleep(15000);
		} catch (InterruptedException interrupted) {
			interrupted.printStackTrace();
		}
    }
    
    @Test(expected = BookNotFoundException.class)
    public void testAsksIteratorExceptions() throws BookNotFoundException {
    	// Analyzing data for a coin that does not exist should throw an exception.
    	quadriga.getAsksIterator("FAKECOIN", "BTC");
    }
      
    /**
     * Analyzing data for a coin that does not exist should throw an exception.
     */
    @Test(expected = BookNotFoundException.class)
    public void testBidsIteratorException() throws BookNotFoundException {
    	// Analyzing data for a coin that does not exist should throw an exception.
    	quadriga.getBidsIterator("FAKECOIN", "BTC");
    }
    
    @Test
    public void testSupports() {
    	//
    	// It is assumed BTC, ETH, LTC will always be supported.
    	//

    	Assert.assertTrue(quadriga.supports("BTC", "USD"));
    	Assert.assertTrue(quadriga.supports("BtC", "USd"));
    	Assert.assertTrue(quadriga.supports("btc", "usd"));
    	Assert.assertTrue(quadriga.supports("BTC", "CAD"));
    	Assert.assertTrue(quadriga.supports("BtC", "cAD"));
    	Assert.assertTrue(quadriga.supports("btc", "cad"));
    	Assert.assertTrue(quadriga.supports("ETH", "BTC"));
    	Assert.assertTrue(quadriga.supports("EtH", "BtC"));
    	Assert.assertTrue(quadriga.supports("eth", "btc"));
    	Assert.assertTrue(quadriga.supports("ETH", "CAD"));
    	Assert.assertTrue(quadriga.supports("EtH", "cAD"));
    	Assert.assertTrue(quadriga.supports("eth", "cad"));
    	Assert.assertTrue(quadriga.supports("LTC", "BTC"));
    	Assert.assertTrue(quadriga.supports("LtC", "BtC"));
    	Assert.assertTrue(quadriga.supports("ltc", "btc"));
    	Assert.assertTrue(quadriga.supports("LTC", "CAD"));
    	Assert.assertTrue(quadriga.supports("LtC", "cAD"));
    	Assert.assertTrue(quadriga.supports("ltc", "cad"));
    }
    
    @Test(expected = BookNotFoundException.class)
    public void testGettingLowestAsk() throws BookNotFoundException {
    	// Assert the ask can be read.
    	BigDecimal zero = new BigDecimal("0");
        Assert.assertEquals(quadriga.getLowestAsk("ETH", "BTC").compareTo(zero), 1 );
     // Should not read LTC, only reading first
		Assert.assertEquals(quadriga.getLowestAsk("LTC", "BTC").compareTo(zero), 1 );
    }
    
    @Test(expected = BookNotFoundException.class)
    public void testGettingHighestBid() throws BookNotFoundException {
    	// Assert the bid can be read.
    	BigDecimal zero = new BigDecimal("0");
        Assert.assertEquals(quadriga.getLowestAsk("ETH", "BTC").compareTo(zero), 1);
        // Should not read LTC, only reading first
		Assert.assertEquals(quadriga.getLowestAsk("LTC", "BTC").compareTo(zero), 1);
    }
    
    @Test
    public void testCanWithdrawAnyCase() {
    	// It shouldn't matter if a symbol is in upper case or lower case
    	// Doesn't matter if it's true or false, just that it is not null
    	// (the asset does not exist).
    	Assert.assertNotNull(quadriga.canWithdraw("eth"));
    	Assert.assertNotNull(quadriga.canWithdraw("EtH"));
    	Assert.assertNotNull(quadriga.canWithdraw("ETH"));
    	Assert.assertNotNull(quadriga.canWithdraw("ltc"));
    	Assert.assertNotNull(quadriga.canWithdraw("LtC"));
    	Assert.assertNotNull(quadriga.canWithdraw("LTC"));
    }
}
