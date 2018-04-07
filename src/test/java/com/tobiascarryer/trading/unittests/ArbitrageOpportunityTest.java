package com.tobiascarryer.trading.unittests;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

import com.tobiascarryer.trading.bots.ArbitrageOpportunity;
import com.tobiascarryer.trading.exchanges.orderbook.OrderBookEntry;
import com.tobiascarryer.trading.unittests.unittestimplementations.ExchangeUnitTestSimulation;

/**
 * Unit test for ArbitrageOpportunity.
 */
public class ArbitrageOpportunityTest extends TestCase {
    /**
     * Create the test case
     * @param testName name of the test case
     */
    public ArbitrageOpportunityTest( String testName ) {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite( ArbitrageOpportunityTest.class );
    }

    /**
     * Amount to buy and sell should be zero if the opportunity is optimized but not profitable.
     */
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

		BigDecimal targetProfit = new BigDecimal("1.001");
		BigDecimal buySellRatio = simulation1.getPostTradingFee().multiply(simulation1.getPostTradingFee()).divide(targetProfit, 8, RoundingMode.FLOOR);
		
		ArbitrageOpportunity opportunity = new ArbitrageOpportunity(simulation1, simulation2, "ETH", "BTC");
		opportunity.optimizeOrders(buySellRatio, simulation1.getBalance("BTC"), simulation2.getBalance("ETH"));
		assertTrue(opportunity.getOpportunityVolume().compareTo(new BigDecimal("0")) == 0);
		
		// Results should be the same when the exchanges are inverted because they were both created with the asks and bids.
		opportunity = new ArbitrageOpportunity(simulation2, simulation1, "ETH", "BTC");
		opportunity.optimizeOrders(buySellRatio, simulation2.getBalance("BTC"), simulation1.getBalance("ETH"));
		assertTrue(opportunity.getOpportunityVolume().compareTo(new BigDecimal("0")) == 0);
    }
    
    public void testProfitableOpportunity() {
    	OrderBookEntry[] bids1 = {new OrderBookEntry("0.985", "0.5"),
				 new OrderBookEntry("0.980", "0.75"),
		 		 new OrderBookEntry("0.975", "1"),
		 		 new OrderBookEntry("0.970", "1.25")};
    	OrderBookEntry[] bids2 = {new OrderBookEntry("1.000", "0.5"),
				 new OrderBookEntry("0.995", "0.75"),
				 new OrderBookEntry("0.990", "1"),
				 new OrderBookEntry("0.985", "1.25")};
    	OrderBookEntry[] asks1 = {new OrderBookEntry("0.989", "0.5"),
				 new OrderBookEntry("0.990", "0.75"),
		 		 new OrderBookEntry("0.995", "1"),
		 		 new OrderBookEntry("1.000", "1.25")};
    	OrderBookEntry[] asks2 = {new OrderBookEntry("1.100", "0.5"),
				 new OrderBookEntry("1.105", "0.75"),
				 new OrderBookEntry("1.110", "1"),
				 new OrderBookEntry("1.115", "1.25")};
    	
    	Map<String, BigDecimal> balances = new HashMap<String, BigDecimal>();
		balances.put("BTC", new BigDecimal("2"));
		balances.put("ETH", new BigDecimal("2"));
		ExchangeUnitTestSimulation simulation1 = new ExchangeUnitTestSimulation(asks1,bids1,balances);
		ExchangeUnitTestSimulation simulation2 = new ExchangeUnitTestSimulation(asks2,bids2,balances);

		BigDecimal targetProfit = new BigDecimal("1.001");
		BigDecimal buySellRatio = simulation1.getPostTradingFee().multiply(simulation1.getPostTradingFee()).divide(targetProfit, RoundingMode.FLOOR);
		
		// Buying on exchange1 and selling on exchange2 is profitable.
		// Therefore, buying on exchange2 and selling on exchange1 should not be profitable.
		ArbitrageOpportunity opportunity = new ArbitrageOpportunity(simulation1, simulation2, "ETH", "BTC");
		opportunity.optimizeOrders(buySellRatio, simulation1.getBalance("BTC"), simulation2.getBalance("ETH"));
		assertTrue(opportunity.getOpportunityVolume().compareTo(new BigDecimal("0")) == 1);
		
		// Results should be the same when the exchanges are inverted because they were both created with the asks and bids.
		opportunity = new ArbitrageOpportunity(simulation2, simulation1, "ETH", "BTC");
		opportunity.optimizeOrders(buySellRatio, simulation2.getBalance("BTC"), simulation1.getBalance("ETH"));
		assertTrue(opportunity.getOpportunityVolume().compareTo(new BigDecimal("0")) == 0);
    }
    
    public void testProfitableOpportunityWithSellLimit() {
    	OrderBookEntry[] bids1 = {new OrderBookEntry("0.985", "0.5"),
				 new OrderBookEntry("0.980", "0.75"),
		 		 new OrderBookEntry("0.975", "1"),
		 		 new OrderBookEntry("0.970", "1.25")};
    	OrderBookEntry[] bids2 = {new OrderBookEntry("1.000", "0.5"),
				 new OrderBookEntry("0.995", "0.75"),
				 new OrderBookEntry("0.990", "1"),
				 new OrderBookEntry("0.985", "1.25")};
   		OrderBookEntry[] asks1 = {new OrderBookEntry("0.989", "0.5"),
				 new OrderBookEntry("0.990", "0.75"),
		 		 new OrderBookEntry("0.995", "1"),
		 		 new OrderBookEntry("1.000", "1.25")};
   		OrderBookEntry[] asks2 = {new OrderBookEntry("1.100", "0.5"),
				 new OrderBookEntry("1.105", "0.75"),
				 new OrderBookEntry("1.110", "1"),
				 new OrderBookEntry("1.115", "1.25")};
    	
    	Map<String, BigDecimal> balances = new HashMap<String, BigDecimal>();
		balances.put("BTC", new BigDecimal("2"));
		balances.put("ETH", new BigDecimal("0.1"));
		ExchangeUnitTestSimulation simulation1 = new ExchangeUnitTestSimulation(asks1,bids1,balances);
		ExchangeUnitTestSimulation simulation2 = new ExchangeUnitTestSimulation(asks2,bids2,balances);

		BigDecimal targetProfit = new BigDecimal("1.001");
		BigDecimal buySellRatio = simulation1.getPostTradingFee().multiply(simulation1.getPostTradingFee()).divide(targetProfit, RoundingMode.FLOOR);
		
		// Buying on exchange1 and selling on exchange2 is profitable.
		// Therefore, buying on exchange2 and selling on exchange1 should not be profitable.
		ArbitrageOpportunity opportunity = new ArbitrageOpportunity(simulation1, simulation2, "ETH", "BTC");
		opportunity.optimizeOrders(buySellRatio, simulation1.getBalance("BTC"), simulation2.getBalance("ETH"));
		assertTrue(opportunity.getOpportunityVolume().compareTo(new BigDecimal("0")) == 1);
		
		// Results should be the same when the exchanges are inverted because they were both created with the asks and bids.
		opportunity = new ArbitrageOpportunity(simulation2, simulation1, "ETH", "BTC");
		opportunity.optimizeOrders(buySellRatio, simulation2.getBalance("BTC"), simulation1.getBalance("ETH"));
		assertTrue(opportunity.getOpportunityVolume().compareTo(new BigDecimal("0")) == 0);
    }
}
