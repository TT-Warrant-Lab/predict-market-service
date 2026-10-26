package com.fadx.predict_market_service.modules.market.api.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Data
public class AnalysisResponse {
  private String symbol;
  private BigDecimal breakevenIncreasePct;
  private BigDecimal breakevenPrice;
  private BigDecimal referencePrice;
  private BigDecimal spotPrice;
  private BigDecimal daysToMaturity;
  private BigDecimal warrantPrice;
  private String status;
  private LocalDate analysisDate;
}
