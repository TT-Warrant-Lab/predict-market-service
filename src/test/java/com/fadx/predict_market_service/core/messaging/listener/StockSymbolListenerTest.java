package com.fadx.predict_market_service.core.messaging.listener;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class StockSymbolListenerTest {
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void listenerOnlyIncludesStocksFromConfiguredMarkets() {
    var symbols = new StockSymbolListener(objectMapper).symbols();

    assertThat(symbols)
        .contains("AAA", "ADC")
        .doesNotContain("VNFIN", "VNFINLEAD", "HNXINDEX", "41B5G9000");
  }
}
