package com.fadx.predict_market_service.modules.common.services;

import com.fadx.predict_market_service.modules.common.dto.OhlcvProjection;
import com.fadx.predict_market_service.modules.common.enums.EndpointType;
import com.fadx.predict_market_service.modules.common.repositories.SymbolPriceHisRepository;
import com.fadx.predict_market_service.modules.common.utils.OhlcvHelper;
import com.fadx.predict_market_service.modules.market.api.dtos.trading.DividendRecord;
import com.fadx.predict_market_service.modules.market.api.dtos.trading.OHLCVResponse;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;

@Service
@RequiredArgsConstructor
public class TradingService {

  private final ICallApiService callApiService;
  private final SymbolPriceHisRepository symbolPriceHisRepository;

  public OHLCVResponse getHistorical(
      String symbol, String fromDate, String toDate, String interval) {
    List<OhlcvProjection> rows =
        symbolPriceHisRepository.findOhlcvBySymbolAndDateRange(
            symbol, LocalDate.parse(fromDate), LocalDate.parse(toDate).plusDays(1));
    if (!rows.isEmpty()) {
      return OhlcvHelper.toResponse(rows, symbol);
    }

    Map<String, String> query = new HashMap<>();
    query.put("from_date", fromDate);
    query.put("to_date", toDate);
    if (interval != null && !interval.isBlank()) {
      query.put("interval", interval);
    }
    return callApiService.callApiGet(
        EndpointType.TRADING,
        "/" + symbol + "/historical",
        null,
        query,
        new TypeReference<OHLCVResponse>() {});
  }

  public List<DividendRecord> getDividend(String symbol, String fromDate, String toDate) {
    return callApiService.callApiGet(
        EndpointType.TRADING,
        "/" + symbol + "/dividend",
        null,
        Map.of("from_date", fromDate, "to_date", toDate),
        new TypeReference<List<DividendRecord>>() {});
  }
}
