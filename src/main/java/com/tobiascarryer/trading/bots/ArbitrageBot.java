package com.tobiascarryer.trading.bots;

import java.util.Map;

import com.tobiascarryer.trading.ApiSecrets;
import com.tobiascarryer.trading.exchanges.Exchange;
import com.tobiascarryer.trading.exchanges.ExchangeFactory;
import com.tobiascarryer.trading.exchanges.exceptions.BookNotFoundException;
import com.tobiascarryer.trading.exchanges.exceptions.NotEnoughBalanceException;

import java.util.HashMap;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class ArbitrageBot {
	
	private String minorCurrency;
	private BigDecimal targetProfit;
	private BigDecimal percentageOfMinorCurrencyToTrade = new BigDecimal("1");
	private BigDecimal percentageOfMajorCurrencyToTrade = new BigDecimal("1");
	
	// Used to stop threads
	private boolean arbitraging = true;
	
	/**
	 * @param minorCurrency, symbol
	 * @param maximumTradeValue, maximum volume on a single trade
	 * @param targetProfit, ROI + 100% (1% ROI would be 1.01)
	 */
	public ArbitrageBot(String minorCurrency, BigDecimal targetProfit) {
		setMinorCurrency(minorCurrency);
		this.targetProfit = targetProfit;
	}
	
	public void startArbitraging(String[] majorCurrenciesToTrade, Exchange[] exchanges) {
		// Prepare the buy/sell target ratios. They are used to determine whether an
		// exchange is profitable to arbitrage.
		final BuySellRatios buySellRatios = calculateBuySellRatios(exchanges);
		
		arbitraging = true;
		for( final String majorCurrency: majorCurrenciesToTrade ) {
			System.out.println("Starting arbitrage thread for major currency "+majorCurrency+" and minor currency "+minorCurrency+".");
			new Thread() {
				public void run() {
					while( arbitraging ) {
				        // O(n!/2) every exchange must be checked against every other exchange
						// Could split up each exchange with each major currency to be its own Thread.
						for( int i=0; i<exchanges.length-1; i++ ) {
							if( exchanges[i].supports(majorCurrency, minorCurrency) && exchanges[i].canWithdraw(majorCurrency) ) {
								for( int n=i+1; n<exchanges.length; n++ ) {
									if( exchanges[n].supports(majorCurrency, minorCurrency) && exchanges[n].canWithdraw(majorCurrency) ) {
										try {
											// Minimum spread between two exchanges
											BigDecimal minBuySellRatio = buySellRatios.get(exchanges[i], exchanges[n]);
											
											// Maximum trade volume is a percentage of the balance on each of the exchanges
											BigDecimal maxExchangeIBuyVolume = exchanges[i].getBalance(minorCurrency).multiply(percentageOfMinorCurrencyToTrade);
											BigDecimal maxExchangeNBuyVolume = exchanges[n].getBalance(minorCurrency).multiply(percentageOfMinorCurrencyToTrade);
											BigDecimal maxExchangeISellVolume = exchanges[i].getBalance(majorCurrency).multiply(percentageOfMajorCurrencyToTrade);
											BigDecimal maxExchangeNSellVolume = exchanges[n].getBalance(majorCurrency).multiply(percentageOfMajorCurrencyToTrade);
											
											// Buy on exchange i and sell on exchange n
											ArbitrageOpportunity opportunity1 = null;
											if( isArbitrageProfitable(minBuySellRatio, exchanges[i], exchanges[n], majorCurrency) ) {
												opportunity1 = new ArbitrageOpportunity(exchanges[i], exchanges[n], majorCurrency, minorCurrency);
												opportunity1.optimizeOrders(minBuySellRatio, maxExchangeIBuyVolume, maxExchangeNSellVolume);
											}
												
											// Buy on exchange n and sell on exchange i
											ArbitrageOpportunity opportunity2 = null;
											if( isArbitrageProfitable(minBuySellRatio, exchanges[n], exchanges[i], majorCurrency) ) {
												opportunity2 = new ArbitrageOpportunity(exchanges[n], exchanges[i], majorCurrency, minorCurrency);
												opportunity2.optimizeOrders(minBuySellRatio, maxExchangeNBuyVolume, maxExchangeISellVolume);
											}
											
											// The biggest minimum of the two exchanges is taken so the order can execute in both.
											BigDecimal minTrade = exchanges[i].getMinimumMinorVolume(majorCurrency, minorCurrency).max(exchanges[n].getMinimumMinorVolume(majorCurrency, minorCurrency));
											boolean opportunity1CanTrade = opportunity1 != null && opportunity1.getOpportunityVolume().compareTo(minTrade) != -1;
											boolean opportunity2CanTrade = opportunity2 != null && opportunity2.getOpportunityVolume().compareTo(minTrade) != -1;
											
											// If there is a tradeable opportunity
											if( opportunity1CanTrade || opportunity2CanTrade ) {	
												// Find the optimal price and volume for the opportunity then execute.
												if( opportunity1CanTrade && opportunity2CanTrade ) {
													ArbitrageOpportunity best = ArbitrageOpportunity.mostValuable(opportunity1, opportunity2);
													best.executeOpportunity();
													System.out.println("Predicted profit: "+best.calculatePredictedProfit()+" "+majorCurrency);
												} else if( opportunity1CanTrade ) {
													opportunity1.executeOpportunity();
													System.out.println("Predicted profit: "+opportunity1.calculatePredictedProfit()+" "+majorCurrency);
												} else if( opportunity2CanTrade ) {
													opportunity2.executeOpportunity();
													System.out.println("Predicted profit: "+opportunity2.calculatePredictedProfit()+" "+majorCurrency);
												}
											
												// Wait so the same opportunity is not exploited twice.
												try {
													Thread.sleep(30000);
												} catch (InterruptedException e) {
													e.printStackTrace();
												}
											}
										} catch( RatioDoesNotExistException e ) {
											e.printStackTrace();
										} catch( NotEnoughBalanceException e ) {
											e.printStackTrace();
											System.out.println("Not enough balance for the opportunity.");
										}
									}
								}
							}
						}
					}
			    }
			}.start();
		}	
	}
	
	private boolean isArbitrageProfitable(BigDecimal minimumBuySellRatio, Exchange exchangeToBuyOn, Exchange exchangeToSellOn, String majorCurrency) {
		// buy (ask) / sell (bid), should be < 1 if the arbitrage is profitable before fees and other deductions
		try {
			BigDecimal buySellRatio = exchangeToBuyOn.getLowestAsk(majorCurrency, minorCurrency).divide(exchangeToSellOn.getHighestBid(majorCurrency, minorCurrency), 8, RoundingMode.FLOOR);
			return buySellRatio.compareTo(minimumBuySellRatio) != 1;
		} catch (BookNotFoundException e) {
			e.printStackTrace();
			System.out.println("Order book has not been received yet.");
			return false;
		}
	}
	
	/**
	 * Set the minor currency used when arbitraging.
	 * @param minorCurrency, the symbol for the minor currency (ex. BTC or ETH)
	 */
	public void setMinorCurrency(String minorCurrency) {
		this.minorCurrency = minorCurrency;
	}
	
	/**
	 * @param majorCurrencyPercentage, maximum amount of the major currency that can be traded at once. Measured as percentage of the balance of a major currency
	 * @param minorCurrencyPercentage, maximum amount of the minor currency that can be traded at once. Measured as percentage of the balance of the bot's minor currency
	 */
	public void setPercentageToTrade(BigDecimal majorCurrencyPercentage, BigDecimal minorCurrencyPercentage) {
		assert(majorCurrencyPercentage.compareTo(new BigDecimal("0")) == 1);
		assert(minorCurrencyPercentage.compareTo(new BigDecimal("0")) == 1);
		this.percentageOfMajorCurrencyToTrade = majorCurrencyPercentage;
		this.percentageOfMinorCurrencyToTrade = minorCurrencyPercentage;
	}
	
	public void stop() {
		this.arbitraging = false;
	}
	
	private BuySellRatios calculateBuySellRatios(Exchange[] exchanges) {
		BuySellRatios buySellRatios = new BuySellRatios();
		for( int i=0; i<exchanges.length-1; i++ ) {
			for( int n=i+1; n<exchanges.length; n++ ) {
				/*
				 * The target price can be determined by multiplying the highest price (the sell) by the ratio.
				 * The amount to buy is determined by dividing the balance by the target price.
				 * Therefore, the numerator and the denominator in the ratio switch places
				 * when in use; the post-fees cancel out since each exchange multiplies by the
				 * post-fee so the only thing left is the target profit as intended!
				 * 
				 * It is also possible to determine if the difference between an ask and a bid is profitable
				 * by comparing the ask/bid (buy/sell) ratio to see if it is smaller than or equal to the
				 * stored ratio.
				 * 
				 * [determine the target buy price to look for]
				 * targetBuyPrice = sellPrice*ratio
				 * targetBuyPrice = ((sellPrice)(fee1)(fee2))/targetProfit
				 *             
				 * [when trading, start by buying]
				 * balanceToBuyWith / targetBuyPrice
				 * balanceToBuyWith / ((sellPrice)(fee1)(fee2))/targetProfit
				 * ((balanceToBuyWith)(targetProfit)) / ((price)(fee1)(fee2))
				 * 
				 * [buying fee]   ((fee1)(balanceToBuyWith)(targetProfit)) / ((sellPrice)(fee1)(fee2))
				 *                ((balanceToBuyWith)(targetProfit)) / ((sellPrice)(fee2))
				 *                
				 * [selling]   ((fee2)(sellPrice)(balanceToBuyWith)(targetProfit)) / ((sellPrice)(fee2))
				 *             (amountToBuy)(targetProfit)
				 */
				BigDecimal buySellRatio = (exchanges[i].getPostTradingFee().multiply(exchanges[n].getPostTradingFee())).divide(this.targetProfit, 8, RoundingMode.FLOOR);
				// Attach ratio to both exchanges since the order the exchanges are accessed/tested could change.
				buySellRatios.put(exchanges[i], exchanges[n], buySellRatio);
				buySellRatios.put(exchanges[n], exchanges[i], buySellRatio);
			}
		}
		return buySellRatios;
	}
	
	private class BuySellRatios {
		Map<Exchange, Map<Exchange, BigDecimal>> buySellRatios = new HashMap<>();
		
		void put(Exchange exchangeAddingTo, Exchange exchangeToAdd, BigDecimal ratio) {
			// Get the order books for exchangeAddingTo and add the ratio when
			// trading against exchangeToAdd.
			Map<Exchange, BigDecimal> ratiosByExchange = buySellRatios.getOrDefault(exchangeAddingTo, new HashMap<Exchange, BigDecimal>());
			ratiosByExchange.put(exchangeToAdd, ratio);
			buySellRatios.put(exchangeAddingTo, ratiosByExchange);
		}
		
		BigDecimal get(Exchange exchange, Exchange otherExchange) throws RatioDoesNotExistException {
			// Return the book if it exists. The exception exists because a book not existing must always
			// be handled and forgetting to handle it is unacceptable.
			if( buySellRatios.get(exchange) == null ) {
				throw new RatioDoesNotExistException("Ratio could not be found between two exchanges. exchange does not exist");
			}
			BigDecimal ratio = buySellRatios.get(exchange).get(otherExchange);
			if( ratio == null ) {
				throw new RatioDoesNotExistException("Ratio could not be found between two exchanges. otherExchange does not exist");
			}
			return ratio;
		}
	}
	
	private class RatioDoesNotExistException extends Exception {
		public RatioDoesNotExistException(String message) { super(message); }
	}
	
	public static void main(String[] args) throws InterruptedException {
		ExchangeFactory factory = ExchangeFactory.newFactory();
		
		// Arbitrage Bot Configuration
		// Set before running
		BigDecimal targetProfit = new BigDecimal("1.01");
		String[] majorCurrenciesToArbitrage = {"ETH", "NEO"};
		String minorCurrency = "BTC";
		Exchange binance = factory.newBinanceExchange(ApiSecrets.binanceKey, ApiSecrets.binanceSecret);
		Exchange cryptopia = factory.newCryptopiaExchange(ApiSecrets.cryptopiaKey, ApiSecrets.cryptopiaSecret);
		Exchange quadriga = factory.newQuadrigaExchange(ApiSecrets.quadrigaKey, ApiSecrets.quadrigaSecret, ApiSecrets.quadrigaClientId, minorCurrency.toUpperCase().equals("CAD") || minorCurrency.toUpperCase().equals("USD"));
		Exchange[] exchangesToArbitrage = {binance, cryptopia, quadriga};
		
		// Let account and supports data arrive from the exchanges
		Thread.sleep(10000);
				
		// Start monitoring the order book for exchanges that will be arbitraged
		binance.startMonitoringBook(majorCurrenciesToArbitrage, minorCurrency);
		cryptopia.startMonitoringBook(majorCurrenciesToArbitrage, minorCurrency);
		quadriga.startMonitoringBook(majorCurrenciesToArbitrage, minorCurrency);
		
		// Let order book data arrive from the exchanges
		Thread.sleep(10000);
		
		ArbitrageBot bot = new ArbitrageBot(minorCurrency, targetProfit);
		bot.startArbitraging(majorCurrenciesToArbitrage, exchangesToArbitrage);
	}
}
