package com.tobiascarryer.trading.unittests;

import java.math.BigDecimal;

import org.junit.Assert;
import org.junit.Test;

import com.tobiascarryer.trading.HelperMethods;

public class RoundToNearestPointFiveTest {
	@Test
	public void testPositiveNumbers() {
		Assert.assertTrue((HelperMethods.roundToNearestPointFive(new BigDecimal(2)).compareTo(new BigDecimal(2)) == 0));
		Assert.assertTrue((HelperMethods.roundToNearestPointFive(new BigDecimal(1)).compareTo(new BigDecimal(1)) == 0));
		Assert.assertTrue((HelperMethods.roundToNearestPointFive(new BigDecimal(0)).compareTo(new BigDecimal(0)) == 0));
		Assert.assertTrue((HelperMethods.roundToNearestPointFive(new BigDecimal(2.25)).compareTo(new BigDecimal(2.5)) == 0));
		Assert.assertTrue((HelperMethods.roundToNearestPointFive(new BigDecimal(1.1)).compareTo(new BigDecimal(1)) == 0));
		Assert.assertTrue((HelperMethods.roundToNearestPointFive(new BigDecimal(0.5)).compareTo(new BigDecimal(0.5)) == 0));
		Assert.assertTrue((HelperMethods.roundToNearestPointFive(new BigDecimal(0.65)).compareTo(new BigDecimal(0.5)) == 0));
		Assert.assertTrue((HelperMethods.roundToNearestPointFive(new BigDecimal(0.74)).compareTo(new BigDecimal(0.5)) == 0));
		Assert.assertTrue((HelperMethods.roundToNearestPointFive(new BigDecimal(0.75)).compareTo(new BigDecimal(1)) == 0));
	}
	
	@Test
	public void testNegativeNumbers() {
		Assert.assertTrue((HelperMethods.roundToNearestPointFive(new BigDecimal(-1)).compareTo(new BigDecimal(-1)) == 0));
		Assert.assertTrue((HelperMethods.roundToNearestPointFive(new BigDecimal(-2)).compareTo(new BigDecimal(-2)) == 0));
		Assert.assertTrue((HelperMethods.roundToNearestPointFive(new BigDecimal(-2.25)).compareTo(new BigDecimal(-2.0)) == 0));
		Assert.assertTrue((HelperMethods.roundToNearestPointFive(new BigDecimal(-1.1)).compareTo(new BigDecimal(-1)) == 0));
		Assert.assertTrue((HelperMethods.roundToNearestPointFive(new BigDecimal(-0.5)).compareTo(new BigDecimal(-0.5)) == 0));
		Assert.assertTrue((HelperMethods.roundToNearestPointFive(new BigDecimal(-0.65)).compareTo(new BigDecimal(-0.5)) == 0));
		Assert.assertTrue((HelperMethods.roundToNearestPointFive(new BigDecimal(-0.74)).compareTo(new BigDecimal(-0.5)) == 0));
		Assert.assertTrue((HelperMethods.roundToNearestPointFive(new BigDecimal(-0.75)).compareTo(new BigDecimal(-0.5)) == 0));
		Assert.assertTrue((HelperMethods.roundToNearestPointFive(new BigDecimal(-0.76)).compareTo(new BigDecimal(-1)) == 0));
	}
}
