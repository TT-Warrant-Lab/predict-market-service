package com.fadx.predict_market_service.modules.common.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class WarrantSignal {
  private BigDecimal rsi;
  private String rsiLabel;
  private BigDecimal macd;
  private String macdLabel;
  private MovingAverage ma;
  private String maLabel;
  private BigDecimal vol;
  private BigDecimal volMa20;
  private String volLabel;
}
