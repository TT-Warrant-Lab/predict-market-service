package com.fadx.predict_market_service.modules.common.dto;

import java.math.BigDecimal;

public class BigDecimalZeroFilter {
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof BigDecimal) {
      return ((BigDecimal) obj).compareTo(BigDecimal.ZERO) == 0;
    }
    return obj == null;
  }
}
