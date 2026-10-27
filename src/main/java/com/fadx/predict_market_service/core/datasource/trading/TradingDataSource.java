package com.fadx.predict_market_service.core.datasource.trading;

import com.fadx.predict_market_service.modules.market.api.dtos.trading.DividendRecord;
import com.fadx.predict_market_service.modules.market.api.dtos.trading.OHLCVResponse;
import java.util.List;

public interface TradingDataSource {
  OHLCVResponse historical(String symbol, String fromDate, String toDate, String interval);

  List<DividendRecord> dividends(String symbol, String fromDate, String toDate);
}
