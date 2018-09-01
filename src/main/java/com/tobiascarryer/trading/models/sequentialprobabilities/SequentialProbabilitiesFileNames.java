package com.tobiascarryer.trading.models.sequentialprobabilities;

public final class SequentialProbabilitiesFileNames {
	
	public static String fileNameBase = "daily_STOCK";
	public static String traderLogsFileName = "trader-logs.csv";
	static String allStocksFileName = "all-stocks.txt";
	static String stocksToObserveFileName = "stocks-to-observe.csv";
	private static String binThresholdsFileNameAppend = "bin-thresholds.csv";
	private static String binsFileNameAppend = "bins.txt";
	private static String weekdayModelFileNameAppend = "weekday-model.txt";
	private static String weekendModelFileNameAppend = "weekend-model.txt";
	private static String latestBinsAppend = "latest-bins.txt";
	private static String previousCandleAppend = "previous-candle.csv";
	
	/**
	 * @param base The start of the file name where STOCK represents where a ticker should be inserted.
	 * @param stockTicker
	 * @param append
	 * @return String
	 */
	public static String createFileName(String base, String stockTicker, String append) {
		String modifiedBase = base.replace("STOCK", stockTicker);
		if( append.startsWith(".") )
			return modifiedBase + append;
		return modifiedBase + "-" + append;
	}
	
	public static String historicalDataFileName(String ticker) {
		return createFileName(fileNameBase, ticker, ".csv");
	}
	
	public static String binThresholdsFileName(String ticker) {
		return createFileName(fileNameBase, ticker, binThresholdsFileNameAppend);
	}
	
	public static String binsFileName(String ticker) {
		return createFileName(fileNameBase, ticker, binsFileNameAppend);
	}
	
	public static String savedWeekdayModelFileName(String ticker) {
		return createFileName(fileNameBase, ticker, weekdayModelFileNameAppend);
	}
	
	public static String savedWeekendModelFileName(String ticker) {
		return createFileName(fileNameBase, ticker, weekendModelFileNameAppend);
	}
	
	public static String latestBinsFileName(String ticker) {
		return createFileName(fileNameBase, ticker, latestBinsAppend);
	}
	
	public static String previousCandleFileName(String ticker) {
		return createFileName(fileNameBase, ticker, previousCandleAppend);
	}
}
