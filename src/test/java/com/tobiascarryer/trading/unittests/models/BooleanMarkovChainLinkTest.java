package com.tobiascarryer.trading.unittests.models;

import static org.junit.Assert.*;

import com.tobiascarryer.trading.models.BooleanMarkovChainLink;
import com.tobiascarryer.trading.models.ModelPrediction;

import org.junit.Test;

public class BooleanMarkovChainLinkTest {

	@Test
	public void testSetOccurencesFromString() {
		BooleanMarkovChainLink<Integer> chainLink = new BooleanMarkovChainLink<Integer>(0);
		String occurencesString = "1,2";
		chainLink.setOccurencesFromString(occurencesString);
		assertTrue(chainLink.getTrueOccurences() == 1);
		assertTrue(chainLink.getFalseOccurences() == 2);
		assertTrue(chainLink.getTotalOccurences() == 3);
	}
	
	@Test
	public void testPrediction() {
		BooleanMarkovChainLink<Integer> chainLink = new BooleanMarkovChainLink<Integer>(0);
		String occurencesString = "15,5";
		chainLink.setOccurencesFromString(occurencesString);
		ModelPrediction<Boolean> prediction = chainLink.predictNextValue();
		assertTrue(prediction.getItem());
		assertTrue(prediction.getProbability() == 15.0/20.0);
	}
}
