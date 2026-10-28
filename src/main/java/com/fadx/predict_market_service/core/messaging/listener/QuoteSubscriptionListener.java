package com.fadx.predict_market_service.core.messaging.listener;

import com.fadx.predict_market_service.core.messaging.subscription.QuoteRoute;

import java.util.Set;

public interface QuoteSubscriptionListener {
  QuoteRoute route();

  Set<String> symbols();
}
