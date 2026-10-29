package com.fadx.predict_market_service.core.api;

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
    kafkaTemplate.send(topic, symbol, payload);
  }
}
