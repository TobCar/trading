package com.tobiascarryer.trading.unittests.models;

import static org.junit.Assert.*;
import org.junit.Test;

import com.tobiascarryer.trading.models.sequentialprobabilities.SequentialProbabilitiesFileNames;

public class SequentialProbabilitiesFileNamesTest {
	
	@Test
	public void testFileNameBaseContainsSTOCK() {
		assertTrue(SequentialProbabilitiesFileNames.fileNameBase.contains("STOCK"));
	}
	
	@Test
	public void testCreateFileName() {
		String fileNameBase = "daily_STOCK";
		String stockTicker = "AMZN";
		String append = "append.txt";
		assertEquals("daily_AMZN.txt", SequentialProbabilitiesFileNames.createFileName(fileNameBase, stockTicker, ".txt"));
		assertEquals("daily_AMZN-append.txt", SequentialProbabilitiesFileNames.createFileName(fileNameBase, stockTicker, append));
	}
}
