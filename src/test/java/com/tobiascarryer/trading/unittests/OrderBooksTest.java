package com.tobiascarryer.trading.unittests;

import com.tobiascarryer.trading.exchanges.exceptions.BookNotFoundException;
import com.tobiascarryer.trading.exchanges.orderbook.OrderBooks;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for OrderBooks.
 */
public class OrderBooksTest extends TestCase {
    /**
     * Create the test case
     * @param testName name of the test case
     */
    public OrderBooksTest( String testName ) {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite( OrderBooksTest.class );
    }

    /**
     * It should be possible to put data concurrently.
     */
    public void testPutting() {
        OrderBooks<Integer> orderBooks = new OrderBooks<Integer>();
        orderBooks.put("ETH", "BTC", 10);
        try {
			assertTrue(orderBooks.get("ETH", "BTC") == 10);
		} catch( BookNotFoundException e ) {
			// Did not put in the right location.
			assertTrue(false);
		}
    }
}
