package com.fadx.predict_market_service.core.messaging.subscription;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Locale;

abstract class StaticJsonQuoteListener implements QuoteSubscriptionListener {
  protected final ObjectMapper objectMapper;

  StaticJsonQuoteListener(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  protected static String normalize(String symbol) {
    return symbol == null ? "" : symbol.trim().toUpperCase(Locale.ROOT);
  }
}
