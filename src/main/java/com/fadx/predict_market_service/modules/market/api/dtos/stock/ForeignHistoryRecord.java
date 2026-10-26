package com.fadx.predict_market_service.modules.market.api.dtos.stock;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForeignHistoryRecord {

  @JsonProperty("td")
  private String tradingDate;

  @JsonProperty("bv")
  private Long buyVolume;

  @JsonProperty("bva")
  private Long buyValue;

  @JsonProperty("sv")
  private Long sellVolume;

  @JsonProperty("sva")
  private Long sellValue;

  @JsonProperty("tbv")
  private Long totalBuyVolume;

  @JsonProperty("tbva")
  private Long totalBuyValue;

  @JsonProperty("tsv")
  private Long totalSellVolume;

  @JsonProperty("tsva")
  private Long totalSellValue;

  @JsonProperty("oar")
  private Double ownershipRatio;
}
