package com.fadx.predict_market_service.modules.market.api.dtos.trading;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OHLCVResponse {

  @JsonProperty("s")
  private String s;

  @JsonProperty("t")
  private long[] t; // Timestamp (epoch seconds)

  @JsonProperty("o")
  private double[] o; // Open

  @JsonProperty("h")
  private double[] h; // High

  @JsonProperty("l")
  private double[] l; // Low

  @JsonProperty("c")
  private double[] c; // Close

  @JsonProperty("v")
  private double[] v; // Volume

  @JsonIgnore private Long nt;
}
