package com.fadx.predict_market_service.modules.market.api.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Current market-session state. */
public record MarketStatusResponse(
    MarketStatus marketStatus,
    String marketStatusVi,
    @JsonProperty("isMarketActive") boolean marketActive) {

  public static MarketStatusResponse from(MarketStatus marketStatus) {
    return new MarketStatusResponse(
        marketStatus, marketStatus.getVietnameseLabel(), marketStatus.isMarketActive());
  }

  public enum MarketStatus {
    PRE_OPEN("Chưa mở cửa", false),
    OPENING_AUCTION("Khớp lệnh định kỳ mở cửa", true),
    CONTINUOUS("Khớp lệnh liên tục", true),
    LUNCH_BREAK("Nghỉ trưa", false),
    CLOSING_AUCTION("Khớp lệnh định kỳ đóng cửa", true),
    AFTER_CLOSE("Đã đóng cửa", false),
    WEEKEND("Nghỉ cuối tuần", false);

    private final String vietnameseLabel;
    private final boolean marketActive;

    MarketStatus(String vietnameseLabel, boolean marketActive) {
      this.vietnameseLabel = vietnameseLabel;
      this.marketActive = marketActive;
    }

    public String getVietnameseLabel() {
      return vietnameseLabel;
    }

    public boolean isMarketActive() {
      return marketActive;
    }
  }
}
