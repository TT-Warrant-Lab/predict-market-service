package com.fadx.predict_market_service.modules.market.api.dtos;

import com.fadx.predict_market_service.modules.market.api.enums.Operator;
import lombok.Data;

@Data
public class FilterCriteria {
  private String field; // tên field trong entity
  private Operator operator; // EQ, GE, BETWEEN ...
  private Object value; // value hoặc List value
  private Object valueTo; // dùng cho BETWEEN
}
