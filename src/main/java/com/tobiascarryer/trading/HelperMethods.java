package com.tobiascarryer.trading;

import java.math.BigDecimal;

public class HelperMethods {
	public static BigDecimal roundToNearestPointFive( BigDecimal toRound ) {
		// * 2 lets you round normally because .5*2 = 1
		// / 2 returns the rounded number to .5 instead of 1
		return new BigDecimal(Math.round(toRound.doubleValue() * 2.0) / 2.0);
	}
}
