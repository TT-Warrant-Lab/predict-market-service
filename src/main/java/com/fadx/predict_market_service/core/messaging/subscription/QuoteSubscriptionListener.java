package com.fadx.predict_market_service.core.messaging.subscription;

import java.util.Set;

public interface QuoteSubscriptionListener {
  QuoteRoute route();

  Set<String> symbols();
}
