package com.fadx.predict_market_service.modules.common.services;

import com.fadx.predict_market_service.core.datasource.trading.TradingDataSource;
import com.fadx.predict_market_service.modules.common.dto.OhlcvProjection;
import com.fadx.predict_market_service.modules.common.repositories.SymbolPriceHisRepository;
import com.fadx.predict_market_service.modules.common.utils.OhlcvHelper;
import com.fadx.predict_market_service.modules.market.api.dtos.trading.DividendRecord;
import com.fadx.predict_market_service.modules.market.api.dtos.trading.OHLCVResponse;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TradingService {

  private final TradingDataSource tradingDataSource;
  private final SymbolPriceHisRepository symbolPriceHisRepository;

  public OHLCVResponse getHistorical(
      String symbol, String fromDate, String toDate, String interval) {
    List<OhlcvProjection> rows =
        symbolPriceHisRepository.findOhlcvBySymbolAndDateRange(
            symbol, LocalDate.parse(fromDate), LocalDate.parse(toDate).plusDays(1));
    if (!rows.isEmpty()) {
      return OhlcvHelper.toResponse(rows, symbol);
    }

    return tradingDataSource.historical(symbol, fromDate, toDate, interval);
  }

  public List<DividendRecord> getDividend(String symbol, String fromDate, String toDate) {
    return tradingDataSource.dividends(symbol, fromDate, toDate);
  }
}
