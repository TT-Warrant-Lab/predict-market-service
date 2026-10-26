package com.fadx.predict_market_service.modules.symbol.internal.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EtfStockKey implements Serializable {

  @Column(name = "symbol_id", length = 50)
  private String symbolId;

  @Column(name = "stock_symbol", length = 50)
  private String stockSymbol;

  @Column(name = "trade_date")
  private LocalDate tradeDate;
}
