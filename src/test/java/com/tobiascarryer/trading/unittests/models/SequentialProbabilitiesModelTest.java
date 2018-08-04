package com.tobiascarryer.trading.unittests.models;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.tobiascarryer.trading.models.ModelPrediction;
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
	
	@Test
	public void testUnifiedProbability() {
		ModelPrediction<Boolean> prediction1 = new ModelPrediction<Boolean>(true, 0.5);
		ModelPrediction<Boolean> prediction2 = new ModelPrediction<Boolean>(true, 0.75);
		ModelPrediction<Boolean> prediction3 = new ModelPrediction<Boolean>(true, 1.0);
		List<ModelPrediction<Boolean>> predictions = new ArrayList<ModelPrediction<Boolean>>();
		predictions.add(prediction1);
		predictions.add(prediction2);
		predictions.add(prediction3);
		List<Double> totalOccurencesForPrediction = new ArrayList<>();
		totalOccurencesForPrediction.add(20.0);
		totalOccurencesForPrediction.add(10.0);
		totalOccurencesForPrediction.add(10.0);
		assertTrue(0.6875 == SequentialProbabilitiesModel.getUnifiedProbability(predictions, totalOccurencesForPrediction));
	}
}
