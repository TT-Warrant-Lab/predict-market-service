package com.fadx.predict_market_service.modules.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "raw_symbol_price_his")
@Getter
@Setter
public class SymbolPriceHis {

  @EmbeddedId private SymbolPriceHisId id;

  @Column(name = "open_price", precision = 15, scale = 3)
  private BigDecimal openPrice;

  @Column(name = "close_price", precision = 15, scale = 3)
  private BigDecimal closePrice;

  @Column(name = "high_price", precision = 15, scale = 3)
  private BigDecimal highPrice;

  @Column(name = "low_price", precision = 15, scale = 3)
  private BigDecimal lowPrice;

  @Column(name = "volume")
  private Long volume;

  @Column(name = "reference_price", precision = 15, scale = 3)
  private BigDecimal referencePrice;
}
