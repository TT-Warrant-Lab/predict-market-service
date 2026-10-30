package com.fadx.predict_market_service.core.api;

import static com.fadx.predict_market_service.core.constant.KafkaTopicConstants.REALTIME_OUTBOUND_BIDOFFER;
import static com.fadx.predict_market_service.core.constant.KafkaTopicConstants.REALTIME_OUTBOUND_QUOTE;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class KafkaRealtimePublisherTest {
  @Test
  void resolvesSymbolSpecificMasQuoteChannelToFixedKafkaTopic() {
    assertThat(KafkaRealtimePublisher.resolveTopic("market.quote.SSI"))
        .isEqualTo(REALTIME_OUTBOUND_QUOTE);
  }

  @Test
  void resolvesSymbolSpecificMasBidofferChannelToFixedKafkaTopic() {
    assertThat(KafkaRealtimePublisher.resolveTopic("market.bidoffer.BSR"))
        .isEqualTo(REALTIME_OUTBOUND_BIDOFFER);
  }

  @Test
  void preservesUnrelatedTopic() {
    assertThat(KafkaRealtimePublisher.resolveTopic("top-movers.VN30")).isEqualTo("top-movers.VN30");
  }
}
