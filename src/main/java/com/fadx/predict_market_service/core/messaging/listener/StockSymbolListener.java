package com.fadx.predict_market_service.core.messaging.listener;

import com.fadx.predict_market_service.core.messaging.subscription.QuoteRoute;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public final class StockSymbolListener extends StaticJsonQuoteListener {
  public StockSymbolListener(ObjectMapper objectMapper) {
    super(objectMapper);
  }

  @Override
  public QuoteRoute route() {
    return QuoteRoute.COMMON;
  }

  @Override
  public Set<String> symbols() {
    return markets("HOSE", "HNX");
  }

  private Set<String> markets(String... markets) {
    Set<String> selectedMarkets = Set.copyOf(Arrays.asList(markets));
    try (InputStream in = getClass().getResourceAsStream("/static/symbol_static_data.json")) {
      List<Map<String, Object>> rows = objectMapper.readValue(in, new TypeReference<>() {});
      Set<String> symbols = new LinkedHashSet<>();
      rows.stream()
          .filter(row -> row.get("m") instanceof String market && selectedMarkets.contains(market))
          .filter(row -> "STOCK".equals(row.get("t")))
          .map(row -> normalize(String.valueOf(row.get("s"))))
          .filter(symbol -> !symbol.isEmpty())
          .forEach(symbols::add);
      return symbols;
    } catch (Exception exception) {
      throw new IllegalStateException(
          "Cannot load stock symbols for " + selectedMarkets, exception);
    }
  }
}
