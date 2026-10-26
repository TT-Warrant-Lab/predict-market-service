package com.fadx.predict_market_service.modules.market.web;

import com.fadx.predict_market_service.modules.market.api.dtos.trading.DividendRecord;
import com.fadx.predict_market_service.modules.common.enums.MatchHistoryDay;
import com.fadx.predict_market_service.modules.market.api.dtos.trading.OHLCVResponse;
import com.fadx.predict_market_service.modules.common.services.ICallApiService;
import com.fadx.predict_market_service.modules.common.services.SymbolMatchHistoryService;
import com.fadx.predict_market_service.modules.common.services.TradingService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trading")
@RequiredArgsConstructor
@Tag(name = "Anonymous | Trading", description = "API for Trading data")
public class TradingController {

  private final ICallApiService callApiService;
  private final TradingService tradingService;
  private final SymbolMatchHistoryService symbolMatchHistoryService;

  @GetMapping("/match-history")
  public List<OHLCVResponse> getMatchHistoryBySymbols(
      @RequestParam String symbols, @RequestParam(defaultValue = "T") MatchHistoryDay day) {
    List<String> requestedSymbols =
        java.util.Arrays.stream(symbols.split(","))
            .map(String::trim)
            .filter(symbol -> !symbol.isEmpty())
            .toList();
    return symbolMatchHistoryService.getHistory(requestedSymbols, day);
  }

  @GetMapping("/{symbol}/match-history")
  public OHLCVResponse getMatchHistoryBySymbol(
      @PathVariable String symbol, @RequestParam(defaultValue = "T") MatchHistoryDay day) {
    return symbolMatchHistoryService.getHistory(symbol, day);
  }

  /** GET /api/trading/{symbol}/historical Get historical OHLCV price data */
  @GetMapping("/{symbol}/historical")
  public OHLCVResponse getHistorical(
      @PathVariable String symbol,
      @RequestParam String from_date,
      @RequestParam String to_date,
      @RequestParam(required = false) String interval) {
    return tradingService.getHistorical(symbol, from_date, to_date, interval);
  }

  /** GET /api/trading/{symbol}/dividend Get dividend history */
  @GetMapping("/{symbol}/dividend")
  public List<DividendRecord> getDividend(
      @PathVariable String symbol, @RequestParam String from_date, @RequestParam String to_date) {
    return tradingService.getDividend(symbol, from_date, to_date);
  }
}
