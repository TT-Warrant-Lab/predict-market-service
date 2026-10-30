package com.fadx.predict_market_service.core.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.mas-api")
public class MasApiProperties {
  private String baseUrl;
  private String stockEndpoint;
  private String marketEndpoint;
  private String tradingEndpoint;
  private boolean insecureSkipTlsVerify;
}
