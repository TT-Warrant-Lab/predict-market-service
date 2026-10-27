package com.fadx.predict_market_service.core.datasource.trading;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class TradingDataSourceConfiguration {
  @Bean
  TradingDataSource tradingDataSource(MasTradingDataSource mas) {
    return mas;
  }
}
