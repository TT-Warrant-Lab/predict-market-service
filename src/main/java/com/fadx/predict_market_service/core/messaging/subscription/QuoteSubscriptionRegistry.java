package com.fadx.predict_market_service.core.messaging.subscription;

import java.util.LinkedHashSet;
import java.util.Set;

/** Owns disjoint quote subscription sets and resolves one publishing route per symbol. */
public final class QuoteSubscriptionRegistry {
  private final Set<String> quoteSymbols;
  private final Set<String> prioritySymbols;
  private final Set<String> warrantSymbols;

  public QuoteSubscriptionRegistry(
      Set<String> quoteSymbols, Set<String> prioritySymbols, Set<String> warrantSymbols) {
    this.prioritySymbols = Set.copyOf(prioritySymbols);
    this.warrantSymbols = Set.copyOf(warrantSymbols);
    LinkedHashSet<String> common = new LinkedHashSet<>(quoteSymbols);
    common.removeAll(this.prioritySymbols);
    common.removeAll(this.warrantSymbols);
    this.quoteSymbols = Set.copyOf(common);
  }

  public Set<String> symbols() {
    LinkedHashSet<String> symbols = new LinkedHashSet<>(quoteSymbols);
    symbols.addAll(prioritySymbols);
    symbols.addAll(warrantSymbols);
    return symbols;
  }

  public QuoteRoute route(String symbol) {
    if (warrantSymbols.contains(symbol)) return QuoteRoute.WARRANT;
    if (prioritySymbols.contains(symbol)) return QuoteRoute.PRIORITY;
    return quoteSymbols.contains(symbol) ? QuoteRoute.COMMON : QuoteRoute.NONE;
  }
}
