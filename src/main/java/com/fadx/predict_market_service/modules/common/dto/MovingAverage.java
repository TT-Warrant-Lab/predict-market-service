package com.fadx.predict_market_service.modules.common.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class MovingAverage {
  private BigDecimal ma20;
  private BigDecimal ma50;
  private BigDecimal ma200;
}
