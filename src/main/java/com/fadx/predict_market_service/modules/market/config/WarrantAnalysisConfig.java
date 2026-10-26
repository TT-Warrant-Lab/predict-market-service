package com.fadx.predict_market_service.modules.market.config;

import java.math.BigDecimal;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "config.warrant.analysis")
public class WarrantAnalysisConfig {
  private BigDecimal defaultMinTimeToMaturity;
  private BigDecimal defaultMaxBreakevenIncreasePct;
  private String defaultSortBy;
  private String defaultSortDirection;
}
