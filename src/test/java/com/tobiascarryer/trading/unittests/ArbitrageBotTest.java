package com.tobiascarryer.trading.unittests;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import com.tobiascarryer.trading.bots.ArbitrageBot;
import com.tobiascarryer.trading.exchanges.Exchange;
import com.tobiascarryer.trading.exchanges.orderbook.OrderBookEntry;
import com.tobiascarryer.trading.unittests.unittestimplementations.ExchangeUnitTestSimulation;

/**
 * Unit tests for ArbitrageBot.
 */
public class ArbitrageBotTest extends TestCase {
    /**
     * Create the test case
     * @param testName name of the test case
     */
    public ArbitrageBotTest( String testName ) {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite( ArbitrageBotTest.class );
    }

    public void testUnprofitableOpportunity() {
    	OrderBookEntry[] asks = {new OrderBookEntry("1.000", "0.5"),
				 new OrderBookEntry("1.005", "0.75"),
				 new OrderBookEntry("1.010", "1"),
				 new OrderBookEntry("1.015", "1.25")};
    	OrderBookEntry[] bids = {new OrderBookEntry("0.995", "0.5"),
				 new OrderBookEntry("0.990", "0.75"),
		 		 new OrderBookEntry("0.985", "1"),
		 		 new OrderBookEntry("0.980", "1.25")};
    	Map<String, BigDecimal> balances = new HashMap<String, BigDecimal>();
		balances.put("BTC", new BigDecimal("2"));
		balances.put("ETH", new BigDecimal("2"));
		ExchangeUnitTestSimulation simulation1 = new ExchangeUnitTestSimulation(asks,bids,balances);
		ExchangeUnitTestSimulation simulation2 = new ExchangeUnitTestSimulation(asks,bids,balances);
		
		// NEO will not be arbitraged, simulation exchanges "don't support it"
		String[] majorCurrenciesToTrade = {"ETH", "NEO"};
		Exchange[] exchanges = {simulation1, simulation2};
		
		BigDecimal originalMajorBalance1 = simulation1.getBalance("ETH");
		BigDecimal originalMinorBalance1 = simulation1.getBalance("BTC");
		BigDecimal originalMajorBalance2 = simulation2.getBalance("ETH");
		BigDecimal originalMinorBalance2 = simulation2.getBalance("BTC");
		ArbitrageBot bot = new ArbitrageBot("BTC", new BigDecimal("1.005"));
		bot.startArbitraging(majorCurrenciesToTrade, exchanges);
		
		// Let an arbitrage occur.
		try {
			Thread.sleep(1000);
		} catch (InterruptedException interrupted) {
			interrupted.printStackTrace();
		}
		bot.stop();
		
		// Closest the test can get to asserting no orders are sent is to test that the balances didn't change.
		assertTrue(originalMajorBalance1.compareTo(simulation1.getBalance("ETH")) == 0);
		assertTrue(originalMinorBalance1.compareTo(simulation1.getBalance("BTC")) == 0);
		assertTrue(originalMajorBalance2.compareTo(simulation2.getBalance("ETH")) == 0);
		assertTrue(originalMinorBalance2.compareTo(simulation2.getBalance("BTC")) == 0);
		
		// Test limiting trade volume by percentage
		bot.setPercentageToTrade(new BigDecimal("0.5"), new BigDecimal("0.5"));
		
		// Reset the simulation exchanges
		simulation1 = new ExchangeUnitTestSimulation(asks,bids,balances);
		simulation2 = new ExchangeUnitTestSimulation(asks,bids,balances);
		
		bot.startArbitraging(majorCurrenciesToTrade, exchanges);
		
		// Let an arbitrage occur.
		try {
			Thread.sleep(1000);
		} catch (InterruptedException interrupted) {
			interrupted.printStackTrace();
		}
		bot.stop();
		
		// Closest the test can get to asserting no orders are sent is to test that the balances didn't change.
		assertTrue(originalMajorBalance1.compareTo(simulation1.getBalance("ETH")) == 0);
		assertTrue(originalMinorBalance1.compareTo(simulation1.getBalance("BTC")) == 0);
		assertTrue(originalMajorBalance2.compareTo(simulation2.getBalance("ETH")) == 0);
		assertTrue(originalMinorBalance2.compareTo(simulation2.getBalance("BTC")) == 0);
    }
    
    public void testProfitableOpportunity() {
    	OrderBookEntry[] bids1 = {new OrderBookEntry("0.979", "0.5"),
				 new OrderBookEntry("0.976", "0.75"),
		 		 new OrderBookEntry("0.975", "1"),
		 		 new OrderBookEntry("0.970", "1.25")};
	   	OrderBookEntry[] bids2 = {new OrderBookEntry("1.000", "0.5"),
					 new OrderBookEntry("0.995", "0.75"),
					 new OrderBookEntry("0.990", "1"),
					 new OrderBookEntry("0.985", "1.25")};
	   	OrderBookEntry[] asks1 = {new OrderBookEntry("0.980", "0.5"),
					 new OrderBookEntry("0.985", "0.75"),
			 		 new OrderBookEntry("0.990", "1"),
			 		 new OrderBookEntry("0.995", "1.25")};
	   	OrderBookEntry[] asks2 = {new OrderBookEntry("1.100", "0.5"),
					 new OrderBookEntry("1.105", "0.75"),
					 new OrderBookEntry("1.110", "1"),
					 new OrderBookEntry("1.115", "1.25")};
	   	// Need two balance objects because the exchanges use the same balances object if the same balances
	   	// object is passed to the constructor.
	   	Map<String, BigDecimal> balances1 = new HashMap<>();
		balances1.put("BTC", new BigDecimal("2"));
		balances1.put("ETH", new BigDecimal("2"));
		Map<String, BigDecimal> balances2 = new HashMap<>();
		balances2.put("BTC", new BigDecimal("2"));
		balances2.put("ETH", new BigDecimal("2"));
		ExchangeUnitTestSimulation simulation1 = new ExchangeUnitTestSimulation(asks1,bids1,balances1);
		ExchangeUnitTestSimulation simulation2 = new ExchangeUnitTestSimulation(asks2,bids2,balances2);
		
		String[] majorCurrenciesToTrade = {"ETH", "NEO"}; // NEO will not be arbitraged, simulation exchanges "don't support it"
		Exchange[] exchanges = {simulation1, simulation2};
		
		BigDecimal originalMajorBalance1 = simulation1.getBalance("ETH");
		BigDecimal originalMinorBalance1 = simulation1.getBalance("BTC");
		BigDecimal originalMajorBalance2 = simulation2.getBalance("ETH");
		BigDecimal originalMinorBalance2 = simulation2.getBalance("BTC");
		ArbitrageBot bot = new ArbitrageBot("BTC", new BigDecimal("1.005"));
		bot.startArbitraging(majorCurrenciesToTrade, exchanges);
		
		// Let an arbitrage occur.
		try {
			Thread.sleep(100);
		} catch (InterruptedException interrupted) {
			interrupted.printStackTrace();
		}
		bot.stop();
		
		// Record the net to compare it to the net when only a percentage of the balance is traded.
		BigDecimal netMajor1 = simulation1.getBalance("ETH").subtract(originalMajorBalance1);
		BigDecimal netMinor1 = simulation1.getBalance("BTC").subtract(originalMinorBalance1);
		BigDecimal netMajor2 = simulation2.getBalance("ETH").subtract(originalMajorBalance2);
		BigDecimal netMinor2 = simulation2.getBalance("BTC").subtract(originalMinorBalance2);
		
		// Assert exchange 1 bought and exchange 2 sold, this is the right behaviour given the test data
		assertTrue(netMajor1.compareTo(new BigDecimal("0")) == 1);
		assertTrue(netMinor1.compareTo(new BigDecimal("0")) == -1);
		assertTrue(netMajor2.compareTo(new BigDecimal("0")) == -1);
		assertTrue(netMinor2.compareTo(new BigDecimal("0")) == 1);
		
		// Assert there was profit
		assertTrue(netMajor1.add(netMajor2).compareTo(new BigDecimal("0")) == 1);
		assertTrue(netMinor1.add(netMinor2).compareTo(new BigDecimal("0")) == 1);
	}
}
