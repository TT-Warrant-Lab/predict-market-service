package com.fadx.predict_market_service.modules.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class SymbolMatchHistoryId implements Serializable {

  @Column(name = "symbol", nullable = false, length = 50)
  private String symbol;

  @Column(name = "trading_time", nullable = false)
  private LocalDateTime tradingTime;

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (!(object instanceof SymbolMatchHistoryId other)) {
      return false;
    }
    return Objects.equals(symbol, other.symbol) && Objects.equals(tradingTime, other.tradingTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(symbol, tradingTime);
  }
}
