package com.fadx.predict_market_service.modules.market.api.dtos;

import java.time.LocalDate;
import java.util.List;

public record ForeignChartResponseDto(
    String ticker,
    LocalDate date,
    String interval, // luôn là "1m"
    List<ForeignChartPointDto> points) {
  public static ForeignChartResponseDto of(
      String ticker, LocalDate date, List<ForeignChartPointDto> points) {
    return new ForeignChartResponseDto(ticker, date, "1m", points);
  }
}
