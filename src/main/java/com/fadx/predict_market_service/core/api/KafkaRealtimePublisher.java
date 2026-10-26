package com.fadx.predict_market_service.core.api;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/** Publishes realtime output without introducing browser authentication into this service. */
@Component
@RequiredArgsConstructor
public class KafkaRealtimePublisher implements RealtimePublisher {
  private static final String OUTBOUND_TOPIC = "market.realtime.outbound";
  private final KafkaTemplate<String, Object> kafkaTemplate;

  @Override
  public void publish(String symbol, Object payload, String topic) {
    kafkaTemplate.send(
        OUTBOUND_TOPIC, symbol, Map.of("symbol", symbol, "topic", topic, "payload", payload));
  }
}
