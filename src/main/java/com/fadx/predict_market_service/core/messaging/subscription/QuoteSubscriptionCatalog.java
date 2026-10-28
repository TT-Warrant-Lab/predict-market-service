package com.fadx.predict_market_service.core.messaging.subscription;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public final class QuoteSubscriptionCatalog {
  private final QuoteSubscriptionRegistry registry;

  public QuoteSubscriptionCatalog(List<QuoteSubscriptionListener> listeners) {
    Set<String> common = new LinkedHashSet<>();
    Set<String> priority = new LinkedHashSet<>();
    Set<String> warrant = new LinkedHashSet<>();
    listeners.forEach(
        listener -> select(listener.route(), common, priority, warrant).addAll(listener.symbols()));
    registry = new QuoteSubscriptionRegistry(common, priority, warrant);
  }

  public QuoteSubscriptionRegistry registry() {
    return registry;
  }

  private static Set<String> select(
      QuoteRoute route, Set<String> common, Set<String> priority, Set<String> warrant) {
    return switch (route) {
      case COMMON -> common;
      case PRIORITY -> priority;
      case WARRANT -> warrant;
      case NONE -> throw new IllegalArgumentException("listener cannot register NONE route");
    };
  }
}
