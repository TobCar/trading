package com.tobiascarryer.trading.models;

import java.util.Objects;

/**
 * @param <T> The type of value represented by this link.
 */
public class BooleanMarkovChainLink<T> {
    private double trueOccurences, falseOccurences, totalOccurences;
    private final T value;
    
    public BooleanMarkovChainLink( T value ) {
        this.value = value;
    }
    
    public T getValue() {
        return this.value;
    }
    
    /**
     * @return ModelPrediction with the next predicted value as well as the probability it's right.
     */
    public ModelPrediction<Boolean> predictNextValue() {
    	if( trueOccurences == falseOccurences )
    		return new ModelPrediction<Boolean>(null, 0.5);
    	Boolean prediction = trueOccurences > falseOccurences;
    	double probability = Math.max(trueOccurences, falseOccurences) / totalOccurences;
    	return new ModelPrediction<Boolean>(prediction, probability);
    }
    
    /**
     * Post: The number of occurrences of nextValue is increased by 1.
     * @param nextValue The boolean value that comes after this link.
     */
    public void increaseOccurencesFor( boolean nextValue ) {
    	if( nextValue )
    		trueOccurences += 1;
    	else
    		falseOccurences += 1;
    	totalOccurences += 1;
    }
    
    public double getTrueOccurences() {
    	return trueOccurences;
    }
    
    public double getFalseOccurences() {
    	return falseOccurences;
    }
    
    public double getTotalOccurences() {
    	return totalOccurences;
    }
    
    @Override
    public String toString() {
    	return value.toString()+":"+trueOccurences+","+falseOccurences;
    }
    
    /**
     * Used to parse the occurences back from the toString() method.
     */
    public void setOccurencesFromString(String occurencesString) {
    	String[] occurences = occurencesString.split(",");
    	trueOccurences = Double.valueOf(occurences[0]);
    	falseOccurences = Double.valueOf(occurences[1]);
    	totalOccurences = trueOccurences + falseOccurences;
    }
    
    /**
     * @param other Object to check for equality.
     * @return True if the other object is also a ChainLink object with the same
     * value stored as this ChainLink object.
     */
    @Override
    public boolean equals( Object other ) {
        if( other instanceof BooleanMarkovChainLink ) {
            return ((BooleanMarkovChainLink) other).value.equals(value);
        }
        
        return false;
    }
    
    @Override
    public int hashCode() {
    	return Objects.hash(value);
    }
}
