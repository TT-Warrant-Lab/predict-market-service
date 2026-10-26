package com.fadx.predict_market_service.modules.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Embeddable
public class SymbolPriceHisId implements Serializable {

  @Column(name = "symbol")
  private String symbol;

  @Column(name = "trading_date")
  private LocalDate tradingDate;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof SymbolPriceHisId that)) return false;
    return Objects.equals(symbol, that.symbol) && Objects.equals(tradingDate, that.tradingDate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(symbol, tradingDate);
  }
}
