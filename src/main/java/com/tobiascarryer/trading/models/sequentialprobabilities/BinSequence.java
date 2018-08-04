package com.tobiascarryer.trading.models.sequentialprobabilities;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BinSequence {
	
	private PercentageChangeBin[] bins;
	
	public BinSequence(PercentageChangeBin[] bins) {
		this.bins = bins;
	}
	
	public static BinSequence parse(String sequence) {
		boolean nextIsNegNum = false;
		List<PercentageChangeBin> bins = new ArrayList<>();
		for( int i = 0; i < sequence.length(); i++ ) {
			char c = sequence.charAt(i);
			if( c == '-' )
				nextIsNegNum = true;
			else {
				int binVal = Integer.valueOf(String.valueOf(c));
				if( nextIsNegNum ) {
					nextIsNegNum = false;
					binVal = -binVal;
				}
				bins.add(new PercentageChangeBin(binVal));
			}
		}
		
		return new BinSequence(copyListToArray(bins));
	}
	
	private static PercentageChangeBin[] copyListToArray(List<PercentageChangeBin> list) {
		PercentageChangeBin[] array = new PercentageChangeBin[list.size()];
		for( int i = 0; i < list.size(); i++ ) {
			array[i] = list.get(i);
		}
		return array;
	}
	
	@Override
	public boolean equals(Object other) {
		if( other == null || getClass() != other.getClass() )
            return false;

        BinSequence otherSequence = (BinSequence) other;
        if( bins.length != otherSequence.bins.length )
        	return false;
        
        if( hashCode() == otherSequence.hashCode() )
        	return true;
        
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
