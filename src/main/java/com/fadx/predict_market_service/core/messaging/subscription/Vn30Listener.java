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
public final class Vn30Listener extends StaticJsonQuoteListener {
  public Vn30Listener(ObjectMapper objectMapper) {
    super(objectMapper);
  }

  @Override
  public QuoteRoute route() {
    return QuoteRoute.PRIORITY;
  }

  @Override
  public Set<String> symbols() {
    return indexes("VN30");
  }

  private Set<String> indexes(String index) {
    try (InputStream in = getClass().getResourceAsStream("/static/indexStockList.json")) {
      Map<String, List<String>> values = objectMapper.readValue(in, new TypeReference<>() {});
      Set<String> out = new LinkedHashSet<>();
      values.getOrDefault(index, List.of()).forEach(v -> out.add(normalize(v)));
      return out;
    } catch (Exception e) {
      throw new IllegalStateException("Cannot load " + index, e);
    }
  }
}
