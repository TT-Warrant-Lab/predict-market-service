package com.fadx.predict_market_service.modules.common.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
    name = "raw_stock_foreign_trend",
    indexes = {
      @Index(name = "idx_foreign_trend_raw_query", columnList = "ticker, trading_time DESC")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockForeignTrendRaw {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 20)
  private String ticker;

  @Column(name = "trading_date", nullable = false)
  private LocalDate tradingDate;

  @Column(name = "trading_time", nullable = false)
  private LocalDateTime tradingTime;

  @Column(name = "foreign_buy_vol", nullable = false)
  private Long foreignBuyVol;

  @Column(name = "foreign_sell_vol", nullable = false)
  private Long foreignSellVol;

  @Column(name = "foreign_net_vol", nullable = false)
  private Long foreignNetVol;

  @Column(name = "current_price")
  private Double currentPrice;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "ohlc", columnDefinition = "jsonb")
  private String ohlc;
}
