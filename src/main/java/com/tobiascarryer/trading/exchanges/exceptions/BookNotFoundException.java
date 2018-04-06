package com.tobiascarryer.trading.exchanges.exceptions;

public class BookNotFoundException extends Exception {
	public BookNotFoundException() { }
	
	public BookNotFoundException(String message) { super(message); }
}