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
public final class HnxSymbolListener extends StaticJsonQuoteListener {
  public HnxSymbolListener(ObjectMapper objectMapper) {
    super(objectMapper);
  }

  @Override
  public QuoteRoute route() {
    return QuoteRoute.COMMON;
  }

  @Override
  public Set<String> symbols() {
    try (InputStream in = getClass().getResourceAsStream("/static/symbol_static_data.json")) {
      List<Map<String, Object>> rows = objectMapper.readValue(in, new TypeReference<>() {});
      Set<String> out = new LinkedHashSet<>();
      rows.stream()
          .filter(row -> "HNX".equals(row.get("m")))
          .map(row -> normalize(String.valueOf(row.get("s"))))
          .filter(s -> !s.isEmpty())
          .forEach(out::add);
      return out;
    } catch (Exception e) {
      throw new IllegalStateException("Cannot load HNX", e);
    }
  }
}
