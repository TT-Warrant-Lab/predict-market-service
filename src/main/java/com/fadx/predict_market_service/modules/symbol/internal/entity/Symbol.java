package com.fadx.predict_market_service.modules.symbol.internal.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "m_symbol")
@Getter
@Setter
public class Symbol {

  @Id
  @Column(name = "symbol", nullable = false, length = 50)
  private String symbol;

  @Column(name = "market", length = 20)
  private String market;

  @Column(name = "name1", length = 255)
  private String name1;

  @Column(name = "name2", length = 255)
  private String name2;

  @Column(name = "type", length = 50)
  private String type;

  @Column(name = "product_id", length = 50)
  private String productId;

  @Column(name = "reference_price", precision = 15, scale = 3)
  private BigDecimal referencePrice;

  @Column(name = "ceiling_price", precision = 15, scale = 3)
  private BigDecimal ceilingPrice;

  @Column(name = "floor_price", precision = 15, scale = 3)
  private BigDecimal floorPrice;

  @Column(name = "previous_volume", precision = 15, scale = 3)
  private Integer previousVolume;

  // CW
  @Column(name = "base_symbol", length = 50)
  private String baseSymbol;

  @Column(name = "exercise_price", precision = 15, scale = 3)
  private BigDecimal exercisePrice;

  @Column(name = "exercise_ratio", length = 50)
  private String exerciseRatio;

  @Column(name = "last_trading_date", length = 20)
  private String lastTradingDate;

  @Column(name = "issuer", length = 100)
  private String issuer;

  // STOCK
  @Column(name = "is_eligible")
  private Boolean isEligible;

  // ETF Stock
  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(
      name = "symbol_id",
      referencedColumnName = "symbol",
      insertable = false,
      updatable = false)
  private List<EtfStock> etfStockList;
}
