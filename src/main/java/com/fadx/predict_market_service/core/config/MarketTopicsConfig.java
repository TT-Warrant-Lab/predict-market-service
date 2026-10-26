package com.fadx.predict_market_service.core.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class MarketTopicsConfig {
  private static final String[] TOPICS = {
    "analysis.warrant",
    "market.bidoffer",
    "market.quote",
    "market.trend",
    "market.realtime.outbound",
    "signals",
    "user.notification"
  };

  @Bean
  NewTopic[] marketTopics() {
    return java.util.Arrays.stream(TOPICS)
        .map(topic -> TopicBuilder.name(topic).partitions(6).replicas(1).build())
        .toArray(NewTopic[]::new);
  }
}
