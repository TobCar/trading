package com.tobiascarryer.trading.unittests.models;

import static org.junit.Assert.*;

import java.util.Calendar;

import com.tobiascarryer.trading.models.sequentialprobabilities.PercentageChangeBin;
import org.junit.Test;

public class PercentageChangeBinTest {

	@Test
	public void testEquality() {
		PercentageChangeBin bin = new PercentageChangeBin(1, Calendar.MONDAY);
		PercentageChangeBin binEqual = new PercentageChangeBin(1, Calendar.TUESDAY);
		PercentageChangeBin binNotEqual = new PercentageChangeBin(-1, Calendar.MONDAY);
		
		assertEquals(bin, bin);
		assertEquals(bin, binEqual);
		assertNotEquals(bin, binNotEqual);
		assertNotEquals(bin, 1);
		assertNotEquals(bin, null);
	}
	
	@Test
	public void testToString() {
		PercentageChangeBin sundayBin = new PercentageChangeBin(1, Calendar.SUNDAY);
		assertEquals("1,1", sundayBin.toString());
		PercentageChangeBin mondayBin = new PercentageChangeBin(1, Calendar.MONDAY);
		assertEquals("1,2", mondayBin.toString());
		PercentageChangeBin tuesdayBin = new PercentageChangeBin(1, Calendar.TUESDAY);
		assertEquals("1,3", tuesdayBin.toString());
		PercentageChangeBin wednesdayBin = new PercentageChangeBin(1, Calendar.WEDNESDAY);
		assertEquals("1,4", wednesdayBin.toString());
		PercentageChangeBin thursdayBin = new PercentageChangeBin(1, Calendar.THURSDAY);
		assertEquals("1,5", thursdayBin.toString());
		PercentageChangeBin fridayBin = new PercentageChangeBin(1, Calendar.FRIDAY);
		assertEquals("1,6", fridayBin.toString());
		PercentageChangeBin saturdayBin = new PercentageChangeBin(1, Calendar.SATURDAY);
		assertEquals("1,7", saturdayBin.toString());
	}
	
	@Test
	public void testParseString() {
		PercentageChangeBin sundayBin = PercentageChangeBin.parseString("1,1");
		assertEquals(1, sundayBin.bin);
		assertFalse(sundayBin.dayOfWeek == null);
		assertEquals(Calendar.SUNDAY, (int) sundayBin.dayOfWeek);
		PercentageChangeBin mondayBin = PercentageChangeBin.parseString("1,2");
		assertEquals(1, mondayBin.bin);
		assertFalse(mondayBin.dayOfWeek == null);
		assertEquals(Calendar.MONDAY, (int) mondayBin.dayOfWeek);
		PercentageChangeBin tuesdayBin = PercentageChangeBin.parseString("1,3");
		assertEquals(1, tuesdayBin.bin);
		assertFalse(tuesdayBin.dayOfWeek == null);
		assertEquals(Calendar.TUESDAY, (int) tuesdayBin.dayOfWeek);
		PercentageChangeBin wednesdayBin = PercentageChangeBin.parseString("1,4");
		assertEquals(1, wednesdayBin.bin);
		assertFalse(wednesdayBin.dayOfWeek == null);
		assertEquals(Calendar.WEDNESDAY, (int) wednesdayBin.dayOfWeek);
		PercentageChangeBin thursdayBin = PercentageChangeBin.parseString("1,5");
		assertEquals(1, thursdayBin.bin);
		assertFalse(thursdayBin.dayOfWeek == null);
		assertEquals(Calendar.THURSDAY, (int) thursdayBin.dayOfWeek);
		PercentageChangeBin fridayBin = PercentageChangeBin.parseString("1,6");
		assertEquals(1, fridayBin.bin);
		assertFalse(fridayBin.dayOfWeek == null);
		assertEquals(Calendar.FRIDAY, (int) fridayBin.dayOfWeek);
		PercentageChangeBin saturdayBin = PercentageChangeBin.parseString("1,7");
		assertEquals(1, saturdayBin.bin);
		assertFalse(saturdayBin.dayOfWeek == null);
		assertEquals(Calendar.SATURDAY, (int) saturdayBin.dayOfWeek);
	}
	
	@Test
	public void testIsDayBeforeTheWeekend() {
		PercentageChangeBin sundayBin = new PercentageChangeBin(1, Calendar.SUNDAY);
		assertFalse(sundayBin.isDayBeforeTheWeekend());
		PercentageChangeBin mondayBin = new PercentageChangeBin(1, Calendar.MONDAY);
		assertFalse(mondayBin.isDayBeforeTheWeekend());
		PercentageChangeBin tuesdayBin = new PercentageChangeBin(1, Calendar.TUESDAY);
		assertFalse(tuesdayBin.isDayBeforeTheWeekend());
		PercentageChangeBin wednesdayBin = new PercentageChangeBin(1, Calendar.WEDNESDAY);
		assertFalse(wednesdayBin.isDayBeforeTheWeekend());
		PercentageChangeBin thursdayBin = new PercentageChangeBin(1, Calendar.THURSDAY);
		assertFalse(thursdayBin.isDayBeforeTheWeekend());
		PercentageChangeBin fridayBin = new PercentageChangeBin(1, Calendar.FRIDAY);
		assertTrue(fridayBin.isDayBeforeTheWeekend());
		PercentageChangeBin saturdayBin = new PercentageChangeBin(1, Calendar.SATURDAY);
		assertFalse(saturdayBin.isDayBeforeTheWeekend());
	}
}
