package com.tobiascarryer.trading.exchanges.orderbook;

import java.util.List;

import org.knowm.xchange.dto.trade.LimitOrder;

import com.dylanjsa.cryptopia.remote.data.MarketOrder;

public class OrderBookIterator {
	private OrderBookEntry[] entries;
	private int currentEntry = 0;
	
	private OrderBookIterator(OrderBookEntry[] entries) {
		this.entries = entries;
	}
	
	public OrderBookEntry next() {
		return this.entries[currentEntry++];
	}

	public boolean hasNext() {
		return currentEntry < this.entries.length;
	}
	
	public static OrderBookIterator createCryptopiaIterator(List<MarketOrder> entriesToProcess) {
		OrderBookEntry[] assimilatedEntries = new OrderBookEntry[entriesToProcess.size()];
		for( int i=0; i < entriesToProcess.size(); i++ )
			assimilatedEntries[i] = new OrderBookEntry(entriesToProcess.get(i).getPrice(), entriesToProcess.get(i).getTotal());
		return new OrderBookIterator(assimilatedEntries);
	}
	
	public static OrderBookIterator createBinanceIterator(List<com.binance.api.client.domain.market.OrderBookEntry> entriesToProcess) {
		OrderBookEntry[] assimilatedEntries = new OrderBookEntry[entriesToProcess.size()];
		for( int i=0; i < entriesToProcess.size(); i++ )
			assimilatedEntries[i] = new OrderBookEntry(entriesToProcess.get(i).getPrice(), entriesToProcess.get(i).getQty());
		return new OrderBookIterator(assimilatedEntries);
	}
	
	public static OrderBookIterator createXChangeIterator(List<LimitOrder> entriesToProcess) {
		OrderBookEntry[] assimilatedEntries = new OrderBookEntry[entriesToProcess.size()];
		for( int i=0; i < entriesToProcess.size(); i++ )
			assimilatedEntries[i] = new OrderBookEntry(entriesToProcess.get(i).getLimitPrice(), entriesToProcess.get(i).getRemainingAmount());
		return new OrderBookIterator(assimilatedEntries);
	}
	
	/**
	 * Cloning entries before iterating is important because modifying the entries' values modifies them for every object
	 * that had a reference to that particular entry.
	 * @param entriesToClone, the entries that should be cloned
	 * @return An instance of OrderBookIterator that iterates through the entries cloned.
	 */
	public static OrderBookIterator createIteratorByCloning(OrderBookEntry[] entriesToClone) {
		OrderBookEntry[] entriesClone = new OrderBookEntry[entriesToClone.length];
		for( int i=0; i < entriesToClone.length; i++ ) {
			entriesClone[i] = new OrderBookEntry(entriesToClone[i].getPrice(), entriesToClone[i].getMinorCurrencyAmount());
		}
		return new OrderBookIterator(entriesClone);
	}
}
