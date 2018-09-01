package com.tobiascarryer.trading.unittests.models;

import static org.junit.Assert.*;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import com.tobiascarryer.trading.models.sequentialprobabilities.BinSequence;
import com.tobiascarryer.trading.models.sequentialprobabilities.PercentageChangeBin;
import org.junit.Test;

public class BinSequenceTest {
	
	/**
     * Getting all latest bin sequences.
     */
	@Test
    public void testGetSequences() {
    	int minLength = 2;
    	int maxLength = 4;
    	
    	PercentageChangeBin zero = new PercentageChangeBin(0, null);
    	PercentageChangeBin negOne = new PercentageChangeBin(-1, null);
    	PercentageChangeBin two = new PercentageChangeBin(2, null);
    	PercentageChangeBin negThree = new PercentageChangeBin(-3, null);
    	
    	PercentageChangeBin[] bins2 = {zero, negOne};
    	PercentageChangeBin[] bins3 = {zero, negOne, two};
    	PercentageChangeBin[] bins4 = {zero, negOne, two, negThree};
    	
    	Set<BinSequence> binSequencesLeftToAssert = new HashSet<>();
    	binSequencesLeftToAssert.add(new BinSequence(bins2));
    	binSequencesLeftToAssert.add(new BinSequence(bins3));
    	binSequencesLeftToAssert.add(new BinSequence(bins4));
    	
    	BinSequence[] sequences = BinSequence.getSequences(bins4, minLength, maxLength);
    	
    	assertEquals(sequences.length, maxLength-minLength+1);
    	
    	for( BinSequence sequence: sequences ) {
    		assertTrue(binSequencesLeftToAssert.remove(sequence));
    	}
    	
    	assertTrue(binSequencesLeftToAssert.isEmpty());
    }

	@Test
	public void testEquality() {
		PercentageChangeBin bin1 = new PercentageChangeBin(1, Calendar.MONDAY);
		PercentageChangeBin bin2 = new PercentageChangeBin(2, Calendar.MONDAY);
		PercentageChangeBin bin3 = new PercentageChangeBin(3, Calendar.MONDAY);
		
		PercentageChangeBin[] bins = {bin1, bin2, bin3};
		BinSequence sequence = new BinSequence(bins);
		
		PercentageChangeBin equalBin1 = new PercentageChangeBin(1, Calendar.MONDAY);
		PercentageChangeBin equalBin2 = new PercentageChangeBin(2, Calendar.MONDAY);
		PercentageChangeBin equalBin3 = new PercentageChangeBin(3, Calendar.MONDAY);
		
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
		PercentageChangeBin bin1 = new PercentageChangeBin(1, Calendar.MONDAY);
		PercentageChangeBin bin2 = new PercentageChangeBin(-2, Calendar.MONDAY);
		PercentageChangeBin bin3 = new PercentageChangeBin(3, Calendar.MONDAY);
		
		PercentageChangeBin[] bins = {bin1, bin2, bin3};
		BinSequence sequence = new BinSequence(bins);
		
		assertEquals("1-23", sequence.toString());
	}
	
	@Test
	public void testParsing() {
		PercentageChangeBin bin1 = new PercentageChangeBin(1, Calendar.MONDAY);
		PercentageChangeBin bin2 = new PercentageChangeBin(-2, Calendar.MONDAY);
		PercentageChangeBin bin3 = new PercentageChangeBin(3, Calendar.MONDAY);
		
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
