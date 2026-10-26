package com.fadx.predict_market_service.modules.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "raw_symbol_match_history",
    indexes = {
      @Index(name = "idx_symbol_match_history_date", columnList = "symbol, trading_date"),
      @Index(name = "idx_symbol_match_history_trading_date", columnList = "trading_date")
    })
@Getter
@Setter
@NoArgsConstructor
public class SymbolMatchHistory {

  @EmbeddedId private SymbolMatchHistoryId id;

  @Column(name = "trading_date", nullable = false)
  private LocalDate tradingDate;

  @Column(name = "open_price", precision = 20, scale = 6)
  private BigDecimal openPrice;

  @Column(name = "high_price", precision = 20, scale = 6)
  private BigDecimal highPrice;

  @Column(name = "low_price", precision = 20, scale = 6)
  private BigDecimal lowPrice;

  @Column(name = "close_price", precision = 20, scale = 6)
  private BigDecimal closePrice;

  @Column(precision = 24, scale = 6)
  private BigDecimal volume;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @jakarta.persistence.PrePersist
  void onCreate() {
    createdAt = LocalDateTime.now();
  }
}
