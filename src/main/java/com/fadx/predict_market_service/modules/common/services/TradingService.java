package com.fadx.predict_market_service.modules.common.services;

import com.fadx.predict_market_service.core.datasource.trading.TradingDataSource;
import com.fadx.predict_market_service.modules.common.dto.OhlcvProjection;
import com.fadx.predict_market_service.modules.common.repositories.SymbolPriceHisRepository;
import com.fadx.predict_market_service.modules.common.utils.OhlcvHelper;
import com.fadx.predict_market_service.modules.market.api.dtos.trading.DividendRecord;
import com.fadx.predict_market_service.modules.market.api.dtos.trading.OHLCVResponse;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TradingService {

  private static final int HOLIDAY_BUFFER_DAYS = 7;

  private final TradingDataSource tradingDataSource;
  private final SymbolPriceHisRepository symbolPriceHisRepository;

  public OHLCVResponse getHistorical(
      String symbol, String fromDate, String toDate, String interval) {
    LocalDate from = LocalDate.parse(fromDate);
    LocalDate to = LocalDate.parse(toDate);
    if (to.isBefore(from)) {
      throw new IllegalArgumentException("toDate must not be before fromDate");
    }

    // raw_symbol_price_his contains end-of-day candles only. Intraday requests must come from MAS.
    if (!isDailyInterval(interval)) {
      return tradingDataSource.historical(symbol, fromDate, toDate, interval);
    }

    List<OhlcvProjection> rows =
        symbolPriceHisRepository.findOhlcvBySymbolAndDateRange(symbol, from, to.plusDays(1));

    int expectedTradingDays = countWeekdaysInclusive(from, to);
    int minimumRequiredRows = Math.max(1, expectedTradingDays - HOLIDAY_BUFFER_DAYS);
    if (rows.size() >= minimumRequiredRows) {
      return OhlcvHelper.toResponse(rows, symbol);
    }

    // TODO: notify to monitor system for sync data from mas

    return tradingDataSource.historical(symbol, fromDate, toDate, interval);
  }

  private static boolean isDailyInterval(String interval) {
    if (interval == null || interval.isBlank()) {
      return true;
    }
    return switch (interval.trim().toUpperCase(Locale.ROOT)) {
      case "1D", "D", "DAY", "DAILY" -> true;
      default -> false;
    };
  }

  static int countWeekdaysInclusive(LocalDate from, LocalDate to) {
    int weekdays = 0;
    for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
      DayOfWeek day = date.getDayOfWeek();
      if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) {
        weekdays++;
      }
    }
    return weekdays;
  }

  public List<DividendRecord> getDividend(String symbol, String fromDate, String toDate) {
    return tradingDataSource.dividends(symbol, fromDate, toDate);
  }
}
