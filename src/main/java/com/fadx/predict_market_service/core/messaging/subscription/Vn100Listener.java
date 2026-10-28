package com.fadx.predict_market_service.core.messaging.subscription;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public final class Vn100Listener extends StaticJsonQuoteListener {
  public Vn100Listener(ObjectMapper objectMapper) {
    super(objectMapper);
  }

  @Override
  public QuoteRoute route() {
    return QuoteRoute.PRIORITY;
  }

  @Override
  public Set<String> symbols() {
    try (InputStream in = getClass().getResourceAsStream("/static/indexStockList.json")) {
      Map<String, List<String>> data = objectMapper.readValue(in, new TypeReference<>() {});
      Set<String> out = new LinkedHashSet<>();
      data.getOrDefault("VN100", List.of()).forEach(v -> out.add(normalize(v)));
      return out;
    } catch (Exception e) {
      throw new IllegalStateException("Cannot load VN100", e);
    }
  }
}
