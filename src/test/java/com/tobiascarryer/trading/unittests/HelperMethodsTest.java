package com.tobiascarryer.trading.unittests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.math.BigDecimal;

import org.junit.Test;

import com.tobiascarryer.trading.HelperMethods;

public class HelperMethodsTest {
	
	// Round to nearest point five
	@Test
	public void testPositiveNumbers() {
		assertTrue((HelperMethods.roundToNearestPointFive(new BigDecimal(2)).compareTo(new BigDecimal(2)) == 0));
		assertTrue((HelperMethods.roundToNearestPointFive(new BigDecimal(1)).compareTo(new BigDecimal(1)) == 0));
		assertTrue((HelperMethods.roundToNearestPointFive(new BigDecimal(0)).compareTo(new BigDecimal(0)) == 0));
		assertTrue((HelperMethods.roundToNearestPointFive(new BigDecimal(2.25)).compareTo(new BigDecimal(2.5)) == 0));
		assertTrue((HelperMethods.roundToNearestPointFive(new BigDecimal(1.1)).compareTo(new BigDecimal(1)) == 0));
		assertTrue((HelperMethods.roundToNearestPointFive(new BigDecimal(0.5)).compareTo(new BigDecimal(0.5)) == 0));
		assertTrue((HelperMethods.roundToNearestPointFive(new BigDecimal(0.65)).compareTo(new BigDecimal(0.5)) == 0));
		assertTrue((HelperMethods.roundToNearestPointFive(new BigDecimal(0.74)).compareTo(new BigDecimal(0.5)) == 0));
		assertTrue((HelperMethods.roundToNearestPointFive(new BigDecimal(0.75)).compareTo(new BigDecimal(1)) == 0));
	}
	
	@Test
	public void testNegativeNumbers() {
		assertTrue((HelperMethods.roundToNearestPointFive(new BigDecimal(-1)).compareTo(new BigDecimal(-1)) == 0));
		assertTrue((HelperMethods.roundToNearestPointFive(new BigDecimal(-2)).compareTo(new BigDecimal(-2)) == 0));
		assertTrue((HelperMethods.roundToNearestPointFive(new BigDecimal(-2.25)).compareTo(new BigDecimal(-2.0)) == 0));
		assertTrue((HelperMethods.roundToNearestPointFive(new BigDecimal(-1.1)).compareTo(new BigDecimal(-1)) == 0));
		assertTrue((HelperMethods.roundToNearestPointFive(new BigDecimal(-0.5)).compareTo(new BigDecimal(-0.5)) == 0));
		assertTrue((HelperMethods.roundToNearestPointFive(new BigDecimal(-0.65)).compareTo(new BigDecimal(-0.5)) == 0));
		assertTrue((HelperMethods.roundToNearestPointFive(new BigDecimal(-0.74)).compareTo(new BigDecimal(-0.5)) == 0));
		assertTrue((HelperMethods.roundToNearestPointFive(new BigDecimal(-0.75)).compareTo(new BigDecimal(-0.5)) == 0));
		assertTrue((HelperMethods.roundToNearestPointFive(new BigDecimal(-0.76)).compareTo(new BigDecimal(-1)) == 0));
	}
	
	// Get file name without extension
	@Test
	public void testFileNameWithoutExtension() {
		File file = new File("c:fake/path/name.txt");
		assertEquals("name", HelperMethods.getFileNameWithoutExtension(file));
	}
}
