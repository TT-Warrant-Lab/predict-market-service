package com.fadx.predict_market_service.modules.market.internal;

import static com.fadx.predict_market_service.core.constant.KafkaTopicConstants.REALTIME_OUTBOUND_QUOTE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.fadx.predict_market_service.core.api.RealtimePublisher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class WebsocketBroadcastServiceTest {
  private final RealtimePublisher realtimePublisher = mock(RealtimePublisher.class);
  private final WebsocketBroadcastService service =
      new WebsocketBroadcastService(
          mock(RedisService.class), new ObjectMapper(), realtimePublisher);

  @Test
  void publishesMasQuoteChannelToFixedKafkaTopic() {
    service.onMarketUpdate("{\"p\":[\"market.quote.SSI\",{\"s\":\"SSI\",\"c\":30.5}]}");

    verify(realtimePublisher).publish(eq("SSI"), any(JsonNode.class), eq(REALTIME_OUTBOUND_QUOTE));
  }
}
