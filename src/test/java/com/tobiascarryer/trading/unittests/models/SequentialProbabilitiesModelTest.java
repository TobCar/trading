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
