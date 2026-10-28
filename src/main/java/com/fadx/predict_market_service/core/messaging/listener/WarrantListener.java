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
public final class WarrantListener extends StaticJsonQuoteListener {
  public WarrantListener(ObjectMapper objectMapper) {
    super(objectMapper);
  }

  @Override
  public QuoteRoute route() {
    return QuoteRoute.WARRANT;
  }

  @Override
  public Set<String> symbols() {
    try (InputStream in = getClass().getResourceAsStream("/static/warrant_static_data.json")) {
      Map<String, Object> root = objectMapper.readValue(in, new TypeReference<>() {});
      Set<String> out = new LinkedHashSet<>();
      for (Object item : (List<?>) root.getOrDefault("records", List.of()))
        if (item instanceof Map<?, ?> row && row.get("s") != null)
          out.add(normalize(String.valueOf(row.get("s"))));
      return out;
    } catch (Exception e) {
      throw new IllegalStateException("Cannot load warrants", e);
    }
  }
}
