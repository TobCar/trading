/**
 * Factory pattern to abstract out the Exchange implementations.
 */

package com.tobiascarryer.trading.exchanges;

public class ExchangeFactory {
	
	public static ExchangeFactory newFactory() {
		return new ExchangeFactory();
	}
	
	public BinanceExchange newBinanceExchange(String key, String secret) {
		return new BinanceExchange(key, secret, false);
	}
	
	public BinanceExchange newBinanceExchange(String key, String secret, boolean useBNBToPayFees) {
		return new BinanceExchange(key, secret, useBNBToPayFees);
	}
	
	public CryptopiaExchange newCryptopiaExchange(String key, String secret) {
		return new CryptopiaExchange(key, secret);
	}
	
	public QuadrigaCXExchange newQuadrigaExchange(String key, String secret, String clientId, boolean usesFiatFees) {
		return new QuadrigaCXExchange(key, secret, clientId, usesFiatFees);
	}
}
