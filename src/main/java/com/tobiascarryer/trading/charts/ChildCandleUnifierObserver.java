package com.tobiascarryer.trading.charts;

import java.math.BigDecimal;

public interface ChildCandleUnifierObserver {
	public void onPartialUnifiedCandle(BigDecimal high, BigDecimal low, BigDecimal open, BigDecimal close);
}
