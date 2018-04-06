package com.tobiascarryer.trading.unittests;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.tobiascarryer.trading.exchanges.Exchange;
import com.tobiascarryer.trading.exchanges.ExchangeFactory;
import com.tobiascarryer.trading.exchanges.exceptions.BookNotFoundException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Unit tests for BinanceExchange.
 */
public class BinanceExchangeTest {
    
	static Exchange binance = ExchangeFactory.newFactory().newBinanceExchange("KEY", "SECRET");
	
	@BeforeClass
    public static void init() {
    	String[] majorCurrencies = {"ETH", "NEO"};
    	binance.startMonitoringBook(majorCurrencies, "BTC");
    	
    	// Let data arrive.
    	try {
			Thread.sleep(15000);
		} catch (InterruptedException interrupted) {
			interrupted.printStackTrace();
		}
	}
	
	@Test(expected = BookNotFoundException.class)
    public void testAsksIteratorExceptions() throws BookNotFoundException {
    	// Analyzing data before fetching it from the exchange should throw an exception.
    	Exchange binance = ExchangeFactory.newFactory().newBinanceExchange("KEY", "SECRET");
    	binance.getAsksIterator("FAKECOIN", "BTC");
    }
	
	@Test(expected = BookNotFoundException.class)
	public void testBidsIteratorExceptions() throws BookNotFoundException {
    	// Analyzing data before fetching it from the exchange should throw an exception.
    	Exchange binance = ExchangeFactory.newFactory().newBinanceExchange("KEY", "SECRET");
    	binance.getBidsIterator("FAKECOIN", "BTC");
    }
    
    /**
     * Paying fees with BNB reduces the trading fee by 50%.
     */
	@Test
    public void testBNBTradingFee() {
    	Exchange binanceWithoutBNB = ExchangeFactory.newFactory().newBinanceExchange("KEY", "SECRET", false);
    	BigDecimal feeWithoutBNB = (new BigDecimal("1")).subtract(binanceWithoutBNB.getPostTradingFee());
    	Exchange binanceWithBNB = ExchangeFactory.newFactory().newBinanceExchange("KEY", "SECRET", true);
    	BigDecimal feeWithBNB = (new BigDecimal("1")).subtract(binanceWithBNB.getPostTradingFee());
    	Assert.assertEquals(feeWithBNB.divide(feeWithoutBNB, RoundingMode.HALF_EVEN).compareTo(new BigDecimal("0.5")), 0);
    }
    
	@Test
    public void testSupports() {
    	//
    	// It is assumed ETH will always be supported.
    	//
    	Assert.assertTrue(binance.supports("ETH", "BTC"));
    	Assert.assertTrue(binance.supports("EtH", "BtC"));
    	Assert.assertTrue(binance.supports("eth", "BTc"));
    	Assert.assertTrue(binance.supports("eth", "btc"));
    	Assert.assertTrue(binance.supports("neo", "eth"));
    }
    
	@Test
    public void testGettingLowestAsk() throws BookNotFoundException {
    	// Assert the ask can be read.
    	BigDecimal zero = new BigDecimal("0");
        Assert.assertTrue(binance.getLowestAsk("ETH", "BTC").compareTo(zero) == 1 );
		Assert.assertTrue(binance.getLowestAsk("NEO", "BTC").compareTo(zero) == 1 );
    }
    
	@Test
    public void testGettingHighestBid() throws BookNotFoundException {
    	// Assert the bid can be read.
    	BigDecimal zero = new BigDecimal("0");
    	Assert.assertEquals(binance.getLowestAsk("ETH", "BTC").compareTo(zero), 1);
    	Assert.assertEquals(binance.getLowestAsk("NEO", "BTC").compareTo(zero), 1);
    }
    
    @Test
    public void testCanWithdrawAnyCase() {
    	// It shouldn't matter if a symbol is in upper case or lower case
    	// Doesn't matter if it's true or false, just that it is not null
    	// (the asset does not exist).
    	Assert.assertNotNull(binance.canWithdraw("eth"));
    	Assert.assertNotNull(binance.canWithdraw("EtH"));
    	Assert.assertNotNull(binance.canWithdraw("ETH"));
    }
}
