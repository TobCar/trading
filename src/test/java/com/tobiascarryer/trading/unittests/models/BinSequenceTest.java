package com.tobiascarryer.trading.unittests.models;

import static org.junit.Assert.*;

import com.tobiascarryer.trading.models.sequentialprobabilities.BinSequence;
import com.tobiascarryer.trading.models.sequentialprobabilities.PercentageChangeBin;
import org.junit.Test;

public class BinSequenceTest {

	@Test
	public void testEquality() {
		PercentageChangeBin bin1 = new PercentageChangeBin(1);
		PercentageChangeBin bin2 = new PercentageChangeBin(2);
		PercentageChangeBin bin3 = new PercentageChangeBin(3);
		
		PercentageChangeBin[] bins = {bin1, bin2, bin3};
		BinSequence sequence = new BinSequence(bins);
		
		PercentageChangeBin equalBin1 = new PercentageChangeBin(1);
		PercentageChangeBin equalBin2 = new PercentageChangeBin(2);
		PercentageChangeBin equalBin3 = new PercentageChangeBin(3);
		
		PercentageChangeBin[] equalBins = {equalBin1, equalBin2, equalBin3};
		BinSequence equalSequence = new BinSequence(equalBins);
		
		PercentageChangeBin[] notEqualBins = {bin1, bin2};
		BinSequence notEqualSequence = new BinSequence(notEqualBins);
		
		assertEquals(sequence, sequence);
		assertEquals(sequence, equalSequence);
		assertNotEquals(sequence, notEqualSequence);
		assertNotEquals(sequence, null);
	}
	
	@Test
	public void testToString() {
		PercentageChangeBin bin1 = new PercentageChangeBin(1);
		PercentageChangeBin bin2 = new PercentageChangeBin(-2);
		PercentageChangeBin bin3 = new PercentageChangeBin(3);
		
		PercentageChangeBin[] bins = {bin1, bin2, bin3};
		BinSequence sequence = new BinSequence(bins);
		
		assertEquals("1-23", sequence.toString());
	}
	
	@Test
	public void testParsing() {
		PercentageChangeBin bin1 = new PercentageChangeBin(1);
		PercentageChangeBin bin2 = new PercentageChangeBin(-2);
		PercentageChangeBin bin3 = new PercentageChangeBin(3);
		
		PercentageChangeBin[] bins = {bin1, bin2, bin3};
		BinSequence sequence = new BinSequence(bins);
		
		PercentageChangeBin[] binsNotEqual = {bin1, bin2};
		BinSequence sequenceNotEqual = new BinSequence(binsNotEqual);
		
		String toParseEqual = sequence.toString();
		String toParseNotEqual = sequenceNotEqual.toString();
		BinSequence equalSequence = BinSequence.parse(toParseEqual);
		BinSequence notEqualSequence = BinSequence.parse(toParseNotEqual);
		
		assertEquals(sequence, equalSequence);
		assertNotEquals(sequence, notEqualSequence);
	}
}
