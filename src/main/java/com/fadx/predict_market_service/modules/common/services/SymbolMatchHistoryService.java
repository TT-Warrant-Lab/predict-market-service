package com.fadx.predict_market_service.modules.common.services;

import com.fadx.predict_market_service.modules.common.entity.SymbolMatchHistory;
import com.fadx.predict_market_service.modules.common.enums.MatchHistoryDay;
import com.fadx.predict_market_service.modules.common.repositories.SymbolMatchHistoryRepository;
import com.fadx.predict_market_service.modules.market.api.dtos.trading.OHLCVResponse;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SymbolMatchHistoryService {

  private final SymbolMatchHistoryRepository symbolMatchHistoryRepository;

  public OHLCVResponse getHistory(String symbol, MatchHistoryDay day) {
    java.time.LocalDate tradingDate = resolveTradingDate(day);
    return getHistory(symbol, tradingDate);
  }

  public List<OHLCVResponse> getHistory(List<String> symbols, MatchHistoryDay day) {
    java.time.LocalDate tradingDate = resolveTradingDate(day);
    return symbols.stream().map(symbol -> getHistory(symbol, tradingDate)).toList();
  }

  private OHLCVResponse getHistory(String symbol, java.time.LocalDate tradingDate) {
    if (tradingDate == null) {
      return emptyResponse(symbol);
    }
    List<SymbolMatchHistory> records =
        symbolMatchHistoryRepository.findByIdSymbolAndTradingDateOrderByIdTradingTimeAsc(
            symbol, tradingDate);
    int size = records.size();
    long[] timestamps = new long[size];
    double[] open = new double[size];
    double[] high = new double[size];
    double[] low = new double[size];
    double[] close = new double[size];
    double[] volume = new double[size];
    for (int index = 0; index < size; index++) {
      SymbolMatchHistory record = records.get(index);
      timestamps[index] = record.getId().getTradingTime().toEpochSecond(ZoneOffset.ofHours(7));
      open[index] = decimalValue(record.getOpenPrice());
      high[index] = decimalValue(record.getHighPrice());
      low[index] = decimalValue(record.getLowPrice());
      close[index] = decimalValue(record.getClosePrice());
      volume[index] = decimalValue(record.getVolume());
    }
    return OHLCVResponse.builder()
        .s(symbol)
        .t(timestamps)
        .o(open)
        .h(high)
        .l(low)
        .c(close)
        .v(volume)
        .build();
  }

  private java.time.LocalDate resolveTradingDate(MatchHistoryDay day) {
    List<java.time.LocalDate> tradingDates =
        symbolMatchHistoryRepository.findDistinctTradingDatesOrderByTradingDateDesc();
    return tradingDates.size() > day.getOffset() ? tradingDates.get(day.getOffset()) : null;
  }

  private OHLCVResponse emptyResponse(String symbol) {
    return OHLCVResponse.builder()
        .s(symbol)
        .t(new long[0])
        .o(new double[0])
        .h(new double[0])
        .l(new double[0])
        .c(new double[0])
        .v(new double[0])
        .build();
  }

  private double decimalValue(java.math.BigDecimal value) {
    return value == null ? 0D : value.doubleValue();
  }
}
