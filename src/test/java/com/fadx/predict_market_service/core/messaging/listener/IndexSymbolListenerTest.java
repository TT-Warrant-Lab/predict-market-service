package com.fadx.predict_market_service.core.messaging.listener;

import static org.assertj.core.api.Assertions.assertThat;

import com.fadx.predict_market_service.core.messaging.subscription.QuoteRoute;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class IndexSymbolListenerTest {
  @Test
  void combinesConfiguredIndexConstituentsAsPrioritySymbols() {
    var listener = new IndexSymbolListener(new ObjectMapper());

    assertThat(listener.route()).isEqualTo(QuoteRoute.PRIORITY);
    assertThat(listener.symbols()).hasSize(100).contains("ACB");
  }
}
