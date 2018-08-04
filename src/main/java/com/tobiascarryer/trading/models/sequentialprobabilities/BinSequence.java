package com.tobiascarryer.trading.models.sequentialprobabilities;

import java.util.Objects;

public class BinSequence {
	
	private PercentageChangeBin[] bins;
	
	public BinSequence(PercentageChangeBin[] bins) {
		this.bins = bins;
	}
	
	@Override
	public boolean equals(Object other) {
		if( other == null )
            return false;

        if( !BinSequence.class.isAssignableFrom(other.getClass()) )
            return false;

        BinSequence otherSequence = (BinSequence) other;
        if( bins.length != otherSequence.bins.length )
        	return false;
        
        for( int i = 0; i < bins.length; i++ ) {
        	if( !otherSequence.bins[i].equals(bins[i]) )
        		return false;
        }
        	
        return true;
	}
	
	@Override
	public String toString() {
		String toReturn = "";
		for( PercentageChangeBin bin: bins )
			toReturn += bin.toString();
		return toReturn;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(bins);
	}
}
