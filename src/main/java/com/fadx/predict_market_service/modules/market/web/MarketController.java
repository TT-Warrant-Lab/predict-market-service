package com.fadx.predict_market_service.modules.market.web;

import com.fadx.predict_market_service.core.datasource.market.MarketDataSource;
import com.fadx.predict_market_service.modules.market.api.dtos.market.MatchedOrderPageRecord;
import com.fadx.predict_market_service.modules.market.api.dtos.market.QuoteSummaryItemRecord;
import com.fadx.predict_market_service.modules.market.api.dtos.market.SymbolCandleRecord;
import com.fadx.predict_market_service.modules.market.api.dtos.market.SymbolQuoteRecord;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
@Tag(name = "Anonymous | Market", description = "API for Market data")
public class MarketController {

  private final MarketDataSource marketDataSource;

  /** GET /api/market/current-price Get current price snapshot for one or more symbols */
  @GetMapping("/current-price")
  public List<SymbolQuoteRecord> getCurrentPrice(@RequestParam String symbols) {

    return marketDataSource.currentPrices(
        Arrays.stream(symbols.split(","))
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .toList());
  }

  /** GET /api/market/{symbol}/quote Get intraday matched order ticks (paginated) */
  @GetMapping("/{symbol}/quote")
  public MatchedOrderPageRecord getQuote(
      @PathVariable String symbol,
      @RequestParam(defaultValue = "100") Integer fetch_count,
      @RequestParam(required = false) Integer last_index,
      @RequestParam(required = false) Integer last_size) {

    return marketDataSource.quote(symbol, fetch_count, last_index, last_size);
  }

  /**
   * GET /api/market/{symbol}/quote-summary Get volume profile (buy/sell distribution by price
   * level) No query params.
   */
  @GetMapping("/{symbol}/quote-summary")
  public List<QuoteSummaryItemRecord> getQuoteSummary(@PathVariable String symbol) {

    return marketDataSource.quoteSummary(symbol);
  }

  /** GET /api/market/{symbol}/history Get symbol price history (OHLCV) */
  @GetMapping("/{symbol}/history")
  public List<SymbolCandleRecord> getHistory(
      @PathVariable String symbol,
      @RequestParam String from_date,
      @RequestParam String to_date,
      @RequestParam(defaultValue = "100") Integer limit) {

    return marketDataSource.history(symbol, from_date, to_date, limit);
  }
}
