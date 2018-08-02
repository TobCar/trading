package com.tobiascarryer.trading.models;

public class PercentageChangeBin {
	
	public int bin;

	public PercentageChangeBin(int bin) {
		this.bin = bin;
	}
	
	@Override
	public String toString() {
		return String.valueOf(bin);
	}
}
