package com.fadx.predict_market_service.core.messaging.listener;

import com.fadx.predict_market_service.core.messaging.subscription.QuoteRoute;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public final class IndexSymbolListener extends StaticJsonQuoteListener {
  public IndexSymbolListener(ObjectMapper objectMapper) {
    super(objectMapper);
  }

  @Override
  public QuoteRoute route() {
    return QuoteRoute.PRIORITY;
  }

  @Override
  public Set<String> symbols() {
    return indexes("VN30", "VN100");
  }

  private Set<String> indexes(String... indexes) {
    try (InputStream in = getClass().getResourceAsStream("/static/indexStockList.json")) {
      Map<String, List<String>> values = objectMapper.readValue(in, new TypeReference<>() {});
      Set<String> symbols = new LinkedHashSet<>();
      for (String index : indexes) {
        values.getOrDefault(index, List.of()).stream()
            .map(StaticJsonQuoteListener::normalize)
            .filter(symbol -> !symbol.isEmpty())
            .forEach(symbols::add);
      }
      return symbols;
    } catch (Exception exception) {
      throw new IllegalStateException("Cannot load indexes " + List.of(indexes), exception);
    }
  }
}
