package com.tobiascarryer.trading.unittests.models;

import static org.junit.Assert.*;
import com.tobiascarryer.trading.models.sequentialprobabilities.PercentageChangeBin;
import org.junit.Test;

public class PercentageChangeBinTest {

	@Test
	public void testEquality() {
		PercentageChangeBin bin = new PercentageChangeBin(1);
		PercentageChangeBin binEqual = new PercentageChangeBin(1);
		PercentageChangeBin binNotEqual = new PercentageChangeBin(-1);
		
		assertEquals(bin, bin);
		assertEquals(bin, binEqual);
		assertNotEquals(bin, binNotEqual);
		assertNotEquals(bin, 1);
		assertNotEquals(bin, null);
	}
}
