package com.fadx.predict_market_service.modules.common.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface OhlcvProjection {
  LocalDate getTradingDate();

  BigDecimal getOpenPrice();

  BigDecimal getHighPrice();

  BigDecimal getLowPrice();

  BigDecimal getClosePrice();

  Long getVolume();

  BigDecimal getReferencePrice();
}
