package com.tobiascarryer.trading.models.sequentialprobabilities;

import java.util.Objects;

public class PercentageChangeBin {
	
	public int bin;

	public PercentageChangeBin(int bin) {
		this.bin = bin;
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
	
	@Override
	public String toString() {
		return String.valueOf(bin);
	}
	
	@Override
	public boolean equals(Object other){
		if( other == null )
            return false;

        if( !PercentageChangeBin.class.isAssignableFrom(other.getClass()) )
            return false;

        PercentageChangeBin otherPercentageChangeBin = (PercentageChangeBin) other;
        return this.bin == otherPercentageChangeBin.bin;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(bin);
	}
}
