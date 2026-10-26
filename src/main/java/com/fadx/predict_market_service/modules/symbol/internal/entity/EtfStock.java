package com.fadx.predict_market_service.modules.symbol.internal.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Entity
@Table(name = "m_etf_stock")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EtfStock {

  @EmbeddedId private EtfStockKey id;

  @Column(name = "units", precision = 15, scale = 3)
  private BigDecimal units;

  @Column(name = "ratio", precision = 15, scale = 3)
  private BigDecimal ratio;
}
