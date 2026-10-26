package com.fadx.predict_market_service.modules.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PriceVolume {
  @JsonInclude(value = JsonInclude.Include.CUSTOM, valueFilter = BigDecimalZeroFilter.class)
  private BigDecimal p;

  @JsonInclude(value = JsonInclude.Include.CUSTOM, valueFilter = BigDecimalZeroFilter.class)
  private BigDecimal v;
}
