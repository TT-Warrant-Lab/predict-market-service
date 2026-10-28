package com.fadx.predict_market_service.core.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Runtime settings for the direct MAS SocketCluster feed. */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.mas-realtime")
public class MasRealtimeProperties {
  private boolean enabled = true;
  private String websocketUrl = "wss://masboard.masvn.com/ws/";
  private String origin = "https://masboard.masvn.com";
  private String cookie = "";
  private String authToken = "";
  private int subscriptionBatchSize = 20;
  private int maxSymbols = 0;
  private long reconnectInitialDelayMs = 1_000;
  private long reconnectMaxDelayMs = 30_000;
  private boolean traceEnabled = true;
}
