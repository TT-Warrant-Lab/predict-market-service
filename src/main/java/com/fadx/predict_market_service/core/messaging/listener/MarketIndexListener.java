package com.fadx.predict_market_service.core.messaging.listener;

import java.util.Set;

import com.fadx.predict_market_service.core.messaging.subscription.QuoteRoute;
import org.springframework.stereotype.Component;

@Component
public final class MarketIndexListener implements QuoteSubscriptionListener {
  @Override
  public QuoteRoute route() {
    return QuoteRoute.COMMON;
  }

  @Override
  public Set<String> symbols() {
    return Set.of("VN-INDEX", "VN30", "HNXINDEX", "HNXUPCOMINDEX");
  }
}
