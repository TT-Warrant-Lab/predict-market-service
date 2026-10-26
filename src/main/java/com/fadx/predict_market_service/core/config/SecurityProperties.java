package com.fadx.predict_market_service.core.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {
  /** Shared internal key required in the {@code X-Internal-Key} header. */
  private String internalKey;
}
