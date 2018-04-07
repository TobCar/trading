package com.tobiascarryer.trading.bots;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.tobiascarryer.trading.exchanges.Exchange;
import com.tobiascarryer.trading.exchanges.exceptions.BookNotFoundException;
import com.tobiascarryer.trading.exchanges.exceptions.NotEnoughBalanceException;
import com.tobiascarryer.trading.exchanges.orderbook.OrderBookEntry;
import com.tobiascarryer.trading.exchanges.orderbook.OrderBookIterator;
import com.tobiascarryer.trading.exchanges.orders.Order;
import com.tobiascarryer.trading.exchanges.orders.OrderType;

public class ArbitrageOpportunity {
	
	private Order buyOrder, sellOrder;
	private Exchange exchangeToBuyOn, exchangeToSellOn;
	private String majorCurrency, minorCurrency;
	private int majorScale, minorScale;
	
	public ArbitrageOpportunity(Exchange exchangeToBuyOn, Exchange exchangeToSellOn, String majorCurrency, String minorCurrency) {
		this.buyOrder = new Order(OrderType.BUY, new BigDecimal("0"), new BigDecimal("0"), majorCurrency, minorCurrency);
		this.sellOrder = new Order(OrderType.SELL, new BigDecimal("0"), new BigDecimal("0"), majorCurrency, minorCurrency);
		this.exchangeToBuyOn = exchangeToBuyOn;
		this.exchangeToSellOn = exchangeToSellOn;
		this.majorCurrency = majorCurrency;
		this.minorCurrency = minorCurrency;
		this.majorScale = Math.min(exchangeToBuyOn.getQuantityDecimalPrecision(majorCurrency, minorCurrency), exchangeToSellOn.getQuantityDecimalPrecision(majorCurrency, minorCurrency));
		this.minorScale = Math.min(exchangeToBuyOn.getQuantityDecimalPrecision(majorCurrency, minorCurrency), exchangeToSellOn.getQuantityDecimalPrecision(majorCurrency, minorCurrency));
	}
	
	/**
	 * Post: buyOrder and sellOrder have set their volume and price to maximize profit when the trades are executed.
	 * @param buySellRatio, buy/sell ratios <= to buySellRatio are considered to be profitable.
	 * @param maxBuyVolume, the maximum volume in the buy-exchange measured in the minor currency
	 * @param maxSellVolume, the maximum volume in the sell-exchange measured in the major currency
	 */
	public void optimizeOrders(BigDecimal buySellRatio, BigDecimal maxBuyVolume, BigDecimal maxSellVolume) {
		/*
		 * While there is currency remaining:
		 * 	Get the side with the smaller order
		 * 	Subtract the smaller order's volume from the volume of each order if the volume is < volume remaining. Otherwise, subtract volume remaining
		 * 	Get the next order from the side with zero volume left (the side with the smaller volume)
		 * 	Recalculate the average buy and sell prices
		 * 	Check if the buy/sell ratio is still profitable
		 * 		If not: stop and revert back to the average buy and sell prices before the recalculation
		 * 		After reverting, commit the prices and volume and stop the method
		 * 
		 * There is a small rounding error involved since all values are rounded down throughout the algorithm to
		 * ensure the right number of decimal places the exchanges demand. Volume will be slightly below the true
		 * maximum arbitrage volume.
		 */
		OrderBookIterator asksIterator;
		OrderBookIterator bidsIterator;
		try {
			asksIterator = exchangeToBuyOn.getAsksIterator(majorCurrency, minorCurrency);
			bidsIterator = exchangeToSellOn.getBidsIterator(majorCurrency, minorCurrency);
		} catch (BookNotFoundException e) {
			e.printStackTrace();
			System.out.println("No order book data to optimize with. Orders are being set to zero volume.");
			this.buyOrder.setMajorAmountToTrade(new BigDecimal("0"));
			this.sellOrder.setMajorAmountToTrade(new BigDecimal("0"));
			return;
		}
		
		// One end case for optimization is when all the minor or major currency is used.
		BigDecimal minorCurrencyRemaining = maxBuyVolume.setScale(this.minorScale, RoundingMode.FLOOR);
		BigDecimal majorCurrencyRemaining = maxSellVolume.setScale(this.majorScale, RoundingMode.FLOOR);
		
		// Track the price that will be committed to the Order objects
		BigDecimal biggestBuyPrice = new BigDecimal("0");
		BigDecimal smallestSellPrice = new BigDecimal("1000");
		
		// Track how much of the major currency is bought to commit it to the Order object
		BigDecimal majorCurrencyBought = new BigDecimal("0");
		
		OrderBookEntry nextBiggestAsk = asksIterator.next();
		OrderBookEntry nextSmallestBid = bidsIterator.next();
		while( minorCurrencyRemaining.compareTo(new BigDecimal("0")) == 1 && majorCurrencyRemaining.compareTo(new BigDecimal("0")) == 1 ) {
			// Get the price before it changes when the next bid/ask is fetched.
			BigDecimal buyPrice = nextBiggestAsk.getPrice();
			BigDecimal sellPrice = nextSmallestBid.getPrice();
			
			// Check if the trade is profitable
			if( buyPrice.divide(sellPrice, 8, RoundingMode.HALF_EVEN).compareTo(buySellRatio) != 1 ) {
				biggestBuyPrice = buyPrice;
				smallestSellPrice = sellPrice;
				
				// Get the volume that can be traded by filling an order or using all remaining currency.
				// If they are equal, the next iteration in the loop will remove the OrderBookEntry that is left with zero volume.
				BigDecimal volumeToReduceBy; // Volume last traded
				if( nextBiggestAsk.getMinorCurrencyAmount().compareTo(nextSmallestBid.getMinorCurrencyAmount()) == 1 ) {
					// Fill the smallest order in full or use up all the remaining volume.
					BigDecimal remainingMajorCurrencyValue = majorCurrencyRemaining.multiply(nextBiggestAsk.getPrice());
					volumeToReduceBy = nextSmallestBid.getMinorCurrencyAmount().min(minorCurrencyRemaining).min(remainingMajorCurrencyValue);
					majorCurrencyBought = majorCurrencyBought.add(volumeToReduceBy.divide(nextBiggestAsk.getPrice(), 8, RoundingMode.HALF_EVEN).setScale(this.majorScale, RoundingMode.FLOOR));
					majorCurrencyRemaining = majorCurrencyRemaining.subtract(volumeToReduceBy.divide(nextSmallestBid.getPrice(), 8, RoundingMode.HALF_EVEN)).setScale(this.majorScale, RoundingMode.FLOOR);
					nextBiggestAsk.setMinorCurrencyAmount(nextBiggestAsk.getMinorCurrencyAmount().subtract(volumeToReduceBy));
					nextSmallestBid = bidsIterator.next();
				} else {
					// Fill the smallest order in full or use up all the remaining volume.
					BigDecimal remainingMajorCurrencyValue = majorCurrencyRemaining.multiply(nextBiggestAsk.getPrice());
					volumeToReduceBy = nextBiggestAsk.getMinorCurrencyAmount().min(minorCurrencyRemaining).min(remainingMajorCurrencyValue);
					majorCurrencyBought = majorCurrencyBought.add(volumeToReduceBy.divide(nextBiggestAsk.getPrice(), 8, RoundingMode.HALF_EVEN).setScale(this.majorScale, RoundingMode.FLOOR));
					majorCurrencyRemaining = majorCurrencyRemaining.subtract(volumeToReduceBy.divide(nextSmallestBid.getPrice(), 8, RoundingMode.HALF_EVEN)).setScale(this.majorScale, RoundingMode.FLOOR);
					nextSmallestBid.setMinorCurrencyAmount(nextSmallestBid.getMinorCurrencyAmount().subtract(volumeToReduceBy));
					nextBiggestAsk = asksIterator.next();
				}
				
				// Keep track of how much of the minor currency was used so it can be committed to the Order objects later.
				minorCurrencyRemaining = minorCurrencyRemaining.subtract(volumeToReduceBy).setScale(this.minorScale, RoundingMode.FLOOR);
			} else {
				break; // The optimal volume and prices have been determined.
			}
		}
		
		// Commit the results to the internal Order objects to be executed.
		this.buyOrder.setPrice(biggestBuyPrice);
		this.buyOrder.setMajorAmountToTrade(majorCurrencyBought);
		this.sellOrder.setPrice(smallestSellPrice);
		this.sellOrder.setMajorAmountToTrade(maxSellVolume.subtract(majorCurrencyRemaining));
	}
	
	/**
	 * @return The minor currency volume of each Order.
	 */
	public BigDecimal getOpportunityVolume() {
		return this.buyOrder.getMajorAmountToTrade().multiply(this.buyOrder.getPrice()).setScale(this.minorScale, RoundingMode.FLOOR);
	}
	
	public void executeOpportunity() throws NotEnoughBalanceException {
		this.exchangeToSellOn.sell(this.sellOrder);
		this.exchangeToBuyOn.buy(this.buyOrder);
		System.out.println("Executed opportunity of volume "+this.getOpportunityVolume()+" "+this.minorCurrency);
	}
	
	/**
	 * @return The predicted amount of the major currency profited after executing the opportunity.
	 */
	public BigDecimal calculatePredictedProfit() {
		return this.buyOrder.getMajorAmountToTrade().subtract(this.sellOrder.getMajorAmountToTrade());
	}
	
	/**
	 * 
	 * @param opportunity1
	 * @param opportunity2
	 * @return The ArbitrageOpportunity with the largest trade volume. Returns opportunity2 if they are equal.
	 */
	public static ArbitrageOpportunity mostValuable(ArbitrageOpportunity opportunity1, ArbitrageOpportunity opportunity2) {
		if( opportunity1.getOpportunityVolume().compareTo(opportunity2.getOpportunityVolume()) == 1 )
			return opportunity1;
		return opportunity2;
	}
}
