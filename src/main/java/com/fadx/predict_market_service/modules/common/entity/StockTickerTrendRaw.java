package com.fadx.predict_market_service.modules.common.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "raw_stock_ticker_trend")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(StockTickerTrendRaw.StockTickerId.class) // Chỉ định lớp khóa chính phức hợp
public class StockTickerTrendRaw {

  @Id // Đánh dấu ID 1
  @Column(name = "trading_time", nullable = false)
  private LocalDateTime tradingTime;

  @Id // Đánh dấu ID 2
  @Column(nullable = false, length = 20)
  private String ticker;

  @Column(name = "trading_date", nullable = false)
  private LocalDate tradingDate;

  @Column(nullable = false)
  private Double price;

  @Column(name = "accumulated_volume", nullable = false)
  private Long accumulatedVolume;

  @Column(name = "match_side", length = 10)
  private String matchSide;

  @Column(name = "match_volume")
  private Long matchVolume;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class StockTickerId implements Serializable {
    private LocalDateTime tradingTime;
    private String ticker;
  }
}
