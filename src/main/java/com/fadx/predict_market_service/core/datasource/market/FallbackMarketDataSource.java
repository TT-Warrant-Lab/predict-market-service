package com.fadx.predict_market_service.core.datasource.market;

import com.fadx.predict_market_service.modules.market.api.dtos.market.MatchedOrderPageRecord;
import com.fadx.predict_market_service.modules.market.api.dtos.market.QuoteSummaryItemRecord;
import com.fadx.predict_market_service.modules.market.api.dtos.market.SymbolCandleRecord;
import com.fadx.predict_market_service.modules.market.api.dtos.market.SymbolQuoteRecord;
import java.util.List;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;

/** Typed fallback decorator; add another {@link MarketDataSource} without changing clients. */
@Slf4j
public final class FallbackMarketDataSource implements MarketDataSource {
  private final List<MarketDataSource> sources;

  public FallbackMarketDataSource(List<MarketDataSource> sources) {
    if (sources.isEmpty())
      throw new IllegalArgumentException("at least one market source is required");
    this.sources = List.copyOf(sources);
  }

  @Override
  public List<SymbolQuoteRecord> currentPrices(List<String> symbols) {
    return attempt(s -> s.currentPrices(symbols));
  }

  @Override
  public MatchedOrderPageRecord quote(String symbol, int count, Integer index, Integer size) {
    return attempt(s -> s.quote(symbol, count, index, size));
  }

  @Override
  public List<QuoteSummaryItemRecord> quoteSummary(String symbol) {
    return attempt(s -> s.quoteSummary(symbol));
  }

  @Override
  public List<SymbolCandleRecord> history(String symbol, String from, String to, int limit) {
    return attempt(s -> s.history(symbol, from, to, limit));
  }

  private <T> T attempt(Function<MarketDataSource, T> operation) {
    RuntimeException last = null;
    for (MarketDataSource source : sources) {
      try {
        return operation.apply(source);
      } catch (RuntimeException error) {
        last = error;
        log.warn(
            "Market source {} failed; trying fallback", source.getClass().getSimpleName(), error);
      }
    }
    throw new IllegalStateException("all market data sources failed", last);
  }
}
