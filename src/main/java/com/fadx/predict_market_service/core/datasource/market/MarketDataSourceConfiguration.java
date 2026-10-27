package com.fadx.predict_market_service.core.datasource.market;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class MarketDataSourceConfiguration {
  @Bean
  MarketDataSource marketDataSource(List<MasMarketDataSource> sources) {
    return new FallbackMarketDataSource(List.copyOf(sources));
  }
}
