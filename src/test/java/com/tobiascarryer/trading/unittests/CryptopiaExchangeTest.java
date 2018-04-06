package com.tobiascarryer.trading.unittests;

import org.junit.BeforeClass;
import org.junit.Assert;
import org.junit.Test;

import com.tobiascarryer.trading.exchanges.Exchange;
import com.tobiascarryer.trading.exchanges.ExchangeFactory;
import com.tobiascarryer.trading.exchanges.exceptions.BookNotFoundException;

import java.math.BigDecimal;

/**
 * Unit tests for CryptopiaExchange.
 */
public class CryptopiaExchangeTest {
	
	static Exchange cryptopia = ExchangeFactory.newFactory().newCryptopiaExchange("KEY", "SECRET");
    
    @BeforeClass
    public static void init() {
    	String[] majorCurrencies = {"ETH", "NEO"};
    	cryptopia.startMonitoringBook(majorCurrencies, "BTC");
    	
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
        cryptopia.getAsksIterator("FAKECOIN", "BTC");
    }
      
    /**
     * Analyzing data for a coin that does not exist should throw an exception.
     */
    @Test(expected = BookNotFoundException.class)
    public void testBidsIteratorException() throws BookNotFoundException {
    	// Analyzing data for a coin that does not exist should throw an exception.
    	cryptopia.getBidsIterator("FAKECOIN", "BTC");
    }
    
    @Test
    public void testSupports() {
    	//
    	// It is assumed ETH will always be supported.
    	//
    	Assert.assertTrue(cryptopia.supports("ETH", "BTC"));
    	Assert.assertTrue(cryptopia.supports("EtH", "BtC"));
    	Assert.assertTrue(cryptopia.supports("eth", "btc"));
    	Assert.assertTrue(cryptopia.supports("ETH", "USDT"));
    	Assert.assertTrue(cryptopia.supports("EtH", "usDt"));
    	Assert.assertTrue(cryptopia.supports("eth", "usdt"));
    	Assert.assertTrue(cryptopia.supports("ETH", "NZDT"));
    	Assert.assertTrue(cryptopia.supports("EtH", "nzDt"));
    	Assert.assertTrue(cryptopia.supports("eth", "nzdt"));
    }
    
    @Test
    public void testGettingLowestAsk() {
    	// Assert the ask can be read.
    	BigDecimal zero = new BigDecimal("0");
        try {
			Assert.assertTrue(cryptopia.getLowestAsk("ETH", "BTC").compareTo(zero) == 1 );
		} catch (BookNotFoundException e) {
			System.out.println(e);
			System.out.println("Did not expect BookNotFoundException");
			Assert.assertTrue(false);
		}
        try {
        	Assert.assertTrue(cryptopia.getLowestAsk("NEO", "BTC").compareTo(zero) == 1 );
		} catch (BookNotFoundException e) {
			System.out.println(e);
			System.out.println("Did not expect BookNotFoundException");
			Assert.assertTrue(false);
		}
    }
    
    @Test
    public void testGettingHighestBid() {
    	// Assert the bid can be read.
    	BigDecimal zero = new BigDecimal("0");
        try {
        	Assert.assertTrue(cryptopia.getLowestAsk("ETH", "BTC").compareTo(zero) == 1 );
		} catch (BookNotFoundException e) {
			System.out.println(e);
			System.out.println("Did not expect BookNotFoundException");
			Assert.assertTrue(false);
		}
        try {
        	Assert.assertTrue(cryptopia.getLowestAsk("NEO", "BTC").compareTo(zero) == 1 );
		} catch (BookNotFoundException e) {
			System.out.println(e);
			System.out.println("Did not expect BookNotFoundException");
			Assert.assertTrue(false);
		}
    }
    
    @Test
    public void testCanWithdrawAnyCase() {
    	// It shouldn't matter if a symbol is in upper case or lower case
    	// Doesn't matter if it's true or false, just that it is not null
    	// (the asset does not exist).
    	Assert.assertNotNull(cryptopia.canWithdraw("eth"));
    	Assert.assertNotNull(cryptopia.canWithdraw("EtH"));
    	Assert.assertNotNull(cryptopia.canWithdraw("ETH"));
    }
}
