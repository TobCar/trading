package com.tobiascarryer.trading.unittests.models;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

import com.tobiascarryer.trading.models.sequentialprobabilities.BinSequence;
import com.tobiascarryer.trading.models.sequentialprobabilities.PercentageChangeBin;
import com.tobiascarryer.trading.models.sequentialprobabilities.SequentialProbabilitiesModel;

import org.junit.Test;

public class SequentialProbabilitiesModelTest {

	/**
     * Getting all latest bin sequences.
     */
	@Test
    public void testGetSequences() {
    	int minLength = 2;
    	int maxLength = 4;
    	
    	PercentageChangeBin zero = new PercentageChangeBin(0);
    	PercentageChangeBin negOne = new PercentageChangeBin(-1);
    	PercentageChangeBin two = new PercentageChangeBin(2);
    	PercentageChangeBin negThree = new PercentageChangeBin(-3);
    	
    	PercentageChangeBin[] bins2 = {zero, negOne};
    	PercentageChangeBin[] bins3 = {zero, negOne, two};
    	PercentageChangeBin[] bins4 = {zero, negOne, two, negThree};
    	
    	Set<BinSequence> binSequencesLeftToAssert = new HashSet<>();
    	binSequencesLeftToAssert.add(new BinSequence(bins2));
    	binSequencesLeftToAssert.add(new BinSequence(bins3));
    	binSequencesLeftToAssert.add(new BinSequence(bins4));
    	
    	BinSequence[] sequences = SequentialProbabilitiesModel.getSequences(bins4, minLength, maxLength);
    	
    	assertEquals(sequences.length, maxLength-minLength+1);
    	
    	for( BinSequence sequence: sequences ) {
    		assertTrue(binSequencesLeftToAssert.remove(sequence));
    	}
    	
    	assertTrue(binSequencesLeftToAssert.isEmpty());
    }
}
