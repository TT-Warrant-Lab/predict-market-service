package com.fadx.predict_market_service.modules.common.dto;

import java.time.LocalDate;
import java.util.List;

public record TickerChartResponseDto(
    String ticker,
    LocalDate date,
    String interval, // luôn là "1m"
    List<TickerChartPointDto> points) {
  public static TickerChartResponseDto of(
      String ticker, LocalDate date, List<TickerChartPointDto> points) {
    return new TickerChartResponseDto(ticker, date, "1m", points);
  }
}
