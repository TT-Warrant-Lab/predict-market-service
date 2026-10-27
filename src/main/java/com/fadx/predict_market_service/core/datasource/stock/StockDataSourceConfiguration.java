package com.fadx.predict_market_service.core.datasource.stock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class StockDataSourceConfiguration {
  @Bean
  StockDataSource stockDataSource(MasStockDataSource mas) {
    return mas;
  }
}
