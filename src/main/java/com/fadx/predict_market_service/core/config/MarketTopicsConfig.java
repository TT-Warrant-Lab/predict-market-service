package com.fadx.predict_market_service.core.config;

import static com.fadx.predict_market_service.core.constant.KafkaTopicConstants.ANALYSIS_WARRANT;
import static com.fadx.predict_market_service.core.constant.KafkaTopicConstants.INDEX_IMPACT;
import static com.fadx.predict_market_service.core.constant.KafkaTopicConstants.MARKET_ANALYSIS;
import static com.fadx.predict_market_service.core.constant.KafkaTopicConstants.MARKET_TREND_FOREIGN;
import static com.fadx.predict_market_service.core.constant.KafkaTopicConstants.MARKET_TREND_TICKER;
import static com.fadx.predict_market_service.core.constant.KafkaTopicConstants.REALTIME_OUTBOUND_BIDOFFER;
import static com.fadx.predict_market_service.core.constant.KafkaTopicConstants.REALTIME_OUTBOUND_QUOTE;
import static com.fadx.predict_market_service.core.constant.KafkaTopicConstants.REALTIME_OUTBOUND_QUOTE_PRIORITY;
import static com.fadx.predict_market_service.core.constant.KafkaTopicConstants.REALTIME_OUTBOUND_TREND;
import static com.fadx.predict_market_service.core.constant.KafkaTopicConstants.REALTIME_OUTBOUND_WARRANT;
import static com.fadx.predict_market_service.core.constant.KafkaTopicConstants.SIGNALS;
import static com.fadx.predict_market_service.core.constant.KafkaTopicConstants.USER_NOTIFICATION;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class MarketTopicsConfig {
  private static final String[] TOPICS = {
    ANALYSIS_WARRANT,
    INDEX_IMPACT,
    MARKET_ANALYSIS,
    MARKET_TREND_FOREIGN,
    MARKET_TREND_TICKER,
    REALTIME_OUTBOUND_BIDOFFER,
    REALTIME_OUTBOUND_QUOTE,
    REALTIME_OUTBOUND_QUOTE_PRIORITY,
    REALTIME_OUTBOUND_TREND,
    REALTIME_OUTBOUND_WARRANT,
    SIGNALS,
    USER_NOTIFICATION
  };

  @Bean
  NewTopic[] marketTopics() {
    return java.util.Arrays.stream(TOPICS)
        .distinct()
        .map(topic -> TopicBuilder.name(topic).partitions(6).replicas(1).build())
        .toArray(NewTopic[]::new);
  }
}
