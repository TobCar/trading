package com.tobiascarryer.trading.models.sequentialprobabilities;

import java.util.Calendar;
import java.util.Objects;

public class PercentageChangeBin extends Object {
	
	public int bin;
	public Integer dayOfWeek;

	public PercentageChangeBin(int bin, Integer dayOfWeek) {
		this.bin = bin;
		this.dayOfWeek = dayOfWeek;
	}
	
	public Boolean isPositiveBin() {
		if( this.bin > 0 ) {
			return true;
		} else if( this.bin < 0 ) {
			return false;
		} else {
			return null;
		}
	}
	
	public boolean isDayBeforeTheWeekend() {
		return dayOfWeek != null && dayOfWeek == Calendar.FRIDAY;
	}
	
	public static PercentageChangeBin parseString(String stringToParse) {
		String[] parts = stringToParse.split(",");
		if( parts.length == 1 || parts[1].equals("null") ) {
			// Support old models
			return new PercentageChangeBin(Integer.valueOf(parts[0]), null);
		}
		return new PercentageChangeBin(Integer.valueOf(parts[0]), Integer.valueOf(parts[1]));
	}
	
	@Override
	public String toString() {
		return String.valueOf(bin)+","+String.valueOf(dayOfWeek);
	}
	
	@Override
	public boolean equals(Object other){
		if( other == null || getClass() != other.getClass() )
            return false;

        PercentageChangeBin otherPercentageChangeBin = (PercentageChangeBin) other;
        return this.bin == otherPercentageChangeBin.bin;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(bin);
	}
}
