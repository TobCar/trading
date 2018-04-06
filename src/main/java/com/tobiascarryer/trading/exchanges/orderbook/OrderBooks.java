package com.tobiascarryer.trading.exchanges.orderbook;

import java.util.concurrent.ConcurrentHashMap;

import com.tobiascarryer.trading.exchanges.exceptions.BookNotFoundException;

public class OrderBooks<T> {
	
	private ConcurrentHashMap<String, ConcurrentHashMap<String, T>> orderBooks = new ConcurrentHashMap<String, ConcurrentHashMap<String, T>>();
	
	/**
	 * Removes previous book and replaces it with the new book.
	 * @param majorCurrency, not case sensitive
	 * @param minorCurrency, not case sensitive
	 * @param book
	 */
	public void put(String majorCurrency, String minorCurrency, T book) {
		// Currencies are equal regardless of case. (ex. ETH = eth)
		majorCurrency = majorCurrency.toLowerCase();
		minorCurrency = minorCurrency.toLowerCase();
		
		// Get the order books for the minor currency and add book to it.
		ConcurrentHashMap<String, T> booksForMinorCurrency = orderBooks.getOrDefault(minorCurrency, new ConcurrentHashMap<String, T>());
		booksForMinorCurrency.put(majorCurrency, book);
		orderBooks.put(minorCurrency, booksForMinorCurrency);
	}
	
	/**
	 * Merges entries in the existing book with the entries from the new book.
	 * @param majorCurrency, not case sensitive
	 * @param minorCurrency, not case sensitive
	 * @return book
	 */
	public T get(String majorCurrency, String minorCurrency) throws BookNotFoundException {
		// Currencies are equal regardless of case. (ex. ETH = eth)
		majorCurrency = majorCurrency.toLowerCase();
		minorCurrency = minorCurrency.toLowerCase();
		
		// Return the book if it exists. The exception exists because a book not existing must always
		// be handled and forgetting to handle it is unacceptable.
		if( orderBooks.get(minorCurrency) == null ) {
			throw new BookNotFoundException("Minor currency ("+minorCurrency+") does not have any books stored.");
		}
		T toReturn = orderBooks.get(minorCurrency).get(majorCurrency);
		if( toReturn == null ) {
			throw new BookNotFoundException("Major currency ("+majorCurrency+") does not have a book stored for this minor currency ("+minorCurrency+").");
		}
		return toReturn;
	}
}
