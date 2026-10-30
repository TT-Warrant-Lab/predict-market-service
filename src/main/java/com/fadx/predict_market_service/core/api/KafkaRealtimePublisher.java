package com.fadx.predict_market_service.core.api;

import static com.fadx.predict_market_service.core.constant.KafkaTopicConstants.REALTIME_OUTBOUND_BIDOFFER;
import static com.fadx.predict_market_service.core.constant.KafkaTopicConstants.REALTIME_OUTBOUND_QUOTE;
import static com.fadx.predict_market_service.core.constant.MasWebSocketConstants.CHANNEL_BIDOFFER;
import static com.fadx.predict_market_service.core.constant.MasWebSocketConstants.CHANNEL_BIDOFFER_PREFIX;
import static com.fadx.predict_market_service.core.constant.MasWebSocketConstants.CHANNEL_QUOTE;
import static com.fadx.predict_market_service.core.constant.MasWebSocketConstants.CHANNEL_QUOTE_PREFIX;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/** Publishes realtime output without introducing browser authentication into this service. */
@Component
@RequiredArgsConstructor
public class KafkaRealtimePublisher implements RealtimePublisher {
  private final KafkaTemplate<String, Object> kafkaTemplate;

  @Override
  public void publish(String symbol, Object payload, String topic) {
    kafkaTemplate.send(resolveTopic(topic), symbol, payload);
  }

  static String resolveTopic(String topic) {
    if (CHANNEL_QUOTE.equals(topic) || topic.startsWith(CHANNEL_QUOTE_PREFIX)) {
      return REALTIME_OUTBOUND_QUOTE;
    }
    if (CHANNEL_BIDOFFER.equals(topic) || topic.startsWith(CHANNEL_BIDOFFER_PREFIX)) {
      return REALTIME_OUTBOUND_BIDOFFER;
    }
    return topic;
  }
}
