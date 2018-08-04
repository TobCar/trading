package com.tobiascarryer.trading;

import java.io.File;
import java.math.BigDecimal;

import javax.swing.JFileChooser;

public class HelperMethods {
	public static BigDecimal roundToNearestPointFive( BigDecimal toRound ) {
		// * 2 lets you round normally because .5*2 = 1
		// / 2 returns the rounded number to .5 instead of 1
		return new BigDecimal(Math.round(toRound.doubleValue() * 2.0) / 2.0);
	}
	
	public static File chooseFile(){
        JFileChooser fc = new JFileChooser();
        File file = null;
        int returnVal = fc.showOpenDialog(null);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            file = fc.getSelectedFile();  
        } 
        return file;
    }
	
	/**
	 * Assumes there is only one period in the file's name.
	 * @param file To get the name for
	 * @return String
	 */
	public static String getFileNameWithoutExtension(File file) {
		return file.getName().substring(0, file.getName().indexOf("."));
	}
}
