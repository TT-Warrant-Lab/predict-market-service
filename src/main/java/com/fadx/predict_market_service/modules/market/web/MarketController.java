package com.fadx.predict_market_service.modules.market.web;

import com.fadx.predict_market_service.modules.common.enums.EndpointType;
import com.fadx.predict_market_service.modules.common.services.ICallApiService;
import com.fadx.predict_market_service.modules.market.api.dtos.market.MatchedOrderPageRecord;
import com.fadx.predict_market_service.modules.market.api.dtos.market.QuoteSummaryItemRecord;
import com.fadx.predict_market_service.modules.market.api.dtos.market.SymbolCandleRecord;
import com.fadx.predict_market_service.modules.market.api.dtos.market.SymbolQuoteRecord;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.core.type.TypeReference;

@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
@Tag(name = "Anonymous | Market", description = "API for Market data")
public class MarketController {

  private final ICallApiService callApiService;

  /** GET /api/market/current-price Get current price snapshot for one or more symbols */
  @GetMapping("/current-price")
  public List<SymbolQuoteRecord> getCurrentPrice(@RequestParam String symbols) {

    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("symbols", symbols);

    return callApiService.callApiGet(
        EndpointType.MARKET,
        "/current-price",
        null,
        queryParams,
        new TypeReference<List<SymbolQuoteRecord>>() {});
  }

  /** GET /api/market/{symbol}/quote Get intraday matched order ticks (paginated) */
  @GetMapping("/{symbol}/quote")
  public MatchedOrderPageRecord getQuote(
      @PathVariable String symbol,
      @RequestParam(defaultValue = "100") Integer fetch_count,
      @RequestParam(required = false) Integer last_index,
      @RequestParam(required = false) Integer last_size) {

    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("fetch_count", fetch_count.toString());
    if (last_index != null) {
      queryParams.put("last_index", last_index.toString());
    }
    if (last_size != null) {
      queryParams.put("last_size", last_size.toString());
    }

    String path = "/" + symbol + "/quote";

    return callApiService.callApiGet(
        EndpointType.MARKET,
        path,
        null,
        queryParams,
        new TypeReference<MatchedOrderPageRecord>() {});
  }

  /**
   * GET /api/market/{symbol}/quote-summary Get volume profile (buy/sell distribution by price
   * level) No query params.
   */
  @GetMapping("/{symbol}/quote-summary")
  public List<QuoteSummaryItemRecord> getQuoteSummary(@PathVariable String symbol) {

    String path = "/" + symbol + "/quote-summary";

    return callApiService.callApiGet(
        EndpointType.MARKET,
        path,
        null,
        null,
        new TypeReference<List<QuoteSummaryItemRecord>>() {});
  }

  /** GET /api/market/{symbol}/history Get symbol price history (OHLCV) */
  @GetMapping("/{symbol}/history")
  public List<SymbolCandleRecord> getHistory(
      @PathVariable String symbol,
      @RequestParam String from_date,
      @RequestParam String to_date,
      @RequestParam(defaultValue = "100") Integer limit) {

    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("from_date", from_date);
    queryParams.put("to_date", to_date);
    queryParams.put("limit", limit.toString());

    String path = "/" + symbol + "/history";

    return callApiService.callApiGet(
        EndpointType.MARKET,
        path,
        null,
        queryParams,
        new TypeReference<List<SymbolCandleRecord>>() {});
  }
}
