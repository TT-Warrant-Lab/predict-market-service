package com.fadx.predict_market_service.modules.market.api.dtos;

import com.fadx.predict_market_service.modules.market.api.enums.SortDirection;
import lombok.Data;

@Data
public class SortCriteria {
  private String field;
  private SortDirection direction;
}
