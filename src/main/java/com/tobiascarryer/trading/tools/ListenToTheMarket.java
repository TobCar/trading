package com.tobiascarryer.trading.tools;

import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.currency.CurrencyPair;

import com.sun.speech.freetts.Voice;
import com.sun.speech.freetts.VoiceManager;
import com.tobiascarryer.trading.ApiSecrets;
import com.tobiascarryer.trading.charts.Candle;

import info.bitrich.xchangestream.bitmex.BitmexStreamingExchange;
import info.bitrich.xchangestream.core.StreamingExchangeFactory;
import io.reactivex.disposables.Disposable;

public class ListenToTheMarket {
	
	private static final String VOICENAME_kevin = "kevin";
	
	public ListenToTheMarket() {
	}
	
	public static void main(String[] args) {
		listenToThePrice("XBT", "USD");
	}
	
	public static void listenToThePrice(String majorCurrency, String minorCurrency) {
		startMonitoringTicker(majorCurrency, minorCurrency, false);
	}
		
	private static void processCandleToListenToThePrice(Candle candle) {
		VoiceManager voiceManager = VoiceManager.getInstance();
		Voice voice = voiceManager.getVoice(VOICENAME_kevin);
		voice.allocate();
		voice.speak(candle.getClose().toString());
	}
	
	public static Disposable startMonitoringTicker(String majorCurrency, String minorCurrency, boolean useTestNet) {
		String key;
		String secret;
		if( useTestNet ) {
			key = ApiSecrets.bitmexTestnetKey;
			secret = ApiSecrets.bitmexTestnetSecret;
		} else {
			key = ApiSecrets.bitmexKey;
			secret = ApiSecrets.bitmexSecret;
		}
		
		// Connect to Bitmex websocket
		BitmexStreamingExchange bitmexWebsocket = (BitmexStreamingExchange) StreamingExchangeFactory.INSTANCE.createExchange(createWebsocketSpecification(key, secret, useTestNet));
		bitmexWebsocket.connect().blockingAwait();
		
		// Subscribe to Bitmex ticker websocket
		return bitmexWebsocket.getStreamingMarketDataService()
		        .getTicker(new CurrencyPair(majorCurrency, minorCurrency))
		        .subscribe(ticker -> {
		        	Candle candle = new Candle(ticker.getAsk(), ticker.getBid(), ticker.getBid(), ticker.getBid());
		        	System.out.println("Socket " + ticker.getTimestamp().getTime());
		        	processCandleToListenToThePrice(candle);
		        }, throwable -> {
		        	System.out.println("Error in subscribing trades. ");
		        	throwable.printStackTrace();
		        });
	}
	
	private static ExchangeSpecification createWebsocketSpecification(String key, String secret, boolean useSandbox) {
		ExchangeSpecification spec = new ExchangeSpecification(BitmexStreamingExchange.class.getName());
		spec.setApiKey(key);
		spec.setSecretKey(secret);
		spec.setShouldLoadRemoteMetaData(false);
		spec.setSslUri("https://www.bitmex.com/");
		spec.setHost("bitmex.com");
		spec.setPort(443);
		spec.setExchangeName("Bitmex");
		spec.setExchangeDescription("Bitmex is a bitcoin exchange");
		spec.setExchangeSpecificParametersItem("Use_Sandbox", useSandbox);
		return spec;
	}
}
