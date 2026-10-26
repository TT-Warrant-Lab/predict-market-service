package com.fadx.predict_market_service.modules.common.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "t_cw_analysis")
@Getter
@Setter
public class CWAnalysis {

  @Id
  @Column(name = "symbol", nullable = false, length = 50)
  private String symbol; // Mã chứng quyền (VD: CHPG2506)

  @Column(name = "base_symbol", length = 50)
  private String baseSymbol;

  @Column(name = "analysis_date", nullable = false)
  private LocalDate analysisDate; // Ngày phân tích

  // Input parameters
  @Column(name = "spot_price", precision = 15, scale = 3)
  private BigDecimal spotPrice; // Giá cổ phiếu cơ sở hiện tại

  @Column(name = "strike_price", precision = 15, scale = 3)
  private BigDecimal strikePrice; // Giá thực hiện

  @Column(name = "risk_free_rate", precision = 15, scale = 3)
  private BigDecimal riskFreeRate; // Lãi suất phi rủi ro

  @Column(name = "volatility", precision = 15, scale = 3)
  private BigDecimal volatility; // Độ biến động (sigma)

  @Column(name = "conversion_ratio", precision = 15, scale = 3)
  private BigDecimal conversionRatio; // Tỷ lệ chuyển đổi

  @Column(name = "reference_price", precision = 15, scale = 3)
  private BigDecimal referencePrice;

  // Output: Black-Scholes price
  @Column(name = "warrant_price", precision = 15, scale = 3)
  private BigDecimal warrantPrice; // Giá chứng quyền tính toán

  @Column(name = "status", length = 10)
  private String status; // ITM, ATM, OTM

  @Column(name = "breakeven_increase_pct", precision = 15, scale = 3)
  private BigDecimal breakevenIncreasePct; // % tăng để hòa vốn

  @Column(name = "breakeven_price", precision = 15, scale = 3)
  private BigDecimal breakevenPrice; // Giá hòa vốn

  @Column(name = "time_to_maturity", precision = 15, scale = 3)
  private BigDecimal timeToMaturity;

  @Column(name = "days_to_maturity")
  private LocalDate daysToMaturity;
}
