package com.tobiascarryer.trading.models;

/**
 * @param <T> The type of value of the item predicted.
 */
public class ModelPrediction<T> {
	
	private T item;
	private double probability;
	
	/**
	 * @param item The item predicted.
	 * @param probability The probability the item is correctly predicted.
	 */
	public ModelPrediction(T item, double probability) {
		this.item = item;
		this.probability = probability;
	}
	
	public T getItem() {
		return item;
	}
	
	public double getProbability() {
		return probability;
	}
}
